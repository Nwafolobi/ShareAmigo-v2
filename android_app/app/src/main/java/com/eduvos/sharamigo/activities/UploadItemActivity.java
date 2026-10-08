package com.eduvos.sharamigo.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.adapters.PhotoTileAdapter;
import com.eduvos.sharamigo.models.Item;
import com.eduvos.sharamigo.models.Photo;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.network.ApiService;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.eduvos.sharamigo.utils.ImageCompressor;
import com.eduvos.sharamigo.utils.Listings;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Create a new listing with photos, or edit one of your own (pass EXTRA_EDIT_ITEM).
public class UploadItemActivity extends AppCompatActivity implements PhotoTileAdapter.Listener {

    public static final String EXTRA_EDIT_ITEM = "EDIT_ITEM";

    private static final String STATE_LOCAL_URIS = "local_uris";
    private static final String STATE_REMOVED_IDS = "removed_ids";

    private TextInputEditText etUploadTitle, etUploadDescription;
    private Spinner spinnerCategory, spinnerCondition;
    private TextView tvPriceLabel, tvPhotosLabel;
    private Slider sliderCreditPrice;
    private MaterialButton btnPublishItem;

    private Item editItem; // null when creating
    private final List<PhotoTileAdapter.Entry> photos = new ArrayList<>();
    private final ArrayList<Integer> removedPhotoIds = new ArrayList<>();
    private PhotoTileAdapter photoAdapter;

    private int selectedPrice = 15;
    private boolean saving = false;

    private final ExecutorService background = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhotos =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(Listings.MAX_PHOTOS), this::onPhotosPicked);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_item);

        editItem = (Item) getIntent().getSerializableExtra(EXTRA_EDIT_ITEM);

        etUploadTitle = findViewById(R.id.etUploadTitle);
        etUploadDescription = findViewById(R.id.etUploadDescription);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCondition = findViewById(R.id.spinnerCondition);
        tvPriceLabel = findViewById(R.id.tvPriceLabel);
        tvPhotosLabel = findViewById(R.id.tvPhotosLabel);
        sliderCreditPrice = findViewById(R.id.sliderCreditPrice);
        btnPublishItem = findViewById(R.id.btnPublishItem);

        RecyclerView rvPhotos = findViewById(R.id.rvPhotos);
        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photoAdapter = new PhotoTileAdapter(this, photos, Listings.MAX_PHOTOS, this);
        rvPhotos.setAdapter(photoAdapter);

        spinnerCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Listings.CATEGORIES));
        spinnerCondition.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Listings.CONDITIONS));
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePriceRange(Listings.CATEGORIES[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        sliderCreditPrice.addOnChangeListener((slider, value, fromUser) -> {
            selectedPrice = (int) value;
            showPrice();
        });

        if (editItem != null) {
            fillForEdit();
        } else {
            updatePriceRange(Listings.CATEGORIES[0]);
        }

        if (savedInstanceState != null) {
            ArrayList<Integer> removed = savedInstanceState.getIntegerArrayList(STATE_REMOVED_IDS);
            if (removed != null) {
                removedPhotoIds.addAll(removed);
                dropRemoved();
            }
            ArrayList<Uri> locals = savedInstanceState.getParcelableArrayList(STATE_LOCAL_URIS);
            if (locals != null) {
                for (Uri uri : locals) photos.add(new PhotoTileAdapter.Entry(null, null, uri));
            }
        }
        refreshPhotos();

        btnPublishItem.setOnClickListener(v -> save());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ArrayList<Uri> locals = new ArrayList<>();
        for (PhotoTileAdapter.Entry e : photos) {
            if (e.isLocal()) locals.add(e.localUri);
        }
        outState.putParcelableArrayList(STATE_LOCAL_URIS, locals);
        outState.putIntegerArrayList(STATE_REMOVED_IDS, removedPhotoIds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        background.shutdown(); // let a save that is already running finish
    }

    @Override
    public void onBackPressed() {
        if (saving) {
            Toast.makeText(this, "Please wait, your listing is still uploading.", Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    // ---------------------------------------------------------------- edit mode

    private void fillForEdit() {
        ((TextView) findViewById(R.id.tvUploadHeading)).setText("Edit Listing");
        ((TextView) findViewById(R.id.tvUploadSubheading)).setText("Changes show on the campus feed straight away");
        btnPublishItem.setText("SAVE CHANGES");

        etUploadTitle.setText(editItem.getTitle());
        etUploadDescription.setText(editItem.getDescription());

        int cat = Math.max(0, Arrays.asList(Listings.CATEGORIES).indexOf(editItem.getCategoryName()));
        spinnerCategory.setSelection(cat);
        int cond = Math.max(0, Arrays.asList(Listings.CONDITIONS).indexOf(editItem.getConditionStatus()));
        spinnerCondition.setSelection(cond);

        updatePriceRange(Listings.CATEGORIES[cat]);
        int max = Listings.maxCredits(Listings.CATEGORIES[cat]);
        sliderCreditPrice.setValue(Math.max(1, Math.min(max, editItem.getCreditCost())));

        setRemotePhotos(editItem.getPhotos());
        loadRemotePhotos(); // the feed doesn't include photo ids, so fetch them
    }

    private void setRemotePhotos(List<Photo> remote) {
        List<PhotoTileAdapter.Entry> locals = new ArrayList<>();
        for (PhotoTileAdapter.Entry e : photos) {
            if (e.isLocal()) locals.add(e);
        }
        photos.clear();
        if (remote != null && !remote.isEmpty()) {
            for (Photo p : remote) photos.add(new PhotoTileAdapter.Entry(p.getId(), p.getPhotoUrl(), null));
        } else if (editItem.getPhotoUrl() != null && !editItem.getPhotoUrl().isEmpty()) {
            // Older listing with a single picture and no photo records.
            photos.add(new PhotoTileAdapter.Entry(null, editItem.getPhotoUrl(), null));
        }
        dropRemoved();
        photos.addAll(locals);
    }

    private void dropRemoved() {
        for (int i = photos.size() - 1; i >= 0; i--) {
            Integer id = photos.get(i).remoteId;
            if (id != null && removedPhotoIds.contains(id)) photos.remove(i);
        }
    }

    private void loadRemotePhotos() {
        ApiClient.getService().getItemPhotos(editItem.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (!response.isSuccessful() || response.body() == null || saving) return;
                Gson gson = new Gson();
                List<Photo> remote = gson.fromJson(gson.toJson(response.body().get("photos")),
                        new TypeToken<List<Photo>>() {}.getType());
                setRemotePhotos(remote);
                refreshPhotos();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Keep what we have; saving will still work.
            }
        });
    }

    // ---------------------------------------------------------------- price

    private void updatePriceRange(String category) {
        int max = Listings.maxCredits(category);
        if (sliderCreditPrice.getValue() > max) {
            sliderCreditPrice.setValue(max);
        }
        sliderCreditPrice.setValueTo(max);
        selectedPrice = (int) sliderCreditPrice.getValue();
        showPrice();
    }

    private void showPrice() {
        tvPriceLabel.setText("Credit Valuation: " + selectedPrice + " AmigoCredits (max "
                + (int) sliderCreditPrice.getValueTo() + ")");
    }

    // ---------------------------------------------------------------- photos

    @Override
    public void onAddPhotos() {
        if (saving) return;
        if (photos.size() >= Listings.MAX_PHOTOS) {
            Toast.makeText(this, "A listing can have up to " + Listings.MAX_PHOTOS + " photos.", Toast.LENGTH_SHORT).show();
            return;
        }
        pickPhotos.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void onPhotosPicked(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;
        int free = Listings.MAX_PHOTOS - photos.size();
        int added = 0;
        for (Uri uri : uris) {
            if (added >= free) break;
            photos.add(new PhotoTileAdapter.Entry(null, null, uri));
            added++;
        }
        if (uris.size() > added) {
            Toast.makeText(this, "Only " + Listings.MAX_PHOTOS + " photos per listing, so we kept the first " + added + ".", Toast.LENGTH_LONG).show();
        }
        refreshPhotos();
    }

    @Override
    public void onRemovePhoto(int position) {
        if (saving) return;
        PhotoTileAdapter.Entry e = photos.remove(position);
        if (e.remoteId != null) removedPhotoIds.add(e.remoteId);
        refreshPhotos();
    }

    private void refreshPhotos() {
        photoAdapter.notifyDataSetChanged();
        tvPhotosLabel.setText("Photos (" + photos.size() + "/" + Listings.MAX_PHOTOS + ")");
    }

    // ---------------------------------------------------------------- save

    private void save() {
        if (saving) return;

        String title = etUploadTitle.getText() == null ? "" : etUploadTitle.getText().toString().trim();
        String description = etUploadDescription.getText() == null ? "" : etUploadDescription.getText().toString().trim();

        if (title.length() < 3) {
            etUploadTitle.setError("Enter a title (at least 3 characters)");
            etUploadTitle.requestFocus();
            return;
        }
        if (photos.isEmpty()) {
            Toast.makeText(this, "Add at least one photo of the item.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("category_name", spinnerCategory.getSelectedItem().toString());
        body.put("condition_status", spinnerCondition.getSelectedItem().toString());
        body.put("description", description);
        body.put("credit_cost", selectedPrice);

        List<Uri> newPhotos = new ArrayList<>();
        for (PhotoTileAdapter.Entry e : photos) {
            if (e.isLocal()) newPhotos.add(e.localUri);
        }
        List<Integer> toDelete = new ArrayList<>(removedPhotoIds);

        setSaving(true, editItem == null ? "PUBLISHING…" : "SAVING…");
        background.execute(() -> runSave(body, title, newPhotos, toDelete));
    }

    // Runs on the background thread, one request after another.
    private void runSave(Map<String, Object> body, String title, List<Uri> newPhotos, List<Integer> toDelete) {
        ApiService api = ApiClient.getService();
        try {
            int itemId;
            if (editItem == null) {
                Response<Map<String, Object>> created = api.createItem(body).execute();
                if (!created.isSuccessful() || created.body() == null) {
                    fail(ApiErrors.message(created, "Could not publish the item."));
                    return;
                }
                itemId = readItemId(created.body());
                if (itemId <= 0) itemId = findMyNewestItem(api, title);
                if (itemId <= 0) {
                    finishWith("Your listing is on the feed, but the photos couldn't be attached. Open it from your profile to add them.");
                    return;
                }
            } else {
                itemId = editItem.getId();
                body.put("item_id", itemId);
                Response<Map<String, Object>> updated = api.updateItem(body).execute();
                if (!updated.isSuccessful()) {
                    fail(ApiErrors.message(updated, "Could not save your changes."));
                    return;
                }
                // Remove first so there's room for the new photos (max 4).
                for (Integer photoId : toDelete) {
                    Map<String, Object> del = new HashMap<>();
                    del.put("photo_id", photoId);
                    api.deleteItemPhoto(del).execute();
                }
            }

            int failed = 0;
            String lastError = null;
            for (int i = 0; i < newPhotos.size(); i++) {
                final String progress = "UPLOADING PHOTO " + (i + 1) + " OF " + newPhotos.size() + "…";
                runOnUiThread(() -> btnPublishItem.setText(progress));
                try {
                    Response<Map<String, Object>> up = api.uploadItemPhoto(
                            ImageCompressor.textPart(String.valueOf(itemId)),
                            ImageCompressor.photoPart(this, newPhotos.get(i))).execute();
                    if (!up.isSuccessful()) {
                        failed++;
                        lastError = ApiErrors.message(up, "upload failed");
                    }
                } catch (IOException e) {
                    failed++;
                    lastError = e.getMessage();
                }
            }

            if (failed > 0) {
                finishWith("Listing saved, but " + failed + " photo" + (failed == 1 ? "" : "s")
                        + " didn't upload" + (lastError != null ? " (" + lastError + ")" : "")
                        + ". You can add them again from your profile.");
            } else {
                finishWith(editItem == null ? "Resource published to campus feed!" : "Listing updated.");
            }
        } catch (IOException e) {
            fail(ApiErrors.NO_CONNECTION);
        }
    }

    // create_item.php returns the new id as "item_id" (or inside "item").
    @SuppressWarnings("unchecked")
    private static int readItemId(Map<String, Object> body) {
        int id = Listings.intValue(body, "item_id");
        if (id <= 0 && body.get("item") instanceof Map) {
            id = Listings.intValue((Map<String, Object>) body.get("item"), "id");
        }
        return id;
    }

    // Fallback if the server didn't send the id: the newest of my listings with this title.
    private static int findMyNewestItem(ApiService api, String title) throws IOException {
        Response<Map<String, Object>> mine = api.getMyItems("Available").execute();
        if (!mine.isSuccessful() || mine.body() == null) return 0;
        Gson gson = new Gson();
        List<Item> items = gson.fromJson(gson.toJson(mine.body().get("items")), new TypeToken<List<Item>>() {}.getType());
        if (items == null) return 0;
        for (Item it : items) {
            if (title.equals(it.getTitle())) return it.getId();
        }
        return 0;
    }

    private void fail(String message) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            setSaving(false, null);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void finishWith(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            if (!isFinishing() && !isDestroyed()) finish();
        });
    }

    private void setSaving(boolean busy, String label) {
        saving = busy;
        btnPublishItem.setEnabled(!busy);
        if (busy) {
            btnPublishItem.setText(label);
        } else {
            btnPublishItem.setText(editItem == null ? "PUBLISH TO CAMPUS FEED" : "SAVE CHANGES");
        }
    }
}
