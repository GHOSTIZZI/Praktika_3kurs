package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.Track;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<Track> tracks;
    private final OnTrackInteractionListener listener;
    private final boolean showFavorites;

    public TrackAdapter(List<Track> tracks, OnTrackInteractionListener listener, boolean showFavorites) {
        this.tracks = tracks;
        this.listener = listener;
        this.showFavorites = showFavorites;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_item, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);

        Glide.with(holder.trackCover.getContext())
                .load(track.getCoverUrl())
                .placeholder(R.drawable.ic_music_note)
                .into(holder.trackCover);

        holder.trackTitle.setText(track.getTitle());
        holder.trackArtist.setText(track.getArtist());

        // ✅ ЛОГИКА ОТОБРАЖЕНИЯ СЕРДЕЧКА
        if (track.isFavorite()) {
            // Если трек избранный, показываем закрашенное сердечко
            holder.favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
            holder.favoriteButton.setVisibility(View.VISIBLE);
        } else {
            // Если трек не избранный, скрываем сердечко в каталоге
            holder.favoriteButton.setVisibility(View.GONE);
        }
        // Кнопка не имеет слушателя, она используется только как индикатор.

        holder.itemView.setOnClickListener(v -> listener.onPlayClick(track));
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public void updateData(List<Track> newTracks) {
        this.tracks = newTracks;
        notifyDataSetChanged();
    }

    public void removeTrack(int position) {
        tracks.remove(position);
        notifyItemRemoved(position);
    }


    public static class TrackViewHolder extends RecyclerView.ViewHolder {

        public final ImageView trackCover;
        public final TextView trackTitle;
        public final TextView trackArtist;
        public final ImageButton favoriteButton;

        public TrackViewHolder(View itemView) {
            super(itemView);
            trackCover = itemView.findViewById(R.id.track_cover_image);
            trackTitle = itemView.findViewById(R.id.track_title);
            trackArtist = itemView.findViewById(R.id.track_artist);
            favoriteButton = itemView.findViewById(R.id.track_btn_favorite);
        }
    }

    public interface OnTrackInteractionListener {
        void onFavoriteClick(Track track, int position);
        void onPlayClick(Track track);
    }
}