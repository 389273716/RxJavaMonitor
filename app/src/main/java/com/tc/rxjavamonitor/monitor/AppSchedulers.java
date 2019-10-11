package com.tc.rxjavamonitor.monitor;


import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;

import rx.Scheduler;

/**
 * author：   tc
 * date：      2019/3/13 & 20:25
 * version    1.0
 * description ,如果只是想直接使用自己的线程池的话，用这个类直接获取已有的Rx调度器
 * modify by
 */
public class AppSchedulers {

    public static Scheduler io(String taskTag, @IOTaskPriorityType int priority) {
        return IOMonitorManager.getInstance().getIOScheduler(taskTag, priority);
    }

    public static Scheduler io() {
        return IOMonitorManager.getInstance().getIOScheduler();
    }

    public static Scheduler immediate() {
        //目前不替换，使用原始的
        return rx.schedulers.Schedulers.immediate();
    }

    public static Scheduler computation() {
        //目前不替换，使用原始的
        return rx.schedulers.Schedulers.computation();
    }

    public static Scheduler newThread() {
        //目前不替换，使用原始的
        return rx.schedulers.Schedulers.newThread();
    }


}
