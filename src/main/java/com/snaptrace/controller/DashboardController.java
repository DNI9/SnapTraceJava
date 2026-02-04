package com.snaptrace.controller;

import com.snaptrace.config.AppConfig;
import com.snaptrace.model.Evidence;
import com.snaptrace.model.Session;
import com.snaptrace.service.ExportService;
import com.snaptrace.service.StorageService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller for the main dashboard UI.
 * Manages session list, evidence grid, and export functionality.
 */
public class DashboardController {
    
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    @FXML private ListView<Session> sessionListView;
    @FXML private FlowPane evidenceGrid;
    @FXML private Label sessionTitleLabel;
    @FXML private Label sessionInfoLabel;
    @FXML private Button newSessionButton;
    @FXML private Button exportButton;
    @FXML private Button deleteSessionButton;
    @FXML private ScrollPane evidenceScrollPane;
    
    private StorageService storageService;
    private ExportService exportService;
    private ObservableList<Session> sessions;
    private Session currentSession;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    
    @FXML
    public void initialize() {
        // Initialize services
        storageService = new StorageService();
        exportService = new ExportService(storageService);
        
        // Set up session list view
        setupSessionListView();
        
        // Load sessions
        refreshSessions();
        
        // Clear evidence grid initially
        clearEvidenceDisplay();
        
        logger.info("Dashboard initialized");
    }
    
    private void setupSessionListView() {
        sessions = FXCollections.observableArrayList();
        sessionListView.setItems(sessions);
        
        // Custom cell factory for session display
        sessionListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Session session, boolean empty) {
                super.updateItem(session, empty);
                if (empty || session == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox container = new VBox(2);
                    Label nameLabel = new Label(session.getSessionName());
                    nameLabel.setStyle("-fx-font-weight: bold;");
                    
                    Label dateLabel = new Label(dateFormatter.format(session.getCreatedAt()));
                    dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                    
                    Label countLabel = new Label(session.getEvidenceCount() + " screenshots");
                    countLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");
                    
                    container.getChildren().addAll(nameLabel, dateLabel, countLabel);
                    setGraphic(container);
                }
            }
        });
        
        // Selection listener
        sessionListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    displaySession(newVal);
                }
            }
        );
    }
    
    /**
     * Refreshes the session list from storage.
     */
    public void refreshSessions() {
        sessions.clear();
        sessions.addAll(storageService.loadAllSessions());
        
        if (!sessions.isEmpty() && currentSession == null) {
            sessionListView.getSelectionModel().selectFirst();
        }
        
        logger.debug("Sessions refreshed: {} sessions loaded", sessions.size());
    }
    
    /**
     * Displays a session's evidence in the grid.
     */
    private void displaySession(Session session) {
        this.currentSession = session;
        
        // Update header
        sessionTitleLabel.setText(session.getSessionName());
        sessionInfoLabel.setText(String.format("Created: %s | %d screenshots",
                dateFormatter.format(session.getCreatedAt()),
                session.getEvidenceCount()));
        
        // Enable buttons
        exportButton.setDisable(false);
        deleteSessionButton.setDisable(false);
        
        // Clear and populate evidence grid
        evidenceGrid.getChildren().clear();
        
        for (Evidence evidence : session.getEvidenceList()) {
            evidenceGrid.getChildren().add(createEvidenceTile(session, evidence));
        }
        
        logger.debug("Displaying session: {}", session.getSessionName());
    }
    
    /**
     * Creates a tile for displaying an evidence item.
     */
    private VBox createEvidenceTile(Session session, Evidence evidence) {
        VBox tile = new VBox(5);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(5));
        tile.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 4;");
        tile.setPrefWidth(AppConfig.THUMBNAIL_SIZE + 20);
        
        // Thumbnail image
        ImageView thumbnail = new ImageView();
        thumbnail.setFitWidth(AppConfig.THUMBNAIL_SIZE);
        thumbnail.setFitHeight(AppConfig.THUMBNAIL_SIZE);
        thumbnail.setPreserveRatio(true);
        
        // Load image asynchronously
        Path imagePath = storageService.getImagePath(session.getSessionId(), evidence.getFilename());
        try {
            Image image = new Image(imagePath.toUri().toString(), 
                    AppConfig.THUMBNAIL_SIZE, AppConfig.THUMBNAIL_SIZE, true, true, true);
            thumbnail.setImage(image);
        } catch (Exception e) {
            logger.warn("Failed to load thumbnail: {}", evidence.getFilename());
        }
        
        // Timestamp label
        Label timeLabel = new Label(dateFormatter.format(evidence.getTimestamp()));
        timeLabel.setStyle("-fx-font-size: 10px;");
        
        // Note label (truncated)
        String noteText = evidence.getNote();
        if (noteText != null && noteText.length() > 20) {
            noteText = noteText.substring(0, 17) + "...";
        }
        Label noteLabel = new Label(noteText != null && !noteText.isEmpty() ? noteText : "No note");
        noteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
        
        // Delete button
        Button deleteBtn = new Button("×");
        deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 6;");
        deleteBtn.setOnAction(e -> deleteEvidence(session, evidence));
        
        tile.getChildren().addAll(thumbnail, timeLabel, noteLabel, deleteBtn);
        
        // Double-click to view full size
        tile.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                viewFullImage(session, evidence);
            }
        });
        
        return tile;
    }
    
    private void clearEvidenceDisplay() {
        sessionTitleLabel.setText("Select a Session");
        sessionInfoLabel.setText("");
        evidenceGrid.getChildren().clear();
        exportButton.setDisable(true);
        deleteSessionButton.setDisable(true);
        currentSession = null;
    }
    
    @FXML
    private void handleNewSession() {
        TextInputDialog dialog = new TextInputDialog("New Test Session");
        dialog.setTitle("New Session");
        dialog.setHeaderText("Create a new testing session");
        dialog.setContentText("Session name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Session session = storageService.createSession(name.trim());
                refreshSessions();
                sessionListView.getSelectionModel().select(session);
                logger.info("New session created: {}", name);
            }
        });
    }
    
    @FXML
    private void handleExport() {
        if (currentSession == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Session to Word");
        fileChooser.setInitialFileName(currentSession.getSessionName() + ".docx");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Word Document", "*.docx"));
        
        Stage stage = (Stage) exportButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        
        if (file != null) {
            try {
                exportService.exportToWord(currentSession, file.toPath());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Session exported to:\n" + file.getAbsolutePath());
                logger.info("Session exported: {}", file.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed",
                        "Failed to export session: " + e.getMessage());
                logger.error("Export failed", e);
            }
        }
    }
    
    @FXML
    private void handleDeleteSession() {
        if (currentSession == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Session");
        confirm.setHeaderText("Delete session: " + currentSession.getSessionName());
        confirm.setContentText("This will permanently delete all screenshots in this session. Continue?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            storageService.deleteSession(currentSession.getSessionId());
            clearEvidenceDisplay();
            refreshSessions();
            logger.info("Session deleted: {}", currentSession.getSessionName());
        }
    }
    
    private void deleteEvidence(Session session, Evidence evidence) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Screenshot");
        confirm.setContentText("Delete this screenshot?");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            storageService.deleteEvidence(session.getSessionId(), evidence.getId());
            // Reload session
            Session updated = storageService.loadSession(session.getSessionId());
            if (updated != null) {
                displaySession(updated);
                refreshSessions();
            }
        }
    }
    
    private void viewFullImage(Session session, Evidence evidence) {
        Path imagePath = storageService.getImagePath(session.getSessionId(), evidence.getFilename());
        
        Stage imageStage = new Stage();
        imageStage.setTitle(evidence.getFilename());
        
        ImageView imageView = new ImageView(new Image(imagePath.toUri().toString()));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(800);
        imageView.setFitHeight(600);
        
        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        imageStage.setScene(new javafx.scene.Scene(scrollPane, 850, 650));
        imageStage.show();
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    /**
     * Gets the current active session.
     */
    public Session getCurrentSession() {
        return currentSession;
    }
    
    /**
     * Adds evidence to the current session and refreshes display.
     */
    public void addEvidenceToCurrentSession(java.awt.image.BufferedImage image, String note) {
        if (currentSession == null) {
            // Create a default session if none exists
            currentSession = storageService.createSession("Session " + 
                    java.time.LocalDate.now().toString());
            refreshSessions();
        }
        
        storageService.addEvidenceToSession(currentSession.getSessionId(), image, note);
        
        // Refresh display
        Platform.runLater(() -> {
            Session updated = storageService.loadSession(currentSession.getSessionId());
            if (updated != null) {
                displaySession(updated);
                refreshSessions();
            }
        });
    }
}
