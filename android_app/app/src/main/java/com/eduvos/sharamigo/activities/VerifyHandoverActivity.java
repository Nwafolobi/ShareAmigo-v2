package com.eduvos.sharamigo.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.google.android.material.button.MaterialButton;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Donor view: scan the receiver's QR code (or type the PIN) to release the escrow credits.
public class VerifyHandoverActivity extends AppCompatActivity {

    private int transactionId;
    private EditText etPin;
    private MaterialButton btnScanQr, btnVerifyPin;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() == null) {
                    return; // scan cancelled
                }
                String[] parts = result.getContents().split("\\|");
                if (parts.length != 3 || !"SHARAMIGO".equals(parts[0])) {
                    Toast.makeText(this, "That is not a SharAmigo QR code.", Toast.LENGTH_LONG).show();
                    return;
                }
                int scannedId;
                try {
                    scannedId = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "That is not a SharAmigo QR code.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (scannedId != transactionId) {
                    Toast.makeText(this, "This QR code is for a different exchange.", Toast.LENGTH_LONG).show();
                    return;
                }
                verify(parts[2]);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_handover);

        transactionId = getIntent().getIntExtra("TXN_ID", 0);
        if (transactionId <= 0) {
            Toast.makeText(this, "Missing exchange details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etPin = findViewById(R.id.etPin);
        btnScanQr = findViewById(R.id.btnScanQr);
        btnVerifyPin = findViewById(R.id.btnVerifyPin);

        btnScanQr.setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt("Scan the receiver's QR code");
            options.setBeepEnabled(false);
            options.setOrientationLocked(false);
            scanLauncher.launch(options);
        });

        btnVerifyPin.setOnClickListener(v -> {
            String pin = etPin.getText().toString().trim();
            if (pin.length() != 6) {
                Toast.makeText(this, "Enter the 6 digit PIN.", Toast.LENGTH_SHORT).show();
                return;
            }
            verify(pin);
        });
    }

    private void setBusy(boolean busy) {
        btnScanQr.setEnabled(!busy);
        btnVerifyPin.setEnabled(!busy);
    }

    private void verify(String code) {
        setBusy(true);

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_id", transactionId);
        body.put("qr_hash_or_pin", code);

        ApiClient.getService().verifyQr(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setBusy(false);
                if (response.isSuccessful()) {
                    Toast.makeText(VerifyHandoverActivity.this, "Exchange completed! Credits released.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(VerifyHandoverActivity.this, ApiErrors.message(response, "Verification failed."), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setBusy(false);
                Toast.makeText(VerifyHandoverActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_LONG).show();
            }
        });
    }
}
