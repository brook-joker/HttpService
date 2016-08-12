/*
 * Copyright (C) 2015 Lianjia, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Converter;

@SuppressWarnings("all") final class GsonResponseBodyConverter<gitT>
    implements Converter<ResponseBody, T> {

  private Gson gson;
  private final TypeAdapter<T> adapter;
  private final boolean enable;

  private static final Charset UTF8 = Charset.forName("UTF-8");

  GsonResponseBodyConverter(Gson gson, TypeAdapter adapter, boolean enable) {
    this.gson = gson;
    this.adapter = adapter;
    this.enable = enable;
  }

  @Override public T convert(ResponseBody value) throws IOException {

    BufferedSource source = null;
    InputStreamReader reader = null;
    JsonReader jsonReader = null;

    try {
      if (HttpService.enableResponseLog() && enable) {
        source = value.source();
        source.request(Long.MAX_VALUE);
        Buffer buffer = source.buffer();

        Charset charset = UTF8;
        MediaType contentType = value.contentType();
        if (contentType != null) {
          charset = contentType.charset(UTF8);
        }

        if (value.contentLength() != 0 && Util.isPlaintext(buffer)) {
          JsonPrinter.json(buffer.clone().readString(charset));
        }
        reader = new InputStreamReader(Okio.buffer(source).inputStream(), charset);
        return getT(reader);
      } else {
        jsonReader = gson.newJsonReader(value.charStream());
        jsonReader.setLenient(true);
        return adapter.read(jsonReader);
      }
    } finally {
      Util.closeQuietly(source);
      Util.closeQuietly(reader);
      Util.closeQuietly(jsonReader);
      Util.closeQuietly(value);
    }
  }

  private T getT(InputStreamReader reader) throws IOException {
    JsonReader jsonReader = gson.newJsonReader(reader);
    jsonReader.setLenient(true);
    return adapter.read(jsonReader);
  }
}
