package com.eduvos.sharamigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.models.Item;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.eduvos.sharamigo.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivDetailPhoto;
    private TextView tvDetailCategory, tvDetailCost, tvDetailTitle, tvDetailCondition, tvDetailDescription, tvDetailDonor;
    private MaterialButton btnClaimItem;

    private Item item;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        item = (Item) getIntent().getSerializableExtra("ITEM");
        currentUserId = SessionManager.get(this).getUserId();

        ivDetailPhoto = findViewById(R.id.ivDetailPhoto);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailCost = findViewById(R.id.tvDetailCost);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailCondition = findViewById(R.id.tvDetailCondition);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailDonor = findViewById(R.id.tvDetailDonor);
        btnClaimItem = findViewById(R.id.btnClaimItem);

        if (item != null) {
            tvDetailCategory.setText(item.getCategoryName());
            tvDetailCost.setText(item.getCreditCost() + " AmigoCredits");
            tvDetailTitle.setText(item.getTitle());
            tvDetailCondition.setText("Condition: " + item.getConditionStatus());
            tvDetailDescription.setText(item.getDescription());
            tvDetailDonor.setText("Listed by: " + item.getDonorName() + " (Verified Student)");

            Glide.with(this).load(item.getPhotoUrl()).into(ivDetailPhoto);

            if (item.getListedBy() == currentUserId) {
                btnClaimItem.setText("YOU OWN THIS LISTING");
                btnClaimItem.setEnabled(false);
            } else {
                btnClaimItem.setOnClickListener(v -> confirmClaim());
            }
        }
    }

    private void confirmClaim() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Escrow Claim")
                .setMessage("Claiming \"" + item.getTitle() + "\" will reserve " + item.getCreditCost() + " AmigoCredits in Escrow.\n\nPoints are only released once you physically meet on campus and scan the QR code.")
                .setPositiveButton("Confirm Claim", (dialog, which) -> executeClaim())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeClaim() {
        btnClaimItem.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("item_id", item.getId());

        ApiClient.getService().claimItem(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnClaimItem.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    Object tx = response.body().get("transaction");
                    Object id = tx instanceof Map ? ((Map<?, ?>) tx).get("id") : null;
                    if (id instanceof Number) {
                        Toast.makeText(ItemDetailActivity.this, "Item reserved in Escrow!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ItemDetailActivity.this, QrHandoverActivity.class);
                        intent.putExtra("TXN_ID", ((Number) id).intValue());
                        startActivity(intent);
                        finish();
                        return;
                    }
                }
                Toast.makeText(ItemDetailActivity.this, ApiErrors.message(response, "Could not claim this item."), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnClaimItem.setEnabled(true);
                Toast.makeText(ItemDetailActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_LONG).show();
            }
        });
    }
}
