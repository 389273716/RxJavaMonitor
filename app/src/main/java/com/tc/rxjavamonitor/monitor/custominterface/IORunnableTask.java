package com.tc.rxjavamonitor.monitor.custominterface;

/**
 * author：   tc
 * date：      2019/4/22 & 14:54
 * version    1.0
 * description 供非RxJava逻辑下的线程调度使用
 * modify by
 */
public class IORunnableTask implements Runnable, IBaseWork,
        Comparable<IBaseWork> {

    private int priority;
    private String taskName;
    private String stackInfo;

    private Runnable runnable;
    /**
     * 提交任务次数，一般是失败次数
     */
    private boolean needCreateNewThread;

    public IORunnableTask(@IOTaskPriorityType int priority, String taskName, String stackInfo, Runnable runnable) {
        if (runnable == null) {
            throw new NullPointerException("runnable is null!");
        }
        this.priority = priority;
        this.taskName = taskName;
        this.stackInfo = stackInfo;
        this.runnable = runnable;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public String getStackInfo() {
        return stackInfo;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setNeedCreateNewThread(boolean needCreateNewThread) {
        this.needCreateNewThread = needCreateNewThread;
    }

    @Override
    public boolean isNeedCreateNewThread() {
        return needCreateNewThread;
    }

    @Override
    public void run() {
        runnable.run();
    }

    /**
     * 当前对象和其他对象做比较，当前优先级大就返回-1，优先级小就返回1
     * 值越小优先级越高
     *
     * @param other 比较对象
     * @return
     */
    @Override
    public int compareTo(IBaseWork other) {
        if (priority >= other.getPriority()) {
            return -1;
        }
        return 1;
    }

    @Override
    public String toString() {
        return "{\"IORunnableTask\":{"
                + "\"priority\":"
                + priority
                + ",\"taskName\":\""
                + taskName + '\"'
                + ",\"stackInfo\":\""
                + stackInfo + '\"'
                + ",\"runnable\":"
                + runnable
                + "}}";

    }
}
