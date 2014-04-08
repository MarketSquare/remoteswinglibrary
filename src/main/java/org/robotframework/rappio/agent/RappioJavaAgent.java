package org.robotframework.rappio.agent;

import java.lang.instrument.Instrumentation;

public class RappioJavaAgent {

	private static final int DEFAULT_RAPPIO_PORT = 8181;

	public static void premain(String agentArgument, Instrumentation instrumentation){
		final int port = getRappioPort(agentArgument);      
                Thread t = new Thread(new RappioServer(port));
                t.start();
	}
        
	private static int getRappioPort(String agentArgument) {
		try{
			return Integer.parseInt(agentArgument);
		}catch(Exception e){
			return DEFAULT_RAPPIO_PORT;
		}
	}	
}
