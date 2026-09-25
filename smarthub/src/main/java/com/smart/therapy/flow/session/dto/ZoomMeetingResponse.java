package com.smart.therapy.flow.session.dto;

public class ZoomMeetingResponse {
    private String meetingId;
    private String joinUrl;
    private String password;

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getJoinUrl() {
        return joinUrl;
    }

    public void setJoinUrl(String joinUrl) {
        this.joinUrl = joinUrl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
