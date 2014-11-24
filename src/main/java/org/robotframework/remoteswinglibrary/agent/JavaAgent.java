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

import java.awt.Window;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.robotframework.remoteswinglibrary.remote.DaemonRemoteServer;
import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

public class JavaAgent {

    private static final int DEFAULT_REMOTESWINGLIBRARY_PORT = 8181;
    private static PrintStream out = System.out;

    public static void premain(String agentArgument, Instrumentation instrumentation) {
        try {
            Thread findAppContext = new Thread(new FindAppContextWithWindow(agentArgument.split(":")));
            findAppContext.setDaemon(true);
            findAppContext.start();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e);
            System.err.println("Error starting remote server");
        }
    }


    private static class FindAppContextWithWindow implements Runnable {

        String [] args;

        public FindAppContextWithWindow(String[] args) {
            this.args = args;
        }

        public void run()  {
            try {
                sun.awt.SunToolkit.invokeLaterOnAppContext(getAppContextWithWindow(), new ServerThread(args));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e);
                System.err.println("Error starting remote server");
            }
        }

        public AppContext getAppContextWithWindow() throws Exception {
            while (true) {
                Set<AppContext> ctxs = AppContext.getAppContexts();
                for (AppContext ctx:ctxs) {
                    if (hasWindow(ctx)) {
                        return ctx;
                    }
                }
                Thread.sleep(2000);
            }

        }

        public boolean hasWindow(AppContext ctx) {
            Vector window = (Vector)ctx.get(Window.class);
            return window != null && window.size() > 0;
        }
    }

    private static class ServerThread implements Runnable {

        String [] args;

        public ServerThread(String[] args) {
            this.args = args;
        }

        public void run()  {

            try {
                if (args.length >= 3 && "DEBUG".equals(args[2])) {
                    RemoteServer.configureLogging();
                } else {
                    noOutput();
                }
                RemoteServer server = new DaemonRemoteServer();
                server.putLibrary("/RPC2", new SwingLibrary());
                server.putLibrary("/services", new ServicesLibrary());
                server.setPort(0);
                server.setAllowStop(true);
                server.start();
                Integer actualPort = server.getLocalPort();
                notifyPort(actualPort, args[0], getRemoteSwingLibraryPort(args[1]));
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(e);
                System.err.println("Error starting remote server");
            } finally {
                System.setOut(out);
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

        private static int getRemoteSwingLibraryPort(String port) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                return DEFAULT_REMOTESWINGLIBRARY_PORT;
            }
        }

        // Silence stdout, some clients expect the output to be valid XML
        private static void noOutput() {
            Logger root = Logger.getRootLogger();
            root.removeAllAppenders();
            BasicConfigurator.configure();
            root.setLevel(Level.OFF);
            root.addAppender(new NullAppender());
            LogFactory.releaseAll();
            LogFactory.getFactory().setAttribute(
                    "org.apache.commons.logging.Log",
                    "org.apache.commons.logging.impl.Log4JLogger");
            Properties p = new Properties();
            p.setProperty("org.eclipse.jetty.LEVEL", "WARN");
            org.eclipse.jetty.util.log.StdErrLog.setProperties(p);

            // Jemmy bootstrap prints to stdout, replace with no-op while SwingLibrary is starting
            System.setOut(new PrintStream(new OutputStream() {
                public void write(int b) {
                    //DO NOTHING
                }
            }));
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
