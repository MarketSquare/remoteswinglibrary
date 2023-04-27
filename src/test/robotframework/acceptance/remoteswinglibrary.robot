*** Settings ***
Library    RemoteSwingLibrary          debug=True
Library    OperatingSystem
Library    Process
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
None Existing Application start fails before timeout
     [Timeout]    3 seconds
     Run Keyword And Expect Error    *    Start Application    no-one    this command fails immediatly   timeout=1

Starting and stopping application with main window
     [Timeout]    10 seconds
     Start Application    myapp2    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5 seconds
     System Exit

Start application removes the JAVA_TOOL_OPTIONS from enviroment
     [Timeout]    10 seconds
     Set environment variable    JAVA_TOOL_OPTIONS   ${EMPTY}
     Start Application    myapp2    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5 seconds
     Should be equal    %{JAVA_TOOL_OPTIONS}    ${EMPTY}
     System Exit

Connecting to a started application
     [Timeout]    15 seconds
     Set java tool options
     ${handle}=  Start Process   java org.robotframework.remoteswinglibrary.MySwingApp   shell=True
     Application Started     myjava
     Exit and check process   ${handle}    myjava
     Set environment variable    JAVA_TOOL_OPTIONS   ${EMPTY}

Connecting to a specific application
     [Timeout]    30 seconds
     Set java tool options
     ${handle1}=  Start Process   java org.robotframework.remoteswinglibrary.MySwingApp one    shell=True
     ${handle2}=  Start Process   java org.robotframework.remoteswinglibrary.MySwingApp two    shell=True
     ${handle3}=  Start Process   java org.robotframework.remoteswinglibrary.MySwingApp three  shell=True
     Application Started    three    name_contains=three
     Application Started    one      name_contains=one
     Application Started    two      name_contains=two
     Exit and check process    ${handle2}   two
     Exit and check process    ${handle3}   three
     Exit and check process    ${handle1}   one
     Set environment variable    JAVA_TOOL_OPTIONS   ${EMPTY}

Connecting to an application and using java agent option
    [Timeout]    20 seconds
    ${agent}=    Set Variable   -javaagent:"${REMOTESWINGLIBRARYPATH}"\=127.0.0.1:${REMOTESWINGLIBRARYPORT}
    log    ${agent}
    ${handle}=    Start Process  java ${agent} org.robotframework.remoteswinglibrary.MySwingApp   shell=True
    Application Started     app
    Exit and check process    ${handle}   app

Ensure application closing
     [Timeout]    15 seconds
     Start Application    myapp    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5
     Ensure Application Should Close    5 seconds   My Closing Keyword    myapp

Ensure application closing when timeout
     [Timeout]    10 seconds
     Start Application    myapp    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5 seconds
     Run Keyword and expect error  *Application was not closed before timeout*  Ensure Application Should Close    1 second   No Operation

Unallowed SwingLibrary keywords
    [Timeout]    15 seconds
    Start Application    myapp3    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5 seconds
    Keyword Should Not Exist   Launch Application
    Keyword Should Not Exist   Start Application In Separate Thread
    [Teardown]  System Exit

Logging java properties
    [Timeout]    15 seconds
    Start Application    mapp    java org.robotframework.remoteswinglibrary.MySwingApp  timeout=5 seconds
    ${props}=       Log Java System Properties
    Should Contain   ${props}   System.getenv():
    [Teardown]  System Exit

*** Keywords ***
Keyword Should Not Exist
   [Arguments]   ${keyword}
   Run Keyword And Expect Error   No keyword with name '${keyword}' found.    Keyword Should Exist   ${keyword}

Exit and check process
   [Arguments]    ${handler}   ${alias}
   Switch To Application  ${alias}
   Process Should Be Running    ${handler}
   System Exit
   Wait until keyword succeeds   5 seconds   0.5 seconds   Process Should Be Stopped    ${handler}

My Closing Keyword
   [Arguments]    ${alias}
   Switch To Application  ${alias}
   Log     something plaah
   System Exit
