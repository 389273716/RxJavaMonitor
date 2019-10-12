package com.tc.rxjavamonitor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.tc.rxjavamonitor.monitor.AppSchedulers;
import com.tc.rxjavamonitor.monitor.IOMonitorManager;
import com.tc.rxjavamonitor.monitor.custominterface.IOTaskPriorityType;
import com.tc.rxjavamonitor.monitor.log.LogUtil;
import com.tc.rxjavamonitor.monitor.log.MonitorLog;

import java.util.Locale;
import java.util.Random;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnTest = (Button) findViewById(R.id.btn_test_start);

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                testMonitorLog(2);
//                testNoStackInfo();
//                testPriority();
                test(1000);
//                IOMonitorManager.getInstance().executeTaskOnIOThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        LogUtil.i("test execute runnable------------->");
//                    }
//                },IOTaskPriorityType.UPDATE_VIEW_TASK);
            }
        });


    }

    private void testMonitorLog(final int count) {
//        MonitorLog.thread("---------thread_info message");
//        MonitorLog.trace("---------trace_info message");
//        MonitorLog.trace("---------trace_info message33333");
//        MonitorLog.thread("---------thread_info message222");
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                Random random = new Random();
                int time;
                for (int j = 0; j < count; j++) {
                    time = random.nextInt(1000);
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    MonitorLog.thread(TAG, MonitorLog.getSplitString(
                            "Observable call test thread:", j, time, IOMonitorManager.getStackInfo(stackTrace, true)));
                }
                subscriber.onNext(1);
                subscriber.onCompleted();
                LogUtil.d("app_custom_io_scheduler", "thread name: " + Thread.currentThread().getName());
            }
        })
//                .subscribeOn(Schedulers.io())
                .subscribe();
    }


    private void testNoStackInfo() {
        //测试无法打印任务所在业务代码的堆栈的问题，这里是例子
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onNext(1);
                subscriber.onCompleted();
                LogUtil.d("app_custom_io_scheduler", "thread name: " + Thread.currentThread().getName());
            }
        })
//                .observeOn(Schedulers.io())
                .map(new Func1<Integer, Object>() {
                    @Override
                    public Object call(Integer integer) {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            LogUtil.e(TAG, e);
                        }
                        return null;
                    }
                })
//                .flatMap(new Func1<Integer, Observable<Integer>>() {
//                    @Override
//                    public Observable<Integer> call(Integer integer) {
//                        return Observable.create(new Observable.OnSubscribe<Integer>() {
//                            @Override
//                            public void call(Subscriber<? super Integer> subscriber) {
//                                try {
//                                    Thread.sleep(3000);
//                                } catch (InterruptedException e) {
//                                    e.printStackTrace();
//                                }
//                                subscriber.onNext(1);
//                                subscriber.onCompleted();
//                                LogUtil.d("app_custom_io_scheduler", "thread name inner: " + Thread.currentThread()
//                                        .getName());
//                            }
//                        }).subscribeOn(Schedulers.io());
//                    }
//                })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private void testRXJavaThread(int num) {
        for (int i = 0; i < num; i++) {
            final int finalI = i;
            Observable.create(new Observable.OnSubscribe<Object>() {
                @Override
                public void call(Subscriber<? super Object> subscriber) {
                    try {
                        Random random = new Random();
                        int time = random.nextInt(1000);
//                    for (int j = 0; j < 10001000; j++) {
//                        time += j;
//                    }
                        LogUtil.d(TAG, String.format(Locale.ENGLISH, "Observable call " +
                                "test thread: %d  sleep time: %d", finalI, time));
                        Thread.sleep(time + 200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    subscriber.onCompleted();
                }
            })
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<Object>() {
                        @Override
                        public void onCompleted() {
                            LogUtil.d(TAG, String.format(Locale.ENGLISH,
                                    "testRXJavaThread onCompleted: %d", finalI));

                        }

                        @Override
                        public void onError(Throwable e) {
//                            LogUtil.e(TAG, "testRXJavaThread onError: ", e);
                            LogUtil.d(TAG, String.format("onError: %s", e.getMessage()));
                        }

                        @Override
                        public void onNext(Object o) {

                        }
                    });

        }

    }

    private void test(final int count) {
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {

                testRXJavaThread(count);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {
                        LogUtil.d(TAG, "test onCompleted: ");

                    }

                    @Override
                    public void onError(Throwable e) {
                        LogUtil.e(TAG, String.format("testRXJavaThread onError: %s", e.getMessage()));
                    }

                    @Override
                    public void onNext(Object o) {
                    }
                });

    }


    private void testPriority() {
        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.UPDATE_VIEW_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.UPDATE_VIEW_TASK + "  sleep time: " + time);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.UPDATE_VIEW_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });
        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.CORE_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.CORE_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.CORE_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });
        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.UPDATE_VIEW_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.UPDATE_VIEW_TASK + "  sleep time: " + time);
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.UPDATE_VIEW_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });

        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NETWORK_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.NETWORK_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });
        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.NORMAL_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });


        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.NORMAL_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });

        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.NORMAL_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });

        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.UPDATE_VIEW_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.UPDATE_VIEW_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.UPDATE_VIEW_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });

        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NORMAL_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.NETWORK_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.NETWORK_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });
        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.DATABASE_IO_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.DATABASE_IO_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.DATABASE_IO_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });

        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.LOAD_DATA_FOR_VIEW_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.LOAD_DATA_FOR_VIEW_TASK + "  sleep time: " +
                            time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.LOAD_DATA_FOR_VIEW_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });

        LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.LOW_PRIORITY_TASK);
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                try {
                    Random random = new Random();
                    int time = random.nextInt(1000);
                    LogUtil.d(TAG, "testPriority- " + IOTaskPriorityType.LOW_PRIORITY_TASK + "  sleep time: " + time);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                subscriber.onCompleted();
            }
        })
                .subscribeOn(AppSchedulers.io("test inner task test", IOTaskPriorityType.LOW_PRIORITY_TASK))
                .subscribe(new Subscriber<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {

                    }
                });
    }
}

