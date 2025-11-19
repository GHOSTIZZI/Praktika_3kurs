package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.SupabaseMusicApi;
import com.example.myapplication.model.User;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private SupabaseMusicApi musicApi;
    // ⚠️ Убедитесь, что это ваш реальный URL
    private static final String BASE_URL = "https://xxatkzdplbgnuibdykyd.supabase.co/rest/v1/";

    private EditText loginInput;
    private EditText passwordInput;
    private Button loginButton;
    // private Button guestButton; // ❌ Удалена ссылка на guestButton
    private TextView registerLink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация UI-элементов
        loginInput = findViewById(R.id.login_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        // guestButton = findViewById(R.id.guest_button); // ❌ Удалена инициализация guestButton
        registerLink = findViewById(R.id.register_link);

        // Инициализация Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        musicApi = retrofit.create(SupabaseMusicApi.class);

        // Назначение слушателей
        loginButton.setOnClickListener(v -> handleLogin());

        // ❌ Удален слушатель гостевого входа (guestButton.setOnClickListener)

        registerLink.setOnClickListener(v -> goToRegister());
    }

    private void handleLogin() {

        String login = loginInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim();

        // Проверки на пустоту
        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Запрос в Supabase: ищем пользователя по логину
        musicApi.getUserByLogin("eq." + login).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                    User user = response.body().get(0);
                    String dbPassword = user.getPassword();

                    if (dbPassword != null && dbPassword.equals(password)) {
                        // Вход успешен!

                        Toast.makeText(MainActivity.this, "Добро пожаловать, " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        // Передаем ID пользователя
                        intent.putExtra("user_id", user.getId());
                        // Передаем логин пользователя для отображения в меню
                        intent.putExtra("username", user.getUsername());
                        startActivity(intent);
                        finish();

                    } else {
                        // Пароль не совпал
                        Log.w(TAG, "Login failed for user: " + login + ". Password mismatch.");
                        Toast.makeText(MainActivity.this, "Ошибка: Пользователь не найден или неверный пароль!", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // Пользователь по логину не найден
                    Toast.makeText(MainActivity.this, "Ошибка: Пользователь не найден или неверный пароль!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e(TAG, "Network Error", t);
                Toast.makeText(MainActivity.this, "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ❌ Удален метод handleGuestLogin

    private void goToRegister() {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}