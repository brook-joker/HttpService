# HttpService
-------------------------------
[![](https://jitpack.io/v/SmartDengg/HttpService.svg)](https://jitpack.io/#SmartDengg/HttpService)
[![TeamCity CodeBetter](https://img.shields.io/teamcity/codebetter/bt428.svg?maxAge=2592000)]()

[![Method count](https://img.shields.io/badge/Method count  - 428-ff69b4.svg?style=flat)](http://www.methodscount.com/?lib=com.github.SmartDengg%3AHttpService%3A0.1.0)
[![Size](https://img.shields.io/badge/Size  - 53 KB-ff69b4.svg?style=flat)](http://www.methodscount.com/?lib=com.github.SmartDengg%3AHttpService%3A0.1.0)

[![GitHub stars](https://img.shields.io/github/stars/SmartDengg/HttpService.svg?style=social&label=Star&maxAge=2592000?style=plastic)](https://github.com/SmartDengg/HttpService/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/SmartDengg/HttpService.svg?style=social&label=Fork&maxAge=2592000?style=plastic)](https://github.com/SmartDengg/HttpService/network)
[![GitHub issues](https://img.shields.io/github/issues/SmartDengg/HttpService.svg?style=social&label=Issue&maxAge=2592000?style=plastic)](https://github.com/SmartDengg/HttpService/issues)

[![GitHub license](https://img.shields.io/badge/License  - MIT-blue.svg?style=flat-square)](LICENSE.txt)

## 简介

一个在Android下使用的类型安全的Http连接库。基于运行时注解和动态代理，能有效减少样板代码的编写，提高开发效率。

## 下载

因为该类库还依赖一些其它Library，如：[Retrofit](https://github.com/square/retrofit)，[RxJava](https://github.com/ReactiveX/RxJava)，[Gson](https://github.com/google/gson)等。
因此在`build.gradle`中需要添加[最新版本](./CHANGELOG.md)的依赖：

- 首先，在根`Gradle`文件中添加JitPack仓库
```groovy
allprojects {
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}
}
```

- 其次，在你的module的`Gradle`中添加类库地址
```groovy
dependencies {
	compile 'com.github.SmartDengg:HttpService:0.2.6'
}
```

## 使用

### 基本请求类型

#### GET请求类型

`@Get()`括号内传入地址的绝对路径，`String`类型。

```java

public interface Service {

    /*"..users/list?sort=sortType"*/
    @GET("users/list")
    HttpCall<Foo> fetch(@Query("sort") String sortType);

    /*如果需要多个参数，可使用@QueryMap*/
    /*"..users/list?key1=value1&key2=value2"*/
    @GET("users/list")
    HttpCall<Foo> fetch(@QueryMap Map<String, String> options););

    /*"..group/groupId/users?sort=sortType"*/
    @GET("group/{id}/users")
    HttpCall<List<User>> groupList(@Path("id") int groupId, @Query("sort") String sortType);
}
```

#### POST

`@Post()`括号内传入地址的绝对路径，`String`类型。

假设你已经知道了[Post](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=2&ved=0ahUKEwjr2NLYrYbNAhVL-mMKHf9ZCewQFggjMAE&url=https%3A%2F%2Fen.wikipedia.org%2Fwiki%2FPOST_(HTTP)&usg=AFQjCNGRkFCcEc4DCLuJfJgre8qtVlFXag&sig2=Qi_OS4wkKpnVAqh8PR2dWQ)与[Get](https://en.wikipedia.org/wiki/Get)的[不同之处](https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0ahUKEwiApM6prYbNAhULw2MKHWL5DxwQFggdMAA&url=http%3A%2F%2Fwww.w3schools.com%2Ftags%2Fref_httpmethods.asp&usg=AFQjCNFbrRI8ZNXZ7cyqdNgMrb0gQFcrQg&sig2=uv5TIoAJ4solKIyple0odA)，其中之一就是Post的请求参数将被放到请求体中，而Get则拼接在Url之后。

- 普通Post请求

```java

public interface Service {

    @FormUrlEncoded
    @POST("user/edit")
    Call<User> updateUser(@Field("first_name") String first, @Field("last_name") String last);

    /*或者，可二选一*/
    @FormUrlEncoded
    @POST("user/edit")
    Call<User> updateUser(@FieldMap Map<String, String> parms);

    /*user实例将会被转换成Json字符串，并最终放到请求体中*/
    @POST("users/new")
    Call<User> createUser(@Body User user);

}
```

- 表单提交

|     字段    |    含义    |    必选    |    类型    |
|-------------|-----------|------------|------------|
| uploadId    |上传Id      |Yes        |String      |
| file        |  文件      |Yes        |MultipartFile  |
  

定义接口：
```java
public interface FileUploadService { 

      @Multipart      
      @POST("/profile/upload")
      HttpCall<Foo> uploadFile(@Part("uploadId") RequestBody uploadId, @Part MultipartBody.Part file);
}
```

Android代码：
```java

private void uploadFile(String filePath) {  

    // 创建FileUploadService接口实例
    FileUploadService service = ServiceGenerator.createService(FileUploadService.class);

    // 创建持有uploadId的RequestBody实例
    String uploadId = "20160601";
    RequestBody uploadIdBody = RequestBody.create(MultipartBody.FORM, uploadId);

    //创建持有file的MultipartBody.Part实例，用来上传真实的文件
    File file= new File(filePath);
    RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), file);
    MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(), fileBody);

    // 获取HttpCall实例
    HttpCall<Foo> httpCall= service.uploadFile(uploadId, fileBody);
    
    // 执行请求
    httpCall.enqueue(new HttpCallbackAdapter<Foo>() {
            @Override
            public void success(Foo foo) {
               System.out.println("success");
            }

            @Override
            public void networkError(IOException e) {
              System.err.println(e.getMessage());
            }

            @Override
            public void unexpectedError(Throwable t) {
                 System.err.println(t.getMessage());
            }
        });
}

```

*0.2.5*版本之后可以使用新的工具类`PartGenerator`来帮你简化**上传多个文件**时所需参数:

```java
    // 创建持有uploadId的RequestBody实例
    RequestBody uploadIdBody = PartGenerator.createPartFromString("206101");
    //创建持有file的MultipartBody.Part实例，用来上传真实的文件
    File file= new File(filePath);
    MultipartBody.Part fileBody = PartGenerator.createPartFromFile(MediaType.parse("image/*"),file.getName(), file);
```

下面示例演示如何在一个接口中上传多个文件,以及如何使用**@PartMap()**注解添加多个`RequestBody`:

首先定义请求接口函数:
```java
public interface FileUploadService {  

    @Multipart
    @POST("user/upload")
    Call<ResponseBody> uploadMultipleFiles(
            @Part("description") RequestBody description,
            @Part MultipartBody.Part file1,
            @Part MultipartBody.Part file2);
                
    @Multipart
    @POST("user/upload")
    Call<ResponseBody> uploadFileWithPartMap(
            @PartMap() Map<String, RequestBody> partMap,
            @Part MultipartBody.Part file);
}
```

逻辑代码如下:
```java
public void multipleFilesSample(){

    File videoFile= new File(videoFileUri);
    File thumbFile= new File(thumbFileUri);
    
    FileUploadService service =  
            ServiceGenerator.createService(FileUploadService.class);
    
    /*注意:请和服务端确认你的MediaType,默认情况下MediaType为"multipart/form-data"*/
    MultipartBody.Part videoBody = PartGenerator.createPartFromFile("video", videoFile);  
    MultipartBody.Part thumbBody = PartGenerator.createPartFromFile("thumb", thumbFile);
    
    RequestBody description = PartGenerator.createPartFromString("This's a description");
    
    Call<ResponseBody> httpCall = service.uploadMultipleFiles(description, videoBody, thumbBody);  
    httpCall.enqueue(new HttpCallbackAdapter<ResponseBody>() {
                @Override
                public void success(ResponseBody responseBody) {
                   System.out.println("success");
                }
            });
    }
    
public void fileWithPartMapSample(){

    File photoFile= new File(photoFileUri);
    
    FileUploadService service =  
            ServiceGenerator.createService(FileUploadService.class);
    
    /*注意:请和服务端确认你的MediaType,默认情况下MediaType为"multipart/form-data"*/
    MultipartBody.Part photoBody = PartGenerator.createPartFromFile("photo", photoFile);  
    
    RequestBody description = PartGenerator.createPartFromString("My profile");
    RequestBody user = PartGenerator.createPartFromString("SmartDengg");  
    RequestBody time = PartGenerator.createPartFromString("20160707");
    
    HashMap<String, RequestBody> partmMap = new HashMap<>();  
    partmMap.put("description", description);  
    partmMap.put("userName", userName);  
    partmMap.put("time", time);
    
    Call<ResponseBody> httpCall = service.uploadFileWithPartMap(partmMap, photoBody);  
    httpCall.enqueue(new HttpCallbackAdapter<ResponseBody>() {
                @Override
                public void success(ResponseBody responseBody) {
                   System.out.println("success");
                }
            });
    }
```

关于`ServiceGenerator`我会在后面提到。

### 灵活运用注解

#### 添加请求头

- 使用方法类型的注解添加静态请求头

```java

public interface Service {

	@Headers("Cache-Control: max-age=640000")
	@GET("widget/list")
	Call<List<Widget>> widgetList();

	@Headers({
	    "Accept: application/vnd.github.v3.full+json",
	    "User-Agent: Retrofit-Sample-App"
	})
	@GET("users/{username}")
	Call<User> getUser(@Path("username") String username);
}

```

- 使用参数类型的注解添加动态请求头

```java

public interface Service {

	@GET("user")
	Call<User> getUser(@Header("Authorization") String authorization)
}

```

#### 动态添加Url

`@GET()`或`@POST()`后面的括号不添加任何路径，通过`@Url`注解动态添加请求地址。

```java

public interface Service {  
	@GET
	public Call<User> profilePicture(@Url String url);
}

```

#### 使用新注解

在HttpServic中添加了两个全新的注解，使网络连接更灵活可靠。

- @RetryCount

使用`@RetryCount`注解为特定的请求添加**最大重试次数**，默认不做失败重试。
值得一提的是，当`Observable`，`Single`或者`Completable`作为返回类型时，加入了[二进制指数退避](http://baike.baidu.com/view/2984826.htm)效果。

```java
private interface Service {

    //最大请求次数：3次
    @RetryCount(count = 3)
    @GET("user/info")
    HttpCall<User> fetch(@QueryMap Map<String, String> params);

}
```

- @LogResult

使用`@LogResult`注解来控制是否打印（Json格式）**响应体**，默认打印所有响应体。


```java

private interface Service {

    //不关心响应体（一般为json字符串），关闭响应体的打印日志，但是会保留状态行和响应头等。
    @LogResult(enable = false)
    @GET("user/ping")
    HttpCall<Foo> loop(@Query("id") String userId);
}
```


#### 使用ServiceGenerator生成接口实例

以下是一个可供参考的`ServiceGenerator`模板类。

```java

public class ServiceGenerator {

    private static OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
    private static Retrofit retrofit;

    static {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                                     .enableComplexMapKeySerialization()
                                     .serializeNulls()
                                     .create();

        //创建HeaderInterceptor实例，添加通用请求头
        HeaderInterceptor headerInterceptor = new HeaderInterceptor() {
            @Override
            public HashMap<String, String> headers() {

                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Host", "Lianjia.com");
                headers.put("Timeline", String.valueOf(System.currentTimeMillis()));

                return headers;
            }
        };

        //创建HttpLoggingInterceptor实例，打印地址访问情况，可根据实际情况选择适当的HttpLoggingInterceptor.Level
        HttpLoggingInterceptor loggingInterceptor = HttpLoggingInterceptor.createLoggingInterceptor()
                                                                          .setLevel(HttpLoggingInterceptor.Level.BODY);

        ServiceGenerator.httpClientBuilder.addInterceptor(headerInterceptor)
                                          .addInterceptor(loggingInterceptor);

        ServiceGenerator.retrofit = new Retrofit.Builder().baseUrl("scheme://host:port/x/y")
                                                          .addCallAdapterFactory(HttpCallAdapterFactory.create())
                                                          .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                                                          .addConverterFactory(GsonConverterFactory.create(gson))
                                                          .client(httpClientBuilder.build())
                                                          .validateEagerly(BuildConfig.DEBUG)
                                                          .build();
    }

    public static <S> S createService(Class<S> serviceClass) {
        return retrofit.create(serviceClass);
    }
}

```

另外需要注意的是，由于使用了`<a href="...">`的组合方式，所以无论是`@Get("path")`还是`@Post("path")`，其后面传入的“path”，都应该注意与`BaseUrl`组合后的结果。

这里罗列了四种组合方式：

|     baseUrl    |    path    |    组合结果
|-------------|-----------|------------
| scheme://host:port/x/y      |/path      |scheme://host:port/path
| scheme://host:port/x/y      |path        |scheme://host:port/x/path
| scheme://host:port/x/y/     |/path      |scheme://host:port/path
| scheme://host:port/x/y/     |path        |scheme://host:port/x/y/path


看起来第四种比较符合书写习惯，因此这里给出的建议是**Base URL: 总是以" / "结尾@Url: 不要以" / "开头**


#### 接口调用方式

以`UserService`为例：

```java

private interface UserService {

    @GET("user")
    HttpCall<User> synchronize(@Query("id") String userId);

    @GET("user")
    HttpCall<User> asynchronous(@Query("id") String userId);

    @GET("user")
    Observable<User> fetch(@Query("id") String userId);
}

```

首先使用上面的的`ServiceGenerator`生成`UserService`实例
```java

private UserService service = ServiceGenerator.createService(UserService.class);

```

- 同步请求（请确保不会阻塞UI线程）

```java

    HttpCall<User> synchronizeCall= service.asynchronous("20160601");
    try {
        Response<User> userResponse = synchronizeCall.execute();
        //处理响应结果逻辑
    } catch (IOException e) {
        e.printStackTrace();
    }

```

- 异步请求

```java

    HttpCall<User> asynchronousCall = service.asynchronous("20160601");
    asynchronousCall.enqueue(new HttpCallback<User>() {
        @Override
        public void success(User entity, HttpCall<User> httpCall) {
          /** Called for [200, 300) responses. But not include 204 or 205 */

        }

        @Override
        public void noContent(Response<?> response, HttpCall<User> httpCall) {
          /** Called for 204 and 205 */

        }

        @Override
        public void unauthenticated(Response<?> response, HttpCall<User> httpCall) {
          /** Called for 401 responses. */

        }

        @Override
        public void clientError(Response<?> response, HttpCall<User> httpCall) {
          /** Called for [400, 500) responses, except 401. */

        }

        @Override
        public void serverError(Response<?> response, HttpCall<User> httpCall) {
          /** Called for [500, 600) response. */

        }

        @Override
        public void networkError(IOException e, HttpCall<User> httpCall) {
          /** Called for network errors while making the call. */

        }

        @Override
        public void unexpectedError(Throwable t, HttpCall<User> httpCall) {
          /** Called for unexpected errors while making the call. */

        }
    });

```

- RxJava调用

```java

    service.fetch("20160601")
           .subscribeOn(Schedulers.newThread())
           .observeOn(AndroidSchedulers.mainThread())
           .subscribe(new Subscriber<User>() {
               @Override
               public void onCompleted() {
                 System.out.println("completed");
               }

               @Override
               public void onError(Throwable e) {
                 System.err.println(e.getMessage());
               }

               @Override
               public void onNext(User userEntity) {
                 System.out.println("success");
               }
           });

```

#### 自定义TAG和限制Response打印

可以在你的`Application`中，设置`HttpService`的一些简单属性，如，网络日志的输出`tag`，是否允许以Json字符串格式打印响应体。需要注意的是，这个响应体输出开关是**全局**的，如果你想禁用某个特定请求的Json输出，请使用上面提到的`@LogResult`。

```java

    HttpService.setHttpTAG("Your TAG")
               .enableResponseLog(true);
```

最后，值得一提的是，当开发者遵循以上提到的所有规范时，日志输出将会变得非常漂亮，这对我们定位异常和追踪网络日志有很大的帮助。


### 日志输出样板：

- GET日志输出样板：

```
D/LOG-HTTP: ╔════════════════════════════════════════════════════════════════════════════════════════
D/LOG-HTTP: ║--> GET http://v.juhe.cn/weather/index?dtype=json&key=f4817e5b8e43721a6fe7352bb60e27b2&format=2&cityname=%25E5%258C%2597%25E4%25BA%25AC HTTP/1.1
D/LOG-HTTP: ║Location: sunshine Beijing
D/LOG-HTTP: ║Timeline: 1465208009723
D/LOG-HTTP: ║Lianjia: com.Lianjia
D/LOG-HTTP: ║--> END GET
D/LOG-HTTP: ╟────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║<-- 200 OK http://v.juhe.cn/weather/index?dtype=json&key=f4817e5b8e43721a6fe7352bb60e27b2&format=2&cityname=%25E5%258C%2597%25E4%25BA%25AC (39ms)
D/LOG-HTTP: ║Server: nginx/1.2.6
D/LOG-HTTP: ║Date: Mon, 06 Jun 2016 10:12:59 GMT
D/LOG-HTTP: ║Content-Type: application/json;charset=utf-8
D/LOG-HTTP: ║Transfer-Encoding: chunked
D/LOG-HTTP: ║Connection: keep-alive
D/LOG-HTTP: ║X-Powered-By: PHP/5.4.36
D/LOG-HTTP: ║Etag: 8cd47694442850479f1806fe089034ab
D/LOG-HTTP: ║OkHttp-Sent-Millis: 1465208009745
D/LOG-HTTP: ║OkHttp-Received-Millis: 1465208009764
D/LOG-HTTP: ╟────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║<-- END HTTP (96-byte body)
D/LOG-HTTP: ╚════════════════════════════════════════════════════════════════════════════════════════
D/LOG-HTTP: ╔════════════════════════════════════════════════════════════════════════════════════════
D/LOG-HTTP: ║ Thread: EventAsyncOrBackground #1
D/LOG-HTTP: ╟────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║ {
D/LOG-HTTP: ║     "error_code": 203902,
D/LOG-HTTP: ║     "result": null,
D/LOG-HTTP: ║     "reason": "查询不到该城市的信息",
D/LOG-HTTP: ║     "resultcode": "202"
D/LOG-HTTP: ║ }
D/LOG-HTTP: ╚════════════════════════════════════════════════════════════════════════════════════════
```

- POST日志输出样板

其中`dtype`、`key`等请求参数被放置到单线框内的请求体中。

```
D/LOG-HTTP: ╔════════════════════════════════════════════════════════════════════════════════════════
D/LOG-HTTP: ║--> POST http://v.juhe.cn/weather/index HTTP/1.1
D/LOG-HTTP: ║Content-Type: application/x-www-form-urlencoded
D/LOG-HTTP: ║Content-Length: 96
D/LOG-HTTP: ║Location: sunshine Beijing
D/LOG-HTTP: ║Timeline: 1465207875648
D/LOG-HTTP: ║Lianjia: com.Lianjia
D/LOG-HTTP: ║    ┌────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║    │dtype=json&key=f4817e5b8e43721a6fe7352bb60e27b2&format=1&cityname=%25E5%258C%2597%25E4%25BA%25AC
D/LOG-HTTP: ║    └────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║--> END POST (96-byte body)
D/LOG-HTTP: ╟────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║<-- 200 OK http://v.juhe.cn/weather/index (1246ms)
D/LOG-HTTP: ║Server: nginx/1.2.6
D/LOG-HTTP: ║Date: Mon, 06 Jun 2016 10:10:46 GMT
D/LOG-HTTP: ║Content-Type: application/json;charset=utf-8
D/LOG-HTTP: ║Transfer-Encoding: chunked
D/LOG-HTTP: ║Connection: keep-alive
D/LOG-HTTP: ║X-Powered-By: PHP/5.4.36
D/LOG-HTTP: ║Etag: 8cd47694442850479f1806fe089034ab
D/LOG-HTTP: ║OkHttp-Sent-Millis: 1465207876879
D/LOG-HTTP: ║OkHttp-Received-Millis: 1465207876898
D/LOG-HTTP: ╟────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║<-- END HTTP (81-byte body)
D/LOG-HTTP: ╚════════════════════════════════════════════════════════════════════════════════════════
D/LOG-HTTP: ╔════════════════════════════════════════════════════════════════════════════════════════
D/LOG-HTTP: ║ Thread: OkHttp http://v.juhe.cn/weather/index
D/LOG-HTTP: ╟────────────────────────────────────────────────────────────────────────────────────────
D/LOG-HTTP: ║ {
D/LOG-HTTP: ║     "error_code": 203902,
D/LOG-HTTP: ║     "result": null,
D/LOG-HTTP: ║     "reason": "查询不到该城市的信息"
D/LOG-HTTP: ║     "resultcode": "202"
D/LOG-HTTP: ║ }
D/LOG-HTTP: ╚════════════════════════════════════════════════════════════════════════════════════════
```

另外值得一提的是，如果认为有些`Header`信息是冗余，或者你想隐藏它们，可以使用`HttpLoggingInterceptor`的重载函数`.createLoggingInterceptor
(List<String> exRequestHeaders, List<String> exResponseHeaders)`，示例如下：

```java
List<String> exRequestHeaders = new ArrayList<>();
/*不打印"loaction"的请求头*/
exRequestHeaders.add("loaction");

List<String> exResponseHeaders = new ArrayList<>();
/*不打印"Content-Type"的响应头*/
exResponseHeaders.add("Content-Type");

HttpLoggingInterceptor loggingInterceptor = HttpLoggingInterceptor.createLoggingInterceptor(exRequestHeaders, exResponseHeaders)
                                                                  .setLevel(HttpLoggingInterceptor.Level.BODY);
```

## 开发者列表

- 邓伟 - Hi4Joker@gmail.com
- [小鄧子的简书](http://www.jianshu.com/users/df40282480b4/latest_articles)

<a href="http://weibo.com/5367097592/profile?rightmod=1&wvr=6&mod=personinfo">
  <img alt="Follow me on Weibo" src="http://upload-images.jianshu.io/upload_images/268450-50e41e15ac29b776.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240" />
</a>

License
--------

	Copyright (c) 2016 小鄧子

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in all
	copies or substantial portions of the Software.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
	SOFTWARE.
	
[▲ 回到顶部](#top)


