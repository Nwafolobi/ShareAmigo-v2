package com.eduvos.sharamigo.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Item implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("listed_by")
    private int listedBy;

    @SerializedName("donor_name")
    private String donorName;

    @SerializedName("category_name")
    private String categoryName;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("condition_status")
    private String conditionStatus;

    @SerializedName("credit_cost")
    private int creditCost;

    @SerializedName("status")
    private String status;

    @SerializedName("photo_url")
    private String photoUrl;

    public int getId() { return id; }
    public int getListedBy() { return listedBy; }
    public String getDonorName() { return donorName; }
    public String getCategoryName() { return categoryName; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getConditionStatus() { return conditionStatus; }
    public int getCreditCost() { return creditCost; }
    public String getStatus() { return status; }
    public String getPhotoUrl() { return photoUrl; }
}
