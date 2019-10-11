package com.tc.rxjavamonitor.monitor.custominterface;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * author：   tc
 * date：      2019/3/27 & 17:36
 * version    1.0
 * description  IO任务执行优先级。在丢弃任务时会抛出rxjava的异常，要实现onError接口
 * modify by
 */
@IntDef({IOTaskPriorityType.LOW_PRIORITY_TASK, IOTaskPriorityType.UPDATE_VIEW_TASK, IOTaskPriorityType.NORMAL_TASK,
        IOTaskPriorityType.NETWORK_TASK,
        IOTaskPriorityType.DATABASE_IO_TASK, IOTaskPriorityType.LOAD_DATA_FOR_VIEW_TASK, IOTaskPriorityType.CORE_TASK})
@Retention(RetentionPolicy.SOURCE)
public @interface IOTaskPriorityType {
    /**
     * 优先级最低的任务，可丢弃任务,在丢弃任务时会抛出rxjava的异常，要实现onError接口
     */
    int LOW_PRIORITY_TASK = -50;

    /**
     * 优先级很低，只是刷新下界面，如果刷新被延迟或者丢弃，下次还可以恢复，优先级最低，
     * 可丢弃任务,在丢弃任务时会抛出rxjava的异常，要实现onError接口
     */
    int UPDATE_VIEW_TASK = -30;


    /**
     * 丢弃任务阈值，这个值以上为不丢弃任务,
     */
    int DISCARD_TASK_VALUE = -10;

    //--------------不丢弃任务区-------------------
    /**
     * 常规任务，任务的默认值，优先级低
     */
    int NORMAL_TASK = 0;

    /**
     * 网络任务，一般不会因为有界阻塞队列进行丢弃，优先级较高
     */
    int NETWORK_TASK = 20;
    /**
     * 数据库操作任务，优先级高
     */
    int DATABASE_IO_TASK = 50;
    /**
     * 比如进入界面时异步加载界面必须的数据，此时不能被阻塞,需要优先处理，优先级高
     */
    int LOAD_DATA_FOR_VIEW_TASK = 60;

    /**
     * 核心任务，一般不会因为有界阻塞队列进行丢弃，优先级高（核心的任务，必须要处理，比如微聊消息，此时不能被阻塞,需要优先处理）
     */
    int CORE_TASK = 100;
}
