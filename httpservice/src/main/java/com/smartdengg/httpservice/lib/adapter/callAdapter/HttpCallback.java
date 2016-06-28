package com.smartdengg.httpservice.lib.adapter.callAdapter;

import java.io.IOException;
import retrofit2.Response;

/**
 * Created by Joker on 2016/2/19.
 */
public interface HttpCallback<T> {

    /** Called for [200, 300) responses. But not include 204 or 205 */
    void success(T entity, HttpCall<T> httpCall);

    /** Called for 204 and 205 */
    void noContent(Response<?> response, HttpCall<T> httpCall);

    /** Called for 401 responses. */
    void unauthenticated(Response<?> response, HttpCall<T> httpCall);

    /** Called for [400, 500) responses, except 401. */
    void clientError(Response<?> response, HttpCall<T> httpCall);

    /** Called for [500, 600) response. */
    void serverError(Response<?> response, HttpCall<T> httpCall);

    /** Called for network errors while making the call. */
    void networkError(IOException e, HttpCall<T> httpCall);

    /** Called for unexpected errors while making the call. */
    void unexpectedError(Throwable t, HttpCall<T> httpCall);
}
