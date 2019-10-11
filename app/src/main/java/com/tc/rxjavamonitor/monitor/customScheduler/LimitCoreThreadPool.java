package com.tc.rxjavamonitor.monitor.customScheduler;

import android.util.Pair;

import com.tc.rxjavamonitor.monitor.IOMonitorConstants;
import com.tc.rxjavamonitor.monitor.custominterface.AbstractRejectedExecutionHandler;
import com.tc.rxjavamonitor.monitor.custominterface.AbstractThreadFactory;
import com.tc.rxjavamonitor.monitor.custominterface.CustomThread;
import com.tc.rxjavamonitor.monitor.custominterface.IBaseWork;
import com.tc.rxjavamonitor.monitor.custominterface.IThreadPool;
import com.tc.rxjavamonitor.monitor.custominterface.MonitorThreadPoolExecutor;
import com.tc.rxjavamonitor.monitor.queue.IOPriorityQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author：   tc
 * date：      2019/3/25 & 11:01
 * version    1.0
 * description 线程数和队列可受限制大小的线程池，带优先级策略和丢弃任务策略。
 * modify by
 */
public class LimitCoreThreadPool implements IThreadPool {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    /**
     * 丢弃任务数
     */
    private final AtomicInteger mDiscardTaskCount = new AtomicInteger(0);
    /**
     * 核心主任务线程池-IO
     */
    private static final String APP_WORK_THREAD_CORE = "app-running-thread-";
    private ThreadGroup mThreadGroup;
    /**
     * 主线程池
     */
    private ThreadPoolExecutor mMainThreadPoolExecutor;
    /**
     * 当前最大正在运行时的任务最大时的记录值，第一个参数是任务数，第二个参数是当时的时间点
     */
    private Pair<Long, Long> mLastMaxTaskCountInfo;

    /**
     * 创建线程池，根据传入参数配置
     *
     * @param corePoolSize           核心线程数
     * @param maxPoolSize            最大线程数
     * @param keepAliveTime          保活时间
     * @param blockingQueueSize      阻塞队列大小,0代表无限制
     * @param allowCoreThreadTimeOut 是否允许回收核心线程
     */
    public LimitCoreThreadPool build(int corePoolSize, int maxPoolSize, long keepAliveTime, int blockingQueueSize,
                                     boolean allowCoreThreadTimeOut) {
        mThreadGroup = new ThreadGroup("app_io_thread_group");
//        IOLimitArrayQueue runnableIOLimitQueue = new IOLimitArrayQueue(blockingQueueSize, mMainThreadPoolExecutor);
        IOPriorityQueue runnableIOLimitQueue;
        if (blockingQueueSize == 0) {
            //0代表无限制大小
            runnableIOLimitQueue = new IOPriorityQueue(100, 0);
        } else {
            runnableIOLimitQueue = new IOPriorityQueue(100, blockingQueueSize);
        }

        //主线程池，主要任务队列处理，当任务数不超过机器性能限制时，使用此线程池
        mMainThreadPoolExecutor = new MonitorThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit
                .SECONDS, runnableIOLimitQueue,
                new AbstractThreadFactory() {
                    @Override
                    public Thread newOneThread(Runnable r, Pair<Long, Long> threadPeakInfo) {
                        int index = threadNumber.getAndIncrement();
                        mLastMaxTaskCountInfo = threadPeakInfo;
                        return new CustomThread(mThreadGroup, r, String.format("%s%s", APP_WORK_THREAD_CORE,
                                index));
                    }
                }, new AbstractRejectedExecutionHandler() {

            @Override
            public void rejectedTask(Runnable runnable, ThreadPoolExecutor threadPoolExecutor, IBaseWork
                    schedulerWorker) {
                String taskName = "";
                if (schedulerWorker != null) {
                    taskName = schedulerWorker.getTaskName();
                }
                mDiscardTaskCount.getAndIncrement();
                String error = String.format(Locale.ENGLISH, IOMonitorConstants.TASK_GIVE_UP_TIP, taskName);
                rejectedExecutionException(error);
            }
        }) {
            @Override
            public void executeCacheTaskAfterExecute() {

            }
        };
        runnableIOLimitQueue.setThreadPoolExecutor(mMainThreadPoolExecutor);
        mMainThreadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        return this;
    }

    private static class SingleInstance {
        private static final LimitCoreThreadPool INSTANCE = new LimitCoreThreadPool();
    }

    public static LimitCoreThreadPool getInstance() {
        return SingleInstance.INSTANCE;
    }

    @Override
    public ThreadPoolExecutor getMainIOThreadPoolExecutor() {
        return mMainThreadPoolExecutor;
    }

    @Override
    public List<ThreadPoolExecutor> getOthersThreadPoolExecutor() {
        return new ArrayList<>();
    }

    @Override
    public void resetThreadCacheInfo() {
        mLastMaxTaskCountInfo = null;
        mDiscardTaskCount.set(0);
    }

    @Override
    public int getActiveTaskCount() {
        return mMainThreadPoolExecutor.getActiveCount();
    }

    @Override
    public int getRunningPoolSize() {
        return mMainThreadPoolExecutor.getPoolSize();
    }

    @Override
    public int getDiscardTaskCount() {
        return mDiscardTaskCount.get();
    }

    @Override
    public ThreadGroup getThreadGroup() {
        return mThreadGroup;
    }

    @Override
    public long getLastRunningTaskPeak() {
        if (mLastMaxTaskCountInfo == null) {
            return 0;
        }
        return mLastMaxTaskCountInfo.first;
    }

    @Override
    public long getLastRunningTaskPeakHappenedTime() {
        if (mLastMaxTaskCountInfo == null) {
            return 0;
        }
        return mLastMaxTaskCountInfo.second;
    }


    @Override
    public String getThreadPoolInfoForLog() {
        return String.format("main-io-thread-pool: %s", mMainThreadPoolExecutor);
    }

    @Override
    public void executeTask(Runnable runnable) {
        mMainThreadPoolExecutor.execute(runnable);
    }
}
