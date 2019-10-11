package com.tc.rxjavamonitor.monitor.custominterface;


import android.util.Pair;

import com.tc.rxjavamonitor.monitor.IOMonitorConstants;
import com.tc.rxjavamonitor.monitor.IOMonitorManager;

import java.util.concurrent.ThreadFactory;

/**
 * author：   tc
 * date：      2019/3/22 & 16:42
 * version    1.0
 * description 自定义thread构造工厂，自定义Scheduler里的ThreadFactory需要继承这个
 * modify by
 */
public abstract class AbstractThreadFactory implements ThreadFactory {
    /**
     * 当前最大正在运行时的任务最大时的记录值，第一个参数是任务数，第二个参数是当时的时间点
     */
    private Pair<Long, Long> mLastMaxTaskCountInfo;

    @Override
    public Thread newThread(Runnable r) {
        Pair<Long, Long> pair = doThreadMonitor();
        return newOneThread(r, pair);
    }

    public Pair<Long, Long> doThreadMonitor() {
        if (!IOMonitorManager.getInstance().isMonitorEnable()) {
            return null;
        }
        long cur = IOMonitorManager.getInstance().getIThreadPool().getRunningPoolSize();
        if (mLastMaxTaskCountInfo == null || mLastMaxTaskCountInfo.first <= cur) {
            //此方法执行完后，会增加一个线程，所以这里+1
            mLastMaxTaskCountInfo = new Pair<>(cur + 1, System.currentTimeMillis());
        }
        boolean logMoreInfo = IOMonitorManager.getInstance().isLogMoreInfo();
        int activeCount = IOMonitorManager.getInstance().getIThreadPool().getActiveTaskCount() + 1;
        if (logMoreInfo && activeCount >= IOMonitorManager.getInstance().getThreadActiveCountLimit()) {
            //只在线程数达到limit后打印log
            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(System
                    .currentTimeMillis(), "--- create too many io thread ,current count:%d", activeCount));
            IOMonitorManager.getInstance().logThreadInfo(true);
        }
        return mLastMaxTaskCountInfo;
    }

    /**
     * 线程当前要执行的任务runnable和峰值监控信息，此方法回调时创建新的thread
     *
     * @param r              任务runnable
     * @param threadPeakInfo 峰值监控信息，第一个值为当前任务正在执行的任务数，第二个参数为触发时间
     * @return
     */
    public abstract Thread newOneThread(Runnable r, Pair<Long, Long> threadPeakInfo);
}
