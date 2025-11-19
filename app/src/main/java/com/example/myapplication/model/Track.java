package com.example.myapplication.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

// Модель для хранения информации о треке
public class Track implements Parcelable {

    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("cover_url")
    private String coverUrl;

    @SerializedName("audio_url")
    private String audioUrl;

    private transient boolean isFavorite = false;

    // Конструктор по умолчанию (требуется Retrofit/Gson)
    public Track() { }

    // Конструктор
    public Track(int id, String title, String artist, String coverUrl, String audioUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.coverUrl = coverUrl;
        this.audioUrl = audioUrl;
    }

    // ----------------------- Parcelable Implementation -----------------------
    protected Track(Parcel in) {
        id = in.readInt();
        title = in.readString();
        artist = in.readString();
        coverUrl = in.readString();
        audioUrl = in.readString();
        isFavorite = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeString(coverUrl);
        dest.writeString(audioUrl);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    // ----------------------- Getters & Setters -----------------------
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getCoverUrl() { return coverUrl; }
    public String getAudioUrl() { return audioUrl; }
    public boolean isFavorite() { return isFavorite; }

    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}