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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    // 🛑 ИСПРАВЛЕНИЕ: Добавлена переменная для логина
    private EditText loginInput;
    private EditText passwordInput;
    private Button registerButton;
    private TextView loginLink;
    private SupabaseMusicApi musicApi;

    // Вставьте ваш реальный URL
    private static final String BASE_URL = "https://xxatkzdplbgnuibdykyd.supabase.co/rest/v1/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // 🛑 ИСПРАВЛЕНИЕ: Инициализация поля логина
        loginInput = findViewById(R.id.login_input); // Предполагаемый ID в вашем register_activity.xml
        passwordInput = findViewById(R.id.password_input);
        registerButton = findViewById(R.id.register_button);
        loginLink = findViewById(R.id.login_link);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        musicApi = retrofit.create(SupabaseMusicApi.class);

        registerButton.setOnClickListener(v -> handleRegister());
        loginLink.setOnClickListener(v -> goToLogin());
    }

    private void handleRegister() {
        // 🛑 ИСПРАВЛЕНИЕ: Получаем логин из loginInput
        String login = loginInput.getText().toString().trim().toLowerCase();
        String password = passwordInput.getText().toString().trim(); // Пароль

        // --- Проверки ---
        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Заполните логин и пароль!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (login.length() < 3) {
            Toast.makeText(this, "Логин должен быть от 3 символов!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 8) {
            Toast.makeText(this, "Пароль должен быть не менее 8 символов!", Toast.LENGTH_SHORT).show();
            return;
        }
        // ------------------

        // 1. Проверяем, существует ли пользователь с таким логином
        musicApi.getUserByLogin("eq." + login).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Пользователь найден -> логин занят
                    Toast.makeText(RegisterActivity.this, "Логин уже используется", Toast.LENGTH_SHORT).show();
                } else {
                    // Логин свободен, регистрируем
                    User newUser = new User();
                    newUser.setUsername(login); // Предполагаем, что у вас есть setUsername или setLogin
                    newUser.setPassword(password); // Устанавливаем пароль
                    newUser.setRole("user"); // Устанавливаем роль по умолчанию

                    // 2. Создаем нового пользователя
                    musicApi.createUser(newUser).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.code() == 201) {
                                Toast.makeText(RegisterActivity.this, "Аккаунт создан успешно!", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Log.e("REG_ERROR", "Code: " + response.code() + ", Body: " + response.errorBody());
                                Toast.makeText(RegisterActivity.this, "Ошибка регистрации! Код: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e("REG_ERROR", "Network failure", t);
                            Toast.makeText(RegisterActivity.this, "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Log.e("REG_ERROR", "Network failure (check existence)", t);
                Toast.makeText(RegisterActivity.this, "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToLogin() {
        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
        finish();
    }
}