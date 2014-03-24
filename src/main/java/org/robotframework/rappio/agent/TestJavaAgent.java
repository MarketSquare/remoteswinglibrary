package org.robotframework.rappio.agent;

import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.logging.LogManager;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.robotframework.rappio.remote.DaemonRemoteServer;

import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;
import sun.awt.AppContext;

public class TestJavaAgent {

	private static PrintStream 	out = System.out;

	public static void premain(String agentArgument, Instrumentation instrumentation){
		try {
			noOutput();
			int port = Integer.parseInt(agentArgument);      
			RemoteServer server = new DaemonRemoteServer();
			server.putLibrary("/RPC2", new SwingLibrary());
			server.setPort(port);
			server.setAllowStop(true);
			server.start();
			AppContext.getAppContext();
		} catch (Exception e) {
			System.err.println("Error starting remote server");
			e.printStackTrace();
		}finally{
			System.setOut(out);
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
