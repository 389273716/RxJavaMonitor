package com.tc.rxjavamonitor.monitor.log;

/**
 * author：   tc
 * date：      2019/4/3 & 9:44
 * version    1.0
 * description  日志写入常量
 * modify by
 */
public class MonitorConstants {
    /**
     * 换行占位符，用来压缩日志文本
     */
    public static final String PLACEHOLDER_NEW_LINE = "&NL&";
    /**
     * 信息间隔标识，在每个信息之间间隔，比如   &SP&15646415614&SP&  间隔时间
     */
    public static final String INFO_SPLIT = "&SP&";


    /**
     * 换行符
     */
    public static final String NEW_LINE_REPLACE_STR = "\n";
    /**
     * 占位符在debug模式下的替换字符
     */
    public static final String SPLIT_REPLACE_STR = "  ";
}
