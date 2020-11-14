package io.adrian.week04.homework.returnThreadValue;

import java.util.concurrent.*;

public class FutureValue {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Callable<Integer> callable = () -> 666;
        Future<Integer> futureRes = executor.submit(callable);
        int value = futureRes.get();
        System.out.println("the thread returned value is: " + value);
        executor.shutdown();
    }
}
