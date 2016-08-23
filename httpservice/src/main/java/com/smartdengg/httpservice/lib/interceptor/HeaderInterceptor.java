package com.smartdengg.httpservice.lib.interceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Joker on 2016/2/19.
 */
public abstract class HeaderInterceptor implements Interceptor {

  public HeaderInterceptor() {
  }

  @Override public Response intercept(Chain chain) throws IOException {

    Request.Builder newBuilder = chain.request().newBuilder();

    HashMap<String, String> headers = this.headers();

    if (headers != null && headers.size() > 0) {

      Set<Map.Entry<String, String>> entries = headers.entrySet();

      for (Map.Entry<String, String> entry : entries) {
        newBuilder.addHeader(entry.getKey(), entry.getValue());
      }
    }

    Request request = newBuilder.build();

    return chain.proceed(request);
  }

  public abstract HashMap<String, String> headers();
}
