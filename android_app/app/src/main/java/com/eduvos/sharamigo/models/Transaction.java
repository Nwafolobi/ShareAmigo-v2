package com.eduvos.sharamigo.models;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("id")
    private int id;

    @SerializedName("item_id")
    private int itemId;

    @SerializedName("item_title")
    private String itemTitle;

    @SerializedName("donor_id")
    private int donorId;

    @SerializedName("donor_name")
    private String donorName;

    @SerializedName("receiver_id")
    private int receiverId;

    @SerializedName("receiver_name")
    private String receiverName;

    @SerializedName("credit_amount")
    private int creditAmount;

    @SerializedName("status")
    private String status;

    @SerializedName("qr_verify_hash")
    private String qrVerifyHash;

    @SerializedName("pin_fallback")
    private String pinFallback;

    @SerializedName("created_at")
    private String createdAt;

    public int getId() { return id; }
    public int getItemId() { return itemId; }
    public String getItemTitle() { return itemTitle; }
    public int getDonorId() { return donorId; }
    public String getDonorName() { return donorName; }
    public int getReceiverId() { return receiverId; }
    public String getReceiverName() { return receiverName; }
    public int getCreditAmount() { return creditAmount; }
    public String getStatus() { return status; }
    public String getQrVerifyHash() { return qrVerifyHash; }
    public String getPinFallback() { return pinFallback; }
    public String getCreatedAt() { return createdAt; }
}
