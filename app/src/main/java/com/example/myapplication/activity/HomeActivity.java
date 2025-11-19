package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myapplication.R;
import com.example.myapplication.model.Track;
import com.example.myapplication.model.Favorite;
import com.example.myapplication.SupabaseMusicApi;
import com.example.myapplication.SupabaseMusicApi.FavoriteWrapper;
import com.example.myapplication.adapter.TrackAdapter;
import com.example.myapplication.adapter.TrackAdapter.OnTrackInteractionListener;
import com.example.myapplication.util.PlaybackManager;
import com.google.android.material.navigation.NavigationView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivity extends AppCompatActivity implements OnTrackInteractionListener {

    private static final String TAG = "HomeActivity";
    private static final int PLAYER_ACTIVITY_REQUEST_CODE = 1;

    // ⚠️ Вставьте ваш реальный URL проекта Supabase здесь!
    private static final String BASE_URL = "https://xxatkzdplbgnuibdykyd.supabase.co/rest/v1/";

    private DrawerLayout drawerLayout;
    private RecyclerView trackRecyclerView;
    private SearchView trackSearchView;
    private SupabaseMusicApi musicApi;
    private TrackAdapter adapter;
    private int currentUserId;
    private String currentUsername;

    private boolean showingFavorites = false;
    private List<Track> currentTracks = new ArrayList<>();
    private List<Track> fullTracksList = new ArrayList<>();

    private PlaybackManager playbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        trackRecyclerView = findViewById(R.id.track_recycler_view);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        trackSearchView = findViewById(R.id.track_search_view);
        setupSearchView();

        currentUserId = getIntent().getIntExtra("user_id", -1);
        currentUsername = getIntent().getStringExtra("username");

        if (currentUserId == -1) {
            Toast.makeText(this, "Гостевой вход или ошибка ID.", Toast.LENGTH_LONG).show();
            currentUsername = "Гость";
        }

        // --- УСТАНОВКА ЛОГИНА В NAV HEADER ---
        View headerView = navigationView.getHeaderView(0);
        TextView usernameTextView = headerView.findViewById(R.id.nav_header_username);
        if (usernameTextView != null && currentUsername != null) {
            usernameTextView.setText(currentUsername);
        }
        // ------------------------------------

        Button btnLogout = headerView.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogout());
        }


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        musicApi = retrofit.create(SupabaseMusicApi.class);

        playbackManager = PlaybackManager.getInstance(getApplicationContext());

        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        loadAllTracks();
    }

    // --- ЛОГИКА ПОИСКА ---
    private void setupSearchView() {
        trackSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterTracks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterTracks(newText);
                return true;
            }
        });
    }

    private void filterTracks(String query) {
        if (fullTracksList == null) return;

        List<Track> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);

        // Используем текущий список для фильтрации, чтобы фильтр работал и в "Избранном"
        List<Track> sourceList = currentTracks;

        if (lowerCaseQuery.isEmpty()) {
            if (showingFavorites) {
                loadMyFavorites(); // Перезагружаем избранное, если запрос пустой
                return;
            } else {
                // В общем каталоге, при пустом запросе, показываем полный список треков
                checkAndMarkFavorites(fullTracksList);
                return;
            }
        } else {
            // Если есть запрос, фильтруем текущий отображаемый список
            for (Track track : sourceList) {
                if (track.getTitle().toLowerCase(Locale.ROOT).contains(lowerCaseQuery) ||
                        track.getArtist().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                    filteredList.add(track);
                }
            }
        }

        currentTracks = filteredList;
        if (adapter != null) {
            adapter.updateData(currentTracks);
        }
    }


    // --- ЛОГИКА НАВИГАЦИИ И ЗАГРУЗКИ ДАННЫХ ---
    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        trackSearchView.setQuery("", false);
        trackSearchView.clearFocus();

        if (id == R.id.nav_my_favorites) {
            loadMyFavorites();
        } else if (id == R.id.nav_all_tracks) {
            loadAllTracks();
        }

        drawerLayout.closeDrawers();
        return true;
    }

    private void loadAllTracks() {
        showingFavorites = false;
        // Шаг 1: Загружаем все треки
        musicApi.getAllTracks().enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTracksList = response.body();
                    // Шаг 2: Проверяем избранное и обновляем UI
                    checkAndMarkFavorites(fullTracksList);
                } else {
                    Toast.makeText(HomeActivity.this, "Ошибка загрузки всех треков: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to load all tracks. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Ошибка сети при загрузке всех треков", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Network error loading all tracks", t);
            }
        });
    }

    /**
     * Загружает избранные треки пользователя и помечает их в общем списке.
     */
    private void checkAndMarkFavorites(List<Track> tracksToMark) {
        if (currentUserId == -1) {
            updateTrackListUI(tracksToMark);
            return;
        }

        musicApi.getFavoriteTracks("eq." + currentUserId).enqueue(new Callback<List<FavoriteWrapper>>() {
            @Override
            public void onResponse(Call<List<FavoriteWrapper>> call, Response<List<FavoriteWrapper>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    // Используем Set<Integer> для ID треков
                    Set<Integer> favoriteTrackIds = new HashSet<>();
                    for (FavoriteWrapper wrapper : response.body()) {
                        if (wrapper.track != null) {
                            favoriteTrackIds.add(wrapper.track.getId());
                        }
                    }

                    // Обновляем флаг isFavorite в основном списке
                    for (Track track : tracksToMark) {
                        track.setFavorite(favoriteTrackIds.contains(track.getId()));
                    }

                    updateTrackListUI(tracksToMark);

                } else {
                    Log.e(TAG, "Failed to load favorites for marking. Code: " + response.code());
                    updateTrackListUI(tracksToMark);
                }
            }

            @Override
            public void onFailure(Call<List<FavoriteWrapper>> call, Throwable t) {
                Log.e(TAG, "Network error loading favorites for marking", t);
                updateTrackListUI(tracksToMark);
            }
        });
    }

    private void updateTrackListUI(List<Track> tracks) {
        currentTracks = new ArrayList<>(tracks);
        if (adapter == null) {
            adapter = new TrackAdapter(currentTracks, HomeActivity.this, false);
            trackRecyclerView.setAdapter(adapter);
        } else {
            adapter.updateData(currentTracks);
        }
    }


    private void loadMyFavorites() {
        if (currentUserId == -1) {
            Toast.makeText(this, "Сначала войдите в аккаунт!", Toast.LENGTH_LONG).show();
            return;
        }
        showingFavorites = true;

        musicApi.getFavoriteTracks("eq." + currentUserId).enqueue(new Callback<List<FavoriteWrapper>>() {
            @Override
            public void onResponse(Call<List<FavoriteWrapper>> call, Response<List<FavoriteWrapper>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Track> favoriteTracks = new ArrayList<>();
                    for (FavoriteWrapper wrapper : response.body()) {
                        Track track = wrapper.track;
                        if (track != null) {
                            track.setFavorite(true);
                            favoriteTracks.add(track);
                        }
                    }
                    // Обновляем fullTracksList, чтобы фильтр работал корректно в "Избранном"
                    fullTracksList = favoriteTracks;
                    currentTracks = favoriteTracks;
                    if (adapter == null) {
                        adapter = new TrackAdapter(currentTracks, HomeActivity.this, true);
                        trackRecyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateData(currentTracks);
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "Ошибка загрузки избранного: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<FavoriteWrapper>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Ошибка сети при загрузке избранного", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- ОБРАБОТЧИКИ АДАПТЕРА И ACTIVITY ---

    @Override
    public void onFavoriteClick(Track track, int position) {
        // Управление избранным теперь происходит только в плеере
    }

    @Override
    public void onPlayClick(Track track) {
        int clickedIndex = currentTracks.indexOf(track);
        if (clickedIndex == -1) return;

        Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);

        intent.putParcelableArrayListExtra("PLAYLIST", (ArrayList<? extends Parcelable>) currentTracks);
        intent.putExtra("START_INDEX", clickedIndex);
        intent.putExtra("USER_ID", currentUserId);

        startActivityForResult(intent, PLAYER_ACTIVITY_REQUEST_CODE);

        Toast.makeText(this, "Открытие плеера для: " + track.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void handleLogout() {
        playbackManager.stop();
        Toast.makeText(HomeActivity.this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // При возвращении из PlayerActivity, обновляем текущий список
        if (requestCode == PLAYER_ACTIVITY_REQUEST_CODE) {
            if (showingFavorites) {
                loadMyFavorites();
            } else {
                // Если мы в общем каталоге, загружаем и проверяем избранное заново
                loadAllTracks();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playbackManager != null) {
            playbackManager.releasePlayer();
        }
    }
}