package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class User {

    // Поля должны соответствовать вашей таблице Supabase
    private int id;

    @SerializedName("username") // Должно совпадать с именем столбца в Supabase
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    // Конструктор по умолчанию (требуется Retrofit/Gson)
    public User() { }

    // --- Setters (нужны для регистрации) ---

    // 🛑 ИСПРАВЛЕНИЕ: Добавлен setUsername
    public void setUsername(String username) {
        this.username = username;
    }

    // Добавлен setPassword
    public void setPassword(String password) {
        this.password = password;
    }

    // 🛑 ИСПРАВЛЕНИЕ: Добавлен setRole
    public void setRole(String role) {
        this.role = role;
    }

    // --- Getters (нужны для входа) ---
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}