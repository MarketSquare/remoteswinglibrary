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

        private static PrintStream out;
    
	public static void premain(String agentArgument, Instrumentation instrumentation){
            noOutput();
            int port = Integer.parseInt(agentArgument);
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
            RemoteServer server = new DaemonRemoteServer();
            server.putLibrary("/RPC2", new SwingLibrary());

            server.setPort(port);
            server.setAllowStop(true);
            try {
                server.start();
            } catch (Exception e) {
                System.out.println("TODODOODODODODOODODODODO!!!");
            }
            AppContext.getAppContext();
            System.setOut(out);
	}

        private static void noOutput() {
            out = System.out;
            System.setOut(new PrintStream(new OutputStream() {
                 public void write(int b) {
                     //DO NOTHING
                 }
             }));
        }
}
