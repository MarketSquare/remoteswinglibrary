package org.robotframework.rappio.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class RappioJavaAgent {

	private static final int DEFAULT_RAPPIO_PORT = 8181;
        public static int PORT = DEFAULT_RAPPIO_PORT;
        private static final AtomicBoolean STARTED = new AtomicBoolean(false);
	
        public static void premain(String agentArguments, final Instrumentation inst) throws Exception {
            PORT = getRappioPort(agentArguments);
            if(isRunningJavaWebStart()) {
                Timer t = new Timer(true);
                t.schedule(new TimerTask() {
                     @Override
                     public void run() {
                         System.out.println("-----");
                        printClassloader(getClass().getClassLoader());
                        System.out.println("-++++-");
                        printClassloader(RappioJavaAgent.class.getClassLoader());
                        System.out.println("-!!!!-");
                        Set<Thread> threads = Thread.getAllStackTraces().keySet();
                        for(Thread t:threads) {
                            ClassLoader c = t.getContextClassLoader();
                            System.out.println("Thread:  "+t+" loader "+c);
                            if(c != null && c.getClass().getName().contains("JNLP")){
                                printClassloader(c);
                                System.out.println("Starting with loader "+c);
                                RappioJavaAgent.startRappioServer(c);
                            }
                        }}}, 2000);
            } else {
                startRappioServer();
            }
        }
        
        public static void printClassloader(ClassLoader classLoader) {
            System.out.println("CLASSLOADER "+classLoader);
            while(classLoader != null) {
                classLoader = classLoader.getParent();
                System.out.println("   PARENT "+classLoader);
            }
        }
        
        public static void startRappioServer(ClassLoader classLoader) {
            // Only do this once
            if(!STARTED.get()) {
                @SuppressWarnings("rawtypes")
                Class runnableClass;
                try {
                    runnableClass = classLoader.loadClass("org.robotframework.rappio.agent.RappioServer");
                    System.out.println("PERKELE");
                    for(Constructor c:runnableClass.getDeclaredConstructors()){
                        System.out.println("Constructor "+c);
                    }
                    System.out.println("PERKELE2");
                    Constructor ct = runnableClass.getDeclaredConstructor();
                    System.out.println("--");
                    Object r = ct.newInstance();
                    System.out.println("--");
                    Thread someThread = new Thread((Runnable) r);
                    System.out.println("--");
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
                Thread someThread = new Thread(new RappioServer());
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
