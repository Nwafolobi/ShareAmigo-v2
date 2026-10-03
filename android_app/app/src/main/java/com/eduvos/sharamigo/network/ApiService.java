package com.eduvos.sharamigo.network;

import java.util.Map;
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

    @POST("transactions/claim_item.php")
    Call<Map<String, Object>> claimItem(@Body Map<String, Object> body);

    @GET("transactions/get_handover.php")
    Call<Map<String, Object>> getHandover(@Query("transaction_id") int transactionId);

    @POST("transactions/verify_qr.php")
    Call<Map<String, Object>> verifyQr(@Body Map<String, Object> body);

    @GET("wallet/get_ledger.php")
    Call<Map<String, Object>> getLedger();
}
