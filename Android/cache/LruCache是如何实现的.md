#### Lru算法

LRU(Least Recently Used)最近最少使用，是操作系统中常用的一种页面置换算法,也是各种缓存机制中常用的算法。

是一种以最近的过去预测最近的未来的一种算法，“最近使用过的还会再近期被使用”

如：一个缓存容量为 maxSize 的容器作为缓存，访问数据时：
1. 如果缓存中不存在该数据，直接添加到该容器中(比如头插)

    - 如果此时容器容量超过 maxSize ，则循环删除最后一个数据，直到不超过 maxSize 为止
2. 如果缓存中存在该数据，则将该数据移动到头部


#### 源码解析
LruCache 是在 android.util 包内，代码量不多

先从 get 和 put 两个最常用的方法入手
```
    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        V mapValue;
        synchronized (this) {
            mapValue = map.get(key);
            if (mapValue != null) {
                hitCount++;//命中次数
                return mapValue;
            }
            missCount++;//未命中次数
        }

        /*
         * Attempt to create a value. This may take a long time, and the map
         * may be different when create() returns. If a conflicting value was
         * added to the map while create() was working, we leave that value in
         * the map and release the created value.
         */

        V createdValue = create(key);
        if (createdValue == null) {
            return null;
        }

        synchronized (this) {
            createCount++;//创建次数
            mapValue = map.put(key, createdValue);

            if (mapValue != null) {
                // There was a conflict so undo that last put
                map.put(key, mapValue);
            } else {
                size += safeSizeOf(key, createdValue);
            }
        }

        if (mapValue != null) {
            entryRemoved(false, key, createdValue, mapValue);
            return mapValue;
        } else {
            trimToSize(maxSize);
            return createdValue;
        }
    }

```
1. 首先对 key 判空，不为空则加锁获取对应的 value ，如果存在直接返回
2. 如果不存在则尝试创建一个value，create(k) 默认返回空值，可重写来返回需要的默认值
3. 如果 create(k) 返回空值，则直接返回
4. 如果不为空，则再次加锁将该值存入，如果存储时该 key 存在对应的 value，则回滚这次操作
5. 不过该key不存在对应的 value ，并增加 size ，safeSizeOf 只是判断 sizeOf 返回值 是否小于 0，sizeOf(k,v)默认返回 1 ，可重写返回指定大小
6. 从临界区中出来后，如果 mapValue (存储时该 key 存在对应的 value) 不为空，则返回该值,entryRemoved 方法是一个空方法，在需要时进行重写
7. 如果 mapValue 为空，则 trimToSize(因为这时put了createValue进去，大小发生了改变) ，然后返回createValue；trimToSize 方法比较简单，就是不断删除最后一个数据，直到 size < maxSize 或者 map 为空


**get方法整体来说比较简单，主要是对 size 的处理较多，这里跟 Lru 看不出明显的关系，只是对 map 的 size 进行了控制，从这里也可以直到不但是可以对数据的缓存数量进行限制，也可以根据数据的总大小进行限制（重写sizeOf方法）**

```
    public final V put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        V previous;
        synchronized (this) {
            putCount++;
            size += safeSizeOf(key, value);
            previous = map.put(key, value);
            if (previous != null) {
                size -= safeSizeOf(key, previous);
            }
        }

        if (previous != null) {
            entryRemoved(false, key, previous, value);
        }

        trimToSize(maxSize);
        return previous;
    }
```

**put方法更简单，没什么可说的，也是主要对大小进行了控制，还有remove方法也是一样**

**到这里可以看出来 Lru 算法其实跟 LruCache 这个类只有一半的关系，这个类主要对map进行了大小的控制，而另一半就在这个map中**

```
this.map = new LinkedHashMap<K, V>(0, 0.75f, true);
```

可以看到，这里是使用了LinkedHashMap来实现的另一半，前两个参数为初始大小和负载因子，重点是第三个参数

“the ordering mode - true for access-order, false for insertion-order”

按照访问顺序排序或者插入顺序排序，刚访问的放在尾部


**这里应该有一个疑问，尾插、尾删、向尾移动，是如何保证LRU的？**

**是的，他不能，而且会导致两个问题**

1. 达到maxSize后，新的数据无法插入（刚插入就会被删掉）
2. 缩容（resize）会删掉最近访问的数据

trimToSize方法中的这一段会找出最后一个节点，随后会删掉这个节点，这里如果将注释的break打开(原本没有这一行)，则会使用第一个节点，实现头删
就达到了LRU真正的功能

```
Map.Entry<K, V> toEvict = null;
for (Map.Entry<K, V> entry : map.entrySet()) {
    toEvict = entry;
    //break;
}

```


可以使用下面的代码自行验证
```
fun main(args : Array<String>){
    val cache = LruCache<Int,Int>(3)
    cache.put(1,1)
    cache.put(2,2)
    cache.put(3,3)

    val t = cache.get(1)

    cache.resize(2)
    cache.put(4,4)

    println()
}
```
如果未修改代码，cache中最后会保存2,3两个节点
如果修改了代码，cache中最后会保存1,4两个节点
