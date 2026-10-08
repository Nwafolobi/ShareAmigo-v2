package com.eduvos.sharamigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.models.Item;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.Listings;
import java.util.List;

// The signed-in student's own listings on the profile page.
public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.Holder> {

    public interface Listener {
        void onOpen(Item item);
        void onEdit(Item item);
        void onDelete(Item item);
    }

    private static final int MENU_EDIT = 1;
    private static final int MENU_DELETE = 2;
    private static final int MENU_VIEW = 3;

    private final Context context;
    private final List<Item> items;
    private final Listener listener;

    public MyListingAdapter(Context context, List<Item> items, Listener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(context).inflate(R.layout.item_my_listing, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Item item = items.get(position);
        boolean editable = "Available".equals(item.getStatus());

        h.tvTitle.setText(item.getTitle());
        h.tvMeta.setText(item.getCategoryName() + " · " + item.getCreditCost() + " credits · " + item.getConditionStatus());

        h.tvStatus.setText(Listings.statusLabel(item.getStatus()));
        if (editable) {
            h.tvStatus.setBackgroundResource(R.drawable.bg_status_dark);
            h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            h.tvStatus.setBackgroundResource(R.drawable.bg_status_outline);
            h.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.grey_dark));
        }

        int photoCount = item.getPhotos().size();
        h.tvPhotoCount.setVisibility(photoCount > 1 ? View.VISIBLE : View.GONE);
        h.tvPhotoCount.setText(photoCount + " photos");

        Glide.with(context)
                .load(ApiClient.imageUrl(item.getPhotoUrl()))
                .placeholder(R.color.grey_light)
                .error(R.color.grey_light)
                .centerCrop()
                .into(h.ivPhoto);

        h.itemView.setOnClickListener(v -> listener.onOpen(item));
        h.btnMore.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(context, h.btnMore);
            if (editable) {
                menu.getMenu().add(0, MENU_EDIT, 0, "Edit listing");
                menu.getMenu().add(0, MENU_DELETE, 1, "Delete listing");
            } else {
                menu.getMenu().add(0, MENU_VIEW, 0, "View listing");
            }
            menu.setOnMenuItemClickListener(mi -> {
                if (mi.getItemId() == MENU_EDIT) listener.onEdit(item);
                else if (mi.getItemId() == MENU_DELETE) listener.onDelete(item);
                else listener.onOpen(item);
                return true;
            });
            menu.show();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView ivPhoto;
        final TextView tvPhotoCount, tvTitle, tvMeta, tvStatus;
        final ImageButton btnMore;

        Holder(@NonNull View v) {
            super(v);
            ivPhoto = v.findViewById(R.id.ivListingPhoto);
            tvPhotoCount = v.findViewById(R.id.tvListingPhotoCount);
            tvTitle = v.findViewById(R.id.tvListingTitle);
            tvMeta = v.findViewById(R.id.tvListingMeta);
            tvStatus = v.findViewById(R.id.tvListingStatus);
            btnMore = v.findViewById(R.id.btnListingMore);
        }
    }
}
