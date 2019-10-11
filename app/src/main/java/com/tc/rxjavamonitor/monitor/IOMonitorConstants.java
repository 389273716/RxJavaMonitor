package com.tc.rxjavamonitor.monitor;

/**
 * author：   tc
 * date：      2019/3/13 & 19:42
 * version    1.0
 * description 常量类，用于日志打印等
 * modify by
 */
public class IOMonitorConstants {
    /**
     * 日志统一打印tag
     */
    public static final String MONITOR_LOG_TAG = "app_io_scheduler";

    /**
     * IO线程前缀名
     */
    public static final String THREAD_NAME_PREFIX = "app-io-scheduler-thread-";
    /**
     * 存在可追踪堆栈的io任务后缀
     */
    public static final String IO_TASK_NAME_SUFFIX = "(exist_stackTrace_task)";
    /**
     * 线程池以及队列满了的错误提示
     */
    public static final String TASK_GIVE_UP_TIP = "task peak,thread pool and task queue was full,this " +
            "task give up :%s";

    /**
     * 运行任务打印前缀
     */
    public static final String NORMAL_TASK = "(Normal-Task)";
    /**
     * 耗时任务打印前缀
     */
    public static final String TIME_CONSUMING = "(Time-consuming)";

    public static final String TIME_CONSUMING_STACK = "(Time-consuming-stack)";
    /**
     * 丢弃任务前缀
     */
    public static final String DISCARD_TASK_PREFIX = "task peak--discard task";

    /**
     * 监控线程信息
     */
    public static final String MONITOR_THREAD_POOL_INFO = "(monitor_thread_pool_info)";

    /**
     * 任务高峰期相关日志tag
     */
    public static final String TASK_PEAK = "(task_peak)";
    /**
     * 打桩监控tag
     */
    public static final String PILING_MONITOR = "(piling monitor)";

}
