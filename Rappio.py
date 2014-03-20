import os
from robot.libraries.Process import Process
from robot.libraries.Remote import Remote
from robot.running import EXECUTION_CONTEXTS
from robot.running.namespace import IMPORTER
from robot.running.testlibraries import TestLibrary
from robot.libraries.BuiltIn import BuiltIn


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
    PORT=8181
    REMOTES = {}
    CURRENT = None
    PROCESS = Process()
    ROBOT_NAMESPACE_BRIDGE = RobotLibraryImporter()
    TIMEOUT = 60

    def __init__(self):
        self.set_env()

    @property
    def current(self):
        if not self.CURRENT:
            return None
        return self.REMOTES[self.CURRENT]

    def set_env(self):
        os.environ['JAVA_TOOL_OPTIONS'] = \
            '-javaagent:robotframework-rappio-1.0-SNAPSHOT-jar-with-dependencies.jar=%s' % Rappio.PORT

    def start_application(self, alias, *command):
        self.PROCESS.start_process(*command, alias=alias, shell=True)
        self.application_started(alias)

    def application_started(self, alias, timeout):
        self.TIMEOUT = int(timeout)
        self.REMOTES[alias] = Remote('localhost:%s' % Rappio.PORT)
        Rappio.CURRENT = alias
        Rappio.PORT += 1
        self.set_env()
        self.ROBOT_NAMESPACE_BRIDGE.re_import_rappio()

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