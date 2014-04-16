package org.robotframework.rappio.agent;

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
import org.robotframework.rappio.remote.DaemonRemoteServer;
import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

public class RappioJavaAgent {

	private static final String LOCALHOST = "127.0.0.1";
	private static final int DEFAULT_RAPPIO_PORT = 8181;
	private static PrintStream out = System.out;

	public static void premain(String agentArgument, Instrumentation instrumentation){
            try {
                noOutput();
                RemoteServer server = new DaemonRemoteServer();
                server.putLibrary("/RPC2", new SwingLibrary());
                server.putLibrary("/rappioservices", new RappioServicesLibrary());
                server.setPort(0);
                server.setAllowStop(true);
                server.start();
                if(AppContext.getAppContext() == null){
                    SunToolkit.createNewAppContext();
                }
                Integer actualPort = server.getLocalPort();
                notifyPort(actualPort, getRappioPort(agentArgument));
            } catch (Exception e) {
                    System.err.println("Error starting remote server");
                    e.printStackTrace();
            }finally{
                    System.setOut(out);
            }
	}
        
        private static void notifyPort(final Integer portToNotify, final Integer serverPort) {
            Timer timer = new Timer("RobotJavaAgentPortNotifyingTimer", true);
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    try {
                        while(Window.getWindows().length == 0) {
                            Thread.sleep(100);
                        }
                        Socket echoSocket = new Socket(LOCALHOST, serverPort);
                        PrintWriter outToServer = new PrintWriter(echoSocket.getOutputStream(), true);
                        outToServer.write(portToNotify.toString()+":"+getName());
                        outToServer.close();
                        echoSocket.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, 100);
        }
        
        private static String getName() {
            for(final Map.Entry<String, String> entry : System.getenv().entrySet())
            {
              if(entry.getKey().startsWith("JAVA_MAIN_CLASS"))
                return entry.getValue();
            }
            return "Unknown";
          }

	private static int getRappioPort(String agentArgument) {
		try{
			return Integer.parseInt(agentArgument);
		}catch(Exception e){
			return DEFAULT_RAPPIO_PORT;
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
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
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
