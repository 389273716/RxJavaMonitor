package com.tc.rxjavamonitor.monitor.custominterface;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * author：   tc
 * date：      2019/3/27 & 20:27
 * version    1.0
 * description 自定义任务丢弃抽象类，方便自定义线程池时做拒绝任务策略
 * modify by
 */
public abstract class AbstractRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
        IBaseWork schedulerWorker = null;
        if (runnable instanceof IBaseWork) {
            schedulerWorker = (IBaseWork) runnable;
        }
        rejectedTask(runnable, threadPoolExecutor, schedulerWorker);
    }

    /**
     * 在任务真正被抛弃时，需要抛出异常，以便应用层感知到
     *
     * @param error 错误提示信息,最好包含堆栈信息
     */
    public void rejectedExecutionException(String error) {
        throw new RejectedExecutionException(error);
    }

    /**
     * 当前任务无法执行时，回调此方法，可以做一些丢弃任务或者再次使用备用线程池处理的动作等
     *
     * @param runnable           任务
     * @param threadPoolExecutor 当前任务所在线程池
     * @param schedulerWorker    自定义任务接口类对象，可以获取一些任务的自定义信息
     */
    public abstract void rejectedTask(Runnable runnable, ThreadPoolExecutor threadPoolExecutor, IBaseWork
            schedulerWorker);

}
