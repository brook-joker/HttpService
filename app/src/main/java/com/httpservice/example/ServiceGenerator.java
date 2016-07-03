package com.httpservice.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smartdengg.httpservice.lib.adapter.callAdapter.HttpCallAdapterFactory;
import com.smartdengg.httpservice.lib.adapter.rxadapter.RxJavaCallAdapterFactory;
import com.smartdengg.httpservice.lib.converter.GsonConverterFactory;
import com.smartdengg.httpservice.lib.interceptor.HttpLoggingInterceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Created by SmartDengg on 2016/7/4.
 */
public class ServiceGenerator {

  private static OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
  private static Retrofit retrofit;

  static {
    Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
        .enableComplexMapKeySerialization()
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    ServiceGenerator.httpClientBuilder.addInterceptor(
        HttpLoggingInterceptor.createLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY));

    ServiceGenerator.retrofit = new Retrofit.Builder().baseUrl("http://v.juhe.cn/")
        .addCallAdapterFactory(HttpCallAdapterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(ServiceGenerator.httpClientBuilder.build())
        .build();
  }

  public static <S> S createService(Class<S> serviceClass) {
    return retrofit.create(serviceClass);
  }
}
