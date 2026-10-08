package com.eduvos.sharamigo.models;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("id")
    private int id;

    @SerializedName("campus_id")
    private int campusId;

    @SerializedName("campus_name")
    private String campusName;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("email")
    private String email;

    @SerializedName("student_number")
    private String studentNumber;

    @SerializedName("credit_balance")
    private int creditBalance;

    @SerializedName("role")
    private String role;

    @SerializedName("bio")
    private String bio;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getCampusId() { return campusId; }
    public String getCampusName() { return campusName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getStudentNumber() { return studentNumber; }
    public int getCreditBalance() { return creditBalance; }
    public void setCreditBalance(int balance) { this.creditBalance = balance; }
    public String getRole() { return role; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getCreatedAt() { return createdAt; }
}
