package com.eduvos.sharamigo.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadItemActivity extends AppCompatActivity {

    private TextInputEditText etUploadTitle, etUploadDescription;
    private Spinner spinnerCategory, spinnerCondition;
    private TextView tvPriceLabel;
    private Slider sliderCreditPrice;
    private MaterialButton btnPublishItem;

    private int selectedPrice = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_item);

        etUploadTitle = findViewById(R.id.etUploadTitle);
        etUploadDescription = findViewById(R.id.etUploadDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        tvPriceLabel = findViewById(R.id.tvPriceLabel);
        sliderCreditPrice = findViewById(R.id.sliderCreditPrice);
        btnPublishItem = findViewById(R.id.btnPublishItem);

        String[] categories = {"Textbooks", "Stationery", "Food/Meals", "Clothing"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        String[] conditions = {"Brand New", "Like New", "Good", "Fair"};
        ArrayAdapter<String> condAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, conditions);
        spinnerCondition.setAdapter(condAdapter);

        sliderCreditPrice.addOnChangeListener((slider, value, fromUser) -> {
            selectedPrice = (int) value;
            tvPriceLabel.setText("Credit Valuation: " + selectedPrice + " AmigoCredits");
        });

        btnPublishItem.setOnClickListener(v -> publishListing());
    }

    private void publishListing() {
        String title = etUploadTitle.getText().toString().trim();
        String description = etUploadDescription.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String condition = spinnerCondition.getSelectedItem().toString();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter an item title.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPublishItem.setEnabled(false);

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("category_name", category);
        body.put("condition_status", condition);
        body.put("description", description);
        body.put("credit_cost", selectedPrice);

        ApiClient.getService().createItem(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnPublishItem.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(UploadItemActivity.this, "Resource published to campus feed!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(UploadItemActivity.this, ApiErrors.message(response, "Could not publish the item."), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnPublishItem.setEnabled(true);
                Toast.makeText(UploadItemActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_LONG).show();
            }
        });
    }
}
