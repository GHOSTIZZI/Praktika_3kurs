package com.example.myapplication.activity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Track;
import com.example.myapplication.model.Favorite;
import com.example.myapplication.util.PlaybackManager;
import com.example.myapplication.SupabaseMusicApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    // ⚠️ Вставьте ваш реальный URL проекта Supabase здесь!
    private static final String BASE_URL = "https://xxatkzdplbgnuibdykyd.supabase.co/rest/v1/";

    private ImageView albumCover;
    private TextView songTitle;
    private TextView songArtist;
    private TextView currentTime;
    private TextView totalTime;
    private SeekBar seekBar;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton favoriteButton; // Кнопка избранного

    private PlaybackManager playbackManager;
    private Handler handler = new Handler();
    private SupabaseMusicApi musicApi;

    private List<Track> playlist;
    private int startTrackIndex;
    private int currentUserId;

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (playbackManager.getDuration() > 0) {
                int currentPosition = playbackManager.getCurrentPosition();

                if (currentPosition >= playbackManager.getDuration()) {
                    updatePlayPauseButton(false);
                    seekBar.setProgress(playbackManager.getDuration());
                    currentTime.setText(totalTime.getText());
                    handler.removeCallbacks(this);
                    return;
                }

                seekBar.setProgress(currentPosition);
                currentTime.setText(formatTime(currentPosition));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // 1. Инициализация UI
        albumCover = findViewById(R.id.player_album_cover);
        songTitle = findViewById(R.id.player_song_title);
        songArtist = findViewById(R.id.player_song_artist);
        currentTime = findViewById(R.id.player_current_time);
        totalTime = findViewById(R.id.player_total_time);
        seekBar = findViewById(R.id.player_seek_bar);
        playPauseButton = findViewById(R.id.player_btn_play_pause);
        previousButton = findViewById(R.id.player_btn_previous);
        nextButton = findViewById(R.id.player_btn_next);
        favoriteButton = findViewById(R.id.player_btn_favorite);

        playbackManager = PlaybackManager.getInstance(getApplicationContext());

        // Инициализация Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        musicApi = retrofit.create(SupabaseMusicApi.class);

        // 2. Получение данных из Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            playlist = extras.getParcelableArrayList("PLAYLIST");
            startTrackIndex = extras.getInt("START_INDEX", 0);
            currentUserId = extras.getInt("USER_ID", -1);
        }

        // Логика запуска плейлиста
        Track currentTrackInManager = playbackManager.getCurrentTrack();
        if (playlist != null) {
            boolean isNewTrack = currentTrackInManager == null ||
                    !currentTrackInManager.getAudioUrl().equals(playlist.get(startTrackIndex).getAudioUrl());

            if (isNewTrack) {
                playbackManager.playNewPlaylist(playlist, startTrackIndex);
            }
        }

        // 3. Обновление UI данными текущего трека
        Track initialTrack = playbackManager.getCurrentTrack();
        if (initialTrack != null) {
            updateUI(initialTrack);
        }

        // 4. Инициализация кнопок и слайдера
        setupPlaybackControls();

        // Инициализация UI, если плеер уже готов/играет
        if (playbackManager.getDuration() > 0) {
            int duration = playbackManager.getDuration();
            seekBar.setMax(duration);
            totalTime.setText(formatTime(duration));
            updatePlayPauseButton(playbackManager.isPlaying());
            handler.post(updateSeekBar);
        }
    }

    private void setupPlaybackControls() {
        playPauseButton.setOnClickListener(v -> togglePlayback());
        previousButton.setOnClickListener(v -> playbackManager.skipToPrevious());
        nextButton.setOnClickListener(v -> playbackManager.skipToNext());

        favoriteButton.setOnClickListener(v -> toggleFavorite()); // Слушатель избранного

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playbackManager.seekTo(seekBar.getProgress());
                handler.post(updateSeekBar);
            }
        });

        playbackManager.setOnPreparedListener(() -> {
            int duration = playbackManager.getDuration();
            seekBar.setMax(duration);
            totalTime.setText(formatTime(duration));
            updatePlayPauseButton(true);
            handler.post(updateSeekBar);
        });

        playbackManager.setOnTrackChangedListener(this::updateUI);

        updatePlayPauseButton(playbackManager.isPlaying());
    }

    // ----------------------- МЕТОД ОБНОВЛЕНИЯ UI -----------------------

    private void updateUI(Track track) {
        if (track == null) return;

        songTitle.setText(track.getTitle());
        songArtist.setText(track.getArtist());

        String coverUrl = track.getCoverUrl();
        if (coverUrl != null) {
            Glide.with(this).load(coverUrl).placeholder(R.drawable.ic_music_note).into(albumCover);
        } else {
            albumCover.setImageResource(R.drawable.ic_music_note);
        }

        checkFavoriteStatus(track);

        currentTime.setText(formatTime(0));
        totalTime.setText(formatTime(0));
        seekBar.setProgress(0);
        seekBar.setMax(100);
    }

    // ----------------------- ЛОГИКА ИЗБРАННОГО -----------------------

    private void checkFavoriteStatus(Track track) {
        if (currentUserId == -1) {
            favoriteButton.setVisibility(View.GONE);
            return;
        }
        favoriteButton.setVisibility(View.VISIBLE);

        updateFavoriteButtonIcon(track.isFavorite());
    }

    private void toggleFavorite() {
        if (currentUserId == -1 || playbackManager.getCurrentTrack() == null) {
            Toast.makeText(this, "Войдите в аккаунт, чтобы управлять избранным.", Toast.LENGTH_SHORT).show();
            return;
        }

        Track track = playbackManager.getCurrentTrack();

        if (track.isFavorite()) {
            deleteFromFavorites(track);
        } else {
            addToFavorites(track);
        }
    }

    private void addToFavorites(Track track) {
        Favorite favorite = new Favorite(currentUserId, track.getId());
        musicApi.addFavorite(favorite).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201) {
                    track.setFavorite(true);
                    updateFavoriteButtonIcon(true);
                    Toast.makeText(PlayerActivity.this, track.getTitle() + " добавлен в избранное!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlayerActivity.this, "Ошибка добавления: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to add favorite. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PlayerActivity.this, "Ошибка сети при добавлении", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteFromFavorites(Track track) {
        String userIdEq = "eq." + currentUserId;
        String trackIdEq = "eq." + track.getId();

        musicApi.deleteFavorite(userIdEq, trackIdEq).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    track.setFavorite(false);
                    updateFavoriteButtonIcon(false);
                    Toast.makeText(PlayerActivity.this, track.getTitle() + " удален из избранного", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PlayerActivity.this, "Ошибка удаления: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete favorite. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PlayerActivity.this, "Ошибка сети при удалении", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteButtonIcon(boolean isFavorite) {
        if (isFavorite) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    // ----------------------- СУЩЕСТВУЮЩИЕ МЕТОДЫ -----------------------

    private void togglePlayback() {
        if (playbackManager.isPlaying()) {
            playbackManager.pause();
            updatePlayPauseButton(false);
        } else {
            playbackManager.resume();
            updatePlayPauseButton(true);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play_arrow);
        }
    }

    private String formatTime(int ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) -
                TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        playbackManager.setOnTrackChangedListener(null);
        playbackManager.setOnPreparedListener(null);
    }
}