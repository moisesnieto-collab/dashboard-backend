package com.lighthouse.dashboard.model;

public class ErrorStat {
    private String message;
    private String status;
    private long count;

    public ErrorStat(String message, String status, long count) {
        this.message = message;
        this.status = status;
        this.count = count;
    }

    // Getters y Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
