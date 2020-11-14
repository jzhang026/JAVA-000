package io.adrian.week04.homework.returnThreadValue;

import java.util.concurrent.CountDownLatch;

public class UseCountDownLatch {
    public static void main(String[] args) {
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] threadValue = new String[1];
        Thread t = new Thread("count-down-latch") {
            @Override
            public void run() {
                threadValue[0] = "value from count down latched thread";
                latch.countDown();
            }
        };
        t.start();
        try {
            latch.await();
            System.out.println(threadValue[0]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
