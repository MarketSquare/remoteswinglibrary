package org.robotframework.rappio.agent;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class RappioServicesLibrary {

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
}
