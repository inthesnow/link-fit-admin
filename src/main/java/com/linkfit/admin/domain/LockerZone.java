package com.linkfit.admin.domain;

public class LockerZone {
    private Long id;
    private Long gymId;
    private String name;
    private int rowsCount;
    private int colsCount;
    private int totalCount;
    private int displayOrder;

    public LockerZone() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getGymId() { return gymId; }
    public void setGymId(Long gymId) { this.gymId = gymId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRowsCount() { return rowsCount; }
    public void setRowsCount(int rowsCount) { this.rowsCount = rowsCount; }
    public int getColsCount() { return colsCount; }
    public void setColsCount(int colsCount) { this.colsCount = colsCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
