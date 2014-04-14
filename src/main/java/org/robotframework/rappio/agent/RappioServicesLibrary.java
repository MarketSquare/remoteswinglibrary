package org.robotframework.rappio.agent;

public class RappioServicesLibrary {

    public void systemExit(int exitCode) {
        System.exit(exitCode);
    }
    
    public String ping() {
        return "pong";
    }
}
