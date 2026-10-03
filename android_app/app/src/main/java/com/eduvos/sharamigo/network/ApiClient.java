package com.eduvos.sharamigo.network;

import android.content.Intent;
import com.eduvos.sharamigo.BuildConfig;
import com.eduvos.sharamigo.SharaApp;
import com.eduvos.sharamigo.activities.AuthActivity;
import com.eduvos.sharamigo.utils.SessionManager;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Set in app/build.gradle (debug = emulator + XAMPP, release = your live HTTPS address).
    public static final String BASE_URL = BuildConfig.BASE_URL;

    private static Retrofit retrofit = null;

    public static ApiService getService() {
        if (retrofit == null) {
            // BASIC only logs the URL and status, so passwords, tokens and PINs stay out of Logcat.
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            logging.redactHeader("Authorization");

            Interceptor authInterceptor = chain -> {
                SessionManager session = SessionManager.get(SharaApp.get());
                String token = session.getToken();

                Request request = chain.request();
                if (token != null) {
                    request = request.newBuilder().header("Authorization", "Bearer " + token).build();
                }

                Response response = chain.proceed(request);

                // Token expired or rejected: clear it and send the student back to sign in.
                if (response.code() == 401 && token != null) {
                    session.clear();
                    Intent intent = new Intent(SharaApp.get(), AuthActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.putExtra("SESSION_EXPIRED", true);
                    SharaApp.get().startActivity(intent);
                }
                return response;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
