package com.eduvos.sharamigo.adapters;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.network.ApiClient;
import java.util.List;

// Full-width photos on the item detail screen (used with ViewPager2).
public class PhotoPagerAdapter extends RecyclerView.Adapter<PhotoPagerAdapter.Holder> {

    private final Context context;
    private final List<String> urls;

    public PhotoPagerAdapter(Context context, List<String> urls) {
        this.context = context;
        this.urls = urls;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(context);
        iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setContentDescription("Listing photo");
        return new Holder(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Glide.with(context)
                .load(ApiClient.imageUrl(urls.get(position)))
                .placeholder(R.color.grey_light)
                .error(R.color.grey_light)
                .into((ImageView) holder.itemView);
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        Holder(@NonNull ImageView v) {
            super(v);
        }
    }
}
