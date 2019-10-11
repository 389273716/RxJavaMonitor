package com.tc.rxjavamonitor.monitor.custominterface;

import android.util.Pair;

import com.tc.rxjavamonitor.monitor.ThreadPoolUtil;

import rx.Scheduler;

/**
 * author：   tc
 * date：      2019/3/20 & 8:41
 * version    1.0
 * description  自定义的scheduler都需要继承这个，以便进行监控
 * modify by
 */
public abstract class AbstractScheduler extends Scheduler {

    /**
     * 主要是使用RxJava的提供hook IO scheduler的方法后，它内部持有第一次hook传入的scheduler，
     * 所以后面想要每个worker有不同的taskName（外部调用者传入的taskName），需要用这个方法去new一个新对象给subscribeOn方法
     *
     * @param taskTag  应用层自定义的任务tag，用来生成每次IO子线程任务的任务名字
     * @param priority 优先级
     * @return
     */
    public abstract AbstractScheduler create(String taskTag, @IOTaskPriorityType int priority);

    @Override
    public Worker createWorker() {
        Pair<String, String> taskNameAndStackInfo = ThreadPoolUtil.getTaskNameAndStackInfo(getNewTaskTag(),
                getPriority());
        return createNewWorker(taskNameAndStackInfo.first, taskNameAndStackInfo.second);
    }

    /**
     * 使用自定义的work或者ExecutorSchedulerWorker去创建新任务
     *
     * @param taskName  最终执行时的任务名
     * @param stackInfo 任务创建所在的栈堆信息
     * @return
     */

    public abstract Worker createNewWorker(String taskName, String stackInfo);

    /**
     * 表示此次执行的动作，用于生成taskName
     * 自定义scheduler时，可以选择实现这个方法，生成自己规则下的taskName
     *
     * @return
     */
    public abstract String getNewTaskTag();

    /**
     * 任务执行时的优先级
     *
     * @return 优先级
     */
    public abstract int getPriority();

}
