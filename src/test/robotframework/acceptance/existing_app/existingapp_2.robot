*** Settings ***
Library    RemoteSwingLibrary          apport=31337  debug=True
Library    OperatingSystem
Library    Process
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Connecting to existing application on selected port and close it
     Connect to application    myapp2    timeout=5 seconds
     System Exit
