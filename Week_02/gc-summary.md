# 对象是否存活 -- 可达性分析

通过一系列的称为"GC Roots"的对象作为起始点，从这些节点开始向下搜索，搜索所走过的路径称为引用链（Reference Chain），当一个对象到GC Roots没有任何引用链相连（用图论的话来说，就是从GC Roots到这个对象不可达）时，则证明此对象是不可用的。

可作为GC Roots的对象包括下面 4 种：
1. 虚拟机栈（栈帧中的本地变量表）中引用的对象。
2. 方法区中类静态属性引用的对象。
3. 方法区中常量引用的对象。
4. 方法区中类静态属性引用的对象的Native方法引用的对象

# GC算法

## 标记-清除算法

- 效率问题：标记和清除两个过程的效率都不高；
- 空间问题：标记清除之后会产生大量不连续的内存碎片，空间碎片太多可能会导致以后在程序运行过程中需要分配较大对象时，无法找到足够的连续内存而不得不提前触发另一次垃圾收集动作。

## 复制算法
为了解决效率问题，一种称为“复制”（Copying）的收集算法出现了，现在的商业虚拟机都采用这种收集算法来回收新生代
- 将可用内存按容量划分为大小相等的两块，每次只使用其中的一块。
- 当这一块的内存用完了，就将还存活着的对象复制到另外一块上面，然后再把已使用过的内存空间一次清理掉。

这样使得每次都是对整个半区进行内存回收，内存分配时也就不用考虑内存碎片等复杂情况，只要移动堆顶指针，按顺序分配内存即可，实现简单，运行高效。只是这种算法的代价是将内存缩小为了原来的一半

### 新生代堆
新生代中的绝大多数对象都是瞬间会不再使用，从每次GC的效率会回收接近90%的新生代对象，可以得出此结论。
所以并不需要按照1:1的比例来划分内存空间，而是将内存分为一块较大的Eden空间和两块较小的Survivor空间，每次使用Eden和其中一块Survivor（8：1：1）。当回收时，将Eden和Survivor中还存活着的对象一次性地复制到另外一块Survivor空间上，最后清理掉Eden和刚才用过的Survivor空间。HotSpot虚拟机默认Eden和Survivor的大小比例是8:1，也就是每次新生代中可用内存空间为整个新生代容量的90%（80%+10%）。
当Survivor空间不够用时，需要依赖其他内存（这里指老年代）进行分配担保（Handle Promotion）。

### 分配担保
如果另外一块Survivor空间没有足够空间存放上一次新生代收集下来的存活对象时，这些对象将直接通过分配担保机制进入老年代。

### 复制算法总结
复制算法的核心特点在于划分内存空间，好处是集中一次性清理垃圾，并且内存碎片少，代价是只有运行时部分内存可被对象申请使用。为了保证可用堆内存足够大，所以只能把用于存放存活对象的Survivor比例设置小一点，但这一切都是基于大部分新生代对象的生命周期较短为前提，一旦新生代不满足这个前提，就需要依赖分担担保来保证存活对象能被存放得下。

## GC算法 -- 标记整理算法
标记整理主要针对于老年代。
复制收集算法在对象存活率较高时就要进行较多的复制操作，效率将会变低，并且需要大量的分担担保由于老年代中对象存活率都是很高，因此一方面50%内存划分的复制算法，存在内存利用率低问题，另一方面8:1:1内存划分的复制算法，又存不下那么多仍存活的对象；因此，复制算法不适合老年代，老年代使用“标记-整理”（Mark-Compact）算法。此时这里内存的整理方法就是移动而不再是复制。

- 标记过程仍然与“标记-清除”算法一样；
- 后续步骤不是直接对可回收对象进行清理，而是让所有存活的对象都向一端移动，然后直接清理掉端边界以外的内存。

## GC算法 -- 分代收集算法 （分代 + 上述的 标记清除 | 复制算法 | mark-sweep-compact）
所以我们会针对堆的不同区域，去选择适合他们的GC算法。既这里的分代收集算法。

具体来说，把Java堆分为新生代和老年代，
- 在新生代中，每次垃圾收集时都发现有大批对象死去，只有少量存活，那就选用复制算法，只需要付出少量存活对象的复制成本就可以完成收集。
- 在老年代中，因为对象存活率高、没有额外空间对它进行分配担保，就必须使用“标记—清理”或者“标记—整理”算法来进行回收。

具体的分代策略搭配，如下图：
(gc_strategy)[/gc_strategy.jpg]

## Serial (新生代单线程复制算法)
它的“单线程”的意义：（Stop The World）
- 一方面说明它只会使用一个CPU或一条收集线程去完成垃圾收集工作，
- 更重要的是在它进行垃圾收集时，必须暂停其他所有的工作线程，直到它收集结束。

对于限定单个CPU的环境来说，Serial收集器由于没有线程交互的开销，专心做垃圾收集自然可以获得最高的单线程收集效率。在用户的桌面应用场景中，分配给虚拟机管理的内存一般来说不会很大，收集几十兆甚至一两百兆的新生代，停顿时间完全可以控制在几十毫秒最多一百多毫秒以内，只要不是频繁发生，这点停顿是可以接受的。
## ParNew (新生代多线程复制算法)
其实是serial收集器的多线程版本
ParNew收集器在单CPU的环境中绝对不会有比Serial收集器更好的效果，甚至由于存在线程交互的开销，但随着可以使用的CPU的数量的增加，它对于GC时系统资源的有效利用还是很有好处的。它默认开启的收集线程数与CPU的数量相同。

它是许多运行在Server模式下的虚拟机中首选的新生代收集器

## Parallel Scavenge(新生代多线程复制算法)
> Parallel Scavenge收集器是一个新生代收集器，它也是使用复制算法的收集器，又是并行的多线程收集器；它的不同之处在于：其他收集器的关注点是尽可能地缩短垃圾收集时用户线程的停顿时间，而Parallel Scavenge收集器的目标则是达到一个可控制的吞吐量（Throughput）。

所谓吞吐量就是CPU用于运行用户代码的时间与CPU总消耗时间的比值，即吞吐量=运行用户代码时间/（运行用户代码时间+垃圾收集时间）。

停顿时间越短就越适合需要与用户交互的程序，良好的响应速度能提升用户体验，而高吞吐量则可以高效率地利用CPU时间，尽快完成程序的运算任务，主要适合在后台运算而不需要太多交互的任务。

## Serial Old(老年代单线程标记 - 整理算法)
> Serial Old是Serial收集器的老年代版本，它同样是一个单线程收集器，使用“标记-整理”算法；

这个收集器的主要意义也是在于给Client模式下的虚拟机使用。

如果在Server模式下，那么它主要还有两大用途：
- 一种用途是在JDK 1.5以及之前的版本中与Parallel Scavenge收集器搭配使用；
- 另一种用途就是作为CMS收集器的后备预案，在并发收集发生Concurrent Mode Failure时使用。

## Parallel Old(老年代多线程标记 - 整理算法)
Parallel Old 只跟新生代的Parallel Scavenge收集器搭配使用，由于二者都是多线程收集器，所以同时搭配只用在Server端，充分发挥服务器多CPU的处理能力；

在注重吞吐量以及CPU资源敏感的场合，都可以优先考虑Parallel Scavenge加Parallel Old收集器。

## CMS(与用户线程并发的老年代标记-清除算法)
优点：
并发收集、低停顿。

由于整个过程中耗时最长的并发标记和并发清除过程收集器线程都可以与用户线程一起工作，所以，从总体上来说，CMS收集器的内存回收过程是与用户线程一起并发执行的。

CMS是HotSpot虚拟机中第一款真正意义上的并发（Concurrent）收集器，它第一次实现了让垃圾收集线程与用户线程（基本上）同时工作。
缺点：
在并发阶段，它虽然不会导致用户线程停顿，但是会因为占用了一部分线程（或者说CPU资源）而导致应用程序变慢，总吞吐量会降低。

由于CMS并发清理阶段用户线程还在运行着，伴随程序运行自然就还会有新的垃圾不断产生，这一部分垃圾出现在标记过程之后，CMS无法在当次收集中处理掉它们，只好留待下一次GC时再清理掉。这一部分垃圾就称为“浮动垃圾”。

CMS收集器无法处理浮动垃圾（Floating Garbage），可能出现"Concurrent Mode Failure"失败而导致另一次Full GC的产生。

另外，由于CMS是一款基于“标记—清除”算法实现的收集器，这意味着收集结束时会有大量空间碎片产生。空间碎片过多时，将会给大对象分配带来很大麻烦，往往会出现老年代还有很大空间剩余，但是无法找到足够大的连续空间来分配当前对象，不得不提前触发一次Full GC。
## G1
与其他GC收集器相比，G1具备如下特点:

（1）并行与并发：

G1能充分利用多CPU、多核环境下的硬件优势，使用多个CPU（CPU或者CPU核心）来缩短Stop-The-World停顿的时间，部分其他收集器原本需要停顿Java线程执行的GC动作，G1收集器仍然可以通过并发的方式让Java程序继续执行。

（2）分代收集：

与其他收集器一样，分代概念在G1中依然得以保留。虽然G1可以不需要其他收集器配合就能独立管理整个GC堆，但它能够采用不同的方式去处理新创建的对象和已经存活了一段时间、熬过多次GC的旧对象以获取更好的收集效果。

（3）空间整合：

与CMS的“标记—清理”算法不同，G1从整体来看是基于“标记—整理”算法实现的收集器，从局部（两个Region之间）上来看是基于“复制”算法实现的，但无论如何，这两种算法都意味着G1运作期间不会产生内存空间碎片，收集后能提供规整的可用内存。这种特性有利于程序长时间运行，分配大对象时不会因为无法找到连续内存空间而提前触发下一次GC。

（4）可预测的停顿：

这是G1相对于CMS的另一大优势，降低停顿时间是G1和CMS共同的关注点，但G1除了追求低停顿外，还能建立可预测的停顿时间模型，能让使用者明确指定在一个长度为M毫秒的时间片段内，消耗在垃圾收集上的时间不得超过N毫秒，这几乎已经是实时Java（RTSJ）的垃圾收集器的特征了。




在G1之前的其他收集器进行收集的范围都是整个新生代或者老年代，而G1不再是这样。使用G1收集器时，Java堆的内存布局就与其他收集器有很大差别，它将整个Java堆划分为多个大小相等的独立区域（Region），虽然还保留有新生代和老年代的概念，但新生代和老年代不再是物理隔离的了，它们都是一部分Region（不需要连续）的集合。