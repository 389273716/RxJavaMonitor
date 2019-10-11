package com.tc.rxjavamonitor.monitor.customScheduler;


import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.custominterface.AbstractScheduler;
import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;

/**
 * author：   tc
 * date：      2019/3/13 & 11:38
 * version    1.0
 * description 自定义RxJava的IO Scheduler
 * modify by
 */
public class IOScheduler extends AbstractScheduler {

    private String mNewTaskTag = null;
    /**
     * 优先级越高,值越大
     */
    private int priority = IOTaskPriorityType.NORMAL_TASK;

    public IOScheduler() {
    }

    public IOScheduler(String newTaskTag, @IOTaskPriorityType int priority) {
        this.mNewTaskTag = newTaskTag;
        this.priority = priority;
    }

    @Override
    public AbstractScheduler create(String taskTag, @IOTaskPriorityType int priority) {
        return new IOScheduler(taskTag, priority);
    }

    @Override
    public Worker createNewWorker(String taskName, String stackInfo) {
        return new ExecutorSchedulerWorker(IOMonitorManager.getInstance().getIThreadPool().getMainIOThreadPoolExecutor()
                , taskName, stackInfo, priority);
    }

    @Override
    public String getNewTaskTag() {
        return mNewTaskTag;
    }

    @Override
    public int getPriority() {
        return priority;
    }


}
