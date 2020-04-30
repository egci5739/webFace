package com.face.nd.entity;

import org.springframework.stereotype.Component;

@Component
public class StaffEntity {
    private int staffId;
    private String staffName;
    private String staffCardId;
    private String staffCardNumber;
    private String staffBirthday;
    private int staffGender;
    private String staffCompany;
    private byte[] staffImage;
    private int staffValidity;

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStaffCardId() {
        return staffCardId;
    }

    public void setStaffCardId(String staffCardId) {
        this.staffCardId = staffCardId;
    }

    public String getStaffCardNumber() {
        return staffCardNumber;
    }

    public void setStaffCardNumber(String staffCardNumber) {
        this.staffCardNumber = staffCardNumber;
    }

    public String getStaffBirthday() {
        return staffBirthday;
    }

    public void setStaffBirthday(String staffBirthday) {
        this.staffBirthday = staffBirthday;
    }

    public int getStaffGender() {
        return staffGender;
    }

    public void setStaffGender(int staffGender) {
        this.staffGender = staffGender;
    }

    public String getStaffCompany() {
        return staffCompany;
    }

    public void setStaffCompany(String staffCompany) {
        this.staffCompany = staffCompany;
    }

    public byte[] getStaffImage() {
        return staffImage;
    }

    public void setStaffImage(byte[] staffImage) {
        this.staffImage = staffImage;
    }

    public int getStaffValidity() {
        return staffValidity;
    }

    public void setStaffValidity(int staffValidity) {
        this.staffValidity = staffValidity;
    }
}
