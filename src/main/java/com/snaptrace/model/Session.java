package com.snaptrace.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data class representing a testing session containing multiple evidence items.
 */
public class Session {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("sessionName")
    private String sessionName;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("evidenceList")
    private List<Evidence> evidenceList;
    
    // Default constructor for Jackson
    public Session() {
        this.evidenceList = new ArrayList<>();
    }
    
    public Session(String sessionName) {
        this.sessionId = UUID.randomUUID().toString();
        this.sessionName = sessionName;
        this.createdAt = Instant.now();
        this.evidenceList = new ArrayList<>();
    }
    
    public Session(String sessionId, String sessionName, Instant createdAt, List<Evidence> evidenceList) {
        this.sessionId = sessionId;
        this.sessionName = sessionName;
        this.createdAt = createdAt;
        this.evidenceList = evidenceList != null ? evidenceList : new ArrayList<>();
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSessionName() {
        return sessionName;
    }
    
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public List<Evidence> getEvidenceList() {
        return evidenceList;
    }
    
    public void setEvidenceList(List<Evidence> evidenceList) {
        this.evidenceList = evidenceList;
    }
    
    // Helper methods
    public void addEvidence(Evidence evidence) {
        if (this.evidenceList == null) {
            this.evidenceList = new ArrayList<>();
        }
        this.evidenceList.add(evidence);
    }
    
    public void removeEvidence(String evidenceId) {
        if (this.evidenceList != null) {
            this.evidenceList.removeIf(e -> e.getId().equals(evidenceId));
        }
    }
    
    public Evidence getEvidenceById(String evidenceId) {
        if (this.evidenceList != null) {
            return this.evidenceList.stream()
                    .filter(e -> e.getId().equals(evidenceId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
    
    public int getEvidenceCount() {
        return evidenceList != null ? evidenceList.size() : 0;
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", sessionName='" + sessionName + '\'' +
                ", createdAt=" + createdAt +
                ", evidenceCount=" + getEvidenceCount() +
                '}';
    }
}
