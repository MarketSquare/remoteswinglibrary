 *** Settings ***
Library    RemoteSwingLibrary
Library    FileServer
Suite Setup     FileServer.Start
Suite Teardown    FileServer.Stop

*** Variables ***
${WEBSTART DIR}=    ${CURDIR}/webstart


*** Test Cases ***
Webstart Test
    Start Application    test-app    javaws ${WEBSTART DIR}/test-app/test-application.jnlp    120
    Select Main Window
    Push Button    Start javaws application
    Sleep     10 seconds
