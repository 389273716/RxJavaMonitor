package com.tc.rxjavamonitor.monitor.custominterface;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * author：   tc
 * date：      2019/3/19 & 15:13
 * version    1.0
 * description 自定义IO线程池的接口
 * modify by
 */
public interface IThreadPool {

    /**
     * 获取主要的线程池
     */
    ThreadPoolExecutor getMainIOThreadPoolExecutor();

    /**
     * 获取其它非主要的线程池组，如果有多个线程池的话
     */
    List<ThreadPoolExecutor> getOthersThreadPoolExecutor();

    /**
     * 在线程信息被统计后，通过此方法去清除线程缓存信息，以便区分每次的线程高峰
     */
    void resetThreadCacheInfo();

    /**
     * 获取当前活跃的任务数
     */
    int getActiveTaskCount();

    /**
     * 获取当前线程池线程数
     */
    int getRunningPoolSize();

    /**
     * 获取丢弃任务数
     */
    int getDiscardTaskCount();

    /**
     * 获取线程池组，如果有的话
     */
    ThreadGroup getThreadGroup();

    /**
     * 上一次执行中任务的数量峰值
     */
    long getLastRunningTaskPeak();

    /**
     * 上一次执行中任务的数量峰值的发生时间
     */
    long getLastRunningTaskPeakHappenedTime();

    /**
     * 获取自定义的线程池当前运行状态信息
     */
    String getThreadPoolInfoForLog();

    void executeTask(Runnable runnable);
}
