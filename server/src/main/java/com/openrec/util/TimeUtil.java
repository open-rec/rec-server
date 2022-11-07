package com.openrec.util;

import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public static long now() {
        return System.currentTimeMillis();
    }

    public static long nowSecs() {
        return System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1);
    }
}
