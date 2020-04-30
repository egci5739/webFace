package com.face.nd.entity;

import org.springframework.stereotype.Component;

@Component
public class EventLogEntity {
    private int eventLogId;
    private String eventLogContent;

    public int getEventLogId() {
        return eventLogId;
    }

    public void setEventLogId(int eventLogId) {
        this.eventLogId = eventLogId;
    }

    public String getEventLogContent() {
        return eventLogContent;
    }

    public void setEventLogContent(String eventLogContent) {
        this.eventLogContent = eventLogContent;
    }
}
