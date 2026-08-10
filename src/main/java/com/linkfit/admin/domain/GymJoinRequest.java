package com.linkfit.admin.domain;

import java.time.LocalDateTime;

public class GymJoinRequest {
    private Long id;             // user_gym.id
    private String memberId;
    private String memberName;
    private String memberPhone;
    private Long gymId;
    private String gymName;
    private String status;       // PENDING, APPROVED, REJECTED
    private LocalDateTime requestedAt; // user_gym.joined_at (요청/재요청 시각)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }
    public Long getGymId() { return gymId; }
    public void setGymId(Long gymId) { this.gymId = gymId; }
    public String getGymName() { return gymName; }
    public void setGymName(String gymName) { this.gymName = gymName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
