package com.smartdengg.httpservice.lib.interceptor;

import android.util.Log;
import com.smartdengg.httpservice.lib.HttpService;
import com.smartdengg.httpservice.lib.utils.Util;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static okhttp3.internal.http.StatusLine.HTTP_CONTINUE;

/**
 * 创建时间: 2016/08/09 17:30 <br>
 * 作者: dengwei <br>
 * 描述: 网络连接状态拦截器,主要用于打印网络日志,但不包含响应体
 */
public class HttpLoggingInterceptor implements Interceptor {

  private static String HTTP_TAG = HttpService.getHttpTAG();

  private static final int MAX_LOG_LENGTH = 4 * 1000;
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private volatile Level level = Level.NONE;
  private Logger logger;

  private static final char TOP_LEFT_CORNER = '╔';
  private static final char TOP_LEFT_CORNER_SINGLE = '┌';
  private static final char BOTTOM_LEFT_CORNER = '╚';
  private static final char BOTTOM_LEFT_CORNER_SINGLE = '└';
  private static final char MIDDLE_CORNER = '╟';
  private static final char HORIZONTAL_DOUBLE_LINE = '║';
  private static final char HORIZONTAL_DOUBLE_LINE_SINGLE = '│';
  private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
  private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";

  public static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
  public static final String TOP_BORDER_SINGE =
      TOP_LEFT_CORNER_SINGLE + SINGLE_DIVIDER + SINGLE_DIVIDER;
  public static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
  public static final String BOTTOM_BORDER_SINGLE =
      BOTTOM_LEFT_CORNER_SINGLE + SINGLE_DIVIDER + SINGLE_DIVIDER;
  public static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;

  private List<String> mExRequestHeaders;
  private List<String> mExResponseHeaders;
  private List<String> mIgnoredUrls;

  public enum Level {
    /** No logs. */
    NONE,
    /**
     * Logs request and response lines.
     * <p>Example:
     * <pre>{@code
     * --> POST /greeting HTTP/1.1 (3-byte body)
     * <-- HTTP/1.1 200 OK (22ms, 6-byte body)
     * }</pre>
     */
    BASIC,
    /**
     * Logs request and response lines and their respective headers.
     * <p>Example:
     * <pre>{@code
     * --> POST /greeting HTTP/1.1
     * Host: example.com
     * Content-Type: plain/text
     * Content-Length: 3
     * --> END POST
     * <-- HTTP/1.1 200 OK (22ms)
     * Content-Type: plain/text
     * Content-Length: 6
     * <-- END HTTP
     * }</pre>
     */
    HEADERS,
    /**
     * Logs request and response lines and their respective headers and bodies (if present).
     * <p>Example:
     * <pre>{@code
     * --> POST /greeting HTTP/1.1
     * Host: example.com
     * Content-Type: plain/text
     * Content-Length: 3
     * Hi?
     * --> END GET
     * <-- HTTP/1.1 200 OK (22ms)
     * Content-Type: plain/text
     * Content-Length: 6
     * Hello!
     * <-- END HTTP
     * }</pre>
     */
    BODY
  }

  private interface Logger {

    void log(String message);

    void logRequestBody(String message);

    void logTopBorder();

    void logMiddleBorder();

    void logBottomBorder();
  }

  /** A {@link Logger} defaults output appropriate for the current platform. */
  private static Logger DEFAULT = new Logger() {
    @Override public void log(String message) {
      for (int i = 0, length = message.length(); i < length; i++) {
        int newline = message.indexOf('\n', i);
        newline = newline != -1 ? newline : length;
        do {
          int end = Math.min(newline, i + MAX_LOG_LENGTH);
          Log.d(HTTP_TAG,
              HttpLoggingInterceptor.HORIZONTAL_DOUBLE_LINE + message.substring(i, end));
          i = end;
        } while (i < newline);
      }
    }

    @Override public void logRequestBody(String message) {

      this.log("    " + HttpLoggingInterceptor.TOP_BORDER_SINGE);
      this.log("    " + HttpLoggingInterceptor.HORIZONTAL_DOUBLE_LINE_SINGLE + message);
      this.log("    " + HttpLoggingInterceptor.BOTTOM_BORDER_SINGLE);
    }

    @Override public void logTopBorder() {
      Log.d(HTTP_TAG, HttpLoggingInterceptor.TOP_BORDER);
    }

    @Override public void logMiddleBorder() {
      Log.d(HTTP_TAG, HttpLoggingInterceptor.MIDDLE_BORDER);
    }

    @Override public void logBottomBorder() {
      Log.d(HTTP_TAG, HttpLoggingInterceptor.BOTTOM_BORDER);
    }
  };

  /**
   * 创建一个默认的网络日志拦截器实例
   */
  public static HttpLoggingInterceptor createLoggingInterceptor() {
    return new HttpLoggingInterceptor();
  }

  private HttpLoggingInterceptor() {
    this.logger = DEFAULT;
  }

  /**
   * 设置请求头和响应头过滤器
   *
   * @param exRequestHeaders 请求头黑名单,如果集合中包含某请求头的'key',则该条请求头不会被打印
   * @param exResponseHeaders 响应头黑名单,如果集合中包含某响应头的'key',则该条相应头不会被打印
   */
  public HttpLoggingInterceptor setExclusiveHeaders(List<String> exRequestHeaders,
      List<String> exResponseHeaders) {
    this.mExRequestHeaders = exRequestHeaders;
    this.mExResponseHeaders = exResponseHeaders;
    return HttpLoggingInterceptor.this;
  }

  /**
   * 设置网络请求地址过滤器
   *
   * @param ignoredUrls 集合中的Url,将不会打印任何信息
   */
  public HttpLoggingInterceptor setIgnoredUrls(List<String> ignoredUrls) {
    this.mIgnoredUrls = ignoredUrls;
    return HttpLoggingInterceptor.this;
  }

  /**
   * 更改该网络日志打印等级,默认{@link Level#NONE},即无任何网络日志输出
   */
  public HttpLoggingInterceptor setLevel(Level level) {
    Util.checkNotNull(level, "level == null. Use Level.NONE instead.");
    this.level = level;
    return this;
  }

  public Level getLevel() {
    return level;
  }

  @Override public Response intercept(Chain chain) throws IOException {

    Level level = this.level;

    Request request = chain.request();
    if (level == Level.NONE) return chain.proceed(request);

    if (mIgnoredUrls != null && !mIgnoredUrls.isEmpty()) {
      for (int i = 0; i < mIgnoredUrls.size(); i++) {
        if (request.url().toString().contains(mIgnoredUrls.get(i))) return chain.proceed(request);
      }
    }

    boolean logBody = level == Level.BODY;
    boolean logHeaders = logBody || level == Level.HEADERS;

    RequestBody requestBody = request.body();
    boolean hasRequestBody = requestBody != null;

    Connection connection = chain.connection();
    Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
    String requestStartMessage =
        "--> " + request.method() + ' ' + request.url() + ' ' + protocol(protocol);
    if (!logHeaders && hasRequestBody) {
      requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
    }

    /**Outputs Top_Border*/
    logger.logTopBorder();
    logger.log(requestStartMessage);

    if (logHeaders) {
      if (hasRequestBody) {
        if (requestBody.contentType() != null) {
          logger.log("Content-Type: " + requestBody.contentType());
        }
        if (requestBody.contentLength() != -1) {
          logger.log("Content-Length: " + requestBody.contentLength());
        }
      }

      Headers headers = request.headers();
      Map<String, List<String>> headersMap = headers.toMultimap();
      if (this.mExRequestHeaders != null && this.mExRequestHeaders.size() > 0) {
        for (String name : mExRequestHeaders) {
          if (headersMap.containsKey(name)) {
            headersMap.remove(name);
          }
        }
      }
      for (Map.Entry<String, List<String>> entry : headersMap.entrySet()) {
        String name = entry.getKey();
        String value = headers.get(name);

        // Skip headers from the request body as they are explicitly logged above.
        if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
          logger.log(name + ": " + value);
        }
      }

      if (!logBody || !hasRequestBody) {
        logger.log("--> END " + request.method());
      } else if (bodyEncoded(headers)) {
        logger.log("--> END " + request.method() + " (encoded body omitted)");
      } else {
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);

        Charset charset = UTF8;
        MediaType contentType = requestBody.contentType();
        if (contentType != null) {
          charset = contentType.charset(UTF8);
          /*ignore "image", "audio" and "video"*/
          String type = contentType.type();
          if (requestBody.contentLength() > 0 && type != null && ("text".equals(type)
              || "application".equals(type))) {
            logger.logRequestBody(buffer.readString(charset));
          }
        }

        logger.log("--> END " + request.method() + " (" + requestBody.contentLength() +
            "-byte body)");
      }
    }

    /**Outputs Middle_Border*/
    logger.logMiddleBorder();

    long startNs = System.nanoTime();
    Response response = chain.proceed(request);
    long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

    ResponseBody responseBody = response.body();
    long contentLength = responseBody.contentLength();
    String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
    logger.log(
        "<-- " + response.code() + ' ' + response.message() + ' ' + response.request().url() +
            " (" + tookMs + "ms" +
            (!logHeaders ? ", " + bodySize + " body" : "") + ')');

    if (logHeaders) {
      Headers headers = response.headers();
      Map<String, List<String>> headersMap = headers.toMultimap();
      if (this.mExResponseHeaders != null && this.mExResponseHeaders.size() > 0) {
        for (String name : mExResponseHeaders) {
          if (headersMap.containsKey(name)) {
            headersMap.remove(name);
          }
        }
      }
      for (Map.Entry<String, List<String>> entry : headersMap.entrySet()) {
        String name = entry.getKey();
        String value = headers.get(name);
        logger.log(name + ": " + value);
      }

      /**Outputs Middle_Border*/
      logger.logMiddleBorder();

      if (!logBody || !hasBody(response)) {
        logger.log("<-- END HTTP");
      } else if (bodyEncoded(headers)) {
        logger.log("<-- END HTTP (encoded body omitted)");
      } else {
        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();

        if (Util.isPlaintext(buffer)) {
          logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
        } else {
          logger.log("<-- END HTTP (" + buffer.size() + "-byte body omitted)");
        }
      }
    }

    /**Outputs Middle_Border*/
    logger.logBottomBorder();

    return response;
  }

  private boolean bodyEncoded(Headers headers) {
    String contentEncoding = headers.get("Content-Encoding");
    return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
  }

  private static String protocol(Protocol protocol) {
    return protocol == Protocol.HTTP_1_0 ? "HTTP/1.0" : "HTTP/1.1";
  }

  private static boolean hasBody(Response response) {
    // HEAD requests never yield a body regardless of the response headers.
    if (response.request().method().equals("HEAD")) {
      return false;
    }

    int responseCode = response.code();
    if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
        && responseCode != HTTP_NO_CONTENT
        && responseCode != HTTP_NOT_MODIFIED) {
      return true;
    }

    // If the Content-Length or Transfer-Encoding headers disagree with the
    // response code, the response is malformed. For best compatibility, we
    // honor the headers.
    return Long.parseLong(response.header("Content-Length")) != -1 || "chunked".equalsIgnoreCase(
        response.header("Transfer-Encoding"));
  }
}
