package org.robotframework.rappio.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class RappioJavaAgent {

	private static final int DEFAULT_RAPPIO_PORT = 8181;
        private static int PORT = DEFAULT_RAPPIO_PORT;
        private static final AtomicBoolean STARTED = new AtomicBoolean(false);
	static Instrumentation instrumentationInstance = null;
	static ClassFileTransformer transformer = null;
        
        public static void premain(String agentArguments, final Instrumentation inst) throws Exception {
            instrumentationInstance = inst;
            PORT = getRappioPort(agentArguments);
            if(isRunningJavaWebStart()) {
                Set<Thread> threads = Thread.getAllStackTraces().keySet();
                for(Thread t:threads) {
                    System.out.println("Thread:  "+t);
                }
                System.exit(0);
            } else {
                startRappioServer();
            }
        }
        
        public static void startRappioServer(ClassLoader classLoader) {
            // Only do this once
            if(!STARTED.get()) {
                @SuppressWarnings("rawtypes")
                Class runnableClass;
                try {
                    instrumentationInstance.removeTransformer(transformer);
                    runnableClass = classLoader.loadClass("org.robotframework.rappio.RappioServer");
                    Object r = runnableClass.getDeclaredConstructor(Integer.class).newInstance(PORT);
                    Thread someThread = new Thread((Runnable) r);
                    someThread.setContextClassLoader(classLoader);
                    someThread.setDaemon(true);
                    someThread.start();
                    STARTED.set(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }    	
        }
        
        public static void startRappioServer() {
            // Only do this once
            if(!STARTED.get()) {
                Thread someThread = new Thread(new RappioServer(PORT));
                someThread.setDaemon(true);
                someThread.start();
                STARTED.set(true);
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
