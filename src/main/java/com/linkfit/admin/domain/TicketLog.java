package com.linkfit.admin.domain;

public class TicketLog {
    private String userId;
    private String userName;
    private String ticketType;
    private String actionType;
    private String description;
    private String createdAt;

    public TicketLog() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getTicketType() { return ticketType; }
    public void setTicketType(String ticketType) { this.ticketType = ticketType; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
