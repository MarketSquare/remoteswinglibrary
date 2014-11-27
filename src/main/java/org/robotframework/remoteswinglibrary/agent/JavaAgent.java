/*
 * Copyright 2014 Nokia Solutions and Networks Oyj
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.robotframework.remoteswinglibrary.agent;

import java.lang.instrument.Instrumentation;


public class JavaAgent {

    private static final int DEFAULT_REMOTESWINGLIBRARY_PORT = 8181;

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        String[] args = agentArgument.split(":");
        String host = args[0];
        int port = getRemoteSwingLibraryPort(args[1]);
        boolean debug = args.length == 3 && args[2].equals("DEBUG");
        try {
            Thread findAppContext = new Thread(new FindAppContextWithWindow(host, port, debug));
            findAppContext.setDaemon(true);
            findAppContext.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.err.println("Error starting remote server");
        }
    }

    private static int getRemoteSwingLibraryPort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            return DEFAULT_REMOTESWINGLIBRARY_PORT;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("This is RemoteSwingLibrary\n"
                + "Usage:\n"
                + "Add this jar file to PYTHONPATH\n"
                + "Import RemoteSwingLibrary in your test cases\n"
                + "\n"
                + "This program will now sleep for 5 seconds\n"
                + "This main is for documentation generation "
                + "and connection testing purposes");
        Thread.sleep(5000);
    }


}
