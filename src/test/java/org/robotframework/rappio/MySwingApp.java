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

package org.robotframework.rappio;

import java.awt.BorderLayout;
import java.awt.Label;
import javax.swing.JFrame;


public class MySwingApp {
    
    public MySwingApp() {
        JFrame frame = new JFrame("My Swing App");
        frame.getContentPane().add(new Label("Hello kitty"), BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
    
    public static void main(String[] args) {
        MySwingApp app = new MySwingApp();
    }
    
}
