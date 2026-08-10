package com.linkfit.admin.domain;

public class Product {
    private Long id;
    private String name;
    private String type;         // MEMBERSHIP, GROUP, PT, LOCKER, ITEM
    private int price;
    private int durationDays;
    private Integer sessionCount; // 횟수 (PT 등 세션 기반 상품용, 없으면 null)
    private String description;
    private boolean active;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public Integer getSessionCount() { return sessionCount; }
    public void setSessionCount(Integer sessionCount) { this.sessionCount = sessionCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
