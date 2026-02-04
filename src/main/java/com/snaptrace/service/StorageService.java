package com.snaptrace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.snaptrace.config.AppConfig;
import com.snaptrace.model.Evidence;
import com.snaptrace.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for handling file I/O and JSON serialization.
 * Manages session storage, image saving, and data persistence.
 */
public class StorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    
    private final ObjectMapper objectMapper;
    private final AppConfig config;
    
    public StorageService() {
        this.config = AppConfig.getInstance();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Ensure directories exist
        initializeDirectories();
    }
    
    /**
     * Creates necessary application directories.
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(config.getSessionsPath());
            logger.info("Storage directories initialized at: {}", config.getAppDataPath());
        } catch (IOException e) {
            logger.error("Failed to create storage directories", e);
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }
    
    /**
     * Saves a session's metadata to JSON.
     * @param session The session to save
     */
    public void saveSession(Session session) {
        try {
            Path sessionDir = config.getSessionPath(session.getSessionId());
            Files.createDirectories(sessionDir);
            
            Path metadataPath = config.getMetadataPath(session.getSessionId());
            objectMapper.writeValue(metadataPath.toFile(), session);
            logger.debug("Session saved: {}", session.getSessionId());
        } catch (IOException e) {
            logger.error("Failed to save session: {}", session.getSessionId(), e);
            throw new RuntimeException("Failed to save session", e);
        }
    }
    
    /**
     * Loads a session from its metadata file.
     * @param sessionId The session ID
     * @return The loaded session, or null if not found
     */
    public Session loadSession(String sessionId) {
        try {
            Path metadataPath = config.getMetadataPath(sessionId);
            if (!Files.exists(metadataPath)) {
                logger.warn("Session not found: {}", sessionId);
                return null;
            }
            
            return objectMapper.readValue(metadataPath.toFile(), Session.class);
        } catch (IOException e) {
            logger.error("Failed to load session: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * Loads all sessions from the storage directory.
     * @return List of all sessions, sorted by creation date (newest first)
     */
    public List<Session> loadAllSessions() {
        List<Session> sessions = new ArrayList<>();
        Path sessionsPath = config.getSessionsPath();
        
        if (!Files.exists(sessionsPath)) {
            return sessions;
        }
        
        try (Stream<Path> paths = Files.list(sessionsPath)) {
            List<Path> sessionDirs = paths
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
            
            for (Path sessionDir : sessionDirs) {
                String sessionId = sessionDir.getFileName().toString();
                Session session = loadSession(sessionId);
                if (session != null) {
                    sessions.add(session);
                }
            }
            
            // Sort by creation date, newest first
            sessions.sort(Comparator.comparing(Session::getCreatedAt).reversed());
            
        } catch (IOException e) {
            logger.error("Failed to load sessions", e);
        }
        
        return sessions;
    }
    
    /**
     * Saves an image to a session's directory.
     * @param sessionId The session ID
     * @param image The image to save
     * @param filename The filename for the image
     * @return Path to the saved image
     */
    public Path saveImage(String sessionId, BufferedImage image, String filename) {
        try {
            Path sessionDir = config.getSessionPath(sessionId);
            Files.createDirectories(sessionDir);
            
            Path imagePath = sessionDir.resolve(filename);
            ImageIO.write(image, "PNG", imagePath.toFile());
            logger.debug("Image saved: {}", imagePath);
            
            return imagePath;
        } catch (IOException e) {
            logger.error("Failed to save image: {}", filename, e);
            throw new RuntimeException("Failed to save image", e);
        }
    }
    
    /**
     * Loads an image from a session's directory.
     * @param sessionId The session ID
     * @param filename The image filename
     * @return The loaded BufferedImage, or null if not found
     */
    public BufferedImage loadImage(String sessionId, String filename) {
        try {
            Path imagePath = config.getSessionPath(sessionId).resolve(filename);
            if (!Files.exists(imagePath)) {
                logger.warn("Image not found: {}", imagePath);
                return null;
            }
            
            return ImageIO.read(imagePath.toFile());
        } catch (IOException e) {
            logger.error("Failed to load image: {}", filename, e);
            return null;
        }
    }
    
    /**
     * Gets the path to an image file.
     * @param sessionId The session ID
     * @param filename The image filename
     * @return Path to the image file
     */
    public Path getImagePath(String sessionId, String filename) {
        return config.getSessionPath(sessionId).resolve(filename);
    }
    
    /**
     * Deletes an entire session and all its files.
     * @param sessionId The session ID to delete
     * @return true if deletion was successful
     */
    public boolean deleteSession(String sessionId) {
        try {
            Path sessionDir = config.getSessionPath(sessionId);
            if (!Files.exists(sessionDir)) {
                return false;
            }
            
            // Delete all files in the session directory
            try (Stream<Path> paths = Files.walk(sessionDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.error("Failed to delete: {}", path, e);
                            }
                        });
            }
            
            logger.info("Session deleted: {}", sessionId);
            return true;
        } catch (IOException e) {
            logger.error("Failed to delete session: {}", sessionId, e);
            return false;
        }
    }
    
    /**
     * Deletes a specific evidence item from a session.
     * @param sessionId The session ID
     * @param evidenceId The evidence ID to delete
     * @return true if deletion was successful
     */
    public boolean deleteEvidence(String sessionId, String evidenceId) {
        Session session = loadSession(sessionId);
        if (session == null) {
            return false;
        }
        
        Evidence evidence = session.getEvidenceById(evidenceId);
        if (evidence == null) {
            return false;
        }
        
        // Delete the image file
        try {
            Path imagePath = getImagePath(sessionId, evidence.getFilename());
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            logger.error("Failed to delete image file for evidence: {}", evidenceId, e);
        }
        
        // Remove from session and save
        session.removeEvidence(evidenceId);
        saveSession(session);
        
        logger.info("Evidence deleted: {} from session: {}", evidenceId, sessionId);
        return true;
    }
    
    /**
     * Generates a unique filename for an image based on timestamp.
     * @return Generated filename
     */
    public String generateImageFilename() {
        return System.currentTimeMillis() + AppConfig.IMAGE_EXTENSION;
    }
    
    /**
     * Creates a new session with the given name.
     * @param sessionName The name for the session
     * @return The created session
     */
    public Session createSession(String sessionName) {
        Session session = new Session(sessionName);
        saveSession(session);
        logger.info("New session created: {} ({})", sessionName, session.getSessionId());
        return session;
    }
    
    /**
     * Adds evidence to an existing session.
     * @param sessionId The session ID
     * @param image The screenshot image
     * @param note Optional note for the evidence
     * @return The created Evidence object
     */
    public Evidence addEvidenceToSession(String sessionId, BufferedImage image, String note) {
        Session session = loadSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        String filename = generateImageFilename();
        saveImage(sessionId, image, filename);
        
        Evidence evidence = new Evidence(filename);
        evidence.setNote(note != null ? note : "");
        
        session.addEvidence(evidence);
        saveSession(session);
        
        logger.info("Evidence added to session {}: {}", sessionId, evidence.getId());
        return evidence;
    }
}
