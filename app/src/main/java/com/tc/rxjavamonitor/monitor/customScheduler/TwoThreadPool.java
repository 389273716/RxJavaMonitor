package com.tc.rxjavamonitor.monitor.customScheduler;


import android.util.Pair;

import com.tc.rxjavamonitor.monitor.IOMonitorConstants;
import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.custominterface.AbstractRejectedExecutionHandler;
import com.tc.rxjavamonitor.monitor.custominterface.AbstractThreadFactory;
import com.tc.rxjavamonitor.monitor.custominterface.CustomThread;
import com.tc.rxjavamonitor.monitor.custominterface.IBaseWork;
import com.tc.rxjavamonitor.monitor.custominterface.IThreadPool;
import com.tc.rxjavamonitor.monitor.custominterface.MonitorThreadPoolExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author：   tc
 * date：      2019/3/13 & 16:43
 * version    1.0
 * description  自定义IO线程池替换RxJava自带的，用于监控项目中IO线程池使用情况
 * 用双线程池替换本身RxJava的无上限IO scheduler
 * 任务管理采用主+备用两个线程池执行，主线程池有上限，超出上限的任务丢给子线程池
 * 备用的子线程池：包含两种策略，1、无上限线程池不丢任务，但是阻塞队列存放数据过大，也有OOM的风险
 * 2、采用有上限的线程池，超出的任务进行丢弃，保证应用的稳定性，减少待执行任务数据量过大时的风险
 * 后期可考虑加入优先级策略，目前按照任务进入队列的顺序进行执行
 * modify by
 */
public class TwoThreadPool implements IThreadPool {
    private ThreadGroup mThreadGroup;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    /**
     * 被丢弃的任务数
     */
    private final AtomicInteger mDiscardTaskCount = new AtomicInteger(0);
    /**
     * 通过主动获取任务调度主线程池完成的任务数
     */
    private final AtomicInteger mCompletedTaskCount = new AtomicInteger(0);
    /**
     * 核心主任务线程池-IO
     */
    private static final String APP_WORK_THREAD_CORE = "app-running-io-thread-";
    /**
     * 主线程池
     */
    private ThreadPoolExecutor mMainThreadPoolExecutor;
    /**
     * 当前最大正在运行时的任务最大时的记录值，第一个参数是任务数，第二个参数是当时的时间点
     */
    private Pair<Long, Long> mLastMaxTaskCountInfo;
    /**
     * 备用线程池
     */
    private ThreadPoolExecutor mStandbyThreadPoolExecutor;
    private IOMonitorManager mIOMonitorManager;
    /**
     * 备用任务线程池
     */
    private static final String APP_WORK_THREAD_STANDBY = "app-standby-thread-";
    private List<ThreadPoolExecutor> mThreadPoolExecutorList;
    /**
     * 缓存任务队列，超出承受范围的任务存放处
     */
    private BlockingQueue<Runnable> mCacheTaskRunnableQueue;

    private static class SingleInstance {
        private static final TwoThreadPool INSTANCE = new TwoThreadPool();
    }

    public static TwoThreadPool getInstance() {
        return SingleInstance.INSTANCE;
    }

    /**
     * 根据传入参数生成线程池
     *
     * @param defaultKeepAliveTime                    默认回收线程时间
     * @param allowCoreThreadTimeOut                  是否允许回收核心线程
     * @param defaultMainIOThreadPoolCoreSize         默认主线程池的核心线程数
     * @param defaultMainIOThreadPoolMaxPoolSize      默认主线程池的最大线程数
     * @param isAllowNoLimitQueueOnDefaultStandbyPool 默认备用线程池是否限制线程的阻塞队列大小
     * @param defaultStandbyIOThreadPoolCoreSize      默认备用线程池的核心线程数
     * @param defaultStandbyIOThreadPoolMaxPoolSize   默认备用线程池的最大线程数
     * @param defaultStandbyIOThreadPoolQueueSize     默认备用线程池的阻塞队列大小
     */
    public TwoThreadPool build(int defaultKeepAliveTime, boolean allowCoreThreadTimeOut,
                               int defaultMainIOThreadPoolCoreSize, int defaultMainIOThreadPoolMaxPoolSize, boolean
                                      isAllowNoLimitQueueOnDefaultStandbyPool, int defaultStandbyIOThreadPoolCoreSize,
                               int defaultStandbyIOThreadPoolMaxPoolSize, int defaultStandbyIOThreadPoolQueueSize) {
        mIOMonitorManager = IOMonitorManager.getInstance();
        mThreadGroup = new ThreadGroup("app_io_thread_group");
        mThreadPoolExecutorList = new ArrayList<>();
        if (isAllowNoLimitQueueOnDefaultStandbyPool) {
            //目前备用线程池采用无限制队列
            mCacheTaskRunnableQueue = new PriorityBlockingQueue<Runnable>();
        } else {
            // 备用任务队列,有界
            mCacheTaskRunnableQueue = new ArrayBlockingQueue<Runnable>(defaultStandbyIOThreadPoolQueueSize);
        }


        //备用线程池，紧急任务队列处理，当任务数超过主处理线程池承受压力，启用这个备用线程
        mStandbyThreadPoolExecutor = new MonitorThreadPoolExecutor(defaultStandbyIOThreadPoolCoreSize,
                defaultStandbyIOThreadPoolMaxPoolSize,
                defaultKeepAliveTime, TimeUnit.SECONDS,
                mCacheTaskRunnableQueue, new
                AbstractThreadFactory() {
                    @Override
                    public Thread newOneThread(Runnable r, Pair<Long, Long> threadPeakInfo) {
                        int index = threadNumber.getAndIncrement();
                        mLastMaxTaskCountInfo = threadPeakInfo;
                        return new CustomThread(mThreadGroup, r, String.format("%s%s", APP_WORK_THREAD_STANDBY, index));
                    }
                }, new AbstractRejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            }

            @Override
            public void rejectedTask(Runnable runnable, ThreadPoolExecutor threadPoolExecutor, IBaseWork
                    schedulerWorker) {
                String taskName = "";
                if (schedulerWorker != null) {
                    taskName = schedulerWorker.getTaskName();
                }
                mDiscardTaskCount.getAndIncrement();
//                mCacheTaskRunnableQueue.add(runnable);
                String format = String.format(Locale.ENGLISH, IOMonitorConstants.TASK_GIVE_UP_TIP, taskName);
                rejectedExecutionException(format);
            }
        }) {
            @Override
            public void executeCacheTaskAfterExecute() {
                executeCacheTask();
            }
        };
        mStandbyThreadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);

        //主线程池，主要任务队列处理，当任务数不超过机器性能限制时，使用此线程池
        mMainThreadPoolExecutor = new MonitorThreadPoolExecutor(defaultMainIOThreadPoolCoreSize,
                defaultMainIOThreadPoolMaxPoolSize,
                defaultKeepAliveTime, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new
                AbstractThreadFactory() {
                    @Override
                    public Thread newOneThread(Runnable r, Pair<Long, Long> threadPeakInfo) {
                        int index = threadNumber.getAndIncrement();
                        mLastMaxTaskCountInfo = threadPeakInfo;
                        return new CustomThread(mThreadGroup, r, String.format("%s%s", APP_WORK_THREAD_CORE, index));
                    }
                }, new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                String taskName = "";
                if (r instanceof IBaseWork) {
                    IBaseWork schedulerWorker = (IBaseWork) r;
                    taskName = schedulerWorker.getTaskName();
                }
                if (mIOMonitorManager.isLogMoreInfo()) {
//                    MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(
//                            "task peak:this task move to standby pool :", taskName));
                }
                //可以去启用备用线程池,继续承担主核心线程池的任务
                mStandbyThreadPoolExecutor.execute(r);
            }
        }) {
            @Override
            public void executeCacheTaskAfterExecute() {
                executeCacheTask();
            }
        };
        mMainThreadPoolExecutor.allowCoreThreadTimeOut(allowCoreThreadTimeOut);

        mThreadPoolExecutorList.add(mMainThreadPoolExecutor);
        mThreadPoolExecutorList.add(mStandbyThreadPoolExecutor);

        return this;
    }

    private void executeCacheTask() {
        if (getMainIOThreadPoolExecutor().getMaximumPoolSize() - getMainIOThreadPoolExecutor().getActiveCount() < 2) {
//            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, "getMainIOThreadPoolExecutor().getMaximumPoolSize
// () - " +
//                    "getMainIOThreadPoolExecutor().getPoolSize() < 2");
            return;
        }
        if (mCacheTaskRunnableQueue.size() <= 1) {
            //剩余任务只有一个以下时，没必要再转交给空闲的主线程池
            return;
        }
        //当主线程池的剩余线程池数可以承担任务时，开始帮助备用线程池处理阻塞队列里的任务
        Runnable r = null;
        try {
            r = mCacheTaskRunnableQueue.poll();
            if (r == null) {
                return;
            }
        } catch (Exception e) {
//            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("task " +
//                    "peak:executeCacheTask:", e.getMessage()));
            return;
        }
        boolean contains = getMainIOThreadPoolExecutor().getQueue().contains(r);
        if (contains) {
//            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("task peak:this task was" +
//                    " doing."));
            return;
        }
        getMainIOThreadPoolExecutor().execute(r);
        int count = mCompletedTaskCount.getAndIncrement();
        String taskName = "";
        if (r instanceof IBaseWork) {
            IBaseWork schedulerWorker = (IBaseWork) r;
            taskName = schedulerWorker.getTaskName();
        }

//        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(" task peak,this task " +
//                "start execute from cache task queue:", taskName, " count:", count));
    }


    @Override
    public ThreadPoolExecutor getMainIOThreadPoolExecutor() {
        return mMainThreadPoolExecutor;
    }

    @Override
    public List<ThreadPoolExecutor> getOthersThreadPoolExecutor() {
        return mThreadPoolExecutorList;
    }

    @Override
    public void resetThreadCacheInfo() {
        mLastMaxTaskCountInfo = null;
        mDiscardTaskCount.set(0);
    }

    @Override
    public int getActiveTaskCount() {
        return mMainThreadPoolExecutor.getActiveCount() + mStandbyThreadPoolExecutor.getActiveCount();
    }

    @Override
    public int getRunningPoolSize() {
        return mMainThreadPoolExecutor.getPoolSize() + mStandbyThreadPoolExecutor.getPoolSize();
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
        return "";
//        return String.format(Locale.ENGLISH, "main-io-thread-pool: %s%s standby thread pool:%s ",
//                mMainThreadPoolExecutor, MonitorConstants.PLACEHOLDER_NEW_LINE, mStandbyThreadPoolExecutor);
    }

    @Override
    public void executeTask(Runnable runnable) {
        mMainThreadPoolExecutor.execute(runnable);
    }

}
