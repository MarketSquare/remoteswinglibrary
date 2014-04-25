from __future__ import with_statement
from contextlib import contextmanager
import os
import threading
import time
import SocketServer
from xmlrpclib import ProtocolError
import robot
from robot.errors import HandlerExecutionFailed
from robot.variables import GLOBAL_VARIABLES
from robot.libraries.Process import Process
from robot.libraries.Remote import Remote
from robot.running import EXECUTION_CONTEXTS
from robot.running.namespace import IMPORTER
from robot.running.testlibraries import TestLibrary
from robot.libraries.BuiltIn import BuiltIn, run_keyword_variant
from robot.api import logger

REMOTE_AGENTS_LIST = []
EXPECTED_AGENT_RECEIVED = threading.Event()

class SimpleServer(SocketServer.BaseRequestHandler):

    def handle(self):
        data = ''.join(iter(self.read_socket, ''))
        port, name = data.decode().split(':', 1)
        print '*DEBUG:%d* Registered java remoteswinglibrary agent "%s" at port %s' % (time.time()*1000, name, port)
        REMOTE_AGENTS_LIST.append((port, name))
        EXPECTED_AGENT_RECEIVED.set()
        self.request.sendall(data)

    def read_socket(self):
        return self.request.recv(1)


class InvalidURLException(Exception):
    pass


class _RobotImporterWrapper(object):
    def remove_library(self, name, args):
        lib = TestLibrary(name, args, None, create_handlers=False)
        key = (name, lib.positional_args, lib.named_args)
        self._remove_library(key)

    def _remove_library(self, key):
        raise NotImplementedError()


class Robot26ImporterWrapper(_RobotImporterWrapper):
    def _remove_library(self, key):
        if key in IMPORTER._library_cache:
            index = IMPORTER._library_cache._keys.index(key)
            IMPORTER._library_cache._keys.pop(index)
            IMPORTER._library_cache._items.pop(index)


class OldRobotImporterWrapper(_RobotImporterWrapper):
    def _remove_library(self, key):
        if IMPORTER._libraries.has_key(key):  # key in dict doesn't work here
            index = IMPORTER._libraries._keys.index(key)
            IMPORTER._libraries._keys.pop(index)
            IMPORTER._libraries._libs.pop(index)


class RobotLibraryImporter(object):
    """Class for manipulating Robot Framework library imports during runtime"""

    def re_import_remoteswinglibrary(self):
        name = 'RemoteSwingLibrary'
        self._remove_lib_from_current_namespace(name)
        self._import_wrapper().remove_library(name, [])
        BuiltIn().import_library(name)

    def _import_wrapper(self):
        if hasattr(IMPORTER, '_library_cache'):
            return Robot26ImporterWrapper()
        return OldRobotImporterWrapper()

    def _remove_lib_from_current_namespace(self, name):
        testlibs = EXECUTION_CONTEXTS.current.namespace._testlibs
        if testlibs.has_key(name):
            del(testlibs[name])


class RemoteSwingLibraryTimeoutError(RuntimeError):
    pass


class RemoteSwingLibrary(object):
    """Robot Framework library leveraging Java-agents to run SwingLibrary keywords on Java-processes. The library contains
    a simple socket server to communicate with Java agents. When taking the library into use, you can specify the port this
    server uses. Providing the port is optional. If you do not provide one,
    RemoteSwingLibrary will ask the OS for an unused port.

    See https://github.com/robotframework/SwingLibrary for details about SwingLibrary keywords revealed through this library.

    Examples:
    | * Settings * |
    | Library | RemoteSwingLibrary |
    | * Test Cases * |
    | Testing java application |
    | | Start Application | myjavaapp | java -jar myjava.jar |
    | | Select Window  | My App |
    | | Ensure Application Should Close | 5 | Push Button | Exit |


    """

    ROBOT_LIBRARY_SCOPE = 'GLOBAL'
    KEYWORDS = ['system_exit', 'start_application', 'application_started', 'switch_to_application',
                'ensure_application_should_close']
    REMOTES = {}
    CURRENT = None
    PROCESS = Process()
    ROBOT_NAMESPACE_BRIDGE = RobotLibraryImporter()
    TIMEOUT = 60
    PORT = None
    AGENT_PATH = os.path.abspath(os.path.dirname(__file__))

    def __init__(self, port=None):
        if RemoteSwingLibrary.PORT is None:
            RemoteSwingLibrary.PORT = self._start_port_server(port or 0)
        self._set_env()

    @property
    def current(self):
        if not self.CURRENT:
            return None
        return self.REMOTES[self.CURRENT][0]

    def _start_port_server(self, port):
        address = ('127.0.0.1', int(port))
        server = SocketServer.TCPServer(address, SimpleServer)
        server.allow_reuse_address = True
        t = threading.Thread(target=server.serve_forever)
        t.setDaemon(True)
        t.start()
        return server.server_address[1]

    def _set_env(self):
        agent_command = '-javaagent:%s=%s' % (RemoteSwingLibrary.AGENT_PATH, RemoteSwingLibrary.PORT)
        os.environ['JAVA_TOOL_OPTIONS'] = agent_command
        logger.info(agent_command)

    def start_application(self, alias, command, timeout=60, name_contains=None):
        """Starts the process in the `command` parameter  on the host operating system.
        The given alias is stored to identify the started application in RemoteSwingLibrary.

        timeout (default 60) is timeout in seconds.
        name_contains is a text that must be part of the name of the java process that we are connecting to.
        name_contains helps in situations where multiple java-processes are started.
        To see the name of the connecting java agents run tests with --loglevel DEBUG.

        """
        EXPECTED_AGENT_RECEIVED.clear() # We are going to wait for a specific agent
        self.PROCESS.start_process(command, alias=alias, shell=True)
        try:
            self.application_started(alias, timeout=timeout, name_contains=name_contains)
        except:
            result = self.PROCESS.wait_for_process(timeout=0.01)
            if result:
                logger.info('STDOUT: %s' % result.stdout)
                logger.info('STDERR: %s' % result.stderr)
            else:
                print "Process is running, but application startup failed"
            raise

    def application_started(self, alias, timeout=60, name_contains=None):
        """Detects new RemoteSwingLibrary Java-agents in applications that are started without
        using the Start Application -keyword. The given alias is stored to identify the
        started application in RemoteSwingLibrary.
        Subsequent keywords will be passed on to this application."""
        self.TIMEOUT = int(timeout)
        port = self._get_agent_port(name_contains)
        url = '127.0.0.1:%s'%port
        logger.info('connecting to started application through port %s' % port)
        self._initialize_remote_libraries(alias, url)
        RemoteSwingLibrary.CURRENT = alias
        logger.debug('modifying robot framework namespace')
        self.ROBOT_NAMESPACE_BRIDGE.re_import_remoteswinglibrary()
        logger.info('connected to started application through port %s' % port)

    def _initialize_remote_libraries(self, alias, url):
        swinglibrary = Remote(url)
        logger.debug('remote swinglibrary instantiated')
        services = Remote(url + '/services')
        logger.debug('remote services instantiated')
        self.REMOTES[alias] = [swinglibrary, services]

    def _get_agent_port(self, name_pattern):
        while True:
            if not REMOTE_AGENTS_LIST:
                EXPECTED_AGENT_RECEIVED.clear()
            EXPECTED_AGENT_RECEIVED.wait(
                timeout=self.TIMEOUT) # Ensure that a waited agent is the one we are receiving and not some older one
            if not EXPECTED_AGENT_RECEIVED.isSet():
                raise RemoteSwingLibraryTimeoutError('Agent port not received before timeout')
            for port, name in reversed(REMOTE_AGENTS_LIST):
                if name_pattern is None or name_pattern in name:
                    REMOTE_AGENTS_LIST.remove((port, name))
                    return port
            time.sleep(0.1)

    def _ping_until_timeout(self, timeout):
        timeout = float(timeout)
        delta = min(0.1, timeout)
        endtime = timeout+time.time()
        while endtime > time.time():
            self._run_from_services(RemoteSwingLibrary.CURRENT, 'ping')
            time.sleep(delta)

    def _run_from_services(self, alias, kw, *args, **kwargs):
        return self.REMOTES[alias][1].run_keyword(kw, args, kwargs)

    @run_keyword_variant(resolve=1)
    def ensure_application_should_close(self, timeout, kw, *args):
        """ Runs the given keyword and waits until timeout for the application to close - the application is the current
        active application.
        If the application doesn't close, the keyword will take a screenshot and close the application
        and after that it will fail.
        In many cases calling the keyword that will close the application under test brakes the remote connection.
        This exception is ignored as it is expected by this keyword.
        Other exceptions will fail this keyword as expected.
        """
        with self._run_and_ignore_connection_lost():
            BuiltIn().run_keyword(kw, *args)
        try:
            self._application_should_be_closed(timeout=timeout)
        except RemoteSwingLibraryTimeoutError, t:
            logger.warn('Application is not closed before timeout - killing application')
            self._take_screenshot()
            self.system_exit(RemoteSwingLibrary.CURRENT)
            raise

    def _take_screenshot(self):
        logdir = self._get_log_dir()
        filepath = os.path.join(logdir, 'remoteswinglibrary-screenshot%s.png' % long(time.time()*1000))
        self._run_from_services(RemoteSwingLibrary.CURRENT, 'takeScreenshot', filepath)
        logger.info('<img src="%s"></img>' % robot.utils.get_link_path(filepath, logdir), html=True)

    # Copied from Selenium2Library _logging.py module ( a6e2c7fbb9098eb6e2e6ccaadb4dbfdbe26542a6 )
    def _get_log_dir(self):
        logfile = GLOBAL_VARIABLES['${LOG FILE}']
        if logfile != 'NONE':
            return os.path.dirname(logfile)
        return GLOBAL_VARIABLES['${OUTPUTDIR}']

    def _application_should_be_closed(self, timeout):
        with self._run_and_ignore_connection_lost():
            self._ping_until_timeout(timeout)
            raise RemoteSwingLibraryTimeoutError('Application was not closed before timeout')

    @contextmanager
    def _run_and_ignore_connection_lost(self):
        try:
            yield
        except RuntimeError, r: # disconnection from remotelibrary
            if 'Connection to remote server broken:' in r.message:
                logger.info('Connection died as expected')
                return
            raise
        except HandlerExecutionFailed, e: # disconnection from xmlrpc wrapped in robot keyword
            if 'Connection to remote server broken:' in e.message:
                logger.info('Connection died as expected')
                return
            raise
        except ProtocolError, r: # disconnection from xmlrpc in jython on some platforms
            logger.info('Connection died as expected')
            return

    def system_exit(self, alias, exit_code=1):
        """ Uses the RemoteSwingLibrary java agent to call system exit for the specific java process with the given
        alias.
        """
        with self._run_and_ignore_connection_lost():
            self._run_from_services(alias, 'systemExit', exit_code)

    def switch_to_application(self, alias):
        """Switches between Java-agents in applications that are known to RemoteSwingLibrary.
        The application is identified using the alias.
        Subsequent keywords will be passed on to this application."""
        RemoteSwingLibrary.CURRENT = alias
        self.ROBOT_NAMESPACE_BRIDGE.re_import_remoteswinglibrary()

    # HYBRID KEYWORDS

    def get_keyword_names(self):
        if self.current:
            return RemoteSwingLibrary.KEYWORDS + [kw for
                                      kw in self.current.get_keyword_names(attempts=RemoteSwingLibrary.TIMEOUT)
                                      if kw != 'startApplication']
        return RemoteSwingLibrary.KEYWORDS

    def __getattr__(self, name):
        current = self.current
        def func(*args, **kwargs):
            return current.run_keyword(name, args, kwargs)
        return func
