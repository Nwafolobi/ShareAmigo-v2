package com.eduvos.sharamigo.activities;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Receiver view: shows the QR code and PIN the donor needs to release the credits.
public class QrHandoverActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private TextView tvPinFallback, tvQrItemTitle, tvQrEscrow;

    private int transactionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_handover);

        transactionId = getIntent().getIntExtra("TXN_ID", 0);

        ivQrCode = findViewById(R.id.ivQrCode);
        tvPinFallback = findViewById(R.id.tvPinFallback);
        tvQrItemTitle = findViewById(R.id.tvQrItemTitle);
        tvQrEscrow = findViewById(R.id.tvQrEscrow);

        if (transactionId <= 0) {
            Toast.makeText(this, "Missing exchange details.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadHandover();
    }

    private void loadHandover() {
        ApiClient.getService().getHandover(transactionId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("transaction") instanceof Map) {
                    Map<?, ?> tx = (Map<?, ?>) response.body().get("transaction");
                    String hash = String.valueOf(tx.get("qr_verify_hash"));
                    String pin = String.valueOf(tx.get("pin_fallback"));
                    Object credits = tx.get("credit_amount");

                    tvQrItemTitle.setText(String.valueOf(tx.get("item_title")));
                    tvQrEscrow.setText("Held in Escrow: " + (credits instanceof Number ? ((Number) credits).intValue() : "?") + " AmigoCredits");
                    tvPinFallback.setText("PIN: " + (pin.length() == 6 ? pin.substring(0, 3) + " " + pin.substring(3) : pin));
                    generateQrCode("SHARAMIGO|" + transactionId + "|" + hash);
                } else {
                    Toast.makeText(QrHandoverActivity.this, ApiErrors.message(response, "Could not load the exchange."), Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(QrHandoverActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void generateQrCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            Toast.makeText(this, "Could not draw the QR code. Use the PIN instead.", Toast.LENGTH_LONG).show();
        }
    }
}
