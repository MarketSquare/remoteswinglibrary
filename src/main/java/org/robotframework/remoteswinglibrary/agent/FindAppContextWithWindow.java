/*
 * Copyright 2014 Nokia Solutions and Networks Oyj
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

package org.robotframework.remoteswinglibrary.agent;

import org.robotframework.swing.SwingLibrary;
import sun.awt.AppContext;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;


class FindAppContextWithWindow implements Runnable {

    String host;
    int port;
    int apport;
    boolean debug;
    boolean closeSecurityDialogs;
    RobotConnection robotConnection;

    HashMap<Dialog, SecurityDialogAccepter> dialogs = new HashMap<Dialog, SecurityDialogAccepter>();

    public FindAppContextWithWindow(String host, int port, int apport, boolean debug, boolean closeSecurityDialogs) {
        this.host = host;
        this.port = port;
        this.apport = apport;
        this.debug = debug;
        this.closeSecurityDialogs = closeSecurityDialogs;
    }

    public void run()  {
        try {
            robotConnection = new RobotConnection(host, port);
            AppContext appContext = getAppContextWithWindow();
            sun.awt.SunToolkit.invokeLaterOnAppContext(appContext, new ServerThread(robotConnection, apport, debug));
        } catch (Exception e) {
            if (debug) {
                e.printStackTrace();
                System.err.println(e);
                System.err.println("Error starting remote server");
            }
        }
    }

    public AppContext getAppContextWithWindow() throws Exception {
        while (true) {
            Set<AppContext> ctxs = AppContext.getAppContexts();
            for (AppContext ctx:ctxs) {
                if (hasMainWindow(ctx)) {
                    return ctx;
                }
            }
            for (Map.Entry<Dialog, SecurityDialogAccepter> entry: dialogs.entrySet()) {
                Dialog dialog = entry.getKey();
                SecurityDialogAccepter accepter = entry.getValue();
                if (accepter.attempts > 0 && !accepter.running && !accepter.done) {
                    accepter.running = true;
                    sun.awt.SunToolkit.invokeLaterOnAppContext(accepter.ctx, accepter);
                }
            }
            Thread.sleep(1000);
        }

    }

    public boolean hasMainWindow(AppContext ctx) {
        Vector<WeakReference<Window>> windowList =
              (Vector<WeakReference<Window>>)ctx.get(Window.class);
        if (windowList == null)
            return false;
        // make a copy of the vector to prevent concurrency errors.
        windowList = new Vector<WeakReference<Window>>(windowList);
        for (WeakReference<Window> ref:windowList) {
            Window window = ref.get();
            if (debug) logWindowDetails("Trying to connect to", window);
            if (closeSecurityDialogs && window instanceof Dialog) {
                Dialog dialog = (Dialog) window;
                if (!dialogs.containsKey(dialog)) {
                    SecurityDialogAccepter accepter = new SecurityDialogAccepter(dialog, ctx, robotConnection);
                    dialogs.put(dialog, accepter);
                }
            }
            if (isFrame(window)
                && window.isVisible()
                && !isConsoleWindow(window)) {
                if (debug) logWindowDetails("Connected to", window);
                dialogs.clear();
                return true;
            }
        }
        return false;
    }

    private void logWindowDetails(String message, Window window) {
        System.err.println(message + " Class:" + window.getClass().getName()
                + " Name:" + window.getName()
                + " Visible:" + window.isVisible()
                + " AppContext:" + AppContext.getAppContext());
    }

    private boolean isFrame(Window window) {
        return window instanceof Frame;
    }

    private boolean isConsoleWindow(Window window) {
        return window.getClass().getName().contains("ConsoleWindow");
    }

    private class SecurityDialogAccepter implements Runnable {

        public boolean running = false;
        private AppContext ctx;
        public Dialog dialog;
        private RobotConnection robotConnection;
        public int attempts = 5;
        public boolean done = false;
        SwingLibrary lib;

        public SecurityDialogAccepter(Dialog dialog, AppContext ctx, RobotConnection robotConnection) {
            this.dialog = dialog;
            this.ctx = ctx;
            this.robotConnection = robotConnection;
            this.lib = SwingLibrary.instance == null ? new SwingLibrary() : SwingLibrary.instance;
        }

        public void run() {
            try {
                ArrayList<String> dialogTitles = new ArrayList<String>();
                dialogTitles.add("Security Warning");
                dialogTitles.add("Security Information");
                dialogTitles.add("Install Java Extension");
                dialogTitles.add("Sicherheitsinformationen");

                String title = dialog.getTitle();
                System.err.println(String.format("Handling Dialog '%s'.",
                        dialog.getTitle()));

                if (dialogTitles.contains(title)) {
                    lib = new SwingLibrary();
                    lib.runKeyword("select_dialog", new Object[]{title});
                    long oldTimeout = (Long) lib.runKeyword("Set Jemmy Timeout",
                            new Object[]{"ComponentOperator.WaitComponentTimeout", "100ms"});
                    checkCheckboxes();
                    clickAcceptButton();
                    lib.runKeyword("Set Jemmy Timeout",
                            new Object[]{"ComponentOperator.WaitComponentTimeout", oldTimeout});

                    System.err.println(String.format("Security Warning Dialog '%s' has been accepted",
                            dialog.getTitle()));
                    robotConnection.connect();
                    robotConnection.send("DIALOG:" + title);
                    robotConnection.close();
                    this.done = true;
                }
                else
                    System.err.println("Unrecognized dialog, skipping.");
            } catch (Throwable t) {
                System.err.println(String.format("Accepting Security Warning Dialog '%s' has failed.",
                        dialog.getTitle()));
                t.printStackTrace();
            }
            running = false;
            attempts--;
        }

        private void checkCheckboxes() {
            ArrayList<String> checkboxList = new ArrayList<String>();
            checkboxList.add("I accept the risk and want to run this application.");;

            for (String text: checkboxList) {
                try {
                    lib.runKeyword("check_check_box", new Object[]{text});
                    return;
                }
                catch (Throwable t) {
                }
            }
        }

        private void clickAcceptButton() {
            ArrayList<String> acceptButtonList = new ArrayList<String>();
            acceptButtonList.add("Run");
            acceptButtonList.add("Continue");
            acceptButtonList.add("Install");
            acceptButtonList.add("Ausführen");

            for (String text: acceptButtonList) {
                try {
                    lib.runKeyword("push_button", new Object[]{text});
                    return;
                }
                catch (Throwable t) {
                }
            }
        }
    }
}
