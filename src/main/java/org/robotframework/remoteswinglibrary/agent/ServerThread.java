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

import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.remoteswinglibrary.remote.DaemonRemoteServer;
import org.robotframework.swing.SwingLibrary;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;


public class ServerThread implements Runnable {

    String host;
    int port;
    boolean debug;

    public ServerThread(String host, int port, boolean debug) {
        this.host = host;
        this.port = port;
        this.debug = debug;
    }

    public void run()  {
        try {
            RemoteServer server = new DaemonRemoteServer();
            server.putLibrary("/RPC2", new SwingLibrary());
            server.putLibrary("/services", new ServicesLibrary());
            server.setPort(0);
            server.setAllowStop(true);
            server.start();
            Integer actualPort = server.getLocalPort();
            notifyPort(actualPort, host, port);
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
                System.err.println(e);
                System.err.println("Error starting remote server");
            }
        }
    }

    private static void notifyPort(final Integer portToNotify, final String serverHost, final Integer serverPort) throws IOException {
        Socket echoSocket = new Socket(serverHost, serverPort);
        PrintWriter outToServer = new PrintWriter(echoSocket.getOutputStream(), true);
        outToServer.write(portToNotify.toString() + ":" + getName());
        outToServer.close();
        echoSocket.close();
    }

    private static String getName() {
        String name = System.getProperty("sun.java.command");
        if (name != null)
            return name;
        for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith("JAVA_MAIN_CLASS"))
                return entry.getValue();
        }
        return "Unknown";
    }
}
