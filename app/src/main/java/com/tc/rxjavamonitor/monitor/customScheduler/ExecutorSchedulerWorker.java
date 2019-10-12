package com.tc.rxjavamonitor.monitor.customScheduler;


import android.os.Looper;

import com.tc.rxjavamonitor.monitor.IOMonitorConstants;
import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.custominterface.IBaseWork;
import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;
import com.tc.rxjavamonitor.monitor.log.MonitorLog;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.schedulers.ScheduledAction;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Worker that schedules tasks on the executor indirectly through a trampoline mechanism.
 */
public class ExecutorSchedulerWorker extends Scheduler.Worker implements Runnable, IBaseWork,
        Comparable<IBaseWork> {
    final Executor executor;
    final CompositeSubscription tasks;
    final ConcurrentLinkedQueue<ScheduledAction> queue;
    final AtomicInteger wip;
    protected int priority;
    protected String taskName;
    protected String stackInfo;
    /**
     * 提交任务次数，一般是失败次数
     */
    private boolean needCreateNewThread;

    public ExecutorSchedulerWorker(Executor executor, String taskName, String stackInfo, @IOTaskPriorityType int
            priority) {
        this.executor = executor;
        this.queue = new ConcurrentLinkedQueue<ScheduledAction>();
        this.wip = new AtomicInteger();
        this.tasks = new CompositeSubscription();
        this.taskName = taskName;
        this.stackInfo = stackInfo;
        this.priority = priority;
    }

    @Override
    public Subscription schedule(Action0 action) {
        judgeChildThreadRepeatChange();
        if (isUnsubscribed()) {
            return Subscriptions.unsubscribed();
        }
        ScheduledAction ea = new ScheduledAction(action, tasks);
        tasks.add(ea);
        queue.offer(ea);
        if (wip.getAndIncrement() == 0) {
            try {
                // note that since we schedule the emission of potentially multiple tasks
                // there is no clear way to cancel this schedule from individual tasks
                // so even if executor is an ExecutorService, we can't associate the future
                // returned by submit() with any particular ScheduledAction
                executor.execute(this);
            } catch (RejectedExecutionException t) {
                IBaseWork iBaseWork = this;
                //一般第二次就会成功，因为目前使用的阻塞队列都是无界，入队会成功
                //@see IOPriorityQueue.isNotAllowOfferQueue方法
                if (needCreateNewThread || iBaseWork.getPriority() >= IOTaskPriorityType
                        .DISCARD_TASK_VALUE) {
                    //如果不是丢弃型任务或因为并发导致创建新线程失败而无法提交的任务，这里容错处理，继续添加到处理队列里，防止并发出错
                    MonitorLog.logCatW(IOMonitorConstants.MONITOR_LOG_TAG, "ExecutorSchedulerWorker execute this task" +
                            " again:" + taskName + "  needCreateNewThread:" + needCreateNewThread);
                    executor.execute(this);
                } else {
                    // cleanup if rejected
                    tasks.remove(ea);
                    wip.decrementAndGet();
                    // report the error to the plugin
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
                    return throwErrorException();
                }
            }
        }
        return ea;
    }

    private Subscription throwErrorException() {
        //当任务被丢弃处理时，这里打印日志，后续可以做更多事情
//        MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(IOMonitorConstants
//                .DISCARD_TASK_PREFIX, taskName));
        throw new UnsupportedOperationException("this task is rejected, you need realize RxJava onError " +
                "action! " + taskName);
    }

    /**
     * 判断是否子线程多次切换线程
     */
    private void judgeChildThreadRepeatChange() {
        if (IOMonitorManager.getInstance().isMonitorEnable()) {
            boolean isMainThread = Looper.myLooper() == Looper.getMainLooper();
            if (IOMonitorManager.getInstance().isLogRepeatChangeThread() && !isMainThread && taskName.startsWith
                    (IOMonitorConstants.IO_TASK_NAME_SUFFIX)) {
//                MonitorLog.thread(IOMonitorConstants.MONITOR_LOG_TAG, MonitorLog.getSplitString(" current is child" +
//                                " thread! Maybe do on this thread,don''t change to other child thread  again" +
//                                ".", MonitorConstants.PLACEHOLDER_NEW_LINE, "current thread", Thread.currentThread(),
//                        MonitorConstants.PLACEHOLDER_NEW_LINE, "taskName:", taskName));
            }
        }
    }

    @Override
    public void run() {
        do {
            ScheduledAction sa = queue.poll();
            if (!sa.isUnsubscribed()) {
                sa.run();
            }
        } while (wip.decrementAndGet() > 0);
    }

    @Override
    public Subscription schedule(final Action0 action, long delayTime, TimeUnit unit) {
        if (delayTime <= 0) {
            return schedule(action);
        }
        if (isUnsubscribed()) {
            return Subscriptions.unsubscribed();
        }
        ScheduledExecutorService service;
        if (executor instanceof ScheduledExecutorService) {
            service = (ScheduledExecutorService) executor;
        } else {
            service = GenericScheduledExecutorService.getInstance();
        }

        final MultipleAssignmentSubscription first = new MultipleAssignmentSubscription();
        final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
        mas.set(first);
        tasks.add(mas);
        final Subscription removeMas = Subscriptions.create(new Action0() {
            @Override
            public void call() {
                tasks.remove(mas);
            }
        });

        ScheduledAction ea = new ScheduledAction(new Action0() {
            @Override
            public void call() {
                if (mas.isUnsubscribed()) {
                    return;
                }
                // schedule the real action untimed
                Subscription s2 = schedule(action);
                mas.set(s2);
                // unless the worker is unsubscribed, we should get a new ScheduledAction
                if (s2.getClass() == ScheduledAction.class) {
                    // when this ScheduledAction completes, we need to remove the
                    // MAS referencing the whole setup to avoid leaks
                    ((ScheduledAction) s2).add(removeMas);
                }
            }
        });
        // This will make sure if ea.call() gets executed before this line
        // we don't override the current task in mas.
        first.set(ea);
        // we don't need to add ea to tasks because it will be tracked through mas/first


        try {
            Future<?> f = service.schedule(ea, delayTime, unit);
            ea.add(f);
        } catch (RejectedExecutionException t) {
            // report the rejection to plugins
            RxJavaPlugins.getInstance().getErrorHandler().handleError(t);
            throw t;
        }

            /*
             * This allows cancelling either the delayed schedule or the actual schedule referenced
             * by mas and makes sure mas is removed from the tasks composite to avoid leaks.
             */
        return removeMas;
    }

    @Override
    public boolean isUnsubscribed() {
        return tasks.isUnsubscribed();
    }

    @Override
    public void unsubscribe() {
        tasks.unsubscribe();
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
        return "{\"ExecutorSchedulerWorker\":{"
                + "\"priority\":"
                + priority
                + ",\"taskName\":\""
                + taskName + '\"'
                + "}";

    }
}

