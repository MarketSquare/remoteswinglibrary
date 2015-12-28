# Copyright 2015 Nokia Solutions and Networks
# Licensed under the Apache License, Version 2.0,
# see licence.txt file for details.
"""A script for running RemoteSwingLibrary Acceptance tests.

Description:
    This script can build from source and run RemoteSwingLibrary.
    Acceptance tests can be run for python 2, python 3 and jython.
    RemoteSwingLibrary is built only once for script execution.
    Tests are executed according to order of passed interpreters.

Usage:
    run_atest.py interpreters [--nobuild]

--nobuild   Skip building RemoteSwingLibrary with maven and just execute tests.

Examples:
    $ python run_atest.py python2
    $ python run_atest.py python3 python2 --nobuild
    $ python run_atest.py jython python2 python3
"""
import sys
import os
from subprocess import call, check_output

TESTED = ''

def get_rsl_ver():
    with open("pom.xml", "r") as f:
        pom = f.read().splitlines()
        for line in pom:
            if "<version>" in line:
                return line.strip()\
                    .lstrip("<version>")\
                    .rstrip("</version>")


def set_env():
    rsl_jar_name = "remoteswinglibrary-" + get_rsl_ver() + ".jar"
    webstart_path = os.path.join(os.getcwd(), "src", "test", "robotframework", "acceptance", "webstart")
    rsl_path = os.path.join(os.getcwd(), "target", rsl_jar_name)
    os.environ["PYTHONPATH"] = os.pathsep.join([os.environ.get("PYTHONPATH",""), webstart_path, rsl_path])


def run_tests(interpreter):
    TESTED = True
    test_path = os.path.join("src", "test", "robotframework", "acceptance")
    if sys.platform.startswith("win"):
        which_program = "where"
    else:
        which_program = "which"
    which_pybot = check_output([which_program, "pybot"]).rstrip()
    call([interpreter, which_pybot, "--loglevel", "DEBUG", test_path])

if __name__ == '__main__':
    if len(sys.argv) == 1 or "-h" in sys.argv or "--help" in sys.argv:
        print(__doc__)
        sys.exit(251)

    if "--nobuild" not in sys.argv[2:]:
        call(["mvn", "clean", "package"])

    set_env()
    
    interpreters = [inter for inter in sys.argv[1:] if not inter.startswith("-")]
    for interpreter in interpreters:
        if "python2" == interpreter:
            run_tests("python2")
        elif "python3" == sys.argv[1]:
            run_tests("python3")
        elif "jython" == interpreter:
            run_tests("jython")

    if not TESTED:
        print(__doc__)
        sys.exit(251)
