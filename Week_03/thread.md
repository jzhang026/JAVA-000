# Thread的状态改变

- Thread.sleep(long millis, long nanos). 线程从running状态改变为Time_wait状态。让出CPU时间片，但不释放自己所持有的锁。
- Thread.yield() 线程从running状态变为就绪runnable状态。也不让出自己持有的锁。让同等级的线程有机会执行。
- t.join() / t.join(millis)  当前线程里调用其他线程t的join方法。当前线程进入waiting/ Timed_waiting状态。当先线程不会释放持有的锁。线程t执行完毕或者millis时间到。进入就绪状态。join会释放被调用线程对象的锁。
- obj.wait 当前线程调用对象的wait方法，当前线程*释放锁*，进入等待队列，依靠notify/notifyall唤醒或者wait(long timeout)时间自动唤醒
- obj.notify 唤醒此对象监视器上等待的单个线程。notifyAll唤醒此对象监视器上的所有线程。