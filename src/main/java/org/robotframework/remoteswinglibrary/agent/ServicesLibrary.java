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
        String envi = "";
        for(final Map.Entry<String, String> entry : System.getenv().entrySet())
        {
            envi = envi + entry.getKey() + " : " + entry.getValue() + "\n";
        }
        for(final Map.Entry<Object,Object> property : System.getProperties().entrySet()){
            envi = envi + property.getKey() + " : " + property.getValue() + "\n";
        }
        return envi;
    }
}
