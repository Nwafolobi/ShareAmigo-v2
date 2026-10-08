package com.eduvos.sharamigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.adapters.ItemAdapter;
import com.eduvos.sharamigo.models.Item;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.eduvos.sharamigo.utils.SessionManager;
import com.eduvos.sharamigo.utils.SignOut;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFeedActivity extends AppCompatActivity {

    private RecyclerView rvItems;
    private ItemAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private int currentUserId;
    private String selectedCategory = "All";
    private String searchQuery = "";

    private MaterialButton btnWalletBalance;
    private EditText etSearch;
    private ChipGroup chipGroupCategories;
    private ExtendedFloatingActionButton fabUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_feed);

        currentUserId = SessionManager.get(this).getUserId();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_home);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_profile || item.getItemId() == R.id.action_my_listings) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_logout) {
                SignOut.run(this);
                return true;
            }
            return false;
        });

        btnWalletBalance = findViewById(R.id.btnWalletBalance);
        etSearch = findViewById(R.id.etSearch);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        fabUpload = findViewById(R.id.fabUpload);
        rvItems = findViewById(R.id.rvItems);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ItemAdapter(this, itemList, currentUserId);
        rvItems.setAdapter(adapter);

        btnWalletBalance.setOnClickListener(v -> {
            Intent intent = new Intent(this, WalletActivity.class);
            startActivity(intent);
        });

        fabUpload.setOnClickListener(v -> {
            Intent intent = new Intent(this, UploadItemActivity.class);
            startActivity(intent);
        });

        setupCategoryChips();
        setupSearch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadItems();
        loadBalance();
    }

    private void setupCategoryChips() {
        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedCategory = "All";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipTextbooks) selectedCategory = "Textbooks";
                else if (id == R.id.chipStationery) selectedCategory = "Stationery";
                else if (id == R.id.chipFood) selectedCategory = "Food/Meals";
                else if (id == R.id.chipClothing) selectedCategory = "Clothing";
                else selectedCategory = "All";
            }
            loadItems();
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                loadItems();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadItems() {
        ApiClient.getService().getItems(selectedCategory, searchQuery).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Gson gson = new Gson();
                    String json = gson.toJson(response.body().get("items"));
                    List<Item> fetched = gson.fromJson(json, new TypeToken<List<Item>>(){}.getType());
                    itemList.clear();
                    if (fetched != null) {
                        itemList.addAll(fetched);
                    }
                    adapter.notifyDataSetChanged();
                } else if (!response.isSuccessful()) {
                    Toast.makeText(HomeFeedActivity.this, ApiErrors.message(response, "Could not load items."), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(HomeFeedActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBalance() {
        ApiClient.getService().getLedger().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object balance = response.body().get("credit_balance");
                    if (balance instanceof Number) {
                        btnWalletBalance.setText(((Number) balance).intValue() + " Pts");
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Keep the old text. The feed already shows a connection message.
            }
        });
    }

}
