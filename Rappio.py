import os
import socket
import threading
import Queue
import SocketServer
import time
from robot.libraries.Process import Process
from robot.libraries.Remote import Remote
from robot.running import EXECUTION_CONTEXTS
from robot.running.namespace import IMPORTER
from robot.running.testlibraries import TestLibrary
from robot.libraries.BuiltIn import BuiltIn
from robot.api import logger
REMOTE_AGENTS = Queue.Queue()

class SimpleServer(SocketServer.BaseRequestHandler):

    def handle(self):
        data = b''.join(iter(self.read_socket, b''))
        REMOTE_AGENTS.put(data.decode())
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

    def re_import_rappio(self):
        name = 'Rappio'
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


class Rappio(object):
    """Robot Framework library leveraging Java-agents to run SwingLibrary keywords on Java-processes. The library contains 
    a simple socket server to communicate with Java agents. When taking the library into use, you can specify the port this
    server uses. Providing the port is optional. If you do not provide one, Rappio will ask the OS for an unused port.

    Examples:
    | Library | Rappio |      |
    | Library | Rappio | 8181 |
    """

    ROBOT_LIBRARY_SCOPE = 'SUITE'
    KEYWORDS = ['start_application', 'application_started', 'switch_to_application', 'stop_application', 'set_rappio_path']
    REMOTES = {}
    CURRENT = None
    PROCESS = Process()
    ROBOT_NAMESPACE_BRIDGE = RobotLibraryImporter()
    TIMEOUT = 60
    PORT = None
    AGENT_PATH = ''

    def __init__(self, port=None):
        if Rappio.PORT is None:
            if port is None:
                Rappio.PORT = self.get_open_port()
            else:
                Rappio.PORT = int(port)            
            address = ('127.0.0.1', Rappio.PORT)
            server = SocketServer.TCPServer(address, SimpleServer)
            server.allow_reuse_address = True
            t = threading.Thread(target=server.serve_forever)
            t.daemon = True # don't hang on exit
            t.start()            
        self.set_env()


    def get_open_port(self):
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(("",0))
        s.listen(1)
        port = s.getsockname()[1]
        s.close()
        return port       

    @property
    def current(self):
        if not self.CURRENT:
            return None
        return self.REMOTES[self.CURRENT]

    def set_env(self):
        if not Rappio.PORT:
            raise Exception("Port is not defined!")
        agent_command = '-javaagent:%srobotframework-rappio-1.0-SNAPSHOT-jar-with-dependencies.jar=%s' % (Rappio.AGENT_PATH, Rappio.PORT)
        os.environ['JAVA_TOOL_OPTIONS'] = agent_command
        logger.info(agent_command)

    def set_rappio_path(self, path):
        """Sets the path to Rappio Java-agent jar. Note that on Windows systems the backslashes in the path should be escaped.

        Examples:
        | Set Rappio Path | /home/robot/libs/rappio.jar        |
        | Set Rappio Path | C:\\\\Users\\\\Robot\\\\Libs\\\\rappio.jar |

        """
        Rappio.AGENT_PATH = path
        self.set_env()

    def start_application(self, alias, command, timeout=60):
        """Starts the process in the `command` parameter  on the host operating system. The given alias is stored to identify the started application in Rappio."""
        self.PROCESS.start_process(command, alias=alias, shell=True)
        self.application_started(alias, timeout=timeout)
        agent_command = '-javaagent:%srobotframework-rappio-1.0-SNAPSHOT-jar-with-dependencies.jar=%s' % (Rappio.AGENT_PATH, Rappio.PORT)
        logger.info(agent_command)


    def application_started(self, alias, timeout=60):
        """Detects new Rappio Java-agents in applications that are started without using the Start Application -keyword. The given alias is stored to identify the started application in Rappio. 
        Subsequent keywords will be passed on to this application."""
        self.TIMEOUT = int(timeout)
        port = REMOTE_AGENTS.get(True, self.TIMEOUT);
        self.REMOTES[alias] = Remote('127.0.0.1:%s' %port)
        Rappio.CURRENT = alias
        self.ROBOT_NAMESPACE_BRIDGE.re_import_rappio()

    def switch_to_application(self, alias):
        """Switches between Java-agents in applications that are known to Rappio. The application is identified using the alias. 
        Subsequent keywords will be passed on to this application."""
        Rappio.CURRENT = alias
        self.ROBOT_NAMESPACE_BRIDGE.re_import_rappio()

    def stop_application(self, alias):
        """Stops the application with the given alias."""
        self.PROCESS.terminate_process(alias)

    # HYBRID KEYWORDS

    def get_keyword_names(self):
        if self.current:
            return Rappio.KEYWORDS + [kw for kw in self.current.get_keyword_names(attempts=Rappio.TIMEOUT)
                                      if kw != 'startApplication']
        return Rappio.KEYWORDS

    def __getattr__(self, name):
        current = self.current
        def func(*args, **kwargs):
            return current.run_keyword(name, args, kwargs)
        return func
 