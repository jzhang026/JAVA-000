package io.adrian.week04.homework.returnThreadValue;
public class UseRunnableClass {
    public static void main(String[] args) {
        ThreadTask task = new ThreadTask();
        Thread t = new Thread(task);
        t.start();
        try {
            t.join();
            int value = task.getValue();
            System.out.println("The thread retuend value is: " + value);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    static class ThreadTask implements Runnable {
        // int value的改变马上刷到主线程可见
        private volatile int value;

        @Override
        public void run() {
            // some other logic here
            value = 2;
        }

        public int getValue() {
            return value;
        }
    }
}
