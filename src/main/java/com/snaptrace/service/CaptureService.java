package com.snaptrace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for capturing screen content using java.awt.Robot.
 * Supports multi-monitor setups.
 */
public class CaptureService {
    
    private static final Logger logger = LoggerFactory.getLogger(CaptureService.class);
    
    private Robot robot;
    
    public CaptureService() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            logger.error("Failed to initialize Robot for screen capture", e);
            throw new RuntimeException("Failed to initialize screen capture", e);
        }
    }
    
    /**
     * Captures the entire screen (all monitors combined).
     * @return BufferedImage of the entire screen area
     */
    public BufferedImage captureFullScreen() {
        Rectangle screenBounds = getFullScreenBounds();
        logger.debug("Capturing full screen: {}", screenBounds);
        return robot.createScreenCapture(screenBounds);
    }
    
    /**
     * Captures a specific region of the screen.
     * @param bounds The rectangle bounds to capture
     * @return BufferedImage of the specified region
     */
    public BufferedImage captureRegion(Rectangle bounds) {
        logger.debug("Capturing region: {}", bounds);
        return robot.createScreenCapture(bounds);
    }
    
    /**
     * Captures a specific monitor.
     * @param monitorIndex The index of the monitor (0-based)
     * @return BufferedImage of the specified monitor
     */
    public BufferedImage captureMonitor(int monitorIndex) {
        GraphicsDevice[] screens = getScreenDevices();
        if (monitorIndex < 0 || monitorIndex >= screens.length) {
            throw new IllegalArgumentException("Invalid monitor index: " + monitorIndex);
        }
        
        Rectangle bounds = screens[monitorIndex].getDefaultConfiguration().getBounds();
        logger.debug("Capturing monitor {}: {}", monitorIndex, bounds);
        return robot.createScreenCapture(bounds);
    }
    
    /**
     * Gets the bounds of the full virtual screen (all monitors).
     * @return Rectangle representing the full screen bounds
     */
    public Rectangle getFullScreenBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            virtualBounds = virtualBounds.union(gc.getBounds());
        }
        
        return virtualBounds;
    }
    
    /**
     * Gets all available screen devices.
     * @return Array of GraphicsDevice
     */
    public GraphicsDevice[] getScreenDevices() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
    }
    
    /**
     * Gets the bounds of each individual monitor.
     * @return List of Rectangle bounds for each monitor
     */
    public List<Rectangle> getMonitorBounds() {
        List<Rectangle> bounds = new ArrayList<>();
        GraphicsDevice[] screens = getScreenDevices();
        
        for (GraphicsDevice screen : screens) {
            bounds.add(screen.getDefaultConfiguration().getBounds());
        }
        
        return bounds;
    }
    
    /**
     * Gets the number of available monitors.
     * @return Number of monitors
     */
    public int getMonitorCount() {
        return getScreenDevices().length;
    }
    
    /**
     * Gets the primary monitor's bounds.
     * @return Rectangle of the primary monitor
     */
    public Rectangle getPrimaryMonitorBounds() {
        GraphicsDevice primaryScreen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return primaryScreen.getDefaultConfiguration().getBounds();
    }
}
