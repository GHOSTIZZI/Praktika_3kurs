package com.example.myapplication.model;



import com.google.gson.annotations.SerializedName;

// Модель для таблицы 'favorites' в Supabase
public class Favorite {

    // ID пользователя, который добавил трек
    @SerializedName("user_id")
    private int userId;

    // ID трека, который добавлен
    @SerializedName("track_id")
    private int trackId;

    public Favorite(int userId, int trackId) {
        this.userId = userId;
        this.trackId = trackId;
    }

    // Getters and Setters (для Gson/Retrofit)
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }
}