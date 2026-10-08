package com.eduvos.sharamigo.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.adapters.MyListingAdapter;
import com.eduvos.sharamigo.models.Item;
import com.eduvos.sharamigo.models.User;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.eduvos.sharamigo.utils.ImageCompressor;
import com.eduvos.sharamigo.utils.Listings;
import com.eduvos.sharamigo.utils.SessionManager;
import com.eduvos.sharamigo.utils.SignOut;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// The student's own page: who they are, their numbers, and everything they have listed.
public class ProfileActivity extends AppCompatActivity implements MyListingAdapter.Listener {

    private SwipeRefreshLayout swipeRefresh;
    private ImageView ivAvatar;
    private TextView tvName, tvEmail, tvMeta, tvBio;
    private TextView tvStatCredits, tvStatActive, tvStatGiven, tvStatReceived;
    private TextView tvListingsEmpty;
    private ChipGroup chipGroupStatus;

    private final List<Item> listings = new ArrayList<>();
    private MyListingAdapter adapter;

    private User user;
    private String statusFilter = null; // null = all
    private int pendingLoads = 0;

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();

    private final ActivityResultLauncher<PickVisualMediaRequest> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) uploadAvatar(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.black);
        swipeRefresh.setOnRefreshListener(this::loadAll);

        ivAvatar = findViewById(R.id.ivAvatar);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvMeta = findViewById(R.id.tvProfileMeta);
        tvBio = findViewById(R.id.tvProfileBio);
        tvStatCredits = findViewById(R.id.tvStatCredits);
        tvStatActive = findViewById(R.id.tvStatActive);
        tvStatGiven = findViewById(R.id.tvStatGiven);
        tvStatReceived = findViewById(R.id.tvStatReceived);
        tvListingsEmpty = findViewById(R.id.tvListingsEmpty);
        chipGroupStatus = findViewById(R.id.chipGroupListingStatus);

        // Fill in what we already know so the page isn't blank while loading.
        tvName.setText(SessionManager.get(this).getName());

        RecyclerView rv = findViewById(R.id.rvMyListings);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyListingAdapter(this, listings, this);
        rv.setAdapter(adapter);

        findViewById(R.id.avatarContainer).setOnClickListener(v -> onAvatarTapped());
        MaterialButton btnEdit = findViewById(R.id.btnEditProfile);
        btnEdit.setOnClickListener(v -> showEditProfile());
        MaterialButton btnPassword = findViewById(R.id.btnChangePassword);
        btnPassword.setOnClickListener(v -> showChangePassword());
        findViewById(R.id.statCredits).setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));
        findViewById(R.id.btnAddListing).setOnClickListener(v -> startActivity(new Intent(this, UploadItemActivity.class)));
        findViewById(R.id.btnSignOut).setOnClickListener(v -> confirmSignOut());

        chipGroupStatus.setOnCheckedStateChangeListener((group, ids) -> {
            int id = ids.isEmpty() ? R.id.chipStatusAll : ids.get(0);
            if (id == R.id.chipStatusActive) statusFilter = "Available";
            else if (id == R.id.chipStatusEscrow) statusFilter = "Escrow";
            else if (id == R.id.chipStatusExchanged) statusFilter = "Exchanged";
            else statusFilter = null;
            loadListings();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAll(); // also refreshes after adding or editing a listing
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        background.shutdownNow();
    }

    private void loadAll() {
        loadProfile();
        loadListings();
    }

    private void startLoad() {
        pendingLoads++;
        swipeRefresh.setRefreshing(true);
    }

    private void endLoad() {
        pendingLoads = Math.max(0, pendingLoads - 1);
        if (pendingLoads == 0) swipeRefresh.setRefreshing(false);
    }

    // ---------------------------------------------------------------- profile

    private void loadProfile() {
        startLoad();
        ApiClient.getService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            @SuppressWarnings("unchecked")
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                endLoad();
                if (response.isSuccessful() && response.body() != null) {
                    user = gson.fromJson(gson.toJson(response.body().get("user")), User.class);
                    Object stats = response.body().get("stats");
                    showProfile(stats instanceof Map ? (Map<String, Object>) stats : null);
                } else {
                    Toast.makeText(ProfileActivity.this, ApiErrors.message(response, "Could not load your profile."), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                endLoad();
                Toast.makeText(ProfileActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProfile(Map<String, Object> stats) {
        if (user == null) return;

        tvName.setText(user.getFullName());
        tvEmail.setText(user.getEmail());

        StringBuilder meta = new StringBuilder();
        if (!TextUtils.isEmpty(user.getStudentNumber())) meta.append(user.getStudentNumber());
        if (!TextUtils.isEmpty(user.getCampusName())) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(user.getCampusName());
        }
        String since = memberSince(user.getCreatedAt());
        if (since != null) meta.append("\nMember since ").append(since);
        if ("ROLE_MODERATOR".equals(user.getRole())) meta.append("\nCampus moderator");
        tvMeta.setText(meta);

        if (TextUtils.isEmpty(user.getBio())) {
            tvBio.setText("Add a short bio so other students know who they're trading with.");
            tvBio.setAlpha(0.6f);
        } else {
            tvBio.setText(user.getBio());
            tvBio.setAlpha(1f);
        }

        showAvatar();

        tvStatCredits.setText(String.valueOf(user.getCreditBalance()));
        if (stats != null) {
            tvStatActive.setText(String.valueOf(Listings.intValue(stats, "active_listings")));
            tvStatGiven.setText(String.valueOf(Listings.intValue(stats, "items_given")));
            tvStatReceived.setText(String.valueOf(Listings.intValue(stats, "items_received")));
        }

        // Keep the name used elsewhere in the app up to date.
        SessionManager session = SessionManager.get(this);
        if (session.getToken() != null) {
            session.save(session.getToken(), session.getUserId(), user.getFullName());
        }
    }

    private void showAvatar() {
        String url = user == null ? null : ApiClient.imageUrl(user.getAvatarUrl());
        if (url == null) {
            Glide.with(this).clear(ivAvatar);
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            ivAvatar.setImageResource(R.drawable.ic_person_large);
        } else {
            ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(url).circleCrop().placeholder(R.drawable.ic_person_large).into(ivAvatar);
        }
    }

    private static String memberSince(String createdAt) {
        if (createdAt == null) return null;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(createdAt);
            return d == null ? null : new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(d);
        } catch (ParseException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- avatar

    private void onAvatarTapped() {
        if (user != null && !TextUtils.isEmpty(user.getAvatarUrl())) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Profile picture")
                    .setItems(new String[]{"Choose a new photo", "Remove photo"}, (d, which) -> {
                        if (which == 0) launchAvatarPicker();
                        else removeAvatar();
                    })
                    .show();
        } else {
            launchAvatarPicker();
        }
    }

    private void launchAvatarPicker() {
        pickAvatar.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void uploadAvatar(Uri uri) {
        Toast.makeText(this, "Uploading photo…", Toast.LENGTH_SHORT).show();
        background.execute(() -> {
            String error;
            String newPath = null;
            try {
                MultipartBody.Part part = ImageCompressor.photoPart(this, uri);
                Response<Map<String, Object>> response = ApiClient.getService().uploadAvatar(part).execute();
                if (response.isSuccessful() && response.body() != null) {
                    Object path = response.body().get("avatar_url");
                    newPath = path instanceof String ? (String) path : null;
                    error = null;
                } else {
                    error = ApiErrors.message(response, "Could not upload the photo.");
                }
            } catch (java.io.IOException e) {
                error = e.getMessage() != null && e.getMessage().contains("photo") ? e.getMessage() : ApiErrors.NO_CONNECTION;
            }
            final String finalError = error;
            final String finalPath = newPath;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (finalError != null) {
                    Toast.makeText(this, finalError, Toast.LENGTH_LONG).show();
                } else {
                    if (user != null) user.setAvatarUrl(finalPath);
                    showAvatar();
                    Toast.makeText(this, "Profile picture updated.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void removeAvatar() {
        ApiClient.getService().removeAvatar(ImageCompressor.textPart("1")).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    if (user != null) user.setAvatarUrl(null);
                    showAvatar();
                } else {
                    Toast.makeText(ProfileActivity.this, ApiErrors.message(response, "Could not remove the photo."), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------- edit profile

    private void showEditProfile() {
        if (user == null) {
            Toast.makeText(this, "Profile is still loading.", Toast.LENGTH_SHORT).show();
            return;
        }
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputLayout layoutName = view.findViewById(R.id.layoutEditName);
        TextInputLayout layoutNumber = view.findViewById(R.id.layoutEditStudentNumber);
        TextInputEditText etName = view.findViewById(R.id.etEditName);
        TextInputEditText etNumber = view.findViewById(R.id.etEditStudentNumber);
        TextInputEditText etBio = view.findViewById(R.id.etEditBio);
        TextView tvEmail = view.findViewById(R.id.tvEditEmail);

        etName.setText(user.getFullName());
        etNumber.setText(user.getStudentNumber());
        etBio.setText(user.getBio());
        tvEmail.setText("Email: " + user.getEmail() + " (can't be changed — it proves you're a student)");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit profile")
                .setView(view)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = text(etName);
            String number = text(etNumber);
            String bio = text(etBio);
            layoutName.setError(name.length() < 2 ? "Enter your full name" : null);
            layoutNumber.setError(number.length() < 3 ? "Enter your student number" : null);
            if (name.length() < 2 || number.length() < 3) return;

            Map<String, Object> body = new HashMap<>();
            body.put("full_name", name);
            body.put("student_number", number);
            body.put("bio", bio);

            v.setEnabled(false);
            ApiClient.getService().updateProfile(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    v.setEnabled(true);
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Profile updated.", Toast.LENGTH_SHORT).show();
                        loadProfile();
                    } else {
                        Toast.makeText(ProfileActivity.this, ApiErrors.message(response, "Could not save your profile."), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    v.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
                }
            });
        }));
        dialog.show();
    }

    // ---------------------------------------------------------------- password

    private void showChangePassword() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputLayout layoutCurrent = view.findViewById(R.id.layoutCurrentPassword);
        TextInputLayout layoutNew = view.findViewById(R.id.layoutNewPassword);
        TextInputLayout layoutConfirm = view.findViewById(R.id.layoutConfirmPassword);
        TextInputEditText etCurrent = view.findViewById(R.id.etCurrentPassword);
        TextInputEditText etNew = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = view.findViewById(R.id.etConfirmPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Change password")
                .setView(view)
                .setPositiveButton("Change", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = raw(etCurrent);
            String fresh = raw(etNew);
            String confirm = raw(etConfirm);
            layoutCurrent.setError(current.isEmpty() ? "Enter your current password" : null);
            layoutNew.setError(fresh.length() < 8 ? "At least 8 characters" : null);
            layoutConfirm.setError(!fresh.equals(confirm) ? "Passwords don't match" : null);
            if (current.isEmpty() || fresh.length() < 8 || !fresh.equals(confirm)) return;

            Map<String, Object> body = new HashMap<>();
            body.put("current_password", current);
            body.put("new_password", fresh);

            v.setEnabled(false);
            ApiClient.getService().changePassword(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    v.setEnabled(true);
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Password changed.", Toast.LENGTH_SHORT).show();
                    } else if (response.code() == 403) {
                        layoutCurrent.setError(ApiErrors.message(response, "Current password is incorrect."));
                    } else {
                        Toast.makeText(ProfileActivity.this, ApiErrors.message(response, "Could not change your password."), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    v.setEnabled(true);
                    Toast.makeText(ProfileActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
                }
            });
        }));
        dialog.show();
    }

    // ---------------------------------------------------------------- listings

    private void loadListings() {
        startLoad();
        ApiClient.getService().getMyItems(statusFilter).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                endLoad();
                if (response.isSuccessful() && response.body() != null) {
                    List<Item> fetched = gson.fromJson(gson.toJson(response.body().get("items")),
                            new TypeToken<List<Item>>() {}.getType());
                    listings.clear();
                    if (fetched != null) listings.addAll(fetched);
                    adapter.notifyDataSetChanged();
                    showEmptyState();
                } else {
                    Toast.makeText(ProfileActivity.this, ApiErrors.message(response, "Could not load your listings."), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                endLoad();
                Toast.makeText(ProfileActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEmptyState() {
        if (!listings.isEmpty()) {
            tvListingsEmpty.setVisibility(View.GONE);
            return;
        }
        tvListingsEmpty.setVisibility(View.VISIBLE);
        tvListingsEmpty.setText(statusFilter == null
                ? "You haven't listed anything yet.\nTap Add listing to share something with your campus."
                : "Nothing here right now.");
    }

    @Override
    public void onOpen(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("ITEM", item);
        startActivity(intent);
    }

    @Override
    public void onEdit(Item item) {
        Intent intent = new Intent(this, UploadItemActivity.class);
        intent.putExtra(UploadItemActivity.EXTRA_EDIT_ITEM, item);
        startActivity(intent);
    }

    @Override
    public void onDelete(Item item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete listing?")
                .setMessage("\"" + item.getTitle() + "\" will be removed from the campus feed and its photos deleted. This can't be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteListing(item))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteListing(Item item) {
        Map<String, Object> body = new HashMap<>();
        body.put("item_id", item.getId());
        ApiClient.getService().deleteItem(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ProfileActivity.this, "Listing deleted.", Toast.LENGTH_SHORT).show();
                    loadAll();
                } else {
                    Toast.makeText(ProfileActivity.this, ApiErrors.message(response, "Could not delete the listing."), Toast.LENGTH_LONG).show();
                    loadListings();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------------------------------------------------------- sign out

    private void confirmSignOut() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign out?")
                .setPositiveButton("Sign out", (d, w) -> SignOut.run(this))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private static String raw(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
