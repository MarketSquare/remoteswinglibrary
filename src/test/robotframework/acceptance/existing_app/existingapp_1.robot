*** Settings ***
Library    RemoteSwingLibrary          apport=31337  debug=True
Library    OperatingSystem
Library    Process
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Starting application with main window
     Start Application    myapp2    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5 seconds
