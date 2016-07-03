/*
 * Copyright (C) 2016 Lianjia, Inc.
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

import com.smartdengg.httpservice.lib.adapter.callAdapter.HttpCallAdapter;
import com.smartdengg.httpservice.lib.errors.HttpException;
import retrofit2.Response;
import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.internal.util.RxJavaPluginUtils;

/**
 * A version of {@link Observable#map(Func1)} which lets us trigger {@code onError} without having
 * to use {@link Observable#flatMap(Func1)} which breaks producer requests from propagating.
 */
final class OperatorMapResponseToBodyOrError<T> implements Operator<T, Response<T>> {
  private static final OperatorMapResponseToBodyOrError<Object> INSTANCE =
      new OperatorMapResponseToBodyOrError<>();

  @SuppressWarnings("unchecked") // Safe because of erasure.
  static <R> OperatorMapResponseToBodyOrError<R> instance() {
    return (OperatorMapResponseToBodyOrError<R>) INSTANCE;
  }

  @Override public Subscriber<? super Response<T>> call(final Subscriber<? super T> child) {
    MapResponseSubscriber<T> parent = new MapResponseSubscriber<>(child);
    child.add(parent);
    return parent;
  }

  static final class MapResponseSubscriber<T> extends Subscriber<Response<T>> {

    private Subscriber<? super T> actual;

    private boolean done;

    public MapResponseSubscriber(Subscriber<? super T> actual) {
      this.actual = actual;
    }

    @Override public void onCompleted() {

      if (done) return;

      done = true;

      actual.onCompleted();
    }

    @Override public void onError(Throwable e) {

      if (done) {
        RxJavaPluginUtils.handleException(e);
        return;
      }

      done = true;

      actual.onError(e);
    }

    @Override public void onNext(Response<T> response) {

      if (isUnsubscribed()) return;

      Integer code = response.code();

      if (response.isSuccessful()
          && code != HttpCallAdapter.CODE_204
          && code != HttpCallAdapter.CODE_205) {
        actual.onNext(response.body());
      } else {
        actual.onError(new HttpException(response));
      }
    }

    @Override public void setProducer(Producer p) {
      actual.setProducer(p);
    }
  }
}
