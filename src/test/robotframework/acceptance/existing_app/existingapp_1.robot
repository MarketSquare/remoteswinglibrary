*** Settings ***
Library    RemoteSwingLibrary          debug=True
Library    OperatingSystem
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Starting application with main window
     Start Application  myapp2  java org.robotframework.remoteswinglibrary.MySwingApp  5 seconds  \  False  31337
