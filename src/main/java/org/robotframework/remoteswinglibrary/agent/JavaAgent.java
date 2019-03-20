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

import javax.swing.*;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;


public class JavaAgent {

    private static final int DEFAULT_REMOTESWINGLIBRARY_PORT = 8181;

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        String[] args = agentArgument.split(":");
        String host = args[0];
        int port = getRemoteSwingLibraryPort(args[1]);
        boolean debug = Arrays.asList(args).contains("DEBUG");
        boolean closeSecurityDialogs = Arrays.asList(args).contains("CLOSE_SECURITY_DIALOGS");
        String dirPath = null;
        if (Arrays.asList(args).contains("DIR_PATH")){
            if (System.getProperty("os.name").contains("Windows"))
                dirPath = args[args.length - 2] + ":" + args[args.length - 1];
            else
                dirPath = args[args.length - 1];
            System.out.println(args[args.length -1]);
        }
        int apport = 0;
        for (String arg: args)
            if (arg.startsWith("APPORT="))
                apport = Integer.parseInt(arg.split("=")[1]);
        try {
            Thread findAppContext = new Thread(new FindAppContextWithWindow(host, port, apport, debug, closeSecurityDialogs, dirPath));
            findAppContext.setDaemon(true);
            findAppContext.start();
            // Sleep to ensure that findAppContext daemon thread is kept alive until the
            // application is started.
            Thread.sleep(1000);
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
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame();
        frame.setTitle("My First Swing Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JTextArea text = new JTextArea("This is RemoteSwingLibrary\n"
                + "Usage:\n"
                + "Add this jar file to PYTHONPATH\n"
                + "Import RemoteSwingLibrary in your test cases\n"
                + "\n"
                + "This program will now sleep for 5 seconds\n"
                + "This main is for documentation generation "
                + "and connection testing purposes");
        text.setLineWrap(true);
        frame.add(text);
        frame.pack();
        frame.setSize(600,200);
        frame.setVisible(true);
        Thread.sleep(5000);
        frame.dispose();
    }


}
