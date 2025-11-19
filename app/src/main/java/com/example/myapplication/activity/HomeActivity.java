package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView; // ✅ НОВЫЙ ИМПОРТ
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
import java.util.List;
import java.util.Locale; // ✅ НОВЫЙ ИМПОРТ для поиска
import retrofit2.*;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeActivity extends AppCompatActivity implements OnTrackInteractionListener {

    private static final String TAG = "HomeActivity";

    private DrawerLayout drawerLayout;
    private RecyclerView trackRecyclerView;
    private SearchView trackSearchView; // ✅ НОВАЯ ПЕРЕМЕННАЯ
    private SupabaseMusicApi musicApi;
    private TrackAdapter adapter;
    private int currentUserId;
    private boolean showingFavorites = false;
    private List<Track> currentTracks = new ArrayList<>();
    private List<Track> fullTracksList = new ArrayList<>(); // ✅ НОВОЕ: Полный список для поиска

    private PlaybackManager playbackManager;

    private static final String BASE_URL = "https://xxatkzdplbgnuibdykyd.supabase.co/rest/v1/";

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

        trackSearchView = findViewById(R.id.track_search_view); // ✅ Инициализация SearchView
        setupSearchView(); // ✅ Настройка SearchView

        currentUserId = getIntent().getIntExtra("user_id", -1);

        if (currentUserId == -1) {
            Toast.makeText(this, "Гостевой вход или ошибка ID.", Toast.LENGTH_LONG).show();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        musicApi = retrofit.create(SupabaseMusicApi.class);

        playbackManager = PlaybackManager.getInstance(getApplicationContext());

        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);

        View headerView = navigationView.getHeaderView(0);
        Button btnLogout = headerView.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogout());
        }

        loadAllTracks();
    }

    // ----------------------- ЛОГИКА ПОИСКА (НОВАЯ) -----------------------

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
        if (currentTracks == null) return;

        List<Track> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);

        // Используем полный список треков для фильтрации, если мы не в избранном
        List<Track> sourceList = showingFavorites ? currentTracks : fullTracksList;

        if (lowerCaseQuery.isEmpty()) {
            filteredList.addAll(sourceList);
        } else {
            for (Track track : sourceList) {
                if (track.getTitle().toLowerCase(Locale.ROOT).contains(lowerCaseQuery) ||
                        track.getArtist().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                    filteredList.add(track);
                }
            }
        }

        // Обновляем текущий отображаемый список и адаптер
        currentTracks = filteredList;
        if (adapter != null) {
            adapter.updateData(currentTracks);
        }
    }


    // ----------------------- ЛОГИКА НАВИГАЦИИ И ЗАГРУЗКИ ДАННЫХ -----------------------

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Сброс поиска при смене вкладки
        trackSearchView.setQuery("", false);
        trackSearchView.clearFocus();

        if (id == R.id.nav_my_favorites) {
            loadMyFavorites();
        } else if (id == R.id.nav_all_tracks) {
            loadAllTracks();
        } else if (id == R.id.nav_add_track) {
            Toast.makeText(this, "Добавление треков отключено.", Toast.LENGTH_SHORT).show();
        }
        drawerLayout.closeDrawers();
        return true;
    }

    private void loadAllTracks() {
        showingFavorites = false;
        musicApi.getAllTracks().enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTracksList = response.body(); // Сохраняем полный список
                    currentTracks = new ArrayList<>(fullTracksList);

                    if (adapter == null) {
                        adapter = new TrackAdapter(currentTracks, HomeActivity.this, false);
                        trackRecyclerView.setAdapter(adapter);
                    } else {
                        adapter.updateData(currentTracks);
                    }
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

    // ----------------------- ОБРАБОТЧИКИ АДАПТЕРА -----------------------

    @Override
    public void onFavoriteClick(Track track, int position) {
        // Логика перенесена в PlayerActivity
    }

    @Override
    public void onPlayClick(Track track) {
        int clickedIndex = currentTracks.indexOf(track);
        if (clickedIndex == -1) return;

        Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);

        // При клике на трек, мы передаем текущий ОТФИЛЬТРОВАННЫЙ список
        intent.putParcelableArrayListExtra("PLAYLIST", (ArrayList<? extends Parcelable>) currentTracks);
        intent.putExtra("START_INDEX", clickedIndex);
        intent.putExtra("USER_ID", currentUserId);

        startActivity(intent);

        Toast.makeText(this, "Открытие плеера для: " + track.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // ... (handleLogout, addToFavorites, deleteFromFavorites, onActivityResult, onDestroy остаются как есть) ...

    private void handleLogout() {
        playbackManager.stop();
        Toast.makeText(HomeActivity.this, "Вы вышли из системы", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void addToFavorites(Track track, int position) {
        Favorite favorite = new Favorite(currentUserId, track.getId());
        musicApi.addFavorite(favorite).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.code() == 201) {
                    track.setFavorite(true);
                } else {
                    Log.e(TAG, "Failed to add favorite. Code: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { /* ... */ }
        });
    }

    private void deleteFromFavorites(Track track, int position) {
        String userIdEq = "eq." + currentUserId;
        String trackIdEq = "eq." + track.getId();

        musicApi.deleteFavorite(userIdEq, trackIdEq).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    if (showingFavorites) {
                        adapter.removeTrack(position);
                    } else {
                        track.setFavorite(false);
                    }
                } else {
                    Log.e(TAG, "Failed to delete favorite. Code: " + response.code());
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) { /* ... */ }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // При возвращении из плеера обновляем нужный список
            if (showingFavorites) {
                loadMyFavorites();
            } else {
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