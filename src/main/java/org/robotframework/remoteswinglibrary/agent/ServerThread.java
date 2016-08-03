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
import java.util.Map;


public class ServerThread implements Runnable {
    int apport;
    boolean debug;
    RobotConnection robotConnection;

    public ServerThread(RobotConnection robotConnection,  int apport, boolean debug) {
        this.robotConnection = robotConnection;
        this.apport = apport;
        this.debug = debug;
    }

    public void run()  {
        try {
            RemoteServer server = new DaemonRemoteServer();
            SwingLibrary swingLibrary = SwingLibrary.instance == null ? new SwingLibrary() : SwingLibrary.instance;
            server.putLibrary("/RPC2", swingLibrary);
            server.putLibrary("/services", new ServicesLibrary());
            server.setPort(apport);
            server.setAllowStop(true);
            server.start();
            Integer actualPort = server.getLocalPort();
            notifyPort(actualPort);
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
                System.err.println(e);
                System.err.println("Error starting remote server");
            }
        }
    }

    private void notifyPort(final Integer portToNotify) throws IOException {
        robotConnection.connect();
        robotConnection.send("PORT:" + portToNotify.toString() + ":" + getName());
        robotConnection.close();
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
