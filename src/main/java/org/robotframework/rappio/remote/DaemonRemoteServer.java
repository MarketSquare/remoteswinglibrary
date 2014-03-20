package org.robotframework.rappio.remote;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.robotframework.remoteserver.RemoteServer;

public class DaemonRemoteServer extends RemoteServer{
    
    public DaemonRemoteServer() {
        super();
        QueuedThreadPool pool = new QueuedThreadPool();
        pool.setDaemon(true);
        this.server.setThreadPool(pool);
    }
        
}
