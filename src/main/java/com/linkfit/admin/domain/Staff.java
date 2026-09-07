package com.linkfit.admin.domain;

import java.time.LocalDate;

public class Staff {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String role;         // SUPER_ADMIN, ADMIN, TRAINER
    private LocalDate hireDate;      // 트레이너 입사일 (crm_users.hire_date)
    private String workStatus;       // ACTIVE(재직) | LEAVE(휴직) | RESIGNED(퇴사) — crm_users.work_status
    private LocalDate resignationDate; // workStatus=RESIGNED일 때만 의미 있음
    private String status;       // ACTIVE, INACTIVE (앱 계정 자체 활성 여부 — 근무상태와는 다른 개념)

    public Staff() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getWorkStatus() { return workStatus; }
    public void setWorkStatus(String workStatus) { this.workStatus = workStatus; }
    public LocalDate getResignationDate() { return resignationDate; }
    public void setResignationDate(LocalDate resignationDate) { this.resignationDate = resignationDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
