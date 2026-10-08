package com.eduvos.sharamigo.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.activities.ItemDetailActivity;
import com.eduvos.sharamigo.models.Item;
import com.eduvos.sharamigo.network.ApiClient;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private final Context context;
    private final List<Item> itemList;
    private final int currentUserId;

    public ItemAdapter(Context context, List<Item> itemList, int currentUserId) {
        this.context = context;
        this.itemList = itemList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = itemList.get(position);
        holder.tvItemTitle.setText(item.getTitle());
        holder.tvItemCategory.setText(item.getCategoryName());
        holder.tvItemCost.setText(item.getCreditCost() + " Credits");
        holder.tvItemDonor.setText("Donor: " + item.getDonorName() + " • " + item.getConditionStatus());

        Glide.with(context)
                .load(ApiClient.imageUrl(item.getPhotoUrl()))
                .placeholder(R.color.grey_light)
                .into(holder.ivItemPhoto);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ItemDetailActivity.class);
            intent.putExtra("ITEM", item);
            intent.putExtra("USER_ID", currentUserId);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemPhoto;
        TextView tvItemCategory, tvItemCost, tvItemTitle, tvItemDonor;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemPhoto = itemView.findViewById(R.id.ivItemPhoto);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemCost = itemView.findViewById(R.id.tvItemCost);
            tvItemTitle = itemView.findViewById(R.id.tvItemTitle);
            tvItemDonor = itemView.findViewById(R.id.tvItemDonor);
        }
    }
}
