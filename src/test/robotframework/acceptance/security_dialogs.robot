*** Settings ***
Library    RemoteSwingLibrary          debug=True
Library    OperatingSystem
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Close Security Dialogs
    [Timeout]    60 seconds
    Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  30 seconds  \  True
    Set Jemmy Timeouts  15
    Select Main Window
    System Exit

Close Security Dialogs Again
    [Timeout]    60 seconds
    Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  30 seconds  \  True
    Set Jemmy Timeouts  15
    Select Main Window
    System Exit
