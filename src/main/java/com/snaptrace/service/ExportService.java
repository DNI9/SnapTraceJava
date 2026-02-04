package com.snaptrace.service;

import com.snaptrace.config.AppConfig;
import com.snaptrace.model.Evidence;
import com.snaptrace.model.Session;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Service for exporting sessions to Word documents using Apache POI.
 */
public class ExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    
    private final StorageService storageService;
    private final DateTimeFormatter dateFormatter;
    
    // Document styling constants
    private static final int TITLE_FONT_SIZE = 18;
    private static final int HEADING_FONT_SIZE = 14;
    private static final int BODY_FONT_SIZE = 11;
    private static final int MAX_IMAGE_WIDTH_INCHES = 6;
    private static final int MAX_IMAGE_HEIGHT_INCHES = 4;
    
    public ExportService(StorageService storageService) {
        this.storageService = storageService;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
    }
    
    /**
     * Exports a session to a Word document.
     * @param session The session to export
     * @param outputPath The path where the document should be saved
     * @return The path to the created document
     */
    public Path exportToWord(Session session, Path outputPath) {
        logger.info("Exporting session '{}' to Word document", session.getSessionName());
        
        try (XWPFDocument document = new XWPFDocument()) {
            // Add title
            addTitle(document, session.getSessionName());
            
            // Add session metadata
            addSessionMetadata(document, session);
            
            // Add separator
            addSeparator(document);
            
            // Add each evidence item
            int index = 1;
            for (Evidence evidence : session.getEvidenceList()) {
                addEvidenceSection(document, session.getSessionId(), evidence, index);
                index++;
            }
            
            // Write to file
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                document.write(out);
            }
            
            logger.info("Document exported successfully: {}", outputPath);
            return outputPath;
            
        } catch (Exception e) {
            logger.error("Failed to export session to Word", e);
            throw new RuntimeException("Failed to export session to Word document", e);
        }
    }
    
    /**
     * Exports a session to a Word document in the default exports directory.
     * @param session The session to export
     * @return The path to the created document
     */
    public Path exportToWord(Session session) {
        String filename = sanitizeFilename(session.getSessionName()) + "_" + 
                System.currentTimeMillis() + ".docx";
        Path outputPath = AppConfig.getInstance().getAppDataPath()
                .resolve("exports")
                .resolve(filename);
        
        try {
            java.nio.file.Files.createDirectories(outputPath.getParent());
        } catch (IOException e) {
            logger.error("Failed to create exports directory", e);
        }
        
        return exportToWord(session, outputPath);
    }
    
    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph titleParagraph = document.createParagraph();
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        titleParagraph.setSpacingAfter(200);
        
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontSize(TITLE_FONT_SIZE);
        titleRun.setFontFamily("Arial");
    }
    
    private void addSessionMetadata(XWPFDocument document, Session session) {
        XWPFParagraph metaParagraph = document.createParagraph();
        metaParagraph.setSpacingAfter(100);
        
        XWPFRun metaRun = metaParagraph.createRun();
        metaRun.setText("Session ID: " + session.getSessionId());
        metaRun.addBreak();
        metaRun.setText("Created: " + dateFormatter.format(session.getCreatedAt()));
        metaRun.addBreak();
        metaRun.setText("Total Screenshots: " + session.getEvidenceCount());
        metaRun.setFontSize(BODY_FONT_SIZE);
        metaRun.setFontFamily("Arial");
        metaRun.setItalic(true);
    }
    
    private void addSeparator(XWPFDocument document) {
        XWPFParagraph separator = document.createParagraph();
        separator.setBorderBottom(Borders.SINGLE);
        separator.setSpacingAfter(200);
    }
    
    private void addEvidenceSection(XWPFDocument document, String sessionId, 
                                     Evidence evidence, int index) {
        // Add heading
        XWPFParagraph headingParagraph = document.createParagraph();
        headingParagraph.setSpacingBefore(200);
        
        XWPFRun headingRun = headingParagraph.createRun();
        headingRun.setText("Screenshot #" + index);
        headingRun.setBold(true);
        headingRun.setFontSize(HEADING_FONT_SIZE);
        headingRun.setFontFamily("Arial");
        
        // Add timestamp
        XWPFParagraph timeParagraph = document.createParagraph();
        XWPFRun timeRun = timeParagraph.createRun();
        timeRun.setText("Captured: " + dateFormatter.format(evidence.getTimestamp()));
        timeRun.setFontSize(BODY_FONT_SIZE);
        timeRun.setFontFamily("Arial");
        timeRun.setItalic(true);
        
        // Add note if present
        if (evidence.getNote() != null && !evidence.getNote().isEmpty()) {
            XWPFParagraph noteParagraph = document.createParagraph();
            XWPFRun noteRun = noteParagraph.createRun();
            noteRun.setText("Note: " + evidence.getNote());
            noteRun.setFontSize(BODY_FONT_SIZE);
            noteRun.setFontFamily("Arial");
        }
        
        // Add image
        addImage(document, sessionId, evidence);
        
        // Add spacing after image
        XWPFParagraph spacer = document.createParagraph();
        spacer.setSpacingAfter(300);
    }
    
    private void addImage(XWPFDocument document, String sessionId, Evidence evidence) {
        try {
            BufferedImage image = storageService.loadImage(sessionId, evidence.getFilename());
            if (image == null) {
                logger.warn("Image not found for evidence: {}", evidence.getId());
                addImagePlaceholder(document, evidence.getFilename());
                return;
            }
            
            // Calculate dimensions to fit within max bounds while maintaining aspect ratio
            int[] dimensions = calculateImageDimensions(image.getWidth(), image.getHeight());
            
            // Convert image to input stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            
            // Add image to document
            XWPFParagraph imageParagraph = document.createParagraph();
            imageParagraph.setAlignment(ParagraphAlignment.CENTER);
            
            XWPFRun imageRun = imageParagraph.createRun();
            imageRun.addPicture(bais, XWPFDocument.PICTURE_TYPE_PNG, 
                    evidence.getFilename(),
                    Units.toEMU(dimensions[0]), 
                    Units.toEMU(dimensions[1]));
            
        } catch (Exception e) {
            logger.error("Failed to add image to document: {}", evidence.getFilename(), e);
            addImagePlaceholder(document, evidence.getFilename());
        }
    }
    
    private void addImagePlaceholder(XWPFDocument document, String filename) {
        XWPFParagraph placeholder = document.createParagraph();
        placeholder.setAlignment(ParagraphAlignment.CENTER);
        
        XWPFRun run = placeholder.createRun();
        run.setText("[Image not found: " + filename + "]");
        run.setItalic(true);
        run.setColor("999999");
    }
    
    /**
     * Calculates image dimensions to fit within max bounds while maintaining aspect ratio.
     * @param originalWidth Original image width in pixels
     * @param originalHeight Original image height in pixels
     * @return Array of [width, height] in points
     */
    private int[] calculateImageDimensions(int originalWidth, int originalHeight) {
        // Convert max dimensions from inches to pixels (assuming 96 DPI for display)
        double maxWidthPx = MAX_IMAGE_WIDTH_INCHES * 72; // points
        double maxHeightPx = MAX_IMAGE_HEIGHT_INCHES * 72; // points
        
        // Calculate scale factor to fit within bounds
        double scaleX = maxWidthPx / originalWidth;
        double scaleY = maxHeightPx / originalHeight;
        double scale = Math.min(scaleX, scaleY);
        
        // Don't upscale images
        if (scale > 1) {
            scale = 1;
        }
        
        int width = (int) (originalWidth * scale);
        int height = (int) (originalHeight * scale);
        
        return new int[]{width, height};
    }
    
    /**
     * Sanitizes a filename by removing invalid characters.
     * @param filename The filename to sanitize
     * @return Sanitized filename
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}
