一种可行高性能惰性单例的实现

已知 invokedynamic是线程安全的，MethodHandles.constant支持MH返回一个常量，那么结合起来如何呢？

生成时用一个key将对应初始化方法缓存起来，这个key可以作为常量写入字节码里面 然后indy的时候拿到这个key取出来对应的单例方法 包裹成methodhandle塞到callsite里面

可以执行下面的命令进行benchmark

```shell
sh ./benchmark.sh
```

在我本人的机子上可以跑出来如下结果

```text
并发线程数目为5
Benchmark                                      Mode  Cnt        Score       Error  Units
stableValue.Benchmark.Main.testClassInit      thrpt   10  2015409.176 ± 11239.498  ops/s
stableValue.Benchmark.Main.testDCL            thrpt   10   447095.024 ± 94378.846  ops/s
stableValue.Benchmark.Main.testIndyStabValue  thrpt   10  2000355.783 ± 33018.604  ops/s
stableValue.Benchmark.Main.testPlain          thrpt   10  2002660.831 ± 25841.030  ops/s

```