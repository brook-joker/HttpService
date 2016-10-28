package com.smartdengg.httpservice.lib.converter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.smartdengg.httpservice.lib.HttpService;
import com.smartdengg.httpservice.lib.utils.JsonPrinter;
import com.smartdengg.httpservice.lib.utils.Util;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.Converter;

final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {

  private Gson gson;
  private final TypeAdapter<T> adapter;
  private boolean enable;
  private List<String> mIgnoredUrls;

  private static final Charset UTF8 = Charset.forName("UTF-8");

  GsonResponseBodyConverter(Gson gson, TypeAdapter adapter, boolean enable,
      List<String> ignoredUrls) {
    this.gson = gson;
    this.adapter = adapter;
    this.enable = enable;
    this.mIgnoredUrls = ignoredUrls;
  }

  @Override public T convert(ResponseBody body) throws IOException {

    String name = Thread.currentThread().getName();
    if (mIgnoredUrls != null && !mIgnoredUrls.isEmpty()) {
      for (int i = 0; i < mIgnoredUrls.size(); i++) {
        if (name.contains(mIgnoredUrls.get(i))) {
          this.enable = false;
          break;
        }
      }
    }

    BufferedSource source = null;
    InputStreamReader reader = null;
    JsonReader jsonReader = null;

    try {
      if (HttpService.enableResponseLog() && enable) {
        Buffer buffer = new Buffer();
        body.source().readAll(buffer);

        Charset charset = UTF8;
        MediaType contentType = body.contentType();
        if (contentType != null) charset = contentType.charset(UTF8);

        if (body.contentLength() != 0 && Util.isPlaintext(buffer)) {
          JsonPrinter.json(buffer.clone().readString(charset), 0);
        }
        reader = new InputStreamReader(buffer.inputStream(), charset);
        return getBody(reader);
      } else {
        jsonReader = gson.newJsonReader(body.charStream());
        jsonReader.setLenient(true);
        return adapter.read(jsonReader);
      }
    } finally {
      Util.closeQuietly(source);
      Util.closeQuietly(reader);
      Util.closeQuietly(jsonReader);
      Util.closeQuietly(body);
    }
  }

  private T getBody(InputStreamReader reader) throws IOException {
    JsonReader jsonReader = gson.newJsonReader(reader);
    jsonReader.setLenient(true);
    return adapter.read(jsonReader);
  }
}
