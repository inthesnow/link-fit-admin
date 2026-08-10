package com.linkfit.admin.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Sale {
    private Long id;
    private String memberId;
    private String memberName;
    private Long productId;
    private String productName;
    private String productType;    // MEMBERSHIP, GROUP, PT, LOCKER, ITEM
    private int amount;
    private int refundAmount;
    private String paymentMethod;  // CARD, CASH, TRANSFER
    private LocalDate saleDate;
    private String memo;
    private LocalDateTime refundedAt;
    private String refundReason;
    private LocalDateTime createdAt;

    public Sale() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
    public int getRefundAmount() { return refundAmount; }
    public void setRefundAmount(int refundAmount) { this.refundAmount = refundAmount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
