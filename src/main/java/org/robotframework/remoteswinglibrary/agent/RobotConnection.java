package org.robotframework.remoteswinglibrary.agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class RobotConnection {

    String host;;
    int port;
    Socket echoSocket;
    PrintWriter outToServer;

    public RobotConnection(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        echoSocket = new Socket(host, port);
        outToServer = new PrintWriter(echoSocket.getOutputStream(), true);
    }

    public void close() throws IOException {
        outToServer.close();
        echoSocket.close();
    }

    public void send(String msg) {
        outToServer.write(msg + "\n");
    }
}
