*** Settings ***
Library    RemoteSwingLibrary          debug=True
Library    OperatingSystem
Suite setup    Set Environment Variable      CLASSPATH     target/test-classes

*** Test Cases ***
Reinitiate
    [Timeout]  15 seconds
    Reinitiate  port=11777  debug=False
    Should Be Equal As Integers  ${REMOTESWINGLIBRARYPORT}  11777
    Reinitiate  debug=True
