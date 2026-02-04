package com.snaptrace.controller;

import com.snaptrace.config.AppConfig;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the overlay screen capture and annotation interface.
 * Handles drawing rectangles and text annotations on captured screenshots.
 */
public class OverlayController {
    
    private static final Logger logger = LoggerFactory.getLogger(OverlayController.class);
    
    @FXML
    private Pane rootPane;
    
    @FXML
    private Canvas canvas;
    
    private GraphicsContext gc;
    private BufferedImage capturedImage;
    private WritableImage fxImage;
    
    // Drawing state
    private double startX, startY;
    private double currentX, currentY;
    private boolean isDrawing = false;
    
    // Annotation storage
    private List<Annotation> annotations = new ArrayList<>();
    
    // Current tool: "rectangle" or "text"
    private String currentTool = "rectangle";
    
    // Text input field for text tool
    private TextField activeTextField = null;
    
    // Callback for when capture is saved
    private CaptureCallback saveCallback;
    
    // Color configuration
    private final Color annotationColor = Color.web(AppConfig.ANNOTATION_COLOR);
    private final double strokeWidth = AppConfig.RECTANGLE_STROKE_WIDTH;
    
    @FunctionalInterface
    public interface CaptureCallback {
        void onCaptureSaved(BufferedImage image);
    }
    
    /**
     * Represents an annotation (rectangle or text) on the canvas.
     */
    private static class Annotation {
        enum Type { RECTANGLE, TEXT }
        
        Type type;
        double x, y, width, height;
        String text;
        
        static Annotation rectangle(double x, double y, double width, double height) {
            Annotation a = new Annotation();
            a.type = Type.RECTANGLE;
            a.x = x;
            a.y = y;
            a.width = width;
            a.height = height;
            return a;
        }
        
        static Annotation text(double x, double y, String text) {
            Annotation a = new Annotation();
            a.type = Type.TEXT;
            a.x = x;
            a.y = y;
            a.text = text;
            return a;
        }
    }
    
    @FXML
    public void initialize() {
        gc = canvas.getGraphicsContext2D();
        
        // Set up mouse event handlers
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        
        // Set crosshair cursor
        canvas.setCursor(Cursor.CROSSHAIR);
        
        logger.debug("OverlayController initialized");
    }
    
    /**
     * Sets up the overlay with the captured image.
     * @param image The captured screenshot
     */
    public void setCapturedImage(BufferedImage image) {
        this.capturedImage = image;
        this.fxImage = SwingFXUtils.toFXImage(image, null);
        
        // Resize canvas to match image
        canvas.setWidth(image.getWidth());
        canvas.setHeight(image.getHeight());
        
        // Draw the background image
        redraw();
        
        logger.debug("Captured image set: {}x{}", image.getWidth(), image.getHeight());
    }
    
    /**
     * Sets the callback for when a capture is saved.
     * @param callback The callback
     */
    public void setSaveCallback(CaptureCallback callback) {
        this.saveCallback = callback;
    }
    
    /**
     * Sets the current drawing tool.
     * @param tool "rectangle" or "text"
     */
    public void setCurrentTool(String tool) {
        this.currentTool = tool;
        logger.debug("Tool changed to: {}", tool);
    }
    
    /**
     * Handles key press events for save (Ctrl+Enter) and discard (Esc).
     * @param event The key event
     */
    @FXML
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            // Discard and close
            logger.debug("Capture discarded (Esc pressed)");
            closeOverlay();
        } else if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
            // Save and close
            logger.debug("Capture saved (Ctrl+Enter pressed)");
            saveCapture();
        } else if (event.getCode() == KeyCode.R) {
            // Switch to rectangle tool
            setCurrentTool("rectangle");
        } else if (event.getCode() == KeyCode.T) {
            // Switch to text tool
            setCurrentTool("text");
        }
    }
    
    private void handleMousePressed(MouseEvent event) {
        if (activeTextField != null) {
            commitTextField();
        }
        
        startX = event.getX();
        startY = event.getY();
        
        if ("text".equals(currentTool)) {
            // Create text field for input
            createTextField(startX, startY);
        } else {
            isDrawing = true;
        }
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (!isDrawing || "text".equals(currentTool)) {
            return;
        }
        
        currentX = event.getX();
        currentY = event.getY();
        
        // Redraw with preview rectangle
        redraw();
        drawPreviewRectangle();
    }
    
    private void handleMouseReleased(MouseEvent event) {
        if (!isDrawing || "text".equals(currentTool)) {
            return;
        }
        
        currentX = event.getX();
        currentY = event.getY();
        
        // Calculate normalized rectangle (handle negative dimensions)
        double x = Math.min(startX, currentX);
        double y = Math.min(startY, currentY);
        double width = Math.abs(currentX - startX);
        double height = Math.abs(currentY - startY);
        
        // Only add if rectangle has meaningful size
        if (width > 5 && height > 5) {
            annotations.add(Annotation.rectangle(x, y, width, height));
            logger.debug("Rectangle added: ({}, {}) {}x{}", x, y, width, height);
        }
        
        isDrawing = false;
        redraw();
    }
    
    private void createTextField(double x, double y) {
        activeTextField = new TextField();
        activeTextField.setLayoutX(x);
        activeTextField.setLayoutY(y);
        activeTextField.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + AppConfig.ANNOTATION_COLOR + "; " +
            "-fx-font-family: '" + AppConfig.ANNOTATION_FONT + "'; " +
            "-fx-font-size: " + AppConfig.ANNOTATION_FONT_SIZE + "px; " +
            "-fx-border-color: " + AppConfig.ANNOTATION_COLOR + "; " +
            "-fx-border-width: 1px;"
        );
        activeTextField.setPrefWidth(200);
        
        activeTextField.setOnAction(e -> commitTextField());
        activeTextField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                cancelTextField();
            }
        });
        
        rootPane.getChildren().add(activeTextField);
        activeTextField.requestFocus();
    }
    
    private void commitTextField() {
        if (activeTextField != null && !activeTextField.getText().isEmpty()) {
            annotations.add(Annotation.text(
                activeTextField.getLayoutX(), 
                activeTextField.getLayoutY() + AppConfig.ANNOTATION_FONT_SIZE,
                activeTextField.getText()
            ));
            logger.debug("Text added: '{}' at ({}, {})", 
                activeTextField.getText(), 
                activeTextField.getLayoutX(), 
                activeTextField.getLayoutY());
        }
        removeTextField();
        redraw();
    }
    
    private void cancelTextField() {
        removeTextField();
    }
    
    private void removeTextField() {
        if (activeTextField != null) {
            rootPane.getChildren().remove(activeTextField);
            activeTextField = null;
            canvas.requestFocus();
        }
    }
    
    /**
     * Redraws the canvas with background image and all annotations.
     */
    private void redraw() {
        if (fxImage == null) return;
        
        // Draw background image
        gc.drawImage(fxImage, 0, 0);
        
        // Draw all annotations
        gc.setStroke(annotationColor);
        gc.setFill(annotationColor);
        gc.setLineWidth(strokeWidth);
        gc.setFont(javafx.scene.text.Font.font(AppConfig.ANNOTATION_FONT, AppConfig.ANNOTATION_FONT_SIZE));
        
        for (Annotation annotation : annotations) {
            if (annotation.type == Annotation.Type.RECTANGLE) {
                gc.strokeRect(annotation.x, annotation.y, annotation.width, annotation.height);
            } else if (annotation.type == Annotation.Type.TEXT) {
                gc.fillText(annotation.text, annotation.x, annotation.y);
            }
        }
    }
    
    /**
     * Draws the preview rectangle while dragging.
     */
    private void drawPreviewRectangle() {
        double x = Math.min(startX, currentX);
        double y = Math.min(startY, currentY);
        double width = Math.abs(currentX - startX);
        double height = Math.abs(currentY - startY);
        
        gc.setStroke(annotationColor);
        gc.setLineWidth(strokeWidth);
        gc.setLineDashes(5);
        gc.strokeRect(x, y, width, height);
        gc.setLineDashes(null);
    }
    
    /**
     * Saves the annotated capture and invokes the callback.
     */
    private void saveCapture() {
        if (activeTextField != null) {
            commitTextField();
        }
        
        // Create final image from canvas
        WritableImage snapshot = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, snapshot);
        BufferedImage finalImage = SwingFXUtils.fromFXImage(snapshot, null);
        
        if (saveCallback != null) {
            saveCallback.onCaptureSaved(finalImage);
        }
        
        closeOverlay();
    }
    
    /**
     * Closes the overlay window.
     */
    private void closeOverlay() {
        Stage stage = (Stage) canvas.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Clears all annotations.
     */
    public void clearAnnotations() {
        annotations.clear();
        redraw();
    }
}
