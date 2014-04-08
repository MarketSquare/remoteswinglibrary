package org.robotframework.rappio.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

public class RappioJavaAgent {

	private static final int DEFAULT_RAPPIO_PORT = 8181;
        private static int PORT = DEFAULT_RAPPIO_PORT;
        static Thread someThread = null;
	static Instrumentation instrumentationInstance = null;
	static ClassFileTransformer transformer = null;
        
        public static void premain(String agentArguments, final Instrumentation inst) throws Exception {
            instrumentationInstance = inst;
            PORT = getRappioPort(agentArguments);
            if(isRunningJavaWebStart()) {
                    transformer = new ClassListenerTransformer();
                    inst.addTransformer(transformer);
            } else {
                    startRappioServer();
            }
        }
        
        public static void startRappioServer(ClassLoader classLoader) {
            // Only do this once
            if(someThread==null) {
                @SuppressWarnings("rawtypes")
                    Class runnableClass;
                    try {
                        instrumentationInstance.removeTransformer(transformer);
                        runnableClass = classLoader.loadClass("org.robotframework.rappio.RappioServer");
                        Object r = runnableClass.getDeclaredConstructor(Integer.class).newInstance(PORT);
                        someThread = new Thread((Runnable) r);
                        someThread.setContextClassLoader(classLoader);
                        someThread.setDaemon(true);
                        someThread.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }    	
        }
        
        public static void startRappioServer() {
            // Only do this once
            if(someThread==null) {
                    someThread = new Thread(new RappioServer(PORT));
                    someThread.setDaemon(true);
                    someThread.start();
            }
        }
        
        private static boolean isRunningJavaWebStart() {
	    boolean hasJNLP = false;
	    try {
	      Class.forName("javax.jnlp.ServiceManager");
	      hasJNLP = true;
	    } catch (ClassNotFoundException ex) {
	      hasJNLP = false;
	    }
	    return hasJNLP;
	}
        
	private static int getRappioPort(String agentArgument) {
		try{
			return Integer.parseInt(agentArgument);
		}catch(Exception e){
			return DEFAULT_RAPPIO_PORT;
		}
	}	
}
