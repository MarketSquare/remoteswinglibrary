package org.robotframework.remoteswinglibrary;

public class ReadJavaVersion {
    public static void main(String[] args) {
        String versionProp = System.getProperty("java.version");
        System.out.print(getJavaVersion(versionProp));
    }

    private static String getJavaVersion(String version){
        if (version.contains(".")) {
            int index = version.indexOf(".");
            index = version.indexOf(".", index + 1);
            return version.substring(0, index);
        }
        return (version);
    }
}

