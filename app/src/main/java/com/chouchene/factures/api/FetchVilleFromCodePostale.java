package com.chouchene.factures.api;

import android.util.Log;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchVilleFromCodePostale {

    public static void fetchDataFromApiWithParams(String place, TextInputEditText txtCity, TextInputEditText txtCountry) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://www.laposte.fr/tool/address/sercadia/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Call<List<VilleDataModel>> call = apiService.getDataWithParams(place);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<List<VilleDataModel>> call, Response<List<VilleDataModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<VilleDataModel> villeList = response.body();
                    if(!villeList.isEmpty()){
                        String ville = response.body().get(0).getCity();
                        txtCity.setText(ville);
                        txtCountry.setText("FRANCE");
                    }
                } else {
                    // Toast.makeText(MainActivity.this, "Failed to retrieve data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<VilleDataModel>> call, Throwable t) {
                Log.e("API Error", t.getMessage());
                //Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
