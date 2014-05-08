*** Settings ***
Library   SSHLibrary
Library   RemoteSwingLibrary
Resource  ../resource.robot    # outside of repo. Contains real username and password

*** Test Cases ***
Connecting to another machine
   Open Connection   127.0.0.1
   Login     ${USERNAME}  ${PASSWORD}
   Put File  ${REMOTESWINGLIBRARYPATH}   remoteswinglibrary.jar
   Write     java -javaagent:remoteswinglibrary.jar=127.0.0.1:${REMOTESWINGLIBRARYPORT} -jar remoteswinglibrary.jar
   Application Started    myjar   timeout=5 seconds
   System Exit
   [Teardown]   Tearing

*** Keywords ***
Tearing
   Read
   Close All Connections
