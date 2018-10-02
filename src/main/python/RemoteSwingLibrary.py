#  Copyright 2014 Nokia Solutions and Networks
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
from contextlib import contextmanager
import inspect
import os
import sys
import tempfile
import threading
import time
import traceback
import swinglibrary
import shutil
import datetime
import re

IS_PYTHON3 = sys.version_info[0] >= 3
if IS_PYTHON3:
    import socketserver as SocketServer
    from xmlrpc.client import ProtocolError
else:
    import SocketServer
    from xmlrpclib import ProtocolError
import uuid

from robot.errors import HandlerExecutionFailed, TimeoutError
from robot.libraries.Process import Process
from robot.libraries.Remote import Remote
from robot.libraries.BuiltIn import BuiltIn, run_keyword_variant
from robot.utils import timestr_to_secs, get_link_path
from robotbackgroundlogger import BackgroundLogger

logger = BackgroundLogger()

try:
    from robot.libraries.BuiltIn import RobotNotRunningError
except ImportError:  # Support RF < 2.8.5
    RobotNotRunningError = AttributeError


class AgentList(object):
    NEW = 'NEW'
    OLD = 'OLD'

    def __init__(self):
        self._remote_agents = []
        self.agent_received = threading.Event()
        self._lock = threading.RLock()

    def append(self, address, name):
        with self._lock:
            self.agent_received.set()
            self._remote_agents.append((address, name, self.NEW))

    def remove(self, address):
        with self._lock:
            for t in reversed(self._remote_agents):
                if t[0] == address:
                    self._remote_agents.remove(t)
                    break

    def get(self, accept_old):
        with self._lock:
            logger.log_background_messages()
            return [(address, name, age) for (address, name, age) in self._remote_agents
                    if accept_old or age == self.NEW]

    def set_received_to_old(self):
        with self._lock:
            self.agent_received.clear()
            for index, (address, name, age) in enumerate(self._remote_agents):
                self._remote_agents[index] = (address, name, self.OLD)


REMOTE_AGENTS_LIST = AgentList()


class SimpleServer(SocketServer.StreamRequestHandler):
    def handle(self):
        data = self.rfile.readline()[:-1]
        fields = data.decode().split(':')
        if fields[0] == 'PORT':
            port = fields[1]
            name = ':'.join(fields[2:])
            address = ':'.join([self.client_address[0], port])
            logger.debug('Registered java remoteswinglibrary agent "%s" at %s' % \
                         (name, address))
            REMOTE_AGENTS_LIST.append(address, name)
        elif fields[0] == 'DIALOG':
            title = fields[1]
            path = ':'.join(fields[2:])
            logger.info('Security Warning "%s" was accepted automatically' % title)
            logger.info('<a href="%s"><img src="%s" width="%s"></a>'
                        % (path, get_link_path(path, RemoteSwingLibrary.get_log_dir()), 800), html=True)
        else:
            logger.debug('Unknown message "%s"' % fields[0])


class RemoteSwingLibraryTimeoutError(RuntimeError):
    pass


def _tobool(value):
    return str(value).lower() in ("true", "1", "yes")


class RemoteSwingLibrary(object):
    """Robot Framework library leveraging Java-agents to run [https://github.com/robotframework/SwingLibrary|SwingLibrary]
    keywords on Java-processes.

    To take the library in to use add remoteswinglibrary-[version].jar to PYTHONPATH.

    The library contains a simple socket server to communicate with Java agents. When taking the library into use,
    you can specify the port this server uses. Providing the port is optional. If you do not provide one,
    RemoteSwingLibrary will ask the OS for an unused port.

    Keywords directly offered by this library on top of SwingLibrary keywords are:
    - [#Application Started|Application Started]
    - [#Ensure Application Should Close|Ensure Application Should Close]
    - [#Log Java System Properties|Log Java System Properties]
    - [#Reinitiate|Reinitiate]
    - [#Set Java Tool Options|Set Java Tool Options]
    - [#Start Application|Start Application]
    - [#Switch To Application|Switch To Application]
    - [#System Exit|System Exit]

    RemoteSwingLibrary also introduces two global variables that can be used during testing:
    - ${REMOTESWINGLIBRARYPATH} the location of the remoteswinglibrary jar file.
    - ${REMOTESWINGLIBRARYPORT} port used by the agents to communicate with the library - this is needed if a java agent
    is started for example from another machine.

    [https://github.com/ombre42/jrobotremoteserver|jrobotremoteserver]
    that is used by RemoteSwingLibrary also offers a keyword:
    - [#Stop Remote Server|Stop Remote Server]

    Following SwingLibrary Keywords are not available through RemoteSwingLibrary:
    - Launch Application
    - SwingLibrary version of Start Application
    - Start Application In Separate Thread

    NOTE! [#Get Table Cell Property|Get Table Cell Property] will return the string representation of that property
    and not the actual object. Complex objects are not passed through Remote library interface.

    Examples:
    | * Settings * |
    | Library | RemoteSwingLibrary |
    | * Test Cases * |
    | Testing java application |
    | | Start Application | myjavaapp | java -jar myjava.jar |
    | | Select Window  | My App |
    | | Ensure Application Should Close | 15 seconds | Push Button | Exit |


    """

    ROBOT_LIBRARY_SCOPE = 'GLOBAL'
    KEYWORDS = ['system_exit', 'start_application', 'application_started', 'switch_to_application',
                'ensure_application_should_close', 'log_java_system_properties', 'set_java_tool_options',
                'reinitiate']
    REMOTES = {}
    CURRENT = None
    PROCESS = Process()
    TIMEOUT = 60
    PORT = None
    DEBUG = None
    AGENT_PATH = os.path.abspath(os.path.dirname(__file__))
    _output_dir = ''

    def __init__(self, port=0, debug=False):
        """
        *port*: optional port for the server receiving connections from remote agents

        *debug*: optional flag that will start agent in mode with more logging for troubleshooting (set to TRUE to enable)

        NOTE! RemoteSwingLibrary is a so called Global Scope library. This means when it is imported once it will be
        available until end of robot run. Parameters used in imports from others suites will be ignored.
        If you need to change import options between suites, please use *Reinitiate* keyword.

        """

        self._initiate(port, debug)

        if os.path.exists(self._output("remote-stderr")):
            shutil.rmtree(self._output("remote-stderr"))
        if os.path.exists(self._output("remote-stdout")):
            shutil.rmtree(self._output("remote-stdout"))

    def _initiate(self, port=0, debug=False):
        if RemoteSwingLibrary.DEBUG is None:
            RemoteSwingLibrary.DEBUG = _tobool(debug)
        if RemoteSwingLibrary.PORT is None:
            RemoteSwingLibrary.PORT = self._start_port_server(int(port))
        try:
            BuiltIn().set_global_variable('\${REMOTESWINGLIBRARYPATH}',
                                          self._escape_path(RemoteSwingLibrary.AGENT_PATH))
            BuiltIn().set_global_variable('\${REMOTESWINGLIBRARYPORT}', RemoteSwingLibrary.PORT)
            self._output_dir = BuiltIn().get_variable_value('${OUTPUTDIR}')
        except RobotNotRunningError:
            pass

    def reinitiate(self, port=0, debug=False):
        """
        Restarts RemoteSwingLibrary with new import parameters.
        """
        RemoteSwingLibrary.CURRENT = None
        global REMOTE_AGENTS_LIST
        REMOTE_AGENTS_LIST = AgentList()
        RemoteSwingLibrary.PORT = None
        RemoteSwingLibrary.DEBUG = None
        RemoteSwingLibrary.TIMEOUT = 60

        self._initiate(port, debug)

    @property
    def current(self):
        if not self.CURRENT:
            return None
        return self.REMOTES[self.CURRENT][0]

    def _start_port_server(self, port):
        address = ('0.0.0.0', int(port))
        server = SocketServer.TCPServer(address, SimpleServer)
        server.allow_reuse_address = True
        # t = threading.Thread(name="RemoteSwingLibrary registration server thread",
        #                     target=server.serve_forever)
        t = threading.Thread(name="RemoteSwingLibrary registration server thread",
                             target=server.serve_forever, args=(0.01,))
        t.setDaemon(True)
        t.start()
        return server.server_address[1]

    def _create_env(self, close_security_dialogs=False, remote_port=0):
        agent_command = '-javaagent:"%s"=127.0.0.1:%s' % (RemoteSwingLibrary.AGENT_PATH, RemoteSwingLibrary.PORT)
        if int(remote_port):
            agent_command += ':APPORT=%s' % remote_port
        if RemoteSwingLibrary.DEBUG:
            agent_command += ':DEBUG'
        if _tobool(close_security_dialogs):
            agent_command += ':CLOSE_SECURITY_DIALOGS'
        self._agent_command = agent_command
        logger.info(agent_command)

    def _escape_path(self, text):
        return text.replace("\\", "\\\\")

    @contextmanager
    def _agent_java_tool_options(self, close_security_dialogs, remote_port):
        old_tool_options = os.environ.get('JAVA_TOOL_OPTIONS', '')
        old_options = os.environ.get('_JAVA_OPTIONS', '')
        logger.debug("Picked old JAVA_TOOL_OPTIONS='%s'" % old_tool_options)
        logger.debug("Picked old _JAVA_OPTIONS='%s'" % old_options)
        self.set_java_tool_options(close_security_dialogs, remote_port)
        try:
            yield
        finally:
            os.environ['JAVA_TOOL_OPTIONS'] = old_tool_options
            os.environ['_JAVA_OPTIONS'] = old_options
            logger.debug("Returned old JAVA_TOOL_OPTIONS='%s'" % old_tool_options)
            logger.debug("Returned old _JAVA_OPTIONS='%s'" % old_options)

    def set_java_tool_options(self, close_security_dialogs=True, remote_port=0):
        """Sets the JAVA_TOOL_OPTIONS to include RemoteSwingLibrary Agent and
        the _JAVA_OPTIONS to set a temporary policy granting all permissions.

        RemoteSwingLibrary Agent is normally enabled by `Start Application` by
        setting the JAVA_TOOL_OPTIONS environment variable only during
        that keyword call. So java processes started by other commands wont
        normally use the RemoteSwingLibrary Agent. This keyword sets that same
        environment variable to be used always. So all java processes started
        after this will use the Agent. This methods also creates temporary
        Java policy file which grants all permissions. This file is set as
        policy for each java command call.
        """
        close_security_dialogs = _tobool(close_security_dialogs)
        en_us_locale = "-Duser.language=en -Duser.country=US "
        self._create_env(close_security_dialogs, remote_port)
        os.environ['JAVA_TOOL_OPTIONS'] = en_us_locale + self._agent_command
        logger.debug("Set JAVA_TOOL_OPTIONS='%s%s'" % (en_us_locale, self._agent_command))
        with tempfile.NamedTemporaryFile(prefix='grant_all_', suffix='.policy', delete=True) as t:
            text = b"""
                grant {
                    permission java.security.AllPermission;
                };
                """
            t.write(text)
        java_policy = '-Djava.security.policy="%s"' % t.name
        os.environ['_JAVA_OPTIONS'] = java_policy
        logger.debug("Set _JAVA_OPTIONS='%s'" % java_policy)

    def start_application(self, alias, command, timeout=60, name_contains="", close_security_dialogs=False,
                          remote_port=0):
        """Starts the process in the `command` parameter  on the host operating system.
        The given alias is stored to identify the started application in RemoteSwingLibrary.

        *timeout* (default 60) is timeout in seconds.
        *name_contains* is a text that must be part of the name of the java process that we are connecting to.
        *name_contains* helps in situations where multiple java-processes are started.
        To see the name of the connecting java agents run tests with --loglevel DEBUG.
        *remote_port* forces RSL agent to run on specific port, this is useful if you want to
        connect to this application later from another robot run.

        """
        close_security_dialogs = _tobool(close_security_dialogs)
        stdout = "remote-stdout" + "/" + "remote-stdout-" + re.sub('[:. ]', '-', str(datetime.datetime.now())) + '.txt'
        stderr = "remote-stderr" + "/" + "remote-stderr-" + re.sub('[:. ]', '-', str(datetime.datetime.now())) + '.txt'

        stderr_dir = self._output("remote-stderr")
        stdout_dir = self._output("remote-stdout")

        if not os.path.exists(stderr_dir):
            os.makedirs(stderr_dir)
        if not os.path.exists(stdout_dir):
            os.makedirs(stdout_dir)

        logger.info('<a href="%s">Link to stdout</a>' % stdout, html=True)
        logger.info('<a href="%s">Link to stderr</a>' % stderr, html=True)
        REMOTE_AGENTS_LIST.set_received_to_old()
        with self._agent_java_tool_options(close_security_dialogs, remote_port):
            self.PROCESS.start_process(command, alias=alias, shell=True,
                                       stdout=self._output(stdout),
                                       stderr=self._output(stderr))
        try:
            self._application_started(alias, timeout, name_contains, remote_port, accept_old=False)
        except TimeoutError:
            raise
        except Exception:
            logger.info("Failed to start application: %s" % traceback.format_exc())
            # FIXME: this may hang, how is that possible?
            result = self.PROCESS.wait_for_process(timeout=0.01)
            if result:
                logger.info('STDOUT: %s' % result.stdout)
                logger.info('STDERR: %s' % result.stderr)
            else:
                logger.info("Process is running, but application startup failed")
            raise

    def _output(self, filename):
        return os.path.join(self._output_dir, filename)

    def application_started(self, alias, timeout=60, name_contains="", remote_port=0, remote_host="127.0.0.1"):
        """Detects new RemoteSwingLibrary Java-agents in applications that are started without
        using the Start Application -keyword. The given alias is stored
        to identify the started application in RemoteSwingLibrary.
        Subsequent keywords will be passed on to this application. Agents in application
        started in previous robot runs can't be detected automatically, so you have to use *remote_port* parameter.
        """
        self._application_started(alias, timeout, name_contains, remote_port, remote_host)

    def _wait_for_api(self, url):
        logger.info('waiting for api at %s' % url)
        attempts = int(RemoteSwingLibrary.TIMEOUT)
        for i in range(attempts):
            try:
                result = self._run_from_services('ping')
                logger.info('api is ready')
                return result
            except Exception as err:
                error = err
            time.sleep(1)
        raise RuntimeError('Connecting to api at %s has failed: %s' % (url, error))

    def _application_started(self, alias, timeout=60, name_contains="",
                             remote_port=0, remote_host="127.0.0.1", accept_old=True):
        RemoteSwingLibrary.TIMEOUT = timestr_to_secs(timeout)
        if remote_port:
            url = '%s:%s' % (remote_host, remote_port)
            REMOTE_AGENTS_LIST.remove(url)
        else:
            url = self._get_agent_address(name_contains, accept_old)
        logger.info('connecting to started application at %s' % url)
        self._initialize_remote_libraries(alias, url)
        RemoteSwingLibrary.CURRENT = alias
        self._wait_for_api(url)
        logger.info('connected to started application at %s' % url)

    def _initialize_remote_libraries(self, alias, url):
        swinglibrary = Remote(url)
        logger.debug('remote swinglibrary instantiated')
        services = Remote(url + '/services')
        logger.debug('remote services instantiated')
        self.REMOTES[alias] = [swinglibrary, services]

    def _get_agent_address(self, name_pattern, accept_old):
        while True:
            if not REMOTE_AGENTS_LIST.get(accept_old):
                REMOTE_AGENTS_LIST.agent_received.clear()
            REMOTE_AGENTS_LIST.agent_received.wait(timeout=RemoteSwingLibrary.TIMEOUT)
            if not REMOTE_AGENTS_LIST.agent_received.isSet():
                raise RemoteSwingLibraryTimeoutError('Agent port not received before timeout')
            for address, name, age in reversed(REMOTE_AGENTS_LIST.get(accept_old)):
                if name_pattern is None or name_pattern in name:
                    REMOTE_AGENTS_LIST.remove(address)
                    return address
            time.sleep(0.1)

    def _ping_until_timeout(self, timeout):
        timeout = float(timeout)
        delta = min(0.1, timeout)
        endtime = timeout + time.time()
        while endtime > time.time():
            self._run_from_services('ping')
            time.sleep(delta)

    def _run_from_services(self, kw, *args, **kwargs):
        return self.REMOTES[RemoteSwingLibrary.CURRENT][1].run_keyword(kw, args, kwargs)

    @run_keyword_variant(resolve=1)
    def ensure_application_should_close(self, timeout, kw, *args):
        """ Runs the given keyword and waits until timeout for the application to close .
        If the application doesn't close, the keyword will take a screenshot and close the application
        and after that it will fail.
        In many cases calling the keyword that will close the application under test brakes the remote connection.
        This exception is ignored as it is expected by this keyword.
        Other exceptions will fail this keyword as expected.
        """
        with self._run_and_ignore_connection_lost():
            BuiltIn().run_keyword(kw, *args)
        try:
            self._application_should_be_closed(timeout=timestr_to_secs(timeout))
        except RemoteSwingLibraryTimeoutError as t:
            logger.warn('Application is not closed before timeout - killing application')
            self._take_screenshot()
            self.system_exit()
            raise

    def _take_screenshot(self):
        logdir = self.get_log_dir()
        screenshotdir = logdir + "/" + "remote-screenshots"
        if not os.path.exists(screenshotdir):
            os.makedirs(screenshotdir)

        filepath = os.path.join(screenshotdir, 'remote-screenshot%s.png' % re.sub('[:. ]', '-', str(datetime.datetime.now())))
        self._run_from_services('takeScreenshot', filepath)
        logger.info('<img src="%s"></img>' % get_link_path(filepath, logdir), html=True)

    @staticmethod
    def get_log_dir():
        variables = BuiltIn().get_variables()
        logfile = variables['${LOG FILE}']
        if logfile != 'NONE':
            return os.path.dirname(logfile)
        return variables['${OUTPUTDIR}']

    def _application_should_be_closed(self, timeout):
        with self._run_and_ignore_connection_lost():
            self._ping_until_timeout(timeout)
            raise RemoteSwingLibraryTimeoutError('Application was not closed before timeout')

    @contextmanager
    def _run_and_ignore_connection_lost(self):
        try:
            yield
        except RuntimeError as r:  # disconnection from remotelibrary
            if 'Connection to remote server broken:' in r.args[0]:
                logger.info('Connection died as expected')
                return
            raise
        except HandlerExecutionFailed as e:  # disconnection from xmlrpc wrapped in robot keyword
            if any(elem in e.args[0] for elem in ('Connection to remote server broken:', 'ProtocolError')):
                logger.info('Connection died as expected')
                return
            raise
        except ProtocolError as r:  # disconnection from xmlrpc in jython on some platforms
            logger.info('Connection died as expected')
            return

    def system_exit(self, exit_code=1):
        """ Uses the RemoteSwingLibrary java agent to call system exit for the current java application.
        """
        with self._run_and_ignore_connection_lost():
            self._run_from_services('systemExit', exit_code)

    def switch_to_application(self, alias):
        """Switches between applications that are known to RemoteSwingLibrary.
        The application is identified using the alias.
        Subsequent keywords will be passed on to this application."""
        RemoteSwingLibrary.CURRENT = alias

    def log_java_system_properties(self):
        """ log and return java properties and environment information from the current java application.
        """
        env = self._run_from_services('getEnvironment')
        logger.info(env)
        return env

    def get_keyword_names(self):
        overrided_keywords = ['startApplication',
                              'launchApplication',
                              'startApplicationInSeparateThread']
        return RemoteSwingLibrary.KEYWORDS + [kw for kw in swinglibrary.keywords
                                              if kw not in overrided_keywords]

    def get_keyword_arguments(self, name):
        if name in RemoteSwingLibrary.KEYWORDS:
            return self._get_args(name)
        return swinglibrary.keyword_arguments[name]

    def _get_args(self, method_name):
        spec = inspect.getargspec(getattr(self, method_name))
        args = spec[0][1:]
        if spec[3]:
            for i, item in enumerate(reversed(spec[3])):
                args[-i - 1] = args[-i - 1] + '=' + str(item)
        if spec[1]:
            args += ['*' + spec[1]]
        if spec[2]:
            args += ['**' + spec[2]]
        return args

    def get_keyword_documentation(self, name):
        if name == '__intro__':
            return RemoteSwingLibrary.__doc__
        if name in RemoteSwingLibrary.KEYWORDS or name == '__init__':
            return getattr(self, name).__doc__
        return swinglibrary.keyword_documentation[name]

    def run_keyword(self, name, arguments, kwargs):
        if name in RemoteSwingLibrary.KEYWORDS:
            return getattr(self, name)(*arguments, **kwargs)
        if self.current:
            return self.current.run_keyword(name, arguments, kwargs)
        if name in swinglibrary.keywords:
            raise Exception("To use this keyword you need to connect to application first.")
