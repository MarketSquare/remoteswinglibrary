import os
from robot.libraries.BuiltIn import BuiltIn
from robot.libraries.Process import Process


class Rappio(object):

    PORT=8181

    def __init__(self):
        os.environ['JAVA_TOOL_OPTIONS'] = '-javaagent:robotframework-rappio-1.0-SNAPSHOT-jar-with-dependencies.jar=%s' % Rappio.PORT
        self._process = Process()
        self._builtin = BuiltIn()

    def start_application(self, alias, *command):
        self._process.start_process(*command, alias=alias, shell=True)
        self.application_started(alias)

    def application_started(self, alias):
        self._builtin.import_library('Remote', 'localhost:%s' % Rappio.PORT, 'WITH NAME', alias)
        Rappio.PORT += 1
        os.environ['JAVA_TOOL_OPTIONS'] = '-javaagent:robotframework-rappio-1.0-SNAPSHOT-jar-with-dependencies.jar=%s' % Rappio.PORT

    def stop_application(self, alias):
        self._process.terminate_process(alias)
