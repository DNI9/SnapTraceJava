package com.snaptrace.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton configuration class for SnapTrace application.
 * Manages paths, constants, and application-wide settings.
 */
public class AppConfig {
    
    private static AppConfig instance;
    
    // Application paths
    private final Path appDataPath;
    private final Path sessionsPath;
    
    // Hotkey configurations
    public static final int CAPTURE_HOTKEY_MODIFIERS = java.awt.event.InputEvent.ALT_DOWN_MASK | java.awt.event.InputEvent.SHIFT_DOWN_MASK;
    public static final int CAPTURE_HOTKEY_CODE = java.awt.event.KeyEvent.VK_S; // Alt + Shift + S
    public static final int DASHBOARD_HOTKEY_CODE = java.awt.event.KeyEvent.VK_D; // Alt + Shift + D
    
    // UI Constants
    public static final String APP_NAME = "SnapTrace";
    public static final String APP_VERSION = "1.0.0";
    public static final int THUMBNAIL_SIZE = 150;
    
    // Annotation defaults
    public static final String ANNOTATION_COLOR = "#FF0000"; // Red
    public static final String ANNOTATION_FONT = "Arial";
    public static final int ANNOTATION_FONT_SIZE = 16;
    public static final int RECTANGLE_STROKE_WIDTH = 3;
    
    // File naming
    public static final String METADATA_FILENAME = "metadata.json";
    public static final String IMAGE_EXTENSION = ".png";
    
    private AppConfig() {
        // Determine user home directory
        String userHome = System.getProperty("user.home");
        this.appDataPath = Paths.get(userHome, ".snaptrace");
        this.sessionsPath = appDataPath.resolve("sessions");
    }
    
    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    public Path getAppDataPath() {
        return appDataPath;
    }
    
    public Path getSessionsPath() {
        return sessionsPath;
    }
    
    public Path getSessionPath(String sessionId) {
        return sessionsPath.resolve(sessionId);
    }
    
    public Path getMetadataPath(String sessionId) {
        return getSessionPath(sessionId).resolve(METADATA_FILENAME);
    }
}
