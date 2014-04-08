/*
 * Copyright 2014 mkorpela.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robotframework.rappio.agent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
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

public class RappioServer implements Runnable {

    private final int port;
    private static final String LOCALHOST = "127.0.0.1";
    private PrintStream out = System.out;
    
    public RappioServer(int port) {
        this.port = port;
    }
    
    public void run() {
            try {
                 noOutput();
                 RemoteServer server = new DaemonRemoteServer();
                 server.putLibrary("/RPC2", new SwingLibrary());
                 server.setPort(0);
                 server.setAllowStop(true);
                 server.start();
                 if(AppContext.getAppContext() == null){
                     SunToolkit.createNewAppContext();
                 }
                 final Integer actualPort = server.getLocalPort();
                 // Find a better solution to get rid of small
                 // java processes that are started when for example
                 // javaws -uninstall is executed.
                 Timer t = new Timer(true);
                 t.schedule(new TimerTask() {
                     @Override
                     public void run() {
                         Socket echoSocket;
                         try {
                             echoSocket = new Socket(LOCALHOST, port);
                             PrintWriter outToServer = new PrintWriter(echoSocket.getOutputStream(), true);
                             outToServer.write(actualPort.toString());
                             outToServer.close();
                             echoSocket.close();
                         } catch (IOException ex) {

                         }
                     }
                 }, 1000);
             } catch (Exception e) {
                     System.err.println("Error starting remote server");
                     e.printStackTrace();
             }finally{
                     System.setOut(out);
             }
    }
    
    // Silence stdout, some clients expect the output to be valid XML
    private void noOutput() {
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
