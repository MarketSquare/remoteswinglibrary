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
import java.util.Set;
import java.util.Vector;


class FindAppContextWithWindow implements Runnable {

    String host;
    int port;
    boolean debug;

    public FindAppContextWithWindow(String host, int port, boolean debug) {
        this.host = host;
        this.port = port;
        this.debug = debug;
    }

    public void run()  {
        try {
            sun.awt.SunToolkit.invokeLaterOnAppContext(getAppContextWithWindow(), new ServerThread(host, port, debug));
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
        boolean alreadyAccepting = false;
        for (WeakReference<Window> ref:windowList) {
            Window window = ref.get();
            if (debug) logWindowDetails("Trying to connect to", window);
            if (window instanceof Dialog && !alreadyAccepting) {
                System.err.println("DIALOG TITLE IS:: " + ((Dialog) window).getTitle());
                SecurityContextAccepter accepter = new SecurityContextAccepter();
                sun.awt.SunToolkit.invokeLaterOnAppContext(ctx, accepter);
                while (!accepter.done) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            }
            if (isFrame(window)
                && window.isVisible()
                && !isConsoleWindow(window)) {
                if (debug) logWindowDetails("Connected to", window);
                return true;
            }
        }
        return false;
    }

    private void logWindowDetails(String message, Window window) {
        System.err.println(message+" Class:"+window.getClass().getName()
                + " Name:"+window.getName()
                + " Visible:"+window.isVisible()
                + " AppContext:"+AppContext.getAppContext());
    }

    private boolean isFrame(Window window) {
        return window instanceof Frame;
    }

    private boolean isConsoleWindow(Window window) {
        return window.getClass().getName().contains("ConsoleWindow");
    }

    private class SecurityContextAccepter implements Runnable {

        public boolean done = false;

        public void run() {
            try {
                SwingLibrary lib = new SwingLibrary();
                lib.runKeyword("select_dialog", new Object[]{"0"});
                lib.runKeyword("check_check_box", new Object[]{"0"});
                //boolean enabled = false;
                //while (!enabled) {
                //    enabled = isEnabledButton(lib);
                //}
                lib.runKeyword("push_button", new Object[]{"Run"});
            } finally {
                this.done = true;
            }
        }
        private boolean isEnabledButton(SwingLibrary lib) {
            try {
                lib.runKeyword("button_should_be_enabled", new Object[]{"Run"});
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
    }
}
