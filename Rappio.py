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

REMOTE_AGENTS = Queue.Queue()

class SimpleServer(SocketServer.BaseRequestHandler):

    def handle(self):
        data = b''.join(iter(self.read_socket, b''))
        print 'port from agent: %s' %data.decode()
        REMOTE_AGENTS.put(data.decode())

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

    ROBOT_LIBRARY_SCOPE = 'SUITE'
    KEYWORDS = ['start_application', 'application_started', 'switch_to_application', 'stop_application']
    REMOTES = {}
    CURRENT = None
    PROCESS = Process()
    ROBOT_NAMESPACE_BRIDGE = RobotLibraryImporter()
    TIMEOUT = 60
    PORT = None
    STARTED = False

    def __init__(self, port=None):
        if port is None:
            Rappio.PORT = self.get_open_port()
        else:
            Rappio.PORT = int(port)
        self.set_env()
        if not Rappio.STARTED:
            address = ('127.0.0.1', Rappio.PORT)
            server = SocketServer.TCPServer(address, SimpleServer)
            t = threading.Thread(target=server.serve_forever)
            t.daemon = True # don't hang on exit
            t.start()            
            Rappio.STARTED = True
            print 'server started at %s' %Rappio.PORT

    def get_open_port(self):
        import socket
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
        os.environ['JAVA_TOOL_OPTIONS'] = \
            '-javaagent:robotframework-rappio-1.0-SNAPSHOT-jar-with-dependencies.jar=%s' % Rappio.PORT

    def start_application(self, alias, command, timeout=60):
        self.PROCESS.start_process(command, alias=alias, shell=True)
        self.application_started(alias, timeout=timeout)

    def application_started(self, alias, timeout=60):
        self.TIMEOUT = int(timeout)
        port = REMOTE_AGENTS.get(True, 10);
        self.REMOTES[alias] = Remote('127.0.0.1:%s' %port)
        Rappio.CURRENT = alias
        self.ROBOT_NAMESPACE_BRIDGE.re_import_rappio()
        print 'added remote agent %s at %s' %(alias, port)

    def switch_to_application(self, alias):
        Rappio.CURRENT = alias
        self.ROBOT_NAMESPACE_BRIDGE.re_import_rappio()

    def stop_application(self, alias):
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
 
if __name__ == "__main__":
    print "__main__ start"
    address = ('127.0.0.1', 8181)
    server = SocketServer.TCPServer(address, SimpleServer)
    t = threading.Thread(target=server.serve_forever)
    t.daemon = True # don't hang on exit
    t.start()  
    time.sleep(20)
    print "__main__ stop"       