package com.tc.rxjavamonitor.monitor.queue;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * author：   tc
 * date：      2019/3/25 & 21:03
 * version    1.0
 * description  优先创建线程的队列，
 * 参见IOLimitQueue的offer方法
 * modify by
 */
public class IOLimitArrayQueue extends ArrayBlockingQueue {

    private ThreadPoolExecutor mThreadPoolExecutor;

    public IOLimitArrayQueue(int capacity, ThreadPoolExecutor threadPoolExecutor) {
        super(capacity);
        mThreadPoolExecutor = threadPoolExecutor;
    }

    public IOLimitArrayQueue(int capacity, boolean fair, ThreadPoolExecutor threadPoolExecutor) {
        super(capacity, fair);
        mThreadPoolExecutor = threadPoolExecutor;
    }

    public IOLimitArrayQueue(int capacity, boolean fair, Collection c, ThreadPoolExecutor threadPoolExecutor) {
        super(capacity, fair, c);
        mThreadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public boolean offer(Object o) {
        if (mThreadPoolExecutor != null && mThreadPoolExecutor.getPoolSize() < mThreadPoolExecutor.getMaximumPoolSize
                ()) {
//            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, "need create new thread.");
            return false;
        }
        return super.offer(o);
    }

    @Override
    public boolean offer(Object o, long timeout, TimeUnit unit) throws InterruptedException {
        if (mThreadPoolExecutor != null && mThreadPoolExecutor.getPoolSize() < mThreadPoolExecutor.getMaximumPoolSize
                ()) {
//            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, "need create new thread.");
            return false;
        }
        return super.offer(o, timeout, unit);
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        mThreadPoolExecutor = threadPoolExecutor;
    }
}
