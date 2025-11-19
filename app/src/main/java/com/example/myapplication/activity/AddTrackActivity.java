package com.example.myapplication.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.Track;
import com.example.myapplication.model.User; // Оставил, если нужен для других целей
import com.example.myapplication.SupabaseMusicApi;
import com.google.android.material.appbar.MaterialToolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.List;

public class AddTrackActivity extends AppCompatActivity {

    private EditText etTitle, etArtist, etCoverUrl, etAudioUrl;
    private Button btnAddTrack;
    private SupabaseMusicApi musicApi;
    private int currentUserId;

    // 🛑 УДАЛЕНО: private String userPhone = ""; (Больше не нужно)

    private static final String BASE_URL = "https://xxatkzdplbgnuibdykyd.supabase.co/rest/v1/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_track);

        currentUserId = getIntent().getIntExtra("user_id", -1);
        if (currentUserId == -1) {
            Toast.makeText(this, "Ошибка: пользователь не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etTitle = findViewById(R.id.et_track_title);
        etArtist = findViewById(R.id.et_artist_name);
        etCoverUrl = findViewById(R.id.et_cover_url);
        etAudioUrl = findViewById(R.id.et_audio_url);

        btnAddTrack = findViewById(R.id.btn_add_track);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        musicApi = retrofit.create(SupabaseMusicApi.class);

        // 🛑 УДАЛЕНО: loadUserPhone(); (Больше не нужно)

        btnAddTrack.setOnClickListener(v -> addTrack());
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // 🛑 УДАЛЕНО: Метод loadUserPhone() (Больше не нужен)
    /*
    private void loadUserPhone() {
        // ... старый код ...
    }
    */

    private void addTrack() {
        if (etTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Заполните название трека", Toast.LENGTH_SHORT).show();
            return;
        }

        // 💡 Создаем объект Track
        Track track = new Track();
        track.setTitle(etTitle.getText().toString().trim());
        track.setArtist(etArtist.getText().toString().trim());
        track.setCoverUrl(etCoverUrl.getText().toString().trim());
        track.setAudioUrl(etAudioUrl.getText().toString().trim());

        // Если вы добавите user_id в модель Track и в схему БД,
        // вы можете установить его здесь:
        // track.setUserId(currentUserId);

        btnAddTrack.setEnabled(false);
        btnAddTrack.setText("Загрузка...");

        // 🛑 РАСКОММЕНТИРОВАНО: Логика добавления трека
        musicApi.addTrack(track).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnAddTrack.setEnabled(true);
                btnAddTrack.setText("Добавить трек");
                if (response.code() == 201) { // 201 Created - Supabase
                    Toast.makeText(AddTrackActivity.this, "Трек добавлен успешно!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // Обработка ошибки
                    String error = "Неизвестная ошибка";
                    if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (Exception e) {
                            error = e.getMessage();
                        }
                    }
                    Toast.makeText(AddTrackActivity.this, "Ошибка добавления: " + response.code() + " (" + error + ")", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnAddTrack.setEnabled(true);
                btnAddTrack.setText("Добавить трек");
                Toast.makeText(AddTrackActivity.this, "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}