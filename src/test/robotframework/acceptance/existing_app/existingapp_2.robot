*** Settings ***
Library    RemoteSwingLibrary          debug=True
Library    OperatingSystem
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Connecting to existing application on selected port and close it
    [Timeout]    15 seconds
     Application Started  myapp2  5 seconds  remote_port=31337
     Select Main Window
     System Exit
