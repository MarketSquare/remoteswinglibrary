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
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

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

	public static void premain(String agentArgument, Instrumentation instrumentation){
            try {
                noOutput();
                RemoteServer server = new DaemonRemoteServer();
                server.putLibrary("/RPC2", new SwingLibrary());
                server.putLibrary("/services", new ServicesLibrary());
                server.setPort(0);
                server.setAllowStop(true);
                server.start();
                if(AppContext.getAppContext() == null){
                    SunToolkit.createNewAppContext();
                }
                Integer actualPort = server.getLocalPort();
                notifyPort(actualPort, getRemoteSwingLibraryHost(agentArgument), getRemoteSwingLibraryPort(agentArgument));
            } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e);
                    System.err.println("Error starting remote server");
            }finally{
                    System.setOut(out);
            }
	}
        
        public static void main(String[] args) throws InterruptedException{
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
        
        private static void notifyPort(final Integer portToNotify, final String serverHost, final Integer serverPort) throws IOException {
            Socket echoSocket = new Socket(serverHost, serverPort);
            PrintWriter outToServer = new PrintWriter(echoSocket.getOutputStream(), true);
            outToServer.write(portToNotify.toString()+":"+getName());
            outToServer.close();
            echoSocket.close();
        }
        
        private static String getName() {
            String name = System.getProperty("sun.java.command");
            if(name != null)
                return name;
            for(final Map.Entry<String, String> entry : System.getenv().entrySet())
            {
              if(entry.getKey().startsWith("JAVA_MAIN_CLASS"))
                return entry.getValue();
            }
            return "Unknown";
          }

	private static int getRemoteSwingLibraryPort(String agentArgument) {
		try{
			return Integer.parseInt(agentArgument.split(":")[1]);
		}catch(NumberFormatException e){
			return DEFAULT_REMOTESWINGLIBRARY_PORT;
		}
	}

        private static String getRemoteSwingLibraryHost(String agentArgument) {
            return agentArgument.split(":")[0];
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
