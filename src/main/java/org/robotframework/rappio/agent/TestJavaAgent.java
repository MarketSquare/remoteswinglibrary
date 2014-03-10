package org.robotframework.rappio.agent;

import java.lang.instrument.Instrumentation;

import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;

public class TestJavaAgent {

	  public static void premain(String agentArgument, Instrumentation instrumentation){
		  System.out.println("Java Agent!");
		  RemoteServer.configureLogging();
		  RemoteServer server = new RemoteServer();
		  server.putLibrary("/", new SwingLibrary());
		  server.setPort(8181);
		  try {
			server.start();
		} catch (Exception e) {
			System.out.println("TODODOODODODODOODODODODO!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	  
}
