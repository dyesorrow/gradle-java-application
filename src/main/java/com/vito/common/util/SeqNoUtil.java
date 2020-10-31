package com.vito.common.util;

public enum SeqNoUtil {
    instance;

    private long seq = 0;

    private SeqNoUtil() {
    }

    private synchronized long nextSeq() {
        return ++seq;
    }

    public static long next() {
        return SeqNoUtil.instance.nextSeq();
    }
}
