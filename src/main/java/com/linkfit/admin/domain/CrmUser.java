package com.linkfit.admin.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CrmUser {
    private String id;           // CHAR(36) UUID
    private Long gymId;
    private String branchCode;   // gym.branch_code (JOIN용)
    private String appUserId;    // users.user_id 연결 (트레이너 선택적)
    private String name;
    private String email;
    private String username;
    private String passwordHash;
    private String secondPasswordHash;  // 2차 비밀번호 (카테고리 잠금 해제용, 로그인 비밀번호와 별도)
    private boolean mustChangePassword; // 지점코드 발급 시 기본 비밀번호로 만들어진 계정 — 최초 로그인 시 1/2차 비밀번호 변경 강제
    private String role;         // super_admin | gym_admin | trainer
    private LocalDate hireDate;          // 트레이너 입사일 (앱 회원가입일과 별개)
    private String workStatus;           // ACTIVE(재직) | LEAVE(휴직) | RESIGNED(퇴사)
    private LocalDate resignationDate;   // workStatus=RESIGNED일 때만 의미 있음
    private String lockedCategories;    // 콤마 구분 카테고리 키, 예: "crm-sales,revenue"
    private boolean active;
    private LocalDateTime createdAt;

    public CrmUser() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getGymId() { return gymId; }
    public void setGymId(Long gymId) { this.gymId = gymId; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public String getAppUserId() { return appUserId; }
    public void setAppUserId(String appUserId) { this.appUserId = appUserId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSecondPasswordHash() { return secondPasswordHash; }
    public void setSecondPasswordHash(String secondPasswordHash) { this.secondPasswordHash = secondPasswordHash; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getWorkStatus() { return workStatus; }
    public void setWorkStatus(String workStatus) { this.workStatus = workStatus; }
    public LocalDate getResignationDate() { return resignationDate; }
    public void setResignationDate(LocalDate resignationDate) { this.resignationDate = resignationDate; }
    public String getLockedCategories() { return lockedCategories; }
    public void setLockedCategories(String lockedCategories) { this.lockedCategories = lockedCategories; }

    public java.util.Set<String> lockedCategorySet() {
        if (lockedCategories == null || lockedCategories.isBlank()) return java.util.Set.of();
        return java.util.Arrays.stream(lockedCategories.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .collect(java.util.stream.Collectors.toSet());
    }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
