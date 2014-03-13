package org.robotframework.rappio.agent;

import java.awt.Frame;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.robotframework.remoteserver.RemoteServer;
import org.robotframework.swing.SwingLibrary;

public class TestJavaAgent {
	static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

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
		
                System.out.println("Frames: "+Frame.getFrames().length);
                for (Frame frame: Frame.getFrames()) {
                        System.out.println(""+frame.getTitle());
                }

                for(long id: ManagementFactory.getThreadMXBean().getAllThreadIds()){
                        ThreadInfo ti = ManagementFactory.getThreadMXBean().getThreadInfo(id);
                        System.out.println("Thread: "+ti.getThreadName());
                }
	}

}
