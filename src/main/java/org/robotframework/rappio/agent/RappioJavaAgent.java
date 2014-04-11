package org.robotframework.rappio.agent;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.net.Socket;
import java.util.Properties;

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
            int port = getRappioPort(agentArgument);
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
            Socket echoSocket = new Socket(LOCALHOST, port);
            PrintWriter outToServer = new PrintWriter(echoSocket.getOutputStream(), true);
            outToServer.write(actualPort.toString());
            outToServer.close();
            echoSocket.close();
		} catch (Exception e) {
			System.err.println("Error starting remote server");
			e.printStackTrace();
		}finally{
			System.setOut(out);
		}
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
