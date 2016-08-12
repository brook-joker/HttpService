package com.smartdengg.httpservice.lib.utils;

import android.os.Looper;
import java.io.Closeable;
import java.io.EOFException;
import okio.Buffer;

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

  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (RuntimeException rethrown) {
        throw rethrown;
      } catch (Exception ignored) {
      }
    }
  }

  public static boolean isPlaintext(Buffer buffer) throws EOFException {
    try {
      Buffer prefix = new Buffer();
      long byteCount = buffer.size() < 64 ? buffer.size() : 64;
      buffer.copyTo(prefix, 0, byteCount);
      for (int i = 0; i < 16; i++) {
        if (prefix.exhausted()) {
          break;
        }
        if (Character.isISOControl(prefix.readUtf8CodePoint())) {
          return false;
        }
      }
      return true;
    } catch (EOFException e) {
      return false; // Truncated UTF-8 sequence.
    }
  }
}

