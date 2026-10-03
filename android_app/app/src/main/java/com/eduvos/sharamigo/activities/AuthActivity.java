package com.eduvos.sharamigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eduvos.sharamigo.R;
import com.eduvos.sharamigo.network.ApiClient;
import com.eduvos.sharamigo.utils.ApiErrors;
import com.eduvos.sharamigo.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthActivity extends AppCompatActivity {

    private TextInputLayout layoutFullName, layoutStudentNumber, layoutEmail, layoutPassword;
    private TextInputEditText etFullName, etStudentNumber, etEmail, etPassword;
    private MaterialButton btnSubmit;
    private TextView tvFormTitle, tvToggleMode;

    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Already signed in: skip the login screen.
        if (SessionManager.get(this).isLoggedIn()) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_auth);

        layoutFullName = findViewById(R.id.layoutFullName);
        layoutStudentNumber = findViewById(R.id.layoutStudentNumber);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutPassword = findViewById(R.id.layoutPassword);

        etFullName = findViewById(R.id.etFullName);
        etStudentNumber = findViewById(R.id.etStudentNumber);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        btnSubmit = findViewById(R.id.btnSubmit);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvToggleMode = findViewById(R.id.tvToggleMode);

        tvToggleMode.setOnClickListener(v -> toggleMode());
        btnSubmit.setOnClickListener(v -> handleSubmit());

        if (getIntent().getBooleanExtra("SESSION_EXPIRED", false)) {
            Toast.makeText(this, "Session expired. Please sign in again.", Toast.LENGTH_LONG).show();
        }
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            tvFormTitle.setText("Student Registration");
            layoutFullName.setVisibility(View.VISIBLE);
            layoutStudentNumber.setVisibility(View.VISIBLE);
            btnSubmit.setText("CREATE ACCOUNT (+10 CREDITS)");
            tvToggleMode.setText("Already registered? Sign In");
        } else {
            tvFormTitle.setText("Student Sign In");
            layoutFullName.setVisibility(View.GONE);
            layoutStudentNumber.setVisibility(View.GONE);
            btnSubmit.setText("SIGN IN");
            tvToggleMode.setText("New to campus? Create Account (+10 Credits)");
        }
    }

    private void handleSubmit() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRegisterMode) {
            String fullName = etFullName.getText().toString().trim();
            String studentNumber = etStudentNumber.getText().toString().trim();

            if (fullName.isEmpty() || studentNumber.isEmpty()) {
                Toast.makeText(this, "Please provide your full name and student number.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 8) {
                Toast.makeText(this, "Password must be at least 8 characters.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmit.setEnabled(false);

            Map<String, Object> body = new HashMap<>();
            body.put("full_name", fullName);
            body.put("student_number", studentNumber);
            body.put("email", email);
            body.put("password", password);
            body.put("campus_id", 1);

            ApiClient.getService().register(body).enqueue(authCallback("Registration failed."));
        } else {
            btnSubmit.setEnabled(false);

            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            ApiClient.getService().login(body).enqueue(authCallback("Invalid email or password."));
        }
    }

    private Callback<Map<String, Object>> authCallback(String failureFallback) {
        return new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && saveSession(response.body())) {
                    navigateToHome();
                } else if (!response.isSuccessful()) {
                    Toast.makeText(AuthActivity.this, ApiErrors.message(response, failureFallback), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(AuthActivity.this, "Unexpected server reply. Please try again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, ApiErrors.NO_CONNECTION, Toast.LENGTH_LONG).show();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private boolean saveSession(Map<String, Object> body) {
        Object token = body.get("token");
        Object user = body.get("user");
        if (!(token instanceof String) || !(user instanceof Map)) {
            return false;
        }
        Map<String, Object> userMap = (Map<String, Object>) user;
        Object id = userMap.get("id");
        if (!(id instanceof Number)) {
            return false;
        }
        Object name = userMap.get("full_name");
        SessionManager.get(this).save((String) token, ((Number) id).intValue(), name instanceof String ? (String) name : "");
        return true;
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeFeedActivity.class));
        finish();
    }
}
