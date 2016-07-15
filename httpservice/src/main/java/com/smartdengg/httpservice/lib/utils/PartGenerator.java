package com.smartdengg.httpservice.lib.utils;

import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by SmartDengg on 16/7/7.
 */
public class PartGenerator {

  private PartGenerator() {
    throw new IllegalStateException("No instance");
  }

  private static final MediaType MULTIPART_FORM_DATA = MultipartBody.FORM;

  public static RequestBody createPartFromString(String descriptionString) {
    return RequestBody.create(MULTIPART_FORM_DATA, descriptionString);
  }

  public static MultipartBody.Part createPartFromFile(String partName, File file) {
    return createPartFromFile(MULTIPART_FORM_DATA, partName, file);
  }

  public static MultipartBody.Part createPartFromFile(MediaType mediaType, String partName,
      File file) {

    // create RequestBody instance from file
    RequestBody requestFile =
        RequestBody.create(mediaType, Util.checkNotNull(file, "file == null"));

    // MultipartBody.Part is used to send also the actual file name
    return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
  }
}
