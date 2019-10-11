package com.tc.rxjavamonitor.monitor.queue;


import com.tc.rxjavamonitor.monitor.custominterface.IBaseWork;
import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * author：   tc
 * date：      2019/3/27 & 21:02
 * version    1.0
 * description IO带任务优先级的无界阻塞队列
 * modify by
 */
public class IOPriorityQueue extends PriorityBlockingQueue {
    private ThreadPoolExecutor mThreadPoolExecutor;
    private int mQueueSize;

    public IOPriorityQueue(int initialCapacity, int maxSize) {
        super(initialCapacity);
        mQueueSize = maxSize;
    }

    public IOPriorityQueue(int initialCapacity, Comparator comparator, int maxSize) {
        super(initialCapacity, comparator);
        mQueueSize = maxSize;
    }


    @Override
    public boolean offer(Object o) {
        if (isNotAllowOfferQueue(o)) {
            return false;
        }
//        MonitorLog.thread("" + o);
        return super.offer(o);
    }

    @Override
    public boolean offer(Object o, long timeout, TimeUnit unit) {
        if (isNotAllowOfferQueue(o)) {
            return false;
        }
        return super.offer(o, timeout, unit);
    }

    /**
     * 自定义条件，是否继续允许入队,true代表不允许入队
     */
    private boolean isNotAllowOfferQueue(Object o) {
        if (mThreadPoolExecutor == null) {
            return false;
        }
        IBaseWork iBaseWork = (IBaseWork) o;
        if (iBaseWork.isNeedCreateNewThread()) {
            //如果是这种状态，说明上次走了创建新线程执行该任务，但是执行失败被拒绝了，所以这次直接把任务加入阻塞队列
            return false;
        }
        int poolSize = mThreadPoolExecutor.getPoolSize();
        int maximumPoolSize = mThreadPoolExecutor.getMaximumPoolSize();
        if (poolSize < maximumPoolSize) {
            iBaseWork.setNeedCreateNewThread(true);
//            MonitorLog.logCatD(IOMonitorConstants.MONITOR_LOG_TAG, String.format(Locale.ENGLISH, "need create new " +
//                    "thread.  poolSize:%d ,maximumPoolSize:%d  iBaseWork:%s", poolSize, maximumPoolSize, iBaseWork));
            return true;
        }

        if (iBaseWork.getPriority() >= IOTaskPriorityType.DISCARD_TASK_VALUE) {
            return false;
        }

        //只丢弃允许丢弃的任务
        if (mQueueSize > 0 && mQueueSize < this.size()) {
            //当前队列大小大于允许的容量，丢弃任务优先级低的任务
//            MonitorLog.logCatE(IOMonitorConstants.MONITOR_LOG_TAG, String.format(Locale.ENGLISH, "remove old task:%s",
//                    iBaseWork));
            return true;
        }
        return false;
    }

    public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
        mThreadPoolExecutor = threadPoolExecutor;
    }
}
