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
package com.smartdengg.httpservice.lib.adapter.rxadapter;

import com.smartdengg.httpservice.lib.annotation.RetryCount;
import com.smartdengg.httpservice.lib.utils.Util;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/**
 * fixed by SmartDengg
 */
public final class RxJavaCallAdapterFactory extends CallAdapter.Factory {

  private static final int INITIAL = 1;

  public static RxJavaCallAdapterFactory create() {
    return new RxJavaCallAdapterFactory();
  }

  private RxJavaCallAdapterFactory() {
  }

  @Override
  public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {

    int retryCount = 0;

    for (Annotation annotation : annotations) {
      if (!RetryCount.class.isAssignableFrom(annotation.getClass())) continue;
      retryCount = ((RetryCount) annotation).count();
      if (retryCount < 0) throw new IllegalArgumentException("@RetryCount must not be less than 0");
    }

    Class<?> rawType = getRawType(returnType);
    boolean isObservable = "rx.Observable".equals(rawType.getCanonicalName());
    boolean isSingle = "rx.Single".equals(rawType.getCanonicalName());
    boolean isCompletable = "rx.Completable".equals(rawType.getCanonicalName());

    if (!isObservable && !isSingle && !isCompletable) return null;

    if (!isCompletable && !(returnType instanceof ParameterizedType)) {

      String name = isSingle ? "Single" : "Observable";
      throw new IllegalStateException(name
          + " return type must be parameterized"
          + " as "
          + name
          + "<Foo> or "
          + name
          + "<? extends Foo>");
    }

    if (isCompletable) return CompletableHelper.createCallAdapter();

    CallAdapter<Observable<?>> callAdapter =
        RxJavaCallAdapterFactory.this.getCallAdapter(returnType, retryCount);
    if (isSingle) {
      return SingleHelper.makeSingle(callAdapter);
    } else {
      return callAdapter;
    }
  }

  private CallAdapter<Observable<?>> getCallAdapter(Type returnType, int retryCount) {
    Type observableType = getParameterUpperBound(0, (ParameterizedType) returnType);
    return new SimpleCallAdapter(observableType, retryCount);
  }

  static final class SimpleCallAdapter implements CallAdapter<Observable<?>> {

    private final Type responseType;
    private final int retryCount;

    SimpleCallAdapter(Type responseType, int retryCount) {
      this.responseType = responseType;
      this.retryCount = retryCount;
    }

    @Override public Type responseType() {
      return responseType;
    }

    @Override public <R> Observable<R> adapt(Call<R> call) {

      return Observable.create(new CallOnSubscribe<>(call))
          .lift(OperatorMapResponseToBodyOrError.<R>instance())
          .retryWhen(new RetryWhenFunc(retryCount));
    }
  }

  static final class CallOnSubscribe<T> implements Observable.OnSubscribe<Response<T>> {

    private final Call<T> originalCall;

    CallOnSubscribe(Call<T> originalCall) {
      this.originalCall = originalCall;
    }

    @Override public void call(final Subscriber<? super Response<T>> subscriber) {
      // Since Call is a one-shot type, clone it for each new subscriber.
      Call<T> call = originalCall.clone();

      // Wrap the call in a helper which handles both unsubscription and backpressure.
      RequestArbiter<T> requestArbiter = new RequestArbiter<>(call, subscriber);
      subscriber.add(requestArbiter);
      subscriber.setProducer(requestArbiter);
    }
  }

  static final class RequestArbiter<T> extends AtomicBoolean implements Subscription, Producer {

    private final Call<T> call;
    private final Subscriber<? super Response<T>> subscriber;

    private final AtomicBoolean unsubscribed = new AtomicBoolean(false);

    RequestArbiter(Call<T> call, Subscriber<? super Response<T>> subscriber) {
      this.call = call;
      this.subscriber = subscriber;
    }

    @Override public void request(long n) {
      if (n < 0) throw new IllegalArgumentException("n < 0: " + n);
      if (n == 0) return; // Nothing to do when requesting 0.
      if (!this.compareAndSet(false, true)) return; // Request was already triggered.

      try {
        Response<T> response = call.execute();
        if (!subscriber.isUnsubscribed() && !call.isCanceled()) {
          subscriber.onNext(response);
          subscriber.onCompleted();
        }
      } catch (Throwable t) {
        Exceptions.throwIfFatal(t);
        if (!subscriber.isUnsubscribed()) {
          subscriber.onError(t);
        }
      }
    }

    @Override public void unsubscribe() {
      if (this.unsubscribed.compareAndSet(false, true)) {
        call.cancel();
      }
    }

    @Override public boolean isUnsubscribed() {
      return unsubscribed.get() && call.isCanceled();
    }
  }

  static final class RetryWhenFunc
      implements Func1<Observable<? extends Throwable>, Observable<Long>> {

    private Integer maxConnectCount = 1;

    public RetryWhenFunc(Integer retryCount) {
      this.maxConnectCount += retryCount;
    }

    @Override public Observable<Long> call(Observable<? extends Throwable> errorObservable) {
      return errorObservable.zipWith(Observable.range(INITIAL, maxConnectCount),
          new Func2<Throwable, Integer, InnerThrowable>() {

            @Override public InnerThrowable call(Throwable throwable, Integer i) {
              if (throwable instanceof IOException) return new InnerThrowable(throwable, i);
              return new InnerThrowable(throwable, i);
            }
          }).concatMap(new Func1<InnerThrowable, Observable<Long>>() {
        @Override public Observable<Long> call(InnerThrowable innerThrowable) {

          Integer currentCount = innerThrowable.getRetryCount();
          if (RetryWhenFunc.this.maxConnectCount.equals(currentCount)) {
            return Observable.error(innerThrowable.getThrowable());
          }

          /*use Schedulers#immediate() to keep on same thread */
          return Observable.timer((long) Math.pow(2, currentCount), TimeUnit.SECONDS,
              Schedulers.immediate());
        }
      });
    }
  }

  private static class InnerThrowable {

    private Throwable throwable;
    private Integer retryCount;

    public InnerThrowable(Throwable throwable, Integer retryCount) {
      this.throwable = Util.checkNotNull(throwable, "throwable == null");
      this.retryCount = Util.checkNotNull(retryCount, "maxConnectCount == null");
    }

    public Throwable getThrowable() {
      return throwable;
    }

    public Integer getRetryCount() {
      return retryCount;
    }
  }
}
