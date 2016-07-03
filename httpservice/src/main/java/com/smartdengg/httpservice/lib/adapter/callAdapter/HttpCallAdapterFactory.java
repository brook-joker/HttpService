package com.smartdengg.httpservice.lib.adapter.callAdapter;

import android.os.Handler;
import android.os.Looper;
import com.smartdengg.httpservice.lib.annotation.RetryCount;
import com.smartdengg.httpservice.lib.utils.Types;
import com.smartdengg.httpservice.lib.utils.Util;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

/**
 * Created by Joker on 2016/2/19.
 */
public class HttpCallAdapterFactory extends CallAdapter.Factory {

    private MainThreadExecutor mainThreadExecutor;

    private HttpCallAdapterFactory() {
        this.mainThreadExecutor = new MainThreadExecutor();
    }

    public static HttpCallAdapterFactory create() {
        return new HttpCallAdapterFactory();
    }

    @Override
    public CallAdapter<?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {

        final int[] retryCount = new int[1];

        if (Types.getRawType(returnType) != HttpCall.class) {
            return null;
        }

        if (!(returnType instanceof ParameterizedType)) {
            /*返回结果应该指定一个泛型，最起码也需要一个ResponseBody作为泛型*/
            throw new IllegalStateException("HttpCall must have generic type (e.g., HttpCall<ResponseBody>)");
        }

        for (Annotation annotation : annotations) {
            if (!RetryCount.class.isAssignableFrom(annotation.getClass())) {
                continue;
            }
            retryCount[0] = ((RetryCount) annotation).count();
            if (retryCount[0] < 0) {
                throw new IllegalArgumentException("@RetryCount must not be less than 0");
            }
        }

        final Type responseType = Types.getParameterUpperBound(0, (ParameterizedType) returnType);

        return new CallAdapter<HttpCall<?>>() {
            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public <R> HttpCall<?> adapt(Call<R> call) {
                return new HttpCallAdapter<>(call, mainThreadExecutor, retryCount[0]);
            }
        };
    }

    public class MainThreadExecutor implements Executor {

        private Handler mainHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
            mainHandler.post(Util.checkNotNull(runnable, "runnable == null"));
        }
    }
}
