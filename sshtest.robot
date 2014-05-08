*** Settings ***
Library   SSHLibrary
Library   RemoteSwingLibrary
Resource  ../resource.robot    # outside of repo. Contains real username and password

*** Test Cases ***
Connecting to another machine
   Open Connection   ${REMOTEIP}
   Login     ${USERNAME}  ${PASSWORD}
   Put File  ${REMOTESWINGLIBRARYPATH}   remoteswinglibrary.jar
   Write     xvfb-run java -javaagent:remoteswinglibrary.jar=${MYIP}:${REMOTESWINGLIBRARYPORT}:DEBUG -jar remoteswinglibrary.jar
   Application Started    myjar   timeout=5 seconds
   System Exit
   [Teardown]   Tearing

*** Keywords ***
Tearing
   Read
   Close All Connections
