package com.smartdengg.httpservice.lib.utils;

import android.os.Looper;

/**
 * Created by Joker on 2016/6/2.
 */
public class Util {

  private Util() {
    throw new IllegalStateException("No instances!");
  }

  public static <T> T checkNotNull(T object, String message) {
    if (object == null) {
      throw new NullPointerException(message);
    }
    return object;
  }

  static void checkMainThread() {
    if (Looper.getMainLooper() != Looper.myLooper()) {
      throw new IllegalStateException(
          "Must be called from the main thread. Was: " + Thread.currentThread());
    }
  }
}

