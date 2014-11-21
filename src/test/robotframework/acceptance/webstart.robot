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
    Set jemmy timeouts     60
    Select Main Window
    Ensure Application Should Close    5 seconds   Push Button  systemExitButton
