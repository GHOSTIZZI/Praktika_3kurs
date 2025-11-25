package com.example.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout; // 💡 Импортируем RelativeLayout
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SearchView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.model.Track;
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

    // 💡 Новые поля для управления режимами
    private RelativeLayout headerContainer;
    private TextView sectionTitle;
    private Button btnViewMore;
    private boolean isDashboardView = true; // Флаг для переключения между дашбордом и полным каталогом
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
        // Начальный LayoutManager будет установлен в loadDashboardView()

        trackSearchView = findViewById(R.id.track_search_view);
        setupSearchView();

        // 💡 Находим новые элементы UI
        headerContainer = findViewById(R.id.header_container);
        sectionTitle = findViewById(R.id.tv_section_title);
        btnViewMore = findViewById(R.id.btn_view_more);
        btnViewMore.setOnClickListener(v -> loadFullCatalogView()); // Слушатель для кнопки "Еще"

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

        // 💡 Загружаем режим дашборда при запуске
        loadDashboardView();
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

        // Поиск должен работать только в режиме полного каталога или избранного
        if (isDashboardView) return;

        List<Track> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase(Locale.ROOT);

        // Используем полный список (каталог или избранное) для фильтрации
        List<Track> sourceList = fullTracksList;

        if (lowerCaseQuery.isEmpty()) {
            if (showingFavorites) {
                // Если запрос пустой в Избранном, показываем все избранное
                loadMyFavorites();
                return;
            } else {
                // Если запрос пустой в Каталоге, показываем весь каталог
                checkAndMarkFavorites(fullTracksList);
                return;
            }
        } else {
            // Если есть запрос, фильтруем полный список
            for (Track track : sourceList) {
                if (track.getTitle().toLowerCase(Locale.ROOT).contains(lowerCaseQuery) ||
                        track.getArtist().toLowerCase(Locale.ROOT).contains(lowerCaseQuery)) {
                    filteredList.add(track);
                }
            }
        }

        // Обновляем UI с отфильтрованным списком
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
            // При нажатии в меню, переходим сразу в полный каталог
            if (fullTracksList.isEmpty()) {
                // Если данные еще не загружены, загружаем их, а затем показываем полный каталог
                loadAllTracksAndShowFullCatalog();
            } else {
                loadFullCatalogView();
            }
        }

        // 💡 Здесь можно добавить goToRadioActivity() если он нужен

        drawerLayout.closeDrawers();
        return true;
    }

    /**
     * Загружает все треки и затем отображает полный каталог.
     * Используется при выборе "Каталог треков" из меню.
     */
    private void loadAllTracksAndShowFullCatalog() {
        musicApi.getAllTracks().enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTracksList = response.body();
                    loadFullCatalogView();
                } else {
                    Toast.makeText(HomeActivity.this, "Ошибка загрузки всех треков: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Ошибка сети при загрузке всех треков", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 💡 Загружает все треки, затем отображает ограниченный дашборд (2 ряда по 4).
     */
    private void loadDashboardView() {
        // Шаг 1: Загружаем все треки
        musicApi.getAllTracks().enqueue(new Callback<List<Track>>() {
            @Override
            public void onResponse(Call<List<Track>> call, Response<List<Track>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullTracksList = response.body();

                    // Обновляем UI для режима дашборда
                    isDashboardView = true;
                    showingFavorites = false;

                    headerContainer.setVisibility(View.VISIBLE);
                    sectionTitle.setText("Новые треки");
                    btnViewMore.setVisibility(View.VISIBLE);
                    trackSearchView.setVisibility(View.GONE);

                    // Устанавливаем GridLayoutManager (4 столбца)
                    trackRecyclerView.setLayoutManager(new GridLayoutManager(HomeActivity.this, 4));
                    // Отключаем прокрутку, чтобы показать только 2 ряда
                    trackRecyclerView.setNestedScrollingEnabled(false);

                    // Шаг 2: Проверяем избранное и обновляем UI (который ограничит список до 8)
                    checkAndMarkFavorites(fullTracksList);
                } else {
                    Toast.makeText(HomeActivity.this, "Ошибка загрузки треков: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Track>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 💡 Переключает на полный, прокручиваемый Grid-вид всего каталога (4 столбца).
     */
    private void loadFullCatalogView() {
        isDashboardView = false;
        showingFavorites = false;

        // Скрываем элементы дашборда, показываем поиск
        headerContainer.setVisibility(View.GONE);
        btnViewMore.setVisibility(View.GONE);
        trackSearchView.setVisibility(View.VISIBLE);

        // Включаем прокрутку для полного списка
        trackRecyclerView.setNestedScrollingEnabled(true);

        // Устанавливаем вертикальный Grid Layout для полного каталога (4 столбца)
        trackRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        // Используем полный список и обновляем UI
        checkAndMarkFavorites(fullTracksList);
    }

    // Переименованный старый loadAllTracks, который теперь просто заглушка, т.к.
    // логика загрузки в loadDashboardView и loadAllTracksAndShowFullCatalog
    private void loadAllTracks() {
        // Эта функция вызывается в onCreate. Мы просто перенаправляем ее на loadDashboardView
        loadDashboardView();
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

        List<Track> listToDisplay = new ArrayList<>(tracks);

        // 💡 Если в режиме дашборда, ограничиваем список до 8
        if (isDashboardView && !listToDisplay.isEmpty()) {
            listToDisplay = listToDisplay.subList(0, Math.min(8, listToDisplay.size()));
        }

        currentTracks = listToDisplay; // Обновляем текущий список

        if (adapter == null) {
            // Note: canDeleteFavorite is false, т.к. это общий каталог/дашборд
            adapter = new TrackAdapter(currentTracks, HomeActivity.this, showingFavorites);
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

        // 💡 Настройки для режима "Избранное"
        isDashboardView = false; // Избранное всегда полный список
        showingFavorites = true;

        // Скрываем элементы дашборда, показываем поиск
        headerContainer.setVisibility(View.GONE);
        btnViewMore.setVisibility(View.GONE);
        trackSearchView.setVisibility(View.VISIBLE);

        // Включаем прокрутку для Избранного
        trackRecyclerView.setNestedScrollingEnabled(true);
        // Устанавливаем GridLayoutManager (4 столбца)
        trackRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));

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
                        // Note: canDeleteFavorite is true для Избранного
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

        // В режиме дашборда, если кликнули по ограниченному списку,
        // нужно передать полный список для воспроизведения!
        List<Track> playlistToSend = showingFavorites ? fullTracksList : fullTracksList;

        // Находим индекс кликнутого трека в полном списке
        int fullListIndex = playlistToSend.indexOf(track);
        if (fullListIndex == -1) fullListIndex = 0;

        Intent intent = new Intent(HomeActivity.this, PlayerActivity.class);

        intent.putParcelableArrayListExtra("PLAYLIST", (ArrayList<? extends Parcelable>) playlistToSend);
        intent.putExtra("START_INDEX", fullListIndex);
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
            } else if (!isDashboardView) {
                // Если мы в полном каталоге, загружаем и проверяем избранное заново
                loadAllTracksAndShowFullCatalog();
            } else {
                // Если вернулись на дашборд, просто обновляем его
                loadDashboardView();
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