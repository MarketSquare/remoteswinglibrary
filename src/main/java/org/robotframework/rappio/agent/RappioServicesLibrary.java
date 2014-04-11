package org.robotframework.rappio.agent;

public class RappioServicesLibrary {

    public void killApplication() {
        System.exit(1);
    }
    
    public String ping() {
        return "pong";
    }
}
