*** Settings ***
Library    RemoteSwingLibrary          debug=True
Library    OperatingSystem
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes
Test Teardown  Remove Files  *.png

*** Test Cases ***
Close Security Dialogs
    [Timeout]    60 seconds
    Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  30 seconds  \  True  dir_path=.
    File Should Exist  security_dialog_*.png
    Set Jemmy Timeouts  15
    Select Main Window
    System Exit

Close Security Dialogs Again
    [Timeout]    60 seconds
    Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  30 seconds  \  True
    Set Jemmy Timeouts  15
    Select Main Window
    System Exit

Close Security Dialogs With Different Screenshot Path
    [Timeout]    60 seconds
    Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  30 seconds  \  True  dir_path=security_dialogs_dir/
    File Should Exist  security_dialogs_dir/security_dialog_*.png
    Set Jemmy Timeouts  15
    Select Main Window
    System Exit
    [Teardown]  Remove Directory  security_dialogs_dir  True