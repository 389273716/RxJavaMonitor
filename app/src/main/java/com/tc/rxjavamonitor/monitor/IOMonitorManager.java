package com.tc.rxjavamonitor.monitor;


import android.text.TextUtils;
import android.util.Pair;

import com.tc.rxjavamonitor.monitor.customScheduler.IOScheduler;
import com.tc.rxjavamonitor.monitor.customScheduler.LimitCoreThreadPool;
import com.tc.rxjavamonitor.monitor.custominterface.AbstractScheduler;
import com.tc.rxjavamonitor.monitor.custominterface.IORunnableTask;
import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;
import com.tc.rxjavamonitor.monitor.custominterface.IThreadPool;
import com.tc.rxjavamonitor.monitor.log.MonitorConstants;
import com.tc.rxjavamonitor.monitor.log.MonitorLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.plugins.RxJavaPlugins;

/**
 * author：   tc
 * date：      2019/3/14 & 14:31
 * version    1.0
 * description  自定义IO线程监控管理类，配置监视器的基本参数，开启各种debug方法等
 * 参看本包下的readme文件
 * modify by
 */
public class IOMonitorManager {


    private Subscription mMonitorIntervalSubscribe;
    /**
     * 打桩测试埋点的监控集合
     */
    private final ConcurrentHashMap<String, Boolean> mPilingMap;

    private static class SingleInstance {
        private static final IOMonitorManager INSTANCE = new IOMonitorManager();
    }

    public static IOMonitorManager getInstance() {
        return SingleInstance.INSTANCE;
    }

    private IOMonitorManager() {
        mPilingMap = new ConcurrentHashMap<>(4);
        mIOScheduler = new IOScheduler();
    }

    /**
     * 当前应用包名
     */
    private String mPackageName = "default";
    /**
     * 堆栈信息获取后根据此集合生成对应的taskName
     */
    private List<String> mTargetNameList = new ArrayList<>(4);
    /**
     * 是否开启打印IO线程以外的线程信息
     */
    private boolean mIsLogOtherThreadInfo;
    /**
     * 是否允许打印IO线程池的当前信息
     */
    private boolean mIsLogThreadPoolInfo;
    /**
     * 是否IO线程池监视器可用
     */
    private boolean mIsMonitorEnable;
    /**
     * 线程执行耗时警告阈值
     */
    private long mCostTimeLimit = 500;
    /**
     * 是否已经注册了RxJava hook替换IO线程池
     */
    private boolean mIsRegisterRxJavaHook;
    /**
     * 打印很多信息下的日志，一般用于debug，
     */
    private boolean mIsLogMoreInfo;
    /**
     * 打印所有任务执行情况，适合打桩分析当前时间段有哪些任务触发
     */
    private boolean mIsLogAllTaskRunningInfo;
    /**
     * 是否过滤堆栈信息，只保存关键堆栈（应用层逻辑代码堆栈信息）
     */
    private boolean mIsFilterStack;
    /**
     * 是否打印重复切换线程
     */
    private boolean mIsLogRepeatChangeThread;
    /**
     * 监控器轮询每次间隔，秒为单位
     */
    private long mMonitorIntervalTime = 15;
    /**
     * 当前线程池不再变化时，不输出log
     */
    private Boolean mIsNeedStopLogThreadInfo = false;

    /**
     * 当前活跃的线程数预警阈值，达到后，就以警告输出log
     */
    private long mThreadActiveCountLimit = 40;
    /**
     * 是否替换IO线程池
     */
    private boolean mIsReplaceIOScheduler;
    /**
     * 自定义替代RxJava的默认Scheduler
     */
    private AbstractScheduler mIOScheduler;
    /**
     * 当前IO scheduler使用的线程池
     */
    private IThreadPool mIThreadPool;


    //-------参数值获取以及默认私有方法----------

    private void startIntervalMonitorLog() {
        //轮询打印当前线程池情况
        stopMonitorLog();
        if (!isMonitorEnable()) {
            return;
        }
        if (!isLogMoreInfo()) {
            return;
        }
        mMonitorIntervalSubscribe = Observable.interval(getMonitorIntervalTime(), TimeUnit.SECONDS)
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("monitor " +
                                "interval stop,onCompleted"));
                    }

                    @Override
                    public void onError(Throwable e) {
                        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(" onError: " +
                                "%s", e.getMessage()));
                    }

                    @Override
                    public void onNext(Long aLong) {
                        logThreadInfo(false);
                    }
                });
    }


    public boolean isLogThreadPoolInfo() {
        return mIsLogThreadPoolInfo;
    }

    public boolean isMonitorEnable() {
        return mIsMonitorEnable;
    }


    public boolean isLogOtherThreadInfo() {
        return mIsLogOtherThreadInfo;
    }

    public boolean isLogAllTaskRunningInfo() {
        return mIsLogAllTaskRunningInfo;
    }

    public long getCostTimeLimit() {
        return mCostTimeLimit;
    }

    public List<String> getTargetNameList() {
        return mTargetNameList == null ? new ArrayList<String>() : mTargetNameList;
    }

    public String getPackageName() {
        return mPackageName == null ? "" : mPackageName;
    }


    public boolean isRegisterRxJavaHook() {
        return mIsRegisterRxJavaHook;
    }

    /**
     * 设置hook替换IO scheduler
     *
     * @param registerRxJavaHook 是否开启
     */
    IOMonitorManager setRegisterRxJavaHook(boolean registerRxJavaHook) {
        mIsRegisterRxJavaHook = registerRxJavaHook;
        return this;
    }

    public boolean isLogMoreInfo() {
        return mIsLogMoreInfo;
    }


    public long getMonitorIntervalTime() {
        return mMonitorIntervalTime;
    }


    public long getThreadActiveCountLimit() {
        return mThreadActiveCountLimit;
    }

    public boolean isFilterStack() {
        return mIsFilterStack;
    }

    /**
     * 是否打印重复切换线程
     *
     * @return 是否打印重复切换线程
     */
    public boolean isLogRepeatChangeThread() {
        return mIsLogRepeatChangeThread;
    }


    /**
     * 获取默认自定义的IO scheduler
     *
     * @return IO scheduler
     */
    public Scheduler getIOScheduler() {
        return mIOScheduler == null ? new IOScheduler() : mIOScheduler.create("", IOTaskPriorityType.NORMAL_TASK);
    }

    /**
     * 获取默认自定义的IO scheduler
     *
     * @return IO scheduler
     */
    public AbstractScheduler getIOScheduler(String taskTag, @IOTaskPriorityType int priority) {
        return mIOScheduler == null ? new IOScheduler(taskTag, priority) : mIOScheduler.create(taskTag, priority);
    }


    public IThreadPool getIThreadPool() {
        if (mIThreadPool == null) {
            throw new NullPointerException("开启监控前，请先初始化自定义IO scheduler的线程池，使用setReplaceIOScheduler和setIOThreadPool方法");
        }
        //如果使用者有自定义线程池，则用自定义,
        return mIThreadPool;
    }


    public static String getStackInfo(StackTraceElement[] stes, boolean isFilterBaseClass) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MonitorConstants.PLACEHOLDER_NEW_LINE);
        for (StackTraceElement ste : stes) {
            String className = ste.getClassName();
            if (isFilterStack(ste, isFilterBaseClass)) {
                continue;
            }

            stringBuilder.append(className);
            stringBuilder.append(".");
            stringBuilder.append(ste.getMethodName());
            stringBuilder.append("(");
            stringBuilder.append(ste.getFileName());
            stringBuilder.append(":");
            stringBuilder.append(ste.getLineNumber());
            stringBuilder.append(")");
            stringBuilder.append(MonitorConstants.PLACEHOLDER_NEW_LINE);
        }
        if (TextUtils.isEmpty(stringBuilder.toString())) {
            return "empty";
        }
        return stringBuilder.toString();
    }

    /**
     * 需要过滤的堆栈
     *
     * @param ste 堆栈
     * @return 是否过滤掉
     */
    public static boolean isFilterStack(StackTraceElement ste, boolean isFilterStack) {
        String className = ste.getClassName();
        if (!isFilterStack) {
            return false;
        }

        if (className.startsWith("java.util.concurrent") || className.startsWith("java.lang.Thread")
                || className.startsWith("dalvik.system.VMStack") || className.startsWith("rx.internal")
                || className.startsWith("com.android.internal") || className.startsWith("java.lang.reflect.")
                || className.startsWith("android.app.ActivityThread") || className.startsWith("android.os")
                || ("rx.Observable".equals(className) && "subscribe".equals(ste.getMethodName()))
                || ("rx.Observable".equals(className) && "unsafeSubscribe".equals(ste.getMethodName())
                || className.startsWith("com.xtc.snmonitor.collector.monitor.thread"))) {
            return true;
        }
        return false;
    }

    /**
     * 需要过滤的堆栈
     *
     * @param ste 堆栈
     * @return 是否过滤掉
     */
    public static boolean isFilterStack(StackTraceElement ste) {
        return isFilterStack(ste, IOMonitorManager.getInstance().isFilterStack());
    }

    public static String getStackInfo(StackTraceElement[] stes) {
        return getStackInfo(stes, false);
    }

    public static void printStack() {
        StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        String stackInfo = getStackInfo(stes);
        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("(stack)", stackInfo));
    }

    public Boolean getNeedStopLogThreadInfo() {
        return mIsNeedStopLogThreadInfo;
    }

    public void setNeedStopLogThreadInfo(Boolean needStopLogThreadInfo) {
        mIsNeedStopLogThreadInfo = needStopLogThreadInfo;
    }

    public boolean isReplaceIOScheduler() {
        return mIsReplaceIOScheduler;
    }
//----------------外部可设置的参数以及调用方法--------------------

    /**
     * 在基础参数配置完后调用此方法
     */
    public void startMonitor() {
        replaceIOScheduler();
        startIntervalMonitorLog();
    }

    private void replaceIOScheduler() {
        if (isReplaceIOScheduler() && !isRegisterRxJavaHook()) {
            //进行全局替换RxJava的线程池，目前只替换IO
            setRegisterRxJavaHook(true);
            RxJavaPlugins.getInstance().registerSchedulersHook(new RxJavaSchedulersHookImpl());
            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("register rxjava hook for" +
                    " io scheduler."));
        }
    }

    /**
     * 停止轮询监控器
     */
    public void stopMonitor() {
        stopMonitorLog();
    }

    /**
     * 恢复轮询监控器
     */
    public void resumeMonitor() {
        if (!mIsMonitorEnable) {
            return;
        }
//        startIntervalMonitorLog();
    }

    public void logThreadInfo(boolean isWarm) {
        if (!isMonitorEnable()) {
            return;
        }
        IThreadPool iTreadPool = getIThreadPool();
        if (iTreadPool == null) {
            return;
        }
        int corePoolSize = iTreadPool.getMainIOThreadPoolExecutor().getCorePoolSize();
        int activeCount = iTreadPool.getActiveTaskCount();
        int threadPoolCount = iTreadPool.getRunningPoolSize();


        if (IOMonitorManager.getInstance().isLogThreadPoolInfo()) {

            boolean isLogThreadInfo = false;
            boolean isNeedLogTaskPeak = false;
            if (isWarm || activeCount > getThreadActiveCountLimit()) {
                isNeedLogTaskPeak = true;
                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("触发峰值log打印"));
            } else {
                if (!mIsNeedStopLogThreadInfo) {
                    isLogThreadInfo = true;
                }
                if (activeCount <= 0 && threadPoolCount <= corePoolSize) {
                    if (!mIsNeedStopLogThreadInfo) {
                        isNeedLogTaskPeak = true;
                        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("触发峰值log打印"));
                    }
                    mIsNeedStopLogThreadInfo = true;

                } else {
                    mIsNeedStopLogThreadInfo = false;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(System.currentTimeMillis());
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            if (isNeedLogTaskPeak) {
                stringBuilder.append(IOMonitorConstants.TASK_PEAK);
            } else {
                stringBuilder.append(IOMonitorConstants.MONITOR_THREAD_POOL_INFO);
            }
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(iTreadPool.getThreadPoolInfoForLog());
            stringBuilder.append(MonitorConstants.PLACEHOLDER_NEW_LINE);
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(iTreadPool.getThreadGroup().getName());
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(iTreadPool.getThreadGroup() == null ? 0 : iTreadPool.getThreadGroup().activeCount());
            stringBuilder.append(MonitorConstants.PLACEHOLDER_NEW_LINE);
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append("last running thread peak: ");
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(iTreadPool.getLastRunningTaskPeak());
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(",task peak happened time: ");
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(String.valueOf(iTreadPool.getLastRunningTaskPeakHappenedTime()));
            stringBuilder.append(MonitorConstants.PLACEHOLDER_NEW_LINE);
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append("discard task count:");
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(iTreadPool.getDiscardTaskCount());
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(" ,monitor interval log task run on:");
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(Thread.currentThread().getName());

            String threadInfo = stringBuilder.toString();


            if (isNeedLogTaskPeak) {
                //因为这里是线程池开始休息时，所以这里上报线程高峰情况比较适合
                //在这里进行统计线程信息上报到日志文件，然后清空上次的缓存信息
                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, threadInfo);
                //上报完后重置
                iTreadPool.resetThreadCacheInfo();
            } else if (isLogThreadInfo) {
                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, threadInfo);
            }


        }
        logOtherThread();
    }

    public void logOtherThread() {
        if (!IOMonitorManager.getInstance().isLogOtherThreadInfo()) {
            return;
        }
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group != null) {
            ThreadGroup temp = group.getParent();
            if (temp == null) {
                break;
            }
            group = temp;
        }
        if (group == null) {
            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString("log other thread " +
                    "info: group==null"));
            return;
        }
        //现在g就是根线程组
        Thread[] all = new Thread[group.activeCount()];
        group.enumerate(all);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.currentTimeMillis());
        stringBuilder.append(MonitorConstants.INFO_SPLIT);
        stringBuilder.append("(current running thread info)");
        stringBuilder.append(MonitorConstants.INFO_SPLIT);
        stringBuilder.append("thread active count is ");
        stringBuilder.append(MonitorConstants.INFO_SPLIT);
        stringBuilder.append(group.activeCount());
        stringBuilder.append(MonitorConstants.INFO_SPLIT);
        stringBuilder.append("(thread list)");
        for (Thread t : all) {
            stringBuilder.append(MonitorConstants.INFO_SPLIT);
            stringBuilder.append(t.getName());

        }
        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, stringBuilder.toString());
    }

    public void stopMonitorLog() {
        if (mMonitorIntervalSubscribe != null && !mMonitorIntervalSubscribe.isUnsubscribed()) {
            mMonitorIntervalSubscribe.unsubscribe();
        }
    }

    /**
     * 打桩测试，通过tag进行统计某个区间段的信息，应用自己控制start和end
     *
     * @param tag 打桩唯一tag
     */
    public void startPiling(String tag) {
        if (!isMonitorEnable()) {
            return;
        }
        Boolean isStarting = mPilingMap.get(tag);
        if (isStarting != null && isStarting) {
            return;
        }
        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(IOMonitorConstants
                .PILING_MONITOR, tag, "piling start"));
        mPilingMap.put(tag, true);
    }

    /**
     * 打桩测试，通过tag进行统计某个区间段的信息，应用自己控制start和end
     *
     * @param tag 打桩唯一tag
     */
    public void endPiling(String tag) {
        if (!isMonitorEnable()) {
            return;
        }
        Boolean isStarting = mPilingMap.get(tag);
        if (isStarting != null && isStarting) {
            MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(IOMonitorConstants
                    .PILING_MONITOR, tag, "piling end"));
            mPilingMap.put(tag, false);
        }
    }

    /**
     * 打桩测试，通过tag进行统计某个区间段的信息，应用自己控制start和end
     *
     * @param tag       打桩唯一tag
     * @param delayTime 延迟一段时间后执行结束打桩
     */
    public void endPiling(final String tag, long delayTime) {
        if (!isMonitorEnable()) {
            return;
        }
        Observable.timer(delayTime, TimeUnit.MILLISECONDS).subscribe(new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(e.getMessage()));
                endPiling(tag);
            }

            @Override
            public void onNext(Long aLong) {
                endPiling(tag);
            }
        });

    }

    /**
     * 是否允许打印IO线程池的当前信息
     *
     * @param logThreadPoolInfo 是否开启
     */
    public IOMonitorManager setLogThreadPoolInfo(boolean logThreadPoolInfo) {
        mIsLogThreadPoolInfo = logThreadPoolInfo;
        return this;
    }

    /**
     * 是否输出除了IO以外的其它线程信息，每次轮询器定时在IO线程池不活跃后以V级别输出。
     *
     * @param logOtherThreadInfo 是否开启
     */
    public IOMonitorManager setLogOtherThreadInfo(boolean logOtherThreadInfo) {
        mIsLogOtherThreadInfo = logOtherThreadInfo;
        return this;
    }

    /**
     * 设置任务耗时阈值
     *
     * @param costTimeLimit 耗时阈值
     */
    public IOMonitorManager setCostTimeLimit(long costTimeLimit) {
        mCostTimeLimit = costTimeLimit;
        return this;
    }

    /**
     * 一般设置和包名相关的参数，以便堆栈信息获取后能快速定位任务名以及代码行号
     * 有子module时，要注意子module的包名路径
     *
     * @param targetNameList 自定义的包名，比如com.xx.xx
     */
    public IOMonitorManager setTargetNameList(List<String> targetNameList) {
        mTargetNameList = targetNameList;
        return this;
    }

    /**
     * 应用层自身的全局packageName
     *
     * @param packageName 包名
     * @return
     */
    public IOMonitorManager setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    /**
     * 允许输出很多其它任务执行过程中的信息
     * 如下：
     * 是否打印IO线程池当前信息
     * 是否打印线程数到达监控阈值的日志
     * 是否打印当前线程耗时的任务以及对应堆栈
     *
     * @param logMoreInfo 是否开启
     */
    public IOMonitorManager setLogMoreInfo(boolean logMoreInfo) {
        mIsLogMoreInfo = logMoreInfo;
        return this;
    }

    /**
     * 监控器轮询每次间隔，秒为单位
     *
     * @param monitorIntervalTime 时间
     */
    public IOMonitorManager setMonitorIntervalTime(long monitorIntervalTime) {
        mMonitorIntervalTime = monitorIntervalTime;
        return this;
    }

    /**
     * 当前活跃的线程数预警阈值，达到后，就以警告输出log
     * 轮询器每次输出信息时打印，或者线程池创建thread时
     */
    public IOMonitorManager setThreadActiveCountLimit(long threadActiveCountLimit) {
        mThreadActiveCountLimit = threadActiveCountLimit;
        return this;
    }

    /**
     * 是否警告当前子线程在重复切换scheduler
     *
     * @param logRepeatChangeThread 是否开启
     */
    public IOMonitorManager setLogRepeatChangeThread(boolean logRepeatChangeThread) {
        mIsLogRepeatChangeThread = logRepeatChangeThread;
        return this;
    }


    /**
     * 设置自定义的ThreadPool，替换基础库自带的IOThreadPool
     * 此方法和setReplaceIOScheduler只能选择一个调用，这里设置IOScheduler的具体线程池策略
     * 设置这个setIOThreadPool和setReplaceIOScheduler，必须在rxjava创建任何一个observable之前调用，不然无法替换
     *
     * @param iTreadPool 自定义的线程池,
     * @see LimitCoreThreadPool
     * 可以参考这个类编写业务层自己需要的IO线程池
     */
    public IOMonitorManager setIOThreadPool(IThreadPool iTreadPool) {
        mIThreadPool = iTreadPool;
        replaceIOScheduler();
        return this;
    }

    /**
     * 是否替换，RxJava的IO scheduler，这里设置为true，
     * 如果没有调用setIOThreadPool或setReplaceIOScheduler方法，则使用本基础库默认的IO scheduler
     *
     * @param replaceIOScheduler 是否替换
     * @return
     */
    public IOMonitorManager setReplaceIOScheduler(boolean replaceIOScheduler) {
        mIsReplaceIOScheduler = replaceIOScheduler;
        return this;
    }

    /**
     * 是否替代RxJava原本的IO scheduler
     * setIOSchedulerThreadPool方法设置的threadPool替代原有的线程池（本包下的IOScheduler类里的线程池），或者是默认的替换策略
     * 此方法和setIOThreadPool只能选择一个调用，这里设置的是scheduler和IOThreadPool，作为一个整体都替换
     * 当前此方法不调用时，IOScheduler使用的是默认的自定义IOScheduler
     * 设置这个setIOThreadPool和setReplaceIOScheduler，必须在rxjava创建任何一个observable之前调用，不然无法替换
     *
     * @param replaceIOScheduler 是否替换
     * @return
     * @see IOScheduler
     */
    public IOMonitorManager setReplaceIOScheduler(AbstractScheduler replaceIOScheduler) {
        mIOScheduler = replaceIOScheduler;
        replaceIOScheduler();
        return this;
    }

    /**
     * 用于打印用，获取优先级对应的优先级名称
     *
     * @param priority
     * @return
     */
    public String getPriorityName(@IOTaskPriorityType int priority) {
        String priorityName;
        switch (priority) {
            case IOTaskPriorityType.LOW_PRIORITY_TASK:
                priorityName = "LOW_PRIORITY_TASK";
                break;
            case IOTaskPriorityType.UPDATE_VIEW_TASK:
                priorityName = "UPDATE_VIEW_TASK";
                break;
            case IOTaskPriorityType.NORMAL_TASK:
                priorityName = "NORMAL_TASK";
                break;
            case IOTaskPriorityType.NETWORK_TASK:
                priorityName = "NETWORK_TASK";
                break;
            case IOTaskPriorityType.DATABASE_IO_TASK:
                priorityName = "DATABASE_IO_TASK";
                break;
            case IOTaskPriorityType.LOAD_DATA_FOR_VIEW_TASK:
                priorityName = "LOAD_DATA_FOR_VIEW_TASK";
                break;
            case IOTaskPriorityType.CORE_TASK:
                priorityName = "CORE_TASK";
                break;
            default:
                priorityName = "DEFAULT";
                break;
        }
        return priorityName;
    }

    public IOMonitorManager setMonitorEnable(boolean monitorEnable) {
        mIsMonitorEnable = monitorEnable;
        return this;
    }

    /**
     * 是否过滤堆栈信息，只保存关键堆栈（应用层逻辑代码堆栈信息）
     *
     * @param isFilterStack 是否过滤堆栈信息
     */
    public IOMonitorManager setFilterStack(boolean isFilterStack) {
        mIsFilterStack = isFilterStack;
        return this;
    }

    /**
     * 打印所有任务执行情况，适合打桩分析当前时间段有哪些任务触发
     *
     * @param logAllTaskRunningInfo 是否允许
     */
    public IOMonitorManager setLogAllTaskRunningInfo(boolean logAllTaskRunningInfo) {
        mIsLogAllTaskRunningInfo = logAllTaskRunningInfo;
        return this;
    }

    /**
     * 运行当前任务到自定义IO线程池，只有执行了ReplaceIOScheduler后才可以使用本方法
     *
     * @param runnable     任务runnable
     * @param priorityType 任务优先级
     * @param newTaskTag   自定义任务名，可以没有
     */
    public IOMonitorManager executeTaskOnIOThread(Runnable runnable, @IOTaskPriorityType int priorityType, String
            newTaskTag) {
        IThreadPool iThreadPool;
        if (isReplaceIOScheduler()) {
            iThreadPool = getIThreadPool();
        } else {
            //如果没有执行替换IO线程池，此时使用默认的参数生成一个线程池执行任务；
            iThreadPool = LimitCoreThreadPool.getInstance()
                    .build(2, 35, 15, 1000, false);
        }

        Pair<String, String> taskNameAndStackInfo = ThreadPoolUtil.getTaskNameAndStackInfo(newTaskTag,
                priorityType);
        IORunnableTask ioRunnableTask = new IORunnableTask(priorityType, taskNameAndStackInfo.first,
                taskNameAndStackInfo.second, runnable);

        try {
            iThreadPool.executeTask(ioRunnableTask);
        } catch (RejectedExecutionException e) {
            MonitorLog.logCatW(IOMonitorConstants.MONITOR_LOG_TAG, "IORunnableTask retry execute this task:" +
                    ioRunnableTask);
            //目前非RxJava方式使用executor，在遇到拒绝时，不执行丢弃策略。二次提交会直接入队
            iThreadPool.executeTask(ioRunnableTask);
        }
        return this;
    }

    /**
     * 运行当前任务到自定义IO线程池，只有执行了ReplaceIOScheduler后才可以使用本方法
     *
     * @param runnable     任务runnable
     * @param priorityType 任务优先级
     */
    public IOMonitorManager executeTaskOnIOThread(Runnable runnable, @IOTaskPriorityType int priorityType) {
        return executeTaskOnIOThread(runnable, priorityType, null);
    }
}
