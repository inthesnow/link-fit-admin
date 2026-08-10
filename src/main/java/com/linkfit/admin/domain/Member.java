package com.linkfit.admin.domain;

import java.time.LocalDate;

public class Member {
    private String id;
    private String email;
    private String name;
    private String phone;
    private String gender;
    private LocalDate birthDate;
    private String status;       // ACTIVE, SUSPENDED
    private LocalDate joinDate;
    private LocalDate membershipStart;
    private LocalDate membershipEnd;
    private String memo;
    private String memberType;   // PT, OT
    private String tier;         // BASIC, LIGHT_FIT, REGULAR_FIT, INTENSIVE_FIT
    private String role;         // MEMBER, TRAINER
    private String trainerId;
    private String trainerName;
    private Integer ptSessionsLeft;
    private Integer servicePtSessionsLeft;
    private Boolean appInstalled;
    private String currentProductName;
    private Integer currentProductPrice;
    private Integer unpaidAmount;

    public Member() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
    public LocalDate getMembershipEnd() { return membershipEnd; }
    public void setMembershipEnd(LocalDate membershipEnd) { this.membershipEnd = membershipEnd; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getMemberType() { return memberType; }
    public void setMemberType(String memberType) { this.memberType = memberType; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getTrainerId() { return trainerId; }
    public void setTrainerId(String trainerId) { this.trainerId = trainerId; }
    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }
    public LocalDate getMembershipStart() { return membershipStart; }
    public void setMembershipStart(LocalDate membershipStart) { this.membershipStart = membershipStart; }
    public Integer getPtSessionsLeft() { return ptSessionsLeft; }
    public void setPtSessionsLeft(Integer ptSessionsLeft) { this.ptSessionsLeft = ptSessionsLeft; }
    public Integer getServicePtSessionsLeft() { return servicePtSessionsLeft; }
    public void setServicePtSessionsLeft(Integer servicePtSessionsLeft) { this.servicePtSessionsLeft = servicePtSessionsLeft; }
    public Boolean getAppInstalled() { return appInstalled; }
    public void setAppInstalled(Boolean appInstalled) { this.appInstalled = appInstalled; }
    public String getCurrentProductName() { return currentProductName; }
    public void setCurrentProductName(String currentProductName) { this.currentProductName = currentProductName; }
    public Integer getCurrentProductPrice() { return currentProductPrice; }
    public void setCurrentProductPrice(Integer currentProductPrice) { this.currentProductPrice = currentProductPrice; }
    public Integer getUnpaidAmount() { return unpaidAmount; }
    public void setUnpaidAmount(Integer unpaidAmount) { this.unpaidAmount = unpaidAmount; }
}
