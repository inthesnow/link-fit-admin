package com.linkfit.admin.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Membership {
    private Long id;
    private String memberId;
    private Long productId;
    private String productName;
    private Long packageId;   // 패키지로 등록된 경우: 요청 시 패키지 ID, 저장 후 원본 패키지 식별용
    private String packageName;
    private Long lockerId;    // type=LOCKER인 경우: 실제 배정된 물리적 라커(locker.id)
    private String type;   // MEMBERSHIP, GROUP, PT, LOCKER, ITEM
    private LocalDate startDate;
    private LocalDate endDate;
    private int price;
    private int discountAmount;
    private int paidAmount;
    private String paymentMethod;
    private String regType;   // NEW, RE, RE_INFLOW, SINGLE_ITEM, TRANSFER — 신규/재등록/재유입/단품결제/양도
    private String status;    // null(정상) | TRANSFERRED(다른 회원에게 양도되어 더 이상 유효하지 않음)
    private String memo;
    private LocalDateTime createdAt;
    // joined fields for list views
    private String memberName;
    private String memberPhone;
    private Integer daysLeft;
    // 개별 상품 부여(패키지 없이 단일 항목 등록) 입력 전용 필드 — DB에는 저장되지 않고
    // 서비스 계층에서 endDate 계산 및 PT 세션 크레딧에만 사용된다.
    private Integer durationMonths;
    private Integer durationDays;
    private Integer sessionCount;

    public Membership() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public Long getLockerId() { return lockerId; }
    public void setLockerId(Long lockerId) { this.lockerId = lockerId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(int discountAmount) { this.discountAmount = discountAmount; }
    public int getPaidAmount() { return paidAmount; }
    public void setPaidAmount(int paidAmount) { this.paidAmount = paidAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getRegType() { return regType; }
    public void setRegType(String regType) { this.regType = regType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }
    public Integer getDaysLeft() { return daysLeft; }
    public void setDaysLeft(Integer daysLeft) { this.daysLeft = daysLeft; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
}
