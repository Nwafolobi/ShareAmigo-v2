package com.eduvos.sharamigo.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.network.ApiClient;
import java.util.List;

// The row of photo thumbnails on the listing form, with an "Add photo" tile at the end.
public class PhotoTileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // A photo already on the server (remoteId/remoteUrl) or one just picked from the phone (localUri).
    public static class Entry {
        public final Integer remoteId;   // null for new photos and for old single-picture listings
        public final String remoteUrl;
        public final Uri localUri;

        public Entry(Integer remoteId, String remoteUrl, Uri localUri) {
            this.remoteId = remoteId;
            this.remoteUrl = remoteUrl;
            this.localUri = localUri;
        }

        public boolean isLocal() { return localUri != null; }
    }

    public interface Listener {
        void onAddPhotos();
        void onRemovePhoto(int position);
    }

    private static final int TYPE_PHOTO = 0;
    private static final int TYPE_ADD = 1;

    private final Context context;
    private final List<Entry> entries;
    private final int maxPhotos;
    private final Listener listener;

    public PhotoTileAdapter(Context context, List<Entry> entries, int maxPhotos, Listener listener) {
        this.context = context;
        this.entries = entries;
        this.maxPhotos = maxPhotos;
        this.listener = listener;
    }

    private boolean showAddTile() {
        return entries.size() < maxPhotos;
    }

    @Override
    public int getItemCount() {
        return entries.size() + (showAddTile() ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return position < entries.size() ? TYPE_PHOTO : TYPE_ADD;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_ADD) {
            return new AddHolder(inflater.inflate(R.layout.item_photo_add, parent, false));
        }
        return new PhotoHolder(inflater.inflate(R.layout.item_photo_tile, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AddHolder) {
            holder.itemView.setOnClickListener(v -> listener.onAddPhotos());
            return;
        }
        PhotoHolder h = (PhotoHolder) holder;
        Entry e = entries.get(position);
        Object source = e.isLocal() ? e.localUri : ApiClient.imageUrl(e.remoteUrl);
        Glide.with(context).load(source).centerCrop().into(h.ivPhoto);
        h.tvCover.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
        h.btnRemove.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && pos < entries.size()) {
                listener.onRemovePhoto(pos);
            }
        });
    }

    static class PhotoHolder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final TextView tvCover;
        final ImageButton btnRemove;

        PhotoHolder(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivTilePhoto);
            tvCover = v.findViewById(R.id.tvTileCover);
            btnRemove = v.findViewById(R.id.btnTileRemove);
        }
    }

    static class AddHolder extends RecyclerView.ViewHolder {
        AddHolder(@NonNull View v) {
            super(v);
        }
    }
}
