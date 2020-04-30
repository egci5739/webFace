package com.face.nd.entity;

import org.springframework.stereotype.Component;

@Component
public class AlarmEntity {
    private int alarmId;
    private String alarmName;
    private int alarmNoteId;
    private String alarmNote;
    private int alarmType;
    private String alarmEquipmentName;
    private String operator;
    private int alarmStatus;
    private int alarmPermission;

    public int getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(int alarmId) {
        this.alarmId = alarmId;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public int getAlarmNoteId() {
        return alarmNoteId;
    }

    public void setAlarmNoteId(int alarmNoteId) {
        this.alarmNoteId = alarmNoteId;
    }

    public String getAlarmNote() {
        return alarmNote;
    }

    public void setAlarmNote(String alarmNote) {
        this.alarmNote = alarmNote;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public int getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(int alarmStatus) {
        this.alarmStatus = alarmStatus;
    }

    public int getAlarmPermission() {
        return alarmPermission;
    }

    public void setAlarmPermission(int alarmPermission) {
        this.alarmPermission = alarmPermission;
    }

    public int getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(int alarmType) {
        this.alarmType = alarmType;
    }

    public String getAlarmEquipmentName() {
        return alarmEquipmentName;
    }

    public void setAlarmEquipmentName(String alarmEquipmentName) {
        this.alarmEquipmentName = alarmEquipmentName;
    }
}
