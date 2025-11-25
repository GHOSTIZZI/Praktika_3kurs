package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Author implements Serializable {
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("photo_url")
    private String photoUrl;

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhotoUrl() { return photoUrl; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}