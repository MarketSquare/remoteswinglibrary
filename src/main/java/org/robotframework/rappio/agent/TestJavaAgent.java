package org.robotframework.rappio.agent;

import java.awt.Frame;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;

import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;

public class TestJavaAgent {

	public static void premain(String agentArgument, Instrumentation instrumentation){
		System.out.println("Java Agent! "+ManagementFactory.getRuntimeMXBean().getVmVersion());
		RemoteServer.configureLogging();
		RemoteServer server = new RemoteServer();
		server.putLibrary("/RPC2", new SwingLibrary());
		server.setPort(8181);
		server.setAllowStop(true);
		final Instrumentation inst = instrumentation;
		try {
			server.start();
		} catch (Exception e) {
			System.out.println("TODODOODODODODOODODODODO!!!");
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
                Frame.getFrames();
	}

}
