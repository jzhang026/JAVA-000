
 
1. Overview

The java.util.concurrent package provides tools for creating concurrent applications.
In this article, we will do an overview of the whole package.
2. Main Components 

The java.util.concurrent contains way too many features to discuss in a single write-up. In this article, we will mainly focus on some of the most useful utilities from this package like:
* Executor
* ExecutorService
* ScheduledExecutorService
* Future
* CountDownLatch
* CyclicBarrier
* Semaphore
* ThreadFactory
* BlockingQueue
* DelayQueue
* Locks
* Phaser

2.1. Executor

Executor is an interface that represents an object that executes provided tasks. 
It depends on the particular implementation (from where the invocation is initiated) if the task should be run on a new or current thread. Hence, using this interface, we can decouple the task execution flow from the actual task execution mechanism.
One point to note here is that Executor does not strictly require the task execution to be asynchronous. In the simplest case, an executor can invoke the submitted task instantly in the invoking thread.
We need to create an invoker to create the executor instance:

```java
public class Invoker implements Executor {
    @Override
    public void execute(Runnable r) {
        r.run();
    }
}
```

Now, we can use this invoker to execute the task.

```java
public void execute() {
    Executor executor = new Invoker();
    executor.execute( () -> {
        // task to be performed
    });
}
```


Point to note here is that if the executor can't accept the task for execution, it will throw `RejectedExecutionException`.
2.2. ExecutorService

ExecutorService is a complete solution for asynchronous processing. It manages an in-memory queue and schedules submitted tasks based on thread availability.
To use ExecutorService, we need to create one Runnable class.

```java
public class Task implements Runnable {
    @Override
    public void run() {
        // task details
    }
}
```
Now we can create the ExecutorService instance and assign this task. At the time of creation, we need to specify the thread-pool size.

```java
ExecutorService executor = Executors.newFixedThreadPool(10);
```
If we want to create a single-threaded ExecutorService instance, we can use newSingleThreadExecutor(ThreadFactory threadFactory) to create the instance.
Once the executor is created, we can use it to submit the task.

```java
public void execute() { 
    executor.submit(new Task()); 
}
```

We can also create the Runnable instance while submitting the task.

```java
executor.submit(() -> {
    new Task();
});
```

It also comes with two out-of-the-box execution termination methods. The first one is shutdown(); it waits until all the submitted tasks finish executing. The other method is shutdownNow() which immediately terminates all the pending/executing tasks.
There is also another method awaitTermination(long timeout, TimeUnit unit) which forcefully blocks until all tasks have completed execution after a shutdown event triggered or execution-timeout occurred, or the execution thread itself is interrupted,

```java
try {
    executor.awaitTermination( 20l, TimeUnit.NANOSECONDS );
} catch (InterruptedException e) {
    e.printStackTrace();
}
```

2.3. ScheduledExecutorService

ScheduledExecutorService is a similar interface to ExecutorService, but it can perform tasks periodically.
Executor and ExecutorService‘s methods are scheduled on the spot without introducing any artificial delay. Zero or any negative value signifies that the request needs to be executed instantly.
We can use both Runnable and Callable interface to define the task.

```java
public void execute() {
    ScheduledExecutorService executorService
      = Executors.newSingleThreadScheduledExecutor();
    Future<String> future = executorService.schedule(() -> {
        // ...
        return "Hello world";
    }, 1, TimeUnit.SECONDS);
    ScheduledFuture<?> scheduledFuture = executorService.schedule(() -> {
        // ...
    }, 1, TimeUnit.SECONDS);
    executorService.shutdown();
}

ScheduledExecutorService can also schedule the task after some given fixed delay:

executorService.scheduleAtFixedRate(() -> {
    // ...
}, 1, 10, TimeUnit.SECONDS);
executorService.scheduleWithFixedDelay(() -> {
    // ...
}, 1, 10, TimeUnit.SECONDS);
```
Here, the scheduleAtFixedRate( Runnable command, long initialDelay, long period, TimeUnit unit ) method creates and executes a periodic action that is invoked firstly after the provided initial delay, and subsequently with the given period until the service instance shutdowns.
The scheduleWithFixedDelay( Runnable command, long initialDelay, long delay, TimeUnit unit ) method creates and executes a periodic action that is invoked firstly after the provided initial delay, and repeatedly with the given delay between the termination of the executing one and the invocation of the next one.
2.4. Future

Future is used to represent the result of an asynchronous operation. It comes with methods for checking if the asynchronous operation is completed or not, getting the computed result, etc.
What's more, the cancel(boolean mayInterruptIfRunning) API cancels the operation and releases the executing thread. If the value of mayInterruptIfRunning is true, the thread executing the task will be terminated instantly.
Otherwise, in-progress tasks will be allowed to complete.
We can use below code snippet to create a future instance:

```java
public void invoke() {
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    Future<String> future = executorService.submit(() -> {
        // ...
        Thread.sleep(10000l);
        return "Hello world";
    });
}
```
We can use following code snippet to check if the future result is ready and fetch the data if the computation is done:

```java
if (future.isDone() && !future.isCancelled()) {
    try {
        str = future.get();
    } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
    }
}
```

We can also specify a timeout for a given operation. If the task takes more than this time, a TimeoutException is thrown:

```java
try {
    future.get(10, TimeUnit.SECONDS);
} catch (InterruptedException | ExecutionException | TimeoutException e) {
    e.printStackTrace();
}
```
2.5. CountDownLatch

CountDownLatch (introduced in JDK 5) is a utility class which blocks a set of threads until some operation completes.
A CountDownLatch is initialized with a counter(Integer type); this counter decrements as the dependent threads complete execution. But once the counter reaches zero, other threads get released.
You can learn more about CountDownLatch here.
2.6. CyclicBarrier

CyclicBarrier works almost the same as CountDownLatch except that we can reuse it. Unlike CountDownLatch, it allows multiple threads to wait for each other using await() method(known as barrier condition) before invoking the final task.
We need to create a Runnable task instance to initiate the barrier condition:

```java
public class Task implements Runnable {

    private CyclicBarrier barrier;

    public Task(CyclicBarrier barrier) {
        this.barrier = barrier;
    }
    @Override
    public void run() {
        try {
            LOG.info(Thread.currentThread().getName() + 
              " is waiting");
            barrier.await();
            LOG.info(Thread.currentThread().getName() + 
              " is released");
        } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
        }
    }
}
```
Now we can invoke some threads to race for the barrier condition:

```java
public void start() {
    CyclicBarrier cyclicBarrier = new CyclicBarrier(3, () -> {
        // ...
        LOG.info("All previous tasks are completed");
    });
    Thread t1 = new Thread(new Task(cyclicBarrier), "T1"); 
    Thread t2 = new Thread(new Task(cyclicBarrier), "T2"); 
    Thread t3 = new Thread(new Task(cyclicBarrier), "T3"); 
    if (!cyclicBarrier.isBroken()) { 
        t1.start(); 
        t2.start(); 
        t3.start(); 
    }
}
```
Here, the isBroken() method checks if any of the threads got interrupted during the execution time. We should always perform this check before performing the actual process.
2.7. Semaphore

The Semaphore is used for blocking thread level access to some part of the physical or logical resource. A semaphore contains a set of permits; whenever a thread tries to enter the critical section, it needs to check the semaphore if a permit is available or not.
If a permit is not available (via tryAcquire()), the thread is not allowed to jump into the critical section; however, if the permit is available the access is granted, and the permit counter decreases. 
Once the executing thread releases the critical section, again the permit counter increases (done by release() method).
We can specify a timeout for acquiring access by using the tryAcquire(long timeout, TimeUnit unit) method.
We can also check the number of available permits or the number of threads waiting to acquire the semaphore.
Following code snippet can be used to implement a semaphore:

```java
static Semaphore semaphore = new Semaphore(10);

public void execute() throws InterruptedException {

    LOG.info("Available permit : " + semaphore.availablePermits());
    LOG.info("Number of threads waiting to acquire: " + 
      semaphore.getQueueLength());
    if (semaphore.tryAcquire()) {
        try {
            // ...
        }
        finally {
            semaphore.release();
        }
    }
}
```

We can implement a Mutex like data-structure using Semaphore. More details on this can be found here.
2.8. ThreadFactory 

As the name suggests, ThreadFactory acts as a thread (non-existing) pool which creates a new thread on demand. It eliminates the need of a lot of boilerplate coding for implementing efficient thread creation mechanisms.
```java
public class BaeldungThreadFactory implements ThreadFactory {
    private int threadId;
    private String name;
    public BaeldungThreadFactory(String name) {
        threadId = 1;
        this.name = name;
    }
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, name + "-Thread_" + threadId);
        LOG.info("created new thread with id : " + threadId +
            " and name : " + t.getName());
        threadId++;
        return t;
    }
}
```

We can use this newThread(Runnable r) method to create a new thread at runtime:

```java
BaeldungThreadFactory factory = new BaeldungThreadFactory( 
    "BaeldungThreadFactory");
for (int i = 0; i < 10; i++) { 
    Thread t = factory.newThread(new Task());
    t.start(); 
}
```

2.9. BlockingQueue

In asynchronous programming, one of the most common integration patterns is the producer-consumer pattern. The java.util.concurrent package comes with a data-structure know as BlockingQueue – which can be very useful in these async scenarios.
More information and a working example on this is available here.
2.10. DelayQueue

DelayQueue is an infinite-size blocking queue of elements where an element can only be pulled if it's expiration time (known as user defined delay) is completed. Hence, the topmost element (head) will have the most amount delay and it will be polled last.
More information and a working example on this is available here.
2.11. Locks

Not surprisingly, Lock is a utility for blocking other threads from accessing a certain segment of code, apart from the thread that's executing it currently.
The main difference between a Lock and a Synchronized block is that synchronized block is fully contained in a method; however, we can have Lock API’s lock() and unlock() operation in separate methods.
More information and a working example on this is available here.
2.12. Phaser

Phaser is a more flexible solution than CyclicBarrier and CountDownLatch – used to act as a reusable barrier on which the dynamic number of threads need to wait before continuing execution. We can coordinate multiple phases of execution, reusing a Phaser instance for each program phase.
More information and a working example on this is available here.
 
