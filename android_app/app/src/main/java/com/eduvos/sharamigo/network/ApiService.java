package com.eduvos.sharamigo.network;

import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @POST("auth/register.php")
    Call<Map<String, Object>> register(@Body Map<String, Object> body);

    @POST("auth/login.php")
    Call<Map<String, Object>> login(@Body Map<String, Object> body);

    @POST("auth/logout.php")
    Call<Map<String, Object>> logout();

    @GET("items/get_items.php")
    Call<Map<String, Object>> getItems(
            @Query("category") String category,
            @Query("search") String search
    );

    @POST("items/create_item.php")
    Call<Map<String, Object>> createItem(@Body Map<String, Object> body);

    // ---- My listings ----

    // status: null for all, or "Available", "Escrow", "Exchanged"
    @GET("items/get_my_items.php")
    Call<Map<String, Object>> getMyItems(@Query("status") String status);

    @POST("items/update_item.php")
    Call<Map<String, Object>> updateItem(@Body Map<String, Object> body);

    @POST("items/delete_item.php")
    Call<Map<String, Object>> deleteItem(@Body Map<String, Object> body);

    @Multipart
    @POST("items/upload_item_photo.php")
    Call<Map<String, Object>> uploadItemPhoto(
            @Part("item_id") RequestBody itemId,
            @Part MultipartBody.Part photo
    );

    @POST("items/delete_item_photo.php")
    Call<Map<String, Object>> deleteItemPhoto(@Body Map<String, Object> body);

    @GET("items/get_item_photos.php")
    Call<Map<String, Object>> getItemPhotos(@Query("item_id") int itemId);

    // ---- Profile ----

    @GET("users/get_profile.php")
    Call<Map<String, Object>> getProfile();

    @POST("users/update_profile.php")
    Call<Map<String, Object>> updateProfile(@Body Map<String, Object> body);

    @POST("users/change_password.php")
    Call<Map<String, Object>> changePassword(@Body Map<String, Object> body);

    @Multipart
    @POST("users/upload_avatar.php")
    Call<Map<String, Object>> uploadAvatar(@Part MultipartBody.Part photo);

    @Multipart
    @POST("users/upload_avatar.php")
    Call<Map<String, Object>> removeAvatar(@Part("remove") RequestBody remove);

    // ---- Exchanges ----

    @POST("transactions/claim_item.php")
    Call<Map<String, Object>> claimItem(@Body Map<String, Object> body);

    @GET("transactions/get_handover.php")
    Call<Map<String, Object>> getHandover(@Query("transaction_id") int transactionId);

    @POST("transactions/verify_qr.php")
    Call<Map<String, Object>> verifyQr(@Body Map<String, Object> body);

    @GET("wallet/get_ledger.php")
    Call<Map<String, Object>> getLedger();
}
