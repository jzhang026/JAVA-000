## Netty server
### `NettyServerApplication`
是我们server的入口类，里面建立一个我们自己定义的`HttpServer`. 绑到`8808` 端口，并run起来

### `HttpServer`

`run()`
启动了两个Nio eventGroup
并且配置我们的server
```java
ServerBootstrap b = new ServerBootstrap();
            b.option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024)
                    .option(ChannelOption.SO_SNDBUF, 32 * 1024)
                    .option(EpollChannelOption.SO_REUSEPORT, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
```

### `HttpInitializer`
这里比较关键的是
```java
b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO)).childHandler(new HttpInitializer(sslCtx));
```
第三句，childHandler里面传入我们自己的服务处理业务逻辑。
在这个initializer里面可以加入我们inbound的各种过滤器，以及真正的应用处理逻辑。

### `HttpHandler`

#### `channelRead()`
把我们的http请求体读进来。获得请求uri，匹配响应的请求处理函数。

做一下压测：
```
➜  ~ wrk -c 40 -d30s http://localhost:8808/test
Running 30s test @ http://localhost:8808/test
  2 threads and 40 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency   804.24us    1.23ms  67.96ms   99.63%
    Req/Sec    26.30k     2.74k   30.13k    80.73%
  1575662 requests in 30.10s, 163.79MB read
Requests/sec:  52343.06
Transfer/sec:      5.44MB
```
从吞吐量，可以感受到基于事件的reactor模型的强大之处。