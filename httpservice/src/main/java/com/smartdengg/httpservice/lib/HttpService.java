package com.smartdengg.httpservice.lib;

/**
 * Created by Joker on 2016/6/2.
 */
public class HttpService {

    private static Settings mSettings = new Settings();
    private static HttpService sHttpService = new HttpService();

    public static HttpService setHttpTAG(String tag) {
        mSettings.setTAG(tag);
        return sHttpService;
    }

    public static HttpService enableResponseLog(boolean enable) {
        mSettings.enableResponse(enable);
        return sHttpService;
    }

    public static String getHttpTAG() {
        return mSettings.httpTag;
    }

    public static boolean enableResponseLog() {
        return mSettings.enableResponseLog;
    }

    static class Settings {

        private static String httpTag = BuildConfig.HTTP_LOG_TAG;
        private static boolean enableResponseLog = true;

        private static Settings sSettings = new Settings();

        public Settings setTAG(String tag) {
            Settings.httpTag = tag;
            return sSettings;
        }

        public Settings enableResponse(boolean enable) {
            Settings.enableResponseLog = enable;
            return sSettings;
        }

    }

}
