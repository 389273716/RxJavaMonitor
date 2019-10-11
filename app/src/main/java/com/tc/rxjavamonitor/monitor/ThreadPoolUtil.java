package com.tc.rxjavamonitor.monitor;

import android.text.TextUtils;
import android.util.Pair;

import com.tc.rxjavamonitor.monitor.custominterface.AbstractScheduler;
import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;

import java.util.Locale;
import java.util.UUID;

/**
 * author：   tc
 * date：      2019/4/22 & 14:58
 * version    1.0
 * description  一些公共逻辑处理
 * modify by
 */
public class ThreadPoolUtil {

    /**
     * 获取当前代码块所在堆栈信息，并且以此生成任务名和堆栈信息字符串
     *
     * @param newTaskTag   当前任务是否有自定义名
     * @param priorityType 任务优先级
     * @return  第一个参数是任务名，第二个是堆栈信息
     */
    public static Pair<String, String> getTaskNameAndStackInfo(String newTaskTag, @IOTaskPriorityType int
            priorityType) {
        Thread currentThread = Thread.currentThread();
        String taskName = "";
        StackTraceElement[] stackTrace = currentThread.getStackTrace();
        boolean isFindTaskName = false;


        //任务名为空时，尝试用堆栈去生成一个
        for (StackTraceElement ste : stackTrace) {
            String className = ste.getClassName();
            if (className.contains(AbstractScheduler.class.getClass().getPackage().getName())) {
                //过滤掉线程管理包目录的路径
                continue;
            }
            if (className.startsWith("com.xtc.snmonitor.collector.monitor.thread")) {
                continue;
            }
            if (IOMonitorManager.isFilterStack(ste)) {
                //过滤掉不关心的堆栈
                continue;
            }
            if (className.contains(IOMonitorManager.getInstance().getPackageName())) {
                //优先找第一个跟包名一致的栈堆信息
                isFindTaskName = true;
            }
            for (String key : IOMonitorManager.getInstance().getTargetNameList()) {
                if (className.contains(key)) {
                    //优先找外部配置的目标名称匹配的栈堆信息
                    isFindTaskName = true;
                }
            }
            if (isFindTaskName) {
                taskName = String.format(Locale.ENGLISH, "(%s:%s)++%s.%s",
                        ste.getFileName(), String.valueOf(ste.getLineNumber()), className, ste.getMethodName());
                break;
            }
        }
        if (!TextUtils.isEmpty(newTaskTag)) {
            //如果是自定义taskTag,则自定义的taskName要加上这个taskTag
            taskName = String.format(Locale.ENGLISH, "%s_%s_%s_%s", newTaskTag, taskName, UUID.randomUUID()
                    .toString(), IOMonitorConstants.IO_TASK_NAME_SUFFIX);
        } else if (!TextUtils.isEmpty(taskName)) {
            //默认有值，说明外部应用层调用subscribeOn时已经传入自定义的任务名字，此时加上统一的前缀
            taskName = String.format(Locale.ENGLISH, "%s_%s_%s_%s", " ", taskName, UUID.randomUUID().toString
                    (), IOMonitorConstants.IO_TASK_NAME_SUFFIX);
        } else {
            //如果是没有自定义taskName或者通过堆栈找不到taskName，则只使用随机字符串作为名字
            taskName = UUID.randomUUID().toString();
        }

        String stackInfo = IOMonitorManager.getStackInfo(stackTrace, true);
        if (TextUtils.isEmpty(taskName)) {
            stackInfo = "code stack is child currentThread,don't write stack info.";
        }
        taskName = String.format(Locale.ENGLISH, "priority:%s (%s)==%s", priorityType,
                IOMonitorManager.getInstance().getPriorityName(priorityType),
                taskName);
        return new Pair<>(taskName, stackInfo);
    }


}
