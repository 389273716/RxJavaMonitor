package com.tc.rxjavamonitor.monitor.custominterface;


import android.text.TextUtils;

import com.tc.rxjavamonitor.monitor.IOMonitorConstants;
import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.log.MonitorLog;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * author：   tc
 * date：      2019/3/9 & 9:55
 * version    1.0
 * description  自定义新的scheduler时，要使用这个类
 * modify by
 */
public abstract class MonitorThreadPoolExecutor extends ThreadPoolExecutor {

    private ConcurrentHashMap<String, Long> mConcurrentHashMap = new ConcurrentHashMap<>(100);

    public MonitorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public MonitorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public MonitorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public MonitorThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                     RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }


    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        if (!IOMonitorManager.getInstance().isMonitorEnable()) {
            return;
        }
        String taskName = null;
        if (r instanceof IBaseWork) {
            IBaseWork schedulerWorker = (IBaseWork) r;
            taskName = schedulerWorker.getTaskName();
        }
        if (TextUtils.isEmpty(taskName)) {
            //任务名为空时，用当前runnable信息作为tag
            taskName = r.toString();
//            MonitorLog.thread(ThreadManagerConstants.MONITOR_LOG_TAG, "task name is null ,new one:" + taskName);
        }
        mConcurrentHashMap.put(taskName, System.currentTimeMillis());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        IOMonitorManager ioMonitorManager = IOMonitorManager.getInstance();
        if (!ioMonitorManager.isMonitorEnable()) {
            return;
        }
        String taskName = "";
        String stackInfo = "";
        if (r instanceof IBaseWork) {
            IBaseWork schedulerWorker = (IBaseWork) r;
            taskName = schedulerWorker.getTaskName();
            stackInfo = schedulerWorker.getStackInfo();
        }
        Long startTime;
        if (!TextUtils.isEmpty(taskName)) {
            //任务名不为空，用原始任务名获取时间
            startTime = mConcurrentHashMap.get(taskName);
        } else {
            //任务名为空时，用当前runnable信息作为tag
            taskName = r.toString();
//            MonitorLog.thread(IOThreadManagerConstants.MONITOR_LOG_TAG, "task name is null ,new one:" + taskName);
            startTime = mConcurrentHashMap.get(taskName);
        }
        mConcurrentHashMap.remove(taskName);
        //打印耗时长的任务信息
        if (startTime != null && startTime > 0L) {
            long endTime = System.currentTimeMillis();
            long diff = endTime - startTime;

            if (ioMonitorManager.isLogMoreInfo() && diff >= ioMonitorManager.getCostTimeLimit()) {
                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(IOMonitorConstants
                        .TIME_CONSUMING, " Execute cost Time:", diff, taskName, "startTime:", startTime, " " +
                        "endTime:", endTime, "task run on:", Thread.currentThread().getName(), stackInfo));

            } else if (ioMonitorManager.isLogAllTaskRunningInfo()) {
                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(IOMonitorConstants
                        .NORMAL_TASK, " Execute cost Time:", diff, taskName, "startTime:", startTime, " " +
                        "endTime:", endTime, "task run on:", Thread.currentThread().getName(), stackInfo));


            }
        }
        executeCacheTaskAfterExecute();
    }

    @Override
    public String toString() {
        return "[Running pool size = " + getPoolSize()
                + ",max pool size = " + getMaximumPoolSize()
                + ",active task count = " + getActiveCount()
                + ",queued tasks = " + getQueue().size()
                + ",completed tasks = " + getCompletedTaskCount()
                + ",all task count:" + getTaskCount()
                + ",core thread=" + getCorePoolSize()
                + ",allow core thread time out=" + allowsCoreThreadTimeOut() + "]";
    }

    /**
     * 如果存在闲置的备用线程池时，可以在每次任务完成时判断下是否需要处理临时缓存的备用线程池的任务
     */
    public abstract void executeCacheTaskAfterExecute();
}
