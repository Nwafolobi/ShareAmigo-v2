package com.eduvos.sharamigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.models.Transaction;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.eduvos.sharamigo.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private TextView tvWalletTotalCredits, tvWalletAvailable, tvWalletEscrow;
    private LinearLayout layoutLedgerEntries;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        currentUserId = SessionManager.get(this).getUserId();

        tvWalletTotalCredits = findViewById(R.id.tvWalletTotalCredits);
        tvWalletAvailable = findViewById(R.id.tvWalletAvailable);
        tvWalletEscrow = findViewById(R.id.tvWalletEscrow);
        layoutLedgerEntries = findViewById(R.id.layoutLedgerEntries);

        loadWalletData();
    }

    private void loadWalletData() {
        ApiClient.getService().getLedger().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body();
                    double balance = ((Number) data.get("credit_balance")).doubleValue();
                    double escrow = ((Number) data.get("escrow_held")).doubleValue();

                    tvWalletTotalCredits.setText(((int) (balance + escrow)) + " AmigoCredits");
                    tvWalletAvailable.setText(((int) balance) + " Pts");
                    tvWalletEscrow.setText(((int) escrow) + " Pts");

                    Gson gson = new Gson();
                    String json = gson.toJson(data.get("transactions"));
                    List<Transaction> txns = gson.fromJson(json, new TypeToken<List<Transaction>>(){}.getType());

                    renderLedgerRows(txns);
                } else if (!response.isSuccessful()) {
                    Toast.makeText(WalletActivity.this, ApiErrors.message(response, "Could not load your wallet."), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(WalletActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderLedgerRows(List<Transaction> txns) {
        layoutLedgerEntries.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (txns == null || txns.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No transaction history recorded yet.");
            tvEmpty.setTextColor(getResources().getColor(R.color.grey_medium));
            layoutLedgerEntries.addView(tvEmpty);
            return;
        }

        for (Transaction t : txns) {
            View view = inflater.inflate(android.R.layout.simple_list_item_2, layoutLedgerEntries, false);
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);

            boolean isDonor = t.getDonorId() == currentUserId;
            text1.setText(t.getItemTitle() + " (" + (isDonor ? "+" : "-") + t.getCreditAmount() + " Pts)");
            text1.setTextSize(14);
            text1.setTextColor(getResources().getColor(R.color.black));

            text2.setText((isDonor ? "Traded to: " + t.getReceiverName() : "Claimed from: " + t.getDonorName()) + " • " + t.getStatus());
            text2.setTextColor(getResources().getColor(R.color.grey_medium));

            // Pending exchanges open the handover screen: donor verifies, receiver shows the QR code.
            if ("Pending".equals(t.getStatus())) {
                text2.setText(text2.getText() + " (tap to open)");
                final int txnId = t.getId();
                view.setOnClickListener(v -> {
                    Intent intent = new Intent(this, isDonor ? VerifyHandoverActivity.class : QrHandoverActivity.class);
                    intent.putExtra("TXN_ID", txnId);
                    startActivity(intent);
                });
            }

            layoutLedgerEntries.addView(view);
        }
    }
}
