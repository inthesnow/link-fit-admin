package com.linkfit.admin.domain;

import java.time.LocalDateTime;

public class GymJoinRequestLog {
    private Long id;
    private String action;      // REQUESTED, APPROVED, REJECTED
    private String memo;
    private String actorName;   // 승인/거절 처리자 이름 (회원 본인의 REQUESTED는 null)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
