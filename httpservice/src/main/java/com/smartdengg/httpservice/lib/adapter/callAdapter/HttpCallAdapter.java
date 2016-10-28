package com.smartdengg.httpservice.lib.adapter.callAdapter;

import com.smartdengg.httpservice.lib.utils.Util;
import java.io.IOException;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 创建时间: 2016/08/09 18:06 <br>
 * 作者: dengwei <br>
 * 描述: 自定义HttpCallAdapter，用于返回值类型为HttpCall的接口执行真正的网络操作
 */
public class HttpCallAdapter<T> implements HttpCall<T> {

  private static int CODE_204 = 204;
  private static int CODE_205 = 205;
  private static int CODE_400 = 400;
  private static int CODE_401 = 401;
  private static int CODE_500 = 500;
  private static int CODE_600 = 600;

  private Call<T> mDelegate;
  private HttpCallAdapterFactory.MainThreadExecutor mMainThreadExecutor;
  private final int mMaxRetryCount;
  private int mCurrentRetryCount = 0;
  private HttpCallAdapterFactory.Callback mCallback;

  public HttpCallAdapter(Call<T> delegate,
      HttpCallAdapterFactory.MainThreadExecutor mainThreadExecutor, int maxRetryCount,
      HttpCallAdapterFactory.Callback callback) {
    this.mDelegate = delegate;
    this.mMainThreadExecutor = mainThreadExecutor;
    this.mMaxRetryCount = maxRetryCount;
    this.mCallback = callback;
  }

  @Override public Response<T> execute() throws IOException {
    return mDelegate.execute();
  }

  @Override public void enqueue(final HttpCallback<T> callback) {
    Util.checkNotNull(callback, "callback == null");

    mDelegate.enqueue(new Callback<T>() {
      @Override public void onResponse(final Call<T> call, final Response<T> response) {

        mMainThreadExecutor.execute(new Runnable() {
          @Override public void run() {

            if (HttpCallAdapter.this.mCallback != null) {
              HttpCallAdapter.this.mCallback.onResponse(response);
            }

            int code = response.code();
            if (response.isSuccessful()) {
              if (code == CODE_204 || code == CODE_205 || response.body() == null) {
                callback.noContent(response, HttpCallAdapter.this);
              } else {
                callback.success(response.body(), HttpCallAdapter.this);
              }
            } else if (code == CODE_401) {
              callback.unauthenticated(response, HttpCallAdapter.this);
            } else if (code >= CODE_400 && code < CODE_500) {
              callback.clientError(response, HttpCallAdapter.this);
            } else if (code >= CODE_500 && code < CODE_600) {
              callback.serverError(response, HttpCallAdapter.this);
            } else {
              callback.unexpectedError(new RuntimeException("Unexpected response " + response),
                  HttpCallAdapter.this);
            }
          }
        });
      }

      @Override public void onFailure(Call<T> call, final Throwable t) {

        if (mCurrentRetryCount++ < mMaxRetryCount) {
          call.clone().enqueue(this);
        } else {
          mMainThreadExecutor.execute(new Runnable() {
            @Override public void run() {
              if (t instanceof IOException) {
                callback.networkError((IOException) t, HttpCallAdapter.this);
              } else {
                callback.unexpectedError(t, HttpCallAdapter.this);
              }
            }
          });
        }
      }
    });
  }

  @Override public void cancel() {
    mDelegate.cancel();
  }

  @Override public boolean isExecuted() {
    return mDelegate.isExecuted();
  }

  @Override public boolean isCanceled() {
    return mDelegate.isCanceled();
  }

  @SuppressWarnings("CloneDoesntCallSuperClone") @Override public HttpCall<T> clone() {
    return new HttpCallAdapter<>(mDelegate.clone(), mMainThreadExecutor, mMaxRetryCount, mCallback);
  }

  @Override public Request request() {
    return mDelegate.request();
  }
}
