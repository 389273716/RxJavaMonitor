package com.tc.rxjavamonitor;

import android.app.Application;

import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.customScheduler.LimitCoreThreadPool;

/**
 * Created by huangxiaojie on 2019/2/20.
 */

public class MyApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        //必须在应用第一次使用observable之前设置，这里会替换rxjava的默认IO scheduler。
        // 如果只调用setReplaceIOScheduler方法，则替换时用基础库里自带的自定义IO scheduler
        //LimitCoreThreadPool不是基础库默认的IO scheduler实现,一般都是替换线程池实现，不直接修改自定义的IO scheduler
        IOMonitorManager.getInstance().setReplaceIOScheduler(true)
                .setIOThreadPool(LimitCoreThreadPool.getInstance()
                        .build(2, BuildConfig.DEBUG ? 35 : 35, 15, 1000, false));
        //如果有很多module组件，那么需要这样处理下，具体看过滤堆栈的方法
//        List<String> targetList = new ArrayList<>(2);
//        targetList.add("com.tc.rxjavamonitor");

        //配置基本的监视器参数，下面参数可以在IOMonitorManager的startMonitor后修改
        IOMonitorManager.getInstance().setCostTimeLimit(0)
                //超出这个活跃线程数就输出到日志
                .setThreadActiveCountLimit(30)
                //打印当前被监控的线程池信息
                .setLogThreadPoolInfo(BuildConfig.DEBUG)
                //打印其它非RxJava的IO线程信息
                .setLogOtherThreadInfo(true)
                .setPackageName(getPackageName())
//                        .setTargetNameList(list)
                //监控器轮询时间，每隔这么久打印一些线程信息
                .setMonitorIntervalTime(10)
                //是否输出更多日志信息，看方法注释
                .setLogMoreInfo(BuildConfig.DEBUG)
                //监控器是否启用
                .setMonitorEnable(BuildConfig.DEBUG)
                //一般在调试时才开启，监控子线程重复切换线程
                .setLogRepeatChangeThread(false)
                //打印所有任务执行情况，适合打桩分析当前时间段有哪些任务触发
                .setLogAllTaskRunningInfo(true)
                //是否过滤堆栈
                .setFilterStack(!BuildConfig.DEBUG);
    }
}
