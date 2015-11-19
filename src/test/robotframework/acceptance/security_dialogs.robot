*** Settings ***

Library    RemoteSwingLibrary          debug=True     close_security_dialogs=True
Library    OperatingSystem
Library    Process
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Close Security Dialogs
     Start Application  securityDialogsApp  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  timeout=60 seconds
     System Exit

Close Security Dialogs 2
     Start Application  securityDialogsApp  java org.robotframework.remoteswinglibrary.SecurityDialogsApp  timeout=60 seconds
     System Exit

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
