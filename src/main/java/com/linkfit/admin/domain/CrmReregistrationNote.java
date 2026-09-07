package com.linkfit.admin.domain;

import java.time.LocalDateTime;

public class CrmReregistrationNote {
    private String id;
    private String reregistrationId;
    private Long gymId;
    private String authorId;
    private String authorName;
    private String content;
    private LocalDateTime createdAt;

    public CrmReregistrationNote() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReregistrationId() { return reregistrationId; }
    public void setReregistrationId(String reregistrationId) { this.reregistrationId = reregistrationId; }
    public Long getGymId() { return gymId; }
    public void setGymId(Long gymId) { this.gymId = gymId; }
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
