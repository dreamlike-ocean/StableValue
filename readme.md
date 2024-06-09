两种可行高性能惰性单例的实现

### invokdynamic
已知 invokedynamic是线程安全的，MethodHandles.constant支持MH返回一个常量，那么结合起来如何呢？

在`java.lang.Class`中存在一个特殊的字段`classData`可以用于存放和这个类相关联的一个对象，当我们定一个hidden class时使用`lookup.defineHiddenClassWithClassData(classByteCode, factory, false)`即可对其进行赋值，当方法执行到对应的invokedynamic时我们使用`MethodHandles.classData(lookup, DEFAULT_NAME, Supplier.class)`再将其取出来，包裹下变成`ConstantCallSite`返回即可


### ConstantDynamic

ConstantDynamic是一个特殊的字节码，其储存在常量池中，当被load到栈顶时会执行BootstrapMethod，然后被jit将返回值缓存起来，之后再次调用时直接返回缓存的值，这个特性正好可以用于实现高性能的懒加载单例。

具体可以参考[JEP 309: Dynamic Class-File Constants](https://openjdk.org/jeps/309) 以及 [hands-on-java-constantdynamic](https://www.javacodegeeks.com/2018/08/hands-on-java-constantdynamic.html)

### 实现细节

请参考[issue的讨论](https://github.com/dreamlike-ocean/StableValue/issues/1)



### benchmark

可以执行下面的命令进行benchmark

```shell
sh ./benchmark.sh
```

在我本人的机子上可以跑出来如下结果

```text
并发线程数目为5
Benchmark                                                               Mode    Cnt        Score       Error  Units
stableValue.Benchmark.StableValueBenchmarkCase.testClassInit            thrpt   10  1998369.352 ± 32731.935  ops/s
stableValue.Benchmark.StableValueBenchmarkCase.testDCL                  thrpt   10   472392.857 ± 10495.215  ops/s
stableValue.Benchmark.StableValueBenchmarkCase.testIndyStabValue        thrpt   10  1996429.795 ± 32976.993  ops/s
stableValue.Benchmark.StableValueBenchmarkCase.testIndyStabValueCody    thrpt   10  1998819.221 ± 29030.771  ops/s
stableValue.Benchmark.StableValueBenchmarkCase.testIndyStabValueHidden  thrpt   10  2006979.049 ± 21622.126  ops/s
stableValue.Benchmark.StableValueBenchmarkCase.testPlain                thrpt   10  2015192.566 ± 16183.806  ops/s

```