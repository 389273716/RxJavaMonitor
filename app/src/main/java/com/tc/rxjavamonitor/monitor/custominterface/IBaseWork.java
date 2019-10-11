package com.tc.rxjavamonitor.monitor.custominterface;

/**
 * author：   tc
 * date：      2019/3/21 & 15:08
 * version    1.0
 * description  所有自定义scheduler的work对象都实现这个，方便进行监控
 * modify by
 */
public interface IBaseWork {
    /**
     * 获取任务名
     */
    String getTaskName();

    /**
     * 获取堆栈信息
     */
    String getStackInfo();

    /**
     * 获取任务的优先级
     */
    int getPriority();

    /**
     * 设置是否需要创建新线程，一般在阻塞队列里判断赋值，拒绝策略执行抛弃任务时进行使用判断
     */
    void setNeedCreateNewThread(boolean needCreateNewThread);

    /**
     * 是否需要创建新线程，一般在阻塞队列里判断赋值，拒绝策略执行抛弃任务时进行使用判断
     */
    boolean isNeedCreateNewThread();

}
