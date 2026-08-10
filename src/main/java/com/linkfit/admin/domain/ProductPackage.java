package com.linkfit.admin.domain;

import java.time.LocalDateTime;

/**
 * 상품(등록 관리 페이지의 "상품 등록"). 이용권/PT/락커/운동복 중 필요한 구성을
 * 한 상품 안에 자유롭게 조합해 담을 수 있다. 이용권/락커/운동복은 개월수+일로
 * 기간을 지정하고, PT는 기간 없이 횟수만 지정한다. 회원에게 등록하면 구성별로
 * 각자 기간(또는 무기한 처리된 PT)에 맞는 이용권(membership) 행이 생성된다.
 */
public class ProductPackage {
    private Long id;
    private String name;
    private int price;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    private boolean membershipEnabled;
    private Integer membershipDurationMonths;
    private Integer membershipDurationDays;

    private boolean ptEnabled;
    private Integer ptSessionCount;

    private boolean lockerEnabled;
    private Integer lockerDurationMonths;
    private Integer lockerDurationDays;

    private boolean uniformEnabled;
    private Integer uniformDurationMonths;
    private Integer uniformDurationDays;

    public ProductPackage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isMembershipEnabled() { return membershipEnabled; }
    public void setMembershipEnabled(boolean membershipEnabled) { this.membershipEnabled = membershipEnabled; }
    public Integer getMembershipDurationMonths() { return membershipDurationMonths; }
    public void setMembershipDurationMonths(Integer membershipDurationMonths) { this.membershipDurationMonths = membershipDurationMonths; }
    public Integer getMembershipDurationDays() { return membershipDurationDays; }
    public void setMembershipDurationDays(Integer membershipDurationDays) { this.membershipDurationDays = membershipDurationDays; }

    public boolean isPtEnabled() { return ptEnabled; }
    public void setPtEnabled(boolean ptEnabled) { this.ptEnabled = ptEnabled; }
    public Integer getPtSessionCount() { return ptSessionCount; }
    public void setPtSessionCount(Integer ptSessionCount) { this.ptSessionCount = ptSessionCount; }

    public boolean isLockerEnabled() { return lockerEnabled; }
    public void setLockerEnabled(boolean lockerEnabled) { this.lockerEnabled = lockerEnabled; }
    public Integer getLockerDurationMonths() { return lockerDurationMonths; }
    public void setLockerDurationMonths(Integer lockerDurationMonths) { this.lockerDurationMonths = lockerDurationMonths; }
    public Integer getLockerDurationDays() { return lockerDurationDays; }
    public void setLockerDurationDays(Integer lockerDurationDays) { this.lockerDurationDays = lockerDurationDays; }

    public boolean isUniformEnabled() { return uniformEnabled; }
    public void setUniformEnabled(boolean uniformEnabled) { this.uniformEnabled = uniformEnabled; }
    public Integer getUniformDurationMonths() { return uniformDurationMonths; }
    public void setUniformDurationMonths(Integer uniformDurationMonths) { this.uniformDurationMonths = uniformDurationMonths; }
    public Integer getUniformDurationDays() { return uniformDurationDays; }
    public void setUniformDurationDays(Integer uniformDurationDays) { this.uniformDurationDays = uniformDurationDays; }
}
