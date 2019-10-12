package com.tc.rxjavamonitor.monitor.log;

import android.text.TextUtils;

import java.util.regex.Pattern;

/**
 * author：   tc
 * date：      2019/3/30 & 11:11
 * version    1.0
 * description  监控日志打印器
 * modify by
 */
public class MonitorLog {
    private boolean mIsInit;
    private static Pattern mNewLinePattern;
    private static Pattern mInfoSplitPattern;


    private static class SingleInstance {
        private static final MonitorLog INSTANCE = new MonitorLog();
    }

    public static MonitorLog getInstance() {
        return SingleInstance.INSTANCE;
    }

    private MonitorLog() {

    }

    public void init() {
        mNewLinePattern = Pattern.compile(MonitorConstants.PLACEHOLDER_NEW_LINE);
        mInfoSplitPattern = Pattern.compile(MonitorConstants.INFO_SPLIT);
    }

    public boolean isInit() {
        return mIsInit;
    }

    private void setInit(boolean init) {
        mIsInit = init;
    }


    private static boolean assertInitialization() {
        if (!MonitorLog.getInstance().isInit()) {
            MonitorLog.getInstance().init();
        }
        return false;
    }

    /**
     * 获取间隔拼接后的字符串
     *
     * @param param 要格式化的字符
     */
    public static String getSplitString(Object... param) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.currentTimeMillis());
        stringBuilder.append(MonitorConstants.INFO_SPLIT);
        for (Object s : param) {
            stringBuilder.append(s);
            if (!MonitorConstants.PLACEHOLDER_NEW_LINE.equals(s)) {
                stringBuilder.append(MonitorConstants.INFO_SPLIT);
            }
        }
        return stringBuilder.toString();
    }

    public static void thread(String tag, String msg) {
        if (assertInitialization()) {
            return;
        }
        log(tag, msg);
    }


    public static void log(String tag, String msg) {
        if (assertInitialization()) {
            return;
        }
        // TODO: 2019/10/12 本log类可以根据需要自行换成自己的业务log处理类

        //先在控制台输出log,格式化输出
        if (!TextUtils.isEmpty(MonitorConstants.PLACEHOLDER_NEW_LINE)) {
            msg = mNewLinePattern.matcher(msg).replaceAll
                    (MonitorConstants.NEW_LINE_REPLACE_STR);
            msg = mInfoSplitPattern.matcher(msg).replaceAll
                    (MonitorConstants.SPLIT_REPLACE_STR);
        }
        MonitorLog.logCatD(tag, msg);
    }


    public static void logCatW(String tag, String msg) {
        LogUtil.w(tag, msg);
    }

    public static void logCatI(String tag, String msg) {
        LogUtil.i(tag, msg);
    }

    public static void logCatD(String tag, String msg) {
        LogUtil.d(tag, msg);
    }

    public static void logCatE(String tag, String msg) {
        LogUtil.e(tag, msg);
    }

}
