package org.robotframework.remoteswinglibrary;

public class ReadJavaVersion {
    public static void main(String[] args) {
        String version = System.getProperty("java.version");
        if (version.contains(".")) {
            int index = version.indexOf(".");
            index = version.indexOf(".", index + 1);
            System.out.println(version.substring(0, index));
        } else {
            System.out.println(version);
        }
    }
}

