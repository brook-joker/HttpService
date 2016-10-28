package com.smartdengg.httpservice.lib.utils;

import android.text.TextUtils;
import android.util.Log;
import com.smartdengg.httpservice.lib.HttpService;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 创建时间: 2016/08/09 17:23 <br>
 * 作者: SmartDengg <br>
 * 描述: Json格式的日志打印类
 */
public class JsonPrinter {

  private static final String DEFAULT_HTTP_TAG = HttpService.getHttpTAG();
  private static final ThreadLocal<String> sLocalTag = new ThreadLocal<>();
  private static final ThreadLocal<Integer> sLocalMethodCount = new ThreadLocal<>();

  /** Drawing toolbox */
  private static final char TOP_LEFT_CORNER = '╔';
  private static final char BOTTOM_LEFT_CORNER = '╚';
  private static final char MIDDLE_CORNER = '╟';
  private static final char HORIZONTAL_DOUBLE_LINE = '║';
  private static final String DOUBLE_DIVIDER = "════════════════════════════════════════════";
  private static final String SINGLE_DIVIDER = "────────────────────────────────────────────";
  private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
  private static final String BOTTOM_BORDER = BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
  private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;

  /** Android平台上log输出的最大长度约等于4076字节,由于默认编码格式为"UTF-8",因此将"4000"作为日志切割阈值 */
  private static final int CHUNK_SIZE = 4000;

  /**
   * Json格式的字符串缩进长度,默认为"4"
   * 如:
   * <pre>
   * {
   *     "query": "Pizza",
   *     "locations": [
   *         94043,
   *         90210
   *     ]
   * }</pre>
   */
  private static final int JSON_INDENT = 4;

  /** 因为堆栈信息的打印在JsonPrinter中先后调用了4个函数,因此偏移量的值为4,过滤掉4个函数的调用 */
  private static final int CALL_STACK_OFFSET = 4;

  private JsonPrinter() {
    throw new IllegalStateException("No instance!");
  }

  public static void json(String json) {
    printer(json);
  }

  /** 在该日志输出上指定一个一次性的TAG(默认为通过HttpService设置的全局TAG变量),用于将字符串转换成Json格式打印 */
  public static void json(String tag, String json) {
    if (tag != null) sLocalTag.set(tag);
    printer(json);
  }

  /** 在该日志输出上指定一个一次性的TAG(默认为通过HttpService设置的全局TAG变量),用于将字符串转换成Json格式打印 */
  public static void json(String json, Integer methodCount) {
    if (methodCount != null && methodCount >= 0) sLocalMethodCount.set(methodCount);
    printer(json);
  }

  /** 在该日志输出上指定一个一次性的TAG(默认为通过HttpService设置的全局TAG变量)和一个一次性的堆栈调用记录条数(默认为1),用于将字符串转换成Json格式打印 */
  public static void json(String tag, Integer methodCount, String json) {
    if (tag != null) sLocalTag.set(tag);
    if (methodCount != null && methodCount >= 0) sLocalMethodCount.set(methodCount);
    printer(json);
  }

  /** 将字符串转成Json格式进行输出 */
  private static void printer(String json) {
    if (TextUtils.isEmpty(json)) {
      d("Empty/Null json content");
      return;
    }
    try {
      if (json.startsWith("{")) {
        JSONObject jsonObject = new JSONObject(json);
        String message = jsonObject.toString(JSON_INDENT);
        d(message);
        return;
      }
      if (json.startsWith("[")) {
        JSONArray jsonArray = new JSONArray(json);
        String message = jsonArray.toString(JSON_INDENT);
        d(message);
      }
    } catch (JSONException e) {
      e(e.getCause().getMessage() + "\n" + json);
    }
  }

  private static void d(String message, Object... args) {
    log(Log.DEBUG, message, args);
  }

  private static void e(String message, Object... args) {
    e(null, message, args);
  }

  private static void e(Throwable throwable, String message, Object... args) {

    if (throwable != null && message != null) message += " : " + getStackTraceString(throwable);
    if (throwable != null && message == null) message = getStackTraceString(throwable);
    if (message == null) message = "No message/exception is set";

    log(Log.ERROR, message, args);
  }

  private static synchronized void log(int logType, String msg, Object... args) {

    String message = createMessage(msg, args);

    String tag = getTag();
    Integer methodCount = getMethodCount();
    logTopBorder(logType, tag);
    logHeaderContent(logType, tag, methodCount);

    byte[] bytes = message.getBytes();
    int length = bytes.length;
    if (length <= CHUNK_SIZE) {
      logContent(logType, tag, message);
    } else {
      for (int i = 0; i < length; i += CHUNK_SIZE) {
        int count = Math.min(length - i, CHUNK_SIZE);
        //create a new String with system's default charset (which is UTF-8 for Android)
        logContent(logType, tag, new String(bytes, i, count));
      }
    }
    logBottomBorder(logType, tag);
  }

  private static String createMessage(String message, Object... args) {
    return args.length == 0 ? message : String.format(message, args);
  }

  private static void logHeaderContent(int logType, String tag, int methodCount) {

    String level = "";
    StackTraceElement[] trace = new Throwable().getStackTrace();

    logChunk(logType, tag, HORIZONTAL_DOUBLE_LINE + " Thread: " + Thread.currentThread().getName());
    logDivider(logType, tag);

    int stackOffset = CALL_STACK_OFFSET;

    //corresponding method count with the current stack may exceeds the stack trace. Trims the count
    if (methodCount + stackOffset > trace.length) methodCount = trace.length - stackOffset - 1;

    for (int i = methodCount; i > 0; i--) {
      int stackIndex = i + stackOffset;
      if (stackIndex >= trace.length) continue;

      StringBuilder builder = new StringBuilder();
      builder.append("║ ")
          .append(level)
          .append(getSimpleClassName(trace[stackIndex].getClassName()))
          .append(".")
          .append(trace[stackIndex].getMethodName())
          .append(" ")
          .append(" (")
          .append(trace[stackIndex].getFileName())
          .append(":")
          .append(trace[stackIndex].getLineNumber())
          .append(")");
      level += "   ";
      logChunk(logType, tag, builder.toString());
    }
  }

  private static void logTopBorder(int logType, String tag) {
    logChunk(logType, tag, TOP_BORDER);
  }

  private static void logContent(int logType, String tag, String chunk) {
    String[] lines = chunk.split(System.getProperty("line.separator"));
    for (String line : lines) {
      logChunk(logType, tag, HORIZONTAL_DOUBLE_LINE + " " + line);
    }
  }

  private static void logBottomBorder(int logType, String tag) {
    logChunk(logType, tag, BOTTOM_BORDER);
  }

  private static void logDivider(int logType, String tag) {
    logChunk(logType, tag, MIDDLE_BORDER);
  }

  private static String getSimpleClassName(String name) {
    int lastIndex = name.lastIndexOf(".");
    return name.substring(lastIndex + 1);
  }

  private static void logChunk(int logType, String tag, String chunk) {
    switch (logType) {
      case Log.ERROR:
        Log.e(tag, chunk);
        break;
      case Log.INFO:
        Log.i(tag, chunk);
        break;
      case Log.VERBOSE:
        Log.v(tag, chunk);
        break;
      case Log.WARN:
        Log.w(tag, chunk);
        break;
      case Log.ASSERT:
        Log.wtf(tag, chunk);
        break;
      case Log.DEBUG:
      default:
        Log.d(tag, chunk);
        break;
    }
  }

  /**
   * Don't replace this with Log.getStackTraceString()
   * - it hides UnknownHostException, which is not what we want.
   */
  private static String getStackTraceString(Throwable throwable) {
    StringWriter stringWriter = new StringWriter(256);
    PrintWriter printWriter = new PrintWriter(stringWriter, false);
    throwable.printStackTrace(printWriter);
    printWriter.flush();
    return stringWriter.toString();
  }

  /**
   * 获取日志输出的TAG,默认情况下是是通过HttpService设置的全局变量
   */
  private static String getTag() {
    String tag = sLocalTag.get();
    if (tag != null) {
      sLocalTag.remove();
      return tag;
    }
    return DEFAULT_HTTP_TAG;
  }

  /**
   * 获取调用日志输出函数的堆栈信息
   */
  private static Integer getMethodCount() {
    Integer count = sLocalMethodCount.get();
    if (count != null) sLocalMethodCount.remove();
    if (count == null || count < 0) count = 1;
    return count;
  }
}
