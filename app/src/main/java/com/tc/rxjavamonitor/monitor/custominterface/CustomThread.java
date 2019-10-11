package com.tc.rxjavamonitor.monitor.custominterface;

/**
 * author：   tc
 * date：      2019/3/28
 * version    1.0
 * description  自定义的thread
 * modify by
 */
public class CustomThread extends Thread {

    public CustomThread() {
    }

    public CustomThread(Runnable target) {
        super(target);
    }

    public CustomThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public CustomThread(String name) {
        super(name);
    }

    public CustomThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public CustomThread(Runnable target, String name) {
        super(target, name);
    }

    public CustomThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public CustomThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    @Override
    public void run() {
        super.run();
    }
}