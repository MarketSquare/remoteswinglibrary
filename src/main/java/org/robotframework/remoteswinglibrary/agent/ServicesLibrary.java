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

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;

public class ServicesLibrary {

    public void systemExit(int exitCode) {
        System.exit(exitCode);
    }
    
    public String ping() {
        return "pong";
    }
    
    public void takeScreenshot(String filepath) throws IOException, AWTException {
        BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        ImageIO.write(image, "png", new File(filepath));
    }
    
    public String getEnvironment() {
        String envi = "System.getenv():\n";
        for(final Map.Entry<String, String> entry : System.getenv().entrySet())
        {
            envi = envi + entry.getKey() + " : " + entry.getValue() + "\n";
        }
        envi = envi + "\nSystem.getProperties():\n";
        for(final Map.Entry<Object,Object> property : System.getProperties().entrySet()){
            envi = envi + property.getKey() + " : " + property.getValue() + "\n";
        }
        return envi;
    }
}
