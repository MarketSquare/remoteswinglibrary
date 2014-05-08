RemoteSwingLibrary
==================

Connect to a java process and use swinglibrary.

Installation
------------

* Download latest RemoteSwingLibrary and documentation from https://github.com/robotframework/remoteswinglibrary/releases/tag/1.1
* Add downloaded jar to PYTHONPATH
* After that, you can import the library in your test cases:
    ```
    *** Settings ***
    Library    RemoteSwingLibrary
    
    *** Test Cases ***
    My Test Case
        Start Application    my_app    java -jar MyDemoApplication.jar
    ```
