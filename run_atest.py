# Copyright 2016 Nokia Solutions and Networks
# Licensed under the Apache License, Version 2.0,
# see licence.txt file for details.
'''A script for running RemoteSwingLibrary Acceptance tests.

Description:
    This script can build from source and run RemoteSwingLibrary.
    Acceptance tests can be run for python 2, python 3 and jython.
    RemoteSwingLibrary is built only once for script execution.
    Tests are executed according to order of passed interpreters.
    If run without specified interpreter[s] the executing one will
    be used for testing.

Usage:
    [python|python3|jython|...] run_atest.py [interpreters] [--nobuild]

--nobuild   Skip building RemoteSwingLibrary with maven and just execute tests.

Examples:
    $ python run_atest.py
    $ python run_atest.py python3.5
    $ python run_atest.py python3 python2.7 --nobuild
    $ python run_atest.py jython python2 python3
'''
import sys
import os
from subprocess import call, check_output
from robot import run_cli
from xml.etree.ElementTree import ElementTree as xml

TESTED = ''


def get_rsl_ver():
    ns = "http://maven.apache.org/POM/4.0.0"
    tree  = xml()
    tree.parse('pom.xml')
    return tree.getroot().find("{%s}version" % ns).text


def get_env():
    rsl_jar_name = 'remoteswinglibrary-' + get_rsl_ver() + '.jar'
    webstart_path = os.path.join(os.getcwd(), 'src', 'test', 'robotframework', 'acceptance', 'webstart')
    rsl_path = os.path.join(os.getcwd(), 'target', rsl_jar_name)
    return (webstart_path, rsl_path)


def run_tests(interpreter=None):
    test_path = os.path.join('src', 'test', 'robotframework', 'acceptance')
    if interpreter is None:
        sys.path.extend(list(get_env()))
        run_cli(['--nostatusrc', '--loglevel', 'DEBUG', test_path])
    else:
        set_env()
        if sys.platform.startswith('win'):
            which_program = 'where'
        else:
            which_program = 'which'
        which_pybot = check_output([which_program, 'pybot']).rstrip()
        call([interpreter, which_pybot, '--nostatusrc', '--loglevel', 'DEBUG', test_path])


def set_env():
    paths = get_env()
    os.environ['PYTHONPATH'] = os.pathsep.join([os.environ.get('PYTHONPATH', ''), paths[0], paths[1]])


if __name__ == '__main__':
    if '-h' in sys.argv or '--help' in sys.argv:
        print(__doc__)
        sys.exit(251)

    if '--nobuild' not in sys.argv:
        if sys.platform.startswith('win'):
            call(['mvn', 'clean', 'package'], shell=True)
        else:
            call(['mvn', 'clean', 'package'])

    interpreters = [inter for inter in sys.argv[1:] if not inter.startswith('-')]
    if len(interpreters) == 0:
        run_tests()
    else:
        for interpreter in interpreters:
            run_tests(interpreter)
