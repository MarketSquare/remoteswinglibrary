RemoteSwingLibrary
==================

[Robot Framework](http://robotframework.org) library for testing and connecting to a java process and using [SwingLibrary](https://github.com/robotframework/SwingLibrary).

This library allows you to use pybot (Python version of Robot Framework) to run test cases although it also works if you are running with jybot (Jython version of Robot Framework). This means that you can use your other pure Python libraries in your test cases that will not work when runnin with Jython.

RemoteSwingLibrary works with Robot Framework 2.8.3 or later.

You can connect to applications running on your local machine or even on a [different machine](https://github.com/robotframework/remoteswinglibrary/blob/master/sshtest.robot).

RemoteSwingLibrary works also with Java Web Start applications. One of its intended usages is deprecating [RemoteApplications](https://github.com/robotframework/RemoteApplications) library.

Installation
------------

* Download latest RemoteSwingLibrary and documentation from https://github.com/robotframework/remoteswinglibrary/releases/
* Add downloaded jar to PYTHONPATH
* After that, you can import the library in your test cases:
    ```robotframework
    *** Settings ***
    Library    RemoteSwingLibrary
    
    *** Test Cases ***
    My Test Case
        Start Application    my_app    java -jar MyDemoApplication.jar
    ```
