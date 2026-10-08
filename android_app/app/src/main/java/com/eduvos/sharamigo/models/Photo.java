package com.eduvos.sharamigo.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Photo implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("photo_url")
    private String photoUrl;

    public int getId() { return id; }
    public String getPhotoUrl() { return photoUrl; }
}
