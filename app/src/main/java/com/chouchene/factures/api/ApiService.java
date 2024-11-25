package com.chouchene.factures.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("search")
    Call<List<VilleDataModel>> getDataWithParams(
            @Query("place") String place
    );
}
