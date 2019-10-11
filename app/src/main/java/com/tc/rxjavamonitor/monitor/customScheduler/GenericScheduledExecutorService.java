package com.tc.rxjavamonitor.monitor.customScheduler;


import com.tc.rxjavamonitor.monitor.IOMonitorConstants;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import rx.internal.schedulers.NewThreadWorker;
import rx.internal.util.RxThreadFactory;

public final class GenericScheduledExecutorService {

    private static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(IOMonitorConstants.THREAD_NAME_PREFIX);
    
    private final static GenericScheduledExecutorService INSTANCE = new GenericScheduledExecutorService();
    private final ScheduledExecutorService executor;

    private GenericScheduledExecutorService() {
        int count = Runtime.getRuntime().availableProcessors();
        if (count > 4) {
            count = count / 2;
        }
        // we don't need more than 8 to handle just scheduling and doing no work
        if (count > 8) {
            count = 8;
        }
        ScheduledExecutorService exec = Executors.newScheduledThreadPool(count, THREAD_FACTORY);
        if (!NewThreadWorker.tryEnableCancelPolicy(exec)) {
            if (exec instanceof ScheduledThreadPoolExecutor) {
                NewThreadWorker.registerExecutor((ScheduledThreadPoolExecutor)exec);
            }
        }
        executor = exec;
    }

    /**
     * See class Javadoc for information on what this is for and how to use.
     * 
     * @return {@link ScheduledExecutorService} for generic use.
     */
    public static ScheduledExecutorService getInstance() {
        return INSTANCE.executor;
    }
}
