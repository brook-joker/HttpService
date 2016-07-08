package com.smartdengg.httpservice.lib.adapter.callAdapter;

import java.io.IOException;
import okhttp3.Request;
import retrofit2.Response;

/**
 * Created by Joker on 2016/2/19.
 */
public interface HttpCall<T> extends Cloneable {

  Response<T> execute() throws IOException;

  void enqueue(HttpCallback<T> callback);

  void cancel();

  boolean isExecuted();

  boolean isCanceled();

  HttpCall<T> clone();

  Request request();
}
