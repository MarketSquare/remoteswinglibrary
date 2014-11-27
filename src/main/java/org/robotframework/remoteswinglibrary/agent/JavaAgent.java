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

import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.robotframework.remoteswinglibrary.remote.DaemonRemoteServer;
import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;

import sun.awt.AppContext;


public class JavaAgent {

    private static final int DEFAULT_REMOTESWINGLIBRARY_PORT = 8181;

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        String[] args = agentArgument.split(":");
        String host = args[0];
        int port = getRemoteSwingLibraryPort(args[1]);
        boolean debug = args.length == 3 ? args[2].equals("DEBUG") : false;
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

    private static class FindAppContextWithWindow implements Runnable {

        String host;
        int port;
        boolean debug;

        public FindAppContextWithWindow(String host, int port, boolean debug) {
            this.host = host;
            this.port = port;
            this.debug = debug;
        }

        public void run()  {
            try {
                sun.awt.SunToolkit.invokeLaterOnAppContext(getAppContextWithWindow(), new ServerThread(host, port, debug));
            } catch (Exception e) {
                if (debug) {
                    e.printStackTrace();
                    System.err.println(e);
                    System.err.println("Error starting remote server");
                }
            }
        }

        public AppContext getAppContextWithWindow() throws Exception {
            while (true) {
                Set<AppContext> ctxs = AppContext.getAppContexts();
                for (AppContext ctx:ctxs) {
                    if (hasMainWindow(ctx)) {
                        return ctx;
                    }
                }
                Thread.sleep(1000);
            }

        }

        public boolean hasMainWindow(AppContext ctx) {
            Vector<WeakReference<Window>> windowList =
                  (Vector<WeakReference<Window>>)ctx.get(Window.class);
            if (windowList == null)
                return false;
            for (WeakReference<Window> ref:windowList) {
                Window window = ref.get();
                if (debug) logWindowDetails("Trying to connect to", window);
                if (isFrame(window)
                    && window.isVisible()
                    && !isConsoleWindow(window)) {
                    if (debug) logWindowDetails("Connected to", window);
                    return true;
                }
            }
            return false;
        }

        private void logWindowDetails(String message, Window window) {
            System.err.println(message+" Class:"+window.getClass().getName()
                    + " Name:"+window.getName()
                    + " Visible:"+window.isVisible()
                    + " AppContext:"+AppContext.getAppContext());
        }

        private boolean isFrame(Window window) {
            return window instanceof Frame;
        }

        private boolean isConsoleWindow(Window window) {
            return window.getClass().getName().contains("ConsoleWindow");
        }
    }

    private static class ServerThread implements Runnable {

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
