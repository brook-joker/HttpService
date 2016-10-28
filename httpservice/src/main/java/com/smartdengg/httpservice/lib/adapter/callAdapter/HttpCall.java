package com.smartdengg.httpservice.lib.adapter.callAdapter;

import java.io.IOException;
import okhttp3.Request;
import retrofit2.Response;

/**
 * 创建时间: 2016/08/09 17:56 <br>
 * 作者: dengwei <br>
 * 描述: 自定义网络执行类,详细使用方法见仓库README.md
 */
public interface HttpCall<T> extends Cloneable {

  /** 同步执行网络请求,不要在主线程中使用 */
  Response<T> execute() throws IOException;

  /** 异步执行网络请求 */
  void enqueue(HttpCallback<T> callback);

  /** 取消网络请求 */
  void cancel();

  /** 判断网络请求是否已经执行 */
  boolean isExecuted();

  /** 判断网络请求是否被取消 */
  boolean isCanceled();

  /** 需要注意的是retrofit只能执行一次,,如果需要重复执行,可使用这个clone方法,或者重新创建接口实例 */
  HttpCall<T> clone();

  /** 获取请求参数信息 */
  Request request();
}
