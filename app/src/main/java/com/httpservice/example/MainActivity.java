package com.httpservice.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import com.smartdengg.httpservice.lib.adapter.rxadapter.rxcompat.SchedulersCompat;
import com.smartdengg.httpservice.lib.annotation.LogResult;
import com.smartdengg.httpservice.lib.annotation.RetryCount;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;
import rx.Subscriber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    findViewById(R.id.tv).setOnClickListener(this);
  }

  @Override public void onClick(View v) {
    ServiceGenerator.createService(InternalService.class)
        .getCityList("test")
        .compose(SchedulersCompat.<ResponseBody>applyExecutorSchedulers())
        .subscribe(new Subscriber<ResponseBody>() {
          @Override public void onCompleted() {
            System.out.println("MainActivity.onCompleted");
          }

          @Override public void onError(Throwable e) {
            System.out.println("e = [" + e + "]");
          }

          @Override public void onNext(ResponseBody responseBody) {
            System.out.println("MainActivity.onNext");
          }
        });
  }

  interface InternalService {

    @LogResult(enable = false) @RetryCount(count = 3) @GET Observable<ResponseBody> getCityList(
        @Url String url);
  }
}
