package com.eduvos.sharamigo.utils;

import android.app.Activity;
import android.content.Intent;
import com.eduvos.sharamigo.activities.AuthActivity;
import com.eduvos.sharamigo.network.ApiClient;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Ends the session on the server (best effort), clears it on the phone, and returns to sign in.
public final class SignOut {

    private SignOut() {}

    public static void run(Activity activity) {
        ApiClient.getService().logout().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                finish(activity);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                finish(activity);
            }
        });
    }

    private static void finish(Activity activity) {
        SessionManager.get(activity).clear();
        Intent intent = new Intent(activity, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
