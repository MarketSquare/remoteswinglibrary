*** Settings ***

Library    RemoteSwingLibrary          debug=True     close_security_dialogs=True
Library    OperatingSystem
Library    Process
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Close Security Dialogs
     Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  timeout=30 seconds
     Select Main Window
     Close Window  Test

Close Security Dialogs Again
     Start Application  App  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  timeout=30 seconds
     Select Main Window
     Close Window  Test

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
