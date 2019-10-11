package com.tc.rxjavamonitor.monitor;

import rx.Scheduler;
import rx.plugins.RxJavaSchedulersHook;

/**
 * author：   tc
 * date：      2019/3/13 & 14:51
 * version    1.0
 * description  提供给RxJava的RxJavaPlugins类，可以无感知替换业务层的所有线程，全局替换IO线程池
 * modify by
 */
class RxJavaSchedulersHookImpl extends RxJavaSchedulersHook {


    @Override
    public Scheduler getComputationScheduler() {
        return null;
    }

    @Override
    public Scheduler getIOScheduler() {
        if (!IOMonitorManager.getInstance().isReplaceIOScheduler()) {
            return null;
        }
        return IOMonitorManager.getInstance().getIOScheduler();
    }

    @Override
    public Scheduler getNewThreadScheduler() {
        return null;
    }
}
