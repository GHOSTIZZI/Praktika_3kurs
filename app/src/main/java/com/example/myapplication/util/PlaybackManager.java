package com.example.myapplication.util;

import android.content.Context;
import android.util.Log;
import com.example.myapplication.model.Track;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackException;
import java.util.List;

/**
 * Singleton-класс для управления жизненным циклом и командами ExoPlayer.
 * Поддерживает плейлист и переключение треков.
 */
public class PlaybackManager {
    private static final String TAG = "PlaybackManager";
    private static PlaybackManager instance;
    private ExoPlayer player;
    private final Context context;

    private List<Track> tracks;
    private int currentIndex = -1;

    private OnPreparedListener preparedListener;
    private OnTrackChangedListener trackChangedListener;

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnTrackChangedListener {
        void onTrackChanged(Track newTrack);
    }

    private PlaybackManager(Context context) {
        this.context = context.getApplicationContext();
        this.player = new ExoPlayer.Builder(this.context).build();

        this.player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "ExoPlayer Error: " + error.getLocalizedMessage(), error);
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    if (preparedListener != null) {
                        preparedListener.onPrepared();
                    }
                }
                // Автопереход к следующему треку при завершении
                if (playbackState == Player.STATE_ENDED) {
                    skipToNext();
                }
            }
        });
    }

    public static synchronized PlaybackManager getInstance(Context context) {
        if (instance == null) {
            instance = new PlaybackManager(context);
        }
        return instance;
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        this.preparedListener = listener;
    }

    public void setOnTrackChangedListener(OnTrackChangedListener listener) {
        this.trackChangedListener = listener;
    }

    /**
     * Загружает новый плейлист и начинает воспроизведение.
     */
    public void playNewPlaylist(List<Track> trackList, int startIndex) {
        if (trackList == null || trackList.isEmpty() || startIndex < 0 || startIndex >= trackList.size()) {
            Log.e(TAG, "Invalid playlist or start index.");
            return;
        }

        this.tracks = trackList;
        this.currentIndex = startIndex;

        startPlaybackForCurrentIndex();
    }

    private void startPlaybackForCurrentIndex() {
        if (tracks == null || currentIndex == -1) return;

        Track track = tracks.get(currentIndex);
        String audioUrl = track.getAudioUrl();

        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }

        MediaItem mediaItem = MediaItem.fromUri(audioUrl);
        player.setMediaItem(mediaItem);

        player.setPlayWhenReady(true);
        player.prepare();

        // Уведомляем Activity о новом треке для обновления UI
        if (trackChangedListener != null) {
            trackChangedListener.onTrackChanged(track);
        }
    }

    // ----------------------- МЕТОДЫ ПЕРЕКЛЮЧЕНИЯ И УПРАВЛЕНИЯ -----------------------

    public void stop() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
            player.setPlayWhenReady(false);
        }
    }

    public void skipToPrevious() {
        if (tracks == null || tracks.isEmpty()) return;

        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = tracks.size() - 1; // Зацикливание
        }
        startPlaybackForCurrentIndex();
    }

    public void skipToNext() {
        if (tracks == null || tracks.isEmpty()) return;

        currentIndex++;
        if (currentIndex >= tracks.size()) {
            currentIndex = 0; // Зацикливание
        }
        startPlaybackForCurrentIndex();
    }

    // ----------------------- Геттеры -----------------------

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void resume() {
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    public int getDuration() {
        if (player != null && player.isCurrentMediaItemDynamic() == false) {
            return (int) player.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (player != null) {
            return (int) player.getCurrentPosition();
        }
        return 0;
    }

    public void seekTo(int positionMs) {
        if (player != null) {
            player.seekTo(positionMs);
        }
    }

    public boolean isPlaying() {
        if (player == null) {
            return false;
        }
        int state = player.getPlaybackState();
        return player.getPlayWhenReady() &&
                (state == Player.STATE_READY || state == Player.STATE_BUFFERING);
    }

    public Track getCurrentTrack() {
        if (tracks != null && currentIndex >= 0 && currentIndex < tracks.size()) {
            return tracks.get(currentIndex);
        }
        return null;
    }

    public String getCurrentAudioUrl() {
        Track current = getCurrentTrack();
        return (current != null) ? current.getAudioUrl() : "";
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            instance = null;
        }
    }
}