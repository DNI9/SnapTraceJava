package com.snaptrace;

import com.snaptrace.config.AppConfig;
import com.snaptrace.controller.DashboardController;
import com.snaptrace.controller.OverlayController;
import com.snaptrace.service.CaptureService;
import com.snaptrace.service.HotkeyService;
import com.snaptrace.service.StorageService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Main entry point for the SnapTrace application.
 * Manages system tray integration, global hotkeys, and window lifecycle.
 */
public class Main extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    private HotkeyService hotkeyService;
    private CaptureService captureService;
    private StorageService storageService;
    
    private Stage dashboardStage;
    private DashboardController dashboardController;
    
    private TrayIcon trayIcon;
    private boolean isCapturing = false;
    
    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting SnapTrace v{}", AppConfig.APP_VERSION);
        
        // Initialize services
        initializeServices();
        
        // Set up the dashboard window
        setupDashboard(primaryStage);
        
        // Set up system tray
        setupSystemTray();
        
        // Register global hotkeys
        registerHotkeys();
        
        // Prevent app from closing when windows are hidden
        Platform.setImplicitExit(false);
        
        // Show dashboard on first launch
        showDashboard();
        
        logger.info("SnapTrace started successfully");
    }
    
    private void initializeServices() {
        captureService = new CaptureService();
        storageService = new StorageService();
        hotkeyService = new HotkeyService();
    }
    
    private void setupDashboard(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/snaptrace/view/dashboard.fxml"));
            Parent root = loader.load();
            dashboardController = loader.getController();
            
            dashboardStage = stage;
            dashboardStage.setTitle("SnapTrace - Evidence Collector");
            dashboardStage.setScene(new Scene(root));
            
            // Set app icon if available
            try {
                dashboardStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/snaptrace.png")));
            } catch (Exception e) {
                logger.debug("App icon not found, using default");
            }
            
            // Handle window close - minimize to tray instead
            dashboardStage.setOnCloseRequest(e -> {
                e.consume();
                hideDashboard();
            });
            
        } catch (IOException e) {
            logger.error("Failed to load dashboard FXML", e);
            throw new RuntimeException("Failed to initialize dashboard", e);
        }
    }
    
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            logger.warn("System tray not supported on this platform");
            return;
        }
        
        try {
            SystemTray tray = SystemTray.getSystemTray();
            
            // Create tray icon image
            java.awt.Image trayImage = createTrayIconImage();
            
            // Create popup menu
            PopupMenu popup = new PopupMenu();
            
            MenuItem captureItem = new MenuItem("Capture (Alt+Shift+S)");
            captureItem.addActionListener(e -> Platform.runLater(this::triggerCapture));
            
            MenuItem dashboardItem = new MenuItem("Open Dashboard (Alt+Shift+D)");
            dashboardItem.addActionListener(e -> Platform.runLater(this::showDashboard));
            
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> exitApplication());
            
            popup.add(captureItem);
            popup.add(dashboardItem);
            popup.addSeparator();
            popup.add(exitItem);
            
            // Create tray icon
            trayIcon = new TrayIcon(trayImage, "SnapTrace", popup);
            trayIcon.setImageAutoSize(true);
            
            // Double-click to open dashboard
            trayIcon.addActionListener(e -> Platform.runLater(this::showDashboard));
            
            tray.add(trayIcon);
            logger.info("System tray icon added");
            
        } catch (AWTException e) {
            logger.error("Failed to add system tray icon", e);
        }
    }
    
    private java.awt.Image createTrayIconImage() {
        // Try to load custom icon
        try {
            URL iconUrl = getClass().getResource("/icons/snaptrace-tray.png");
            if (iconUrl != null) {
                return ImageIO.read(iconUrl);
            }
        } catch (Exception e) {
            logger.debug("Custom tray icon not found");
        }
        
        // Create a simple default icon (red camera shape)
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(220, 53, 69)); // Red
        g.fillRoundRect(1, 3, 14, 10, 3, 3);
        g.setColor(Color.WHITE);
        g.fillOval(5, 5, 6, 6);
        g.setColor(new Color(220, 53, 69));
        g.fillOval(7, 7, 2, 2);
        g.dispose();
        return image;
    }
    
    private void registerHotkeys() {
        hotkeyService.setCaptureCallback(() -> Platform.runLater(this::triggerCapture));
        hotkeyService.setDashboardCallback(() -> Platform.runLater(this::toggleDashboard));
        
        try {
            hotkeyService.register();
        } catch (Exception e) {
            logger.error("Failed to register global hotkeys", e);
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING
                );
                alert.setTitle("Hotkey Registration Failed");
                alert.setHeaderText("Could not register global hotkeys");
                alert.setContentText("SnapTrace will still work, but you'll need to use the system tray menu instead of keyboard shortcuts.\n\nYou may need to grant accessibility permissions to this application.");
                alert.showAndWait();
            });
        }
    }
    
    private void triggerCapture() {
        if (isCapturing) {
            logger.debug("Capture already in progress, ignoring");
            return;
        }
        
        isCapturing = true;
        logger.debug("Capture triggered");
        
        try {
            // Capture the screen
            BufferedImage screenshot = captureService.captureFullScreen();
            Rectangle screenBounds = captureService.getFullScreenBounds();
            
            // Show overlay for annotation
            showOverlay(screenshot, screenBounds);
            
        } catch (Exception e) {
            logger.error("Failed to capture screen", e);
            isCapturing = false;
        }
    }
    
    private void showOverlay(BufferedImage screenshot, Rectangle bounds) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/snaptrace/view/overlay.fxml"));
            Parent root = loader.load();
            OverlayController overlayController = loader.getController();
            
            // Set up the captured image
            overlayController.setCapturedImage(screenshot);
            
            // Set up save callback
            overlayController.setSaveCallback(annotatedImage -> {
                saveCapture(annotatedImage);
                isCapturing = false;
            });
            
            // Create fullscreen transparent stage
            Stage overlayStage = new Stage();
            overlayStage.initStyle(StageStyle.TRANSPARENT);
            overlayStage.setAlwaysOnTop(true);
            
            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            
            // Handle key events at scene level
            scene.setOnKeyPressed(overlayController::handleKeyPressed);
            
            overlayStage.setScene(scene);
            
            // Position to cover all screens
            overlayStage.setX(bounds.x);
            overlayStage.setY(bounds.y);
            overlayStage.setWidth(bounds.width);
            overlayStage.setHeight(bounds.height);
            
            // Handle close
            overlayStage.setOnHidden(e -> isCapturing = false);
            
            overlayStage.show();
            overlayStage.requestFocus();
            root.requestFocus();
            
            logger.debug("Overlay shown at ({}, {}) size {}x{}", 
                bounds.x, bounds.y, bounds.width, bounds.height);
            
        } catch (IOException e) {
            logger.error("Failed to show overlay", e);
            isCapturing = false;
        }
    }
    
    private void saveCapture(BufferedImage image) {
        Platform.runLater(() -> {
            // Prompt for note (optional)
            javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Add Note");
            dialog.setHeaderText("Add a note for this screenshot (optional)");
            dialog.setContentText("Note:");
            
            String note = dialog.showAndWait().orElse("");
            
            // Save to current session
            dashboardController.addEvidenceToCurrentSession(image, note);
            
            // Show notification
            if (trayIcon != null) {
                trayIcon.displayMessage("SnapTrace", "Screenshot saved!", TrayIcon.MessageType.INFO);
            }
            
            logger.info("Screenshot saved with note: '{}'", note);
        });
    }
    
    private void showDashboard() {
        if (dashboardStage != null) {
            dashboardStage.show();
            dashboardStage.toFront();
            dashboardStage.requestFocus();
            dashboardController.refreshSessions();
        }
    }
    
    private void hideDashboard() {
        if (dashboardStage != null) {
            dashboardStage.hide();
        }
    }
    
    private void toggleDashboard() {
        if (dashboardStage != null) {
            if (dashboardStage.isShowing()) {
                hideDashboard();
            } else {
                showDashboard();
            }
        }
    }
    
    private void exitApplication() {
        logger.info("Exiting SnapTrace");
        
        // Unregister hotkeys
        if (hotkeyService != null) {
            hotkeyService.unregister();
        }
        
        // Remove tray icon
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        
        // Exit
        Platform.exit();
        System.exit(0);
    }
    
    @Override
    public void stop() {
        logger.info("Application stopping");
        if (hotkeyService != null) {
            hotkeyService.unregister();
        }
    }
    
    public static void main(String[] args) {
        // Set system properties for better rendering
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        
        launch(args);
    }
}
