package com.lighthouse.dashboard.model;

public class TimeSeriesData {
    private String date;
    private String status;
    private long count;

    public TimeSeriesData(String date, String status, long count) {
        this.date = date;
        this.status = status;
        this.count = count;
    }

    // Getters y Setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
