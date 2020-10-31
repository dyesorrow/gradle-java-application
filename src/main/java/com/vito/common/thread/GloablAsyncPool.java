package com.vito.common.thread;

import lombok.Setter;

public class GloablAsyncPool extends AsyncPool {

    private static volatile GloablAsyncPool instance;

    @Setter
    private static int size = 16;

    // 双重检查模式
    public static GloablAsyncPool getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (GloablAsyncPool.class) {
            if (instance == null) {
                instance = new GloablAsyncPool(size);
            }
        }
        return instance;
    }

    private GloablAsyncPool(int size) {
        super(size);
    }

}
