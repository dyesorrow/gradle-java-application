package com.vito.common.thread;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

// 建议仅对无法预估阻塞时常的任务添加到任务池，如网络连接。对于可以预估的如 磁盘读写，建议直接同步操作

@Slf4j
public class AsyncPool {

    public static interface AsyncTask {
        void todo();
    }

    private Queue<AsyncTask> pool;
    // 此处自己实现线程池，方便翻译到 c++
    private List<Thread> threads;
    private Integer taskLock = 0;

    @Getter
    private final int size;

    public AsyncPool(int size) {
        this.size = size;
        this.pool = new LinkedList<>();
        this.threads = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Thread thread = new Thread(() -> {
                this.doTask();
            });
            thread.start();
            this.threads.add(thread);
        }
    }

    private void doTask() {
        try {
            while (true) {
                AsyncTask task = this.pool.peek();
                if (task == null) {
                    synchronized (taskLock) {
                        log.info("wait a task...");
                        taskLock.wait();
                    }
                    log.info("wait a task notify");
                    continue;
                }
                log.info("poll a task to handle");
                synchronized (this.pool) {
                    task = this.pool.poll();
                    if (task == null) {
                        continue;
                    }
                }
                task.todo();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void push(AsyncTask task) {
        synchronized (pool) {
            pool.add(task);
            synchronized (taskLock) {
                log.info("notify to handle task");
                taskLock.notify();
            }
        }
    }

}
