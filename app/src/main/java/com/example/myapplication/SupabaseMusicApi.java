package com.example.myapplication;

import com.example.myapplication.model.Track;
import com.example.myapplication.model.User;
import com.example.myapplication.model.Favorite; // Предполагаем, что такая модель существует
import com.google.gson.annotations.SerializedName;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface SupabaseMusicApi {

    // ⚠️ ВАЖНО: ЗАМЕНИТЕ ЭТИ КЛЮЧИ НА СВОИ!
    String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh4YXRremRwbGJnbnVpYmR5a3lkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM0MTI0MjEsImV4cCI6MjA3ODk4ODQyMX0.svFtSKnqmarh3TiybOjJYQ_t0sZ_vUv8D9Q2QUcdxkk";
    String AUTH_HEADER = "Authorization: Bearer " + API_KEY;
    String CONTENT_TYPE = "Content-Type: application/json";

    // ---------------------- ТРЕКИ (TRACKS) ----------------------

    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @GET("tracks") // Таблица 'tracks'
    Call<List<Track>> getAllTracks();

    // 🛑 ДОБАВЛЕНО: Добавить трек (POST)
    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @POST("tracks") // Таблица 'tracks'
    Call<Void> addTrack(@Body Track track);

    // ---------------------- ИЗБРАННОЕ (FAVORITES) ----------------------

    // Получить избранные треки пользователя (JOIN: favorites -> tracks)
    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @GET("favorites?select=track_id,tracks(*)")
    Call<List<FavoriteWrapper>> getFavoriteTracks(@Query("user_id") String userIdEq);

    // Добавить трек в избранное (POST)
    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @POST("favorites")
    Call<Void> addFavorite(@Body Favorite favorite);

    // Удалить трек из избранного (DELETE)
    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @DELETE("favorites")
    Call<Void> deleteFavorite(@Query("user_id") String userIdEq, @Query("track_id") String trackIdEq);

    // ---------------------- ПОЛЬЗОВАТЕЛИ (USERS) ----------------------

    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @GET("users")
    Call<List<User>> getUserByLogin(@Query("username") String loginEq);

    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @GET("users")
    Call<List<User>> getUserById(@Query("id") String userIdEq);

    @Headers({ "apikey: " + API_KEY, AUTH_HEADER, CONTENT_TYPE })
    @POST("users")
    Call<Void> createUser(@Body User user);

    // ---------------------- Дополнительные Модели для JOIN ----------------------
    // Supabase с JOIN'ом возвращает вложенный объект, нам нужен враппер:
    class FavoriteWrapper {
        @SerializedName("track_id") public int trackId;
        @SerializedName("tracks") public Track track;
    }
}