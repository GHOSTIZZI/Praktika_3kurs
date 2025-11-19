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
    private Button guestButton; // Если вы планируете его использовать
    private TextView registerLink;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация UI-элементов
        loginInput = findViewById(R.id.login_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        guestButton = findViewById(R.id.guest_button); // Если он есть в XML
        registerLink = findViewById(R.id.register_link);

        // Инициализация Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        musicApi = retrofit.create(SupabaseMusicApi.class);

        // Назначение слушателей
        loginButton.setOnClickListener(v -> handleLogin());

        // Если вы используете guestButton, добавьте его слушатель
        // guestButton.setOnClickListener(v -> handleGuestLogin());

        registerLink.setOnClickListener(v -> goToRegister());
    }

    private void handleLogin() {
        // 🛑 ИСПРАВЛЕНИЕ: Приводим логин к нижнему регистру для совпадения с БД
        String login = loginInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim(); // Пароль остается в исходном регистре

        // Проверки на пустоту
        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните все поля!", Toast.LENGTH_SHORT).show();
            return;
        }
        // Здесь могут быть дополнительные проверки длины

        // 1. Запрос в Supabase: ищем пользователя по логину
        // 💡 Предполагается, что в SupabaseMusicApi есть getUserByLogin
        musicApi.getUserByLogin("eq." + login).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                    User user = response.body().get(0);

                    // 🛑 КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: ПРОВЕРКА ПАРОЛЯ В ПРИЛОЖЕНИИ
                    // Убедитесь, что в User.java есть public String getPassword()
                    String dbPassword = user.getPassword();

                    if (dbPassword != null && dbPassword.equals(password)) {
                        // Вход успешен!

                        Toast.makeText(MainActivity.this, "Добро пожаловать, " + user.getUsername() + "!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        // Передаем ID пользователя для дальнейшей работы (например, с избранным)
                        intent.putExtra("user_id", user.getId());
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

    // Если у вас есть гостевой вход
    /*
    private void handleGuestLogin() {
        Toast.makeText(this, "Вход как гость", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, GuestViewActivity.class); // Укажите свой класс
        startActivity(intent);
    }
    */

    private void goToRegister() {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}