## Version 1.3.2
*2016-06-23*

- FIXED : 修复contentType.type()空指针异常行为

## Version 1.3.1
*2016-06-20*

- FIXED : 修复RxJava下的请求重试问题。

## Version 1.3.0
*2016-06-20*

- NEW : 为`HttpCallback`类中的所有方法函数，添加`HttpCall`回调参数。现在你可以从结果回调中拿这个对象，做更多的事，如判断网络请求是否被取消等`.isCanceled()`。

## Version 1.2.3
*2016-06-17*

- **Change** :  添加特地用来打印**Json**的`JsonPrinter`类，调用`.json(String json)`打印格式化的Json字符串。

## Version 1.2.2
*2016-06-06*
 
- **Change : 统一包名前缀，由`com.homelink`变更为`com.lianjia`，请仔细阅读Readme中的[下载](./README.md/#下载)章节，以确保正确的依赖关系**
- New : 添加`HttpLoggingInterceptor.createLoggingInterceptor(List<String> exRequestHeaders, List<String> exResponseHeaders)`函数，隐藏不需要的请求头和响应头信息.

## Version 1.2.1
*2016-06-02*

- Fixed : 修改拼写错误的包名


## Version 1.2.0
*2016-06-02*

- New : 添加`HttpService`类，允许手动设置个日志输出的`TAG`，以及控制全局网络请求结果（Json字符串）的打印。

## ~~Version 1.1.0~~
*2016-06-01*

**不建议使用该版本**

移除Logger类库，创建`LoggerPrinter`用于打印响应结果的Json字符串。


## Version 1.0.0
*2016-05-31*

Initial release.

公共类的抽取和一些细节优化。

- New : 添加了两个全新的注解：`@RetryCount`，用来控制本次网络请求的最大重试次数；`@LogResult`用来控制本次网络请求是否输出响应体日志。
- Change : 为了保证各端调用的通用性，`LinkCall`接口统一更名为`HttpCall`。
- Change : 使用`HeaderInterceptor`添加通用请求头时，需要实现`headers`抽象方法，并提供持有请求头的`HashMap<String,String>`类型的集合。