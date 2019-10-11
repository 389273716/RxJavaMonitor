package com.tc.rxjavamonitor;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.customScheduler.LimitCoreThreadPool;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


/*
 * description  自定义IO线程监控管理类，配置监视器的基本参数，开启各种debug方法等
 * 监控要求：
 * 1、自定义的scheduler需要继承AbstractScheduler
 * 2、参考或者直接使用ExecutorSchedulerWorker创建任务
 * 3、如果是要替换原始RxJava的IO线程池，需要额外实现IThreadPool，创建自己的线程池类
 * 4、IThreadPool的实现类里，线程池的构造使用MonitorThreadPoolExecutor类，便于监控
 * 5、编写新的scheduler参考自定义的IOScheduler类
 * 6、如果需要自定义SchedulerWork，需要实现Runnable, IBaseWork 接口，继承Scheduler.Worker
 * 7、默认提供IOThreadPool和LimitCoreThreadPool两种基础线程池，还有自定义的IOScheduler（用来替换原本RxJava的IOScheduler）
 * 8、IOTaskPriorityType优先级类型，RxJava observable在subscribeOn时可以选择传入
 * 9、AbstractRejectedExecutionHandler可以做一些拒绝任务的策略动作
 *
 * @see ExecutorSchedulerWorker
 * 使用方式：
 * 1、所有public方法提供配置参数
 * 2、在基础参数配置完后调用此方法startMonitor
 * modify by
 */


        //必须在应用第一次使用observable之前设置，这里会替换rxjava的默认IO scheduler。
        // 如果只调用setReplaceIOScheduler方法，则替换时用基础库里自带的自定义IO scheduler
        //LimitCoreThreadPool不是基础库默认的IO scheduler实现,一般都是替换线程池实现，不直接修改自定义的IO scheduler
        IOMonitorManager.getInstance().setReplaceIOScheduler(true)
                .setIOThreadPool(LimitCoreThreadPool.getInstance()
                        .build(2, BuildConfig.DEBUG ? 35 : 35, 15, 1000, false));

        List<String> targetList = new ArrayList<>(2);
        targetList.add("com.xtc");
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
