package com.snaptrace.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.UUID;

/**
 * Data class representing a single screenshot/evidence item.
 */
public class Evidence {
    
    private String id;
    private String filename;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;
    
    private String note;
    
    // Default constructor for Jackson
    public Evidence() {
    }
    
    public Evidence(String filename) {
        this.id = UUID.randomUUID().toString();
        this.filename = filename;
        this.timestamp = Instant.now();
        this.note = "";
    }
    
    public Evidence(String id, String filename, Instant timestamp, String note) {
        this.id = id;
        this.filename = filename;
        this.timestamp = timestamp;
        this.note = note;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    @JsonIgnore
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.toString() : "";
    }
    
    @Override
    public String toString() {
        return "Evidence{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", timestamp=" + timestamp +
                ", note='" + note + '\'' +
                '}';
    }
}
