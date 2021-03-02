ThreadLocal 是一个线程内部的数据存储类，可以在线程内存存储数据，存储以后只有在指定线程中能获取到数据，其他线程是无法获取到的

#### 用法举例
```
public class Main {

    public static void main(String[] args) {
        // write your code here
        ThreadLocal<Integer> threadLocal = new ThreadLocal<>();
        threadLocal.set(0);
        System.out.println("main thread " + threadLocal.get());

        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("1 thread " + threadLocal.get());
                threadLocal.set(1);
                System.out.println("1 thread " + threadLocal.get());
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("2 thread " + threadLocal.get());
            }
        }).start();

        System.out.println("main thread " + threadLocal.get());
    }
}

输出

main thread 0
1 thread null
1 thread 1
main thread 0
2 thread null
```

从上面的代码和输出可以看出 主线程、线程1、线程2  三个线程中均使用了同一个threadLocal，但是其值却互不影响（毫无关系）

Thread是如何做到这一点的呢

看一下其get和set方法

```
public T get() {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        ThreadLocalMap.Entry e = map.getEntry(this);
        if (e != null) {
            @SuppressWarnings("unchecked")
            T result = (T)e.value;
            return result;
        }
    }
    return setInitialValue();
}

ThreadLocalMap getMap(Thread t) {
    return t.threadLocals;
}

private T setInitialValue() {
    T value = initialValue();//默认返回空
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        map.set(this, value);
    } else {
        createMap(t, value);
    }
    if (this instanceof TerminatingThreadLocal) {
        TerminatingThreadLocal.register((TerminatingThreadLocal<?>) this);
    }
    return value;
}
```
get 方法比较简单，首先获取到当前线程，然后通过当前线程拿到其中的ThreadLocalMap，这是一个 Thread 的成员变量，每个线程都有一份

ThreadLocalMap 是 ThreadLocal 中的一个静态内部类，这是一个以 ThreadLocal 为key的Map （并没有继承Map），实际上数据存储在ThreadLocalMap的一个Entry数组中，需要获取时通过 key.threadLocalHashCode & (table.length - 1) 计算出下标获取对应的Entry
如果计算到的下表未获取到正确的值，则说明产生了hash碰撞，这里使用了线性探测来进行查找，在查找过程中会顺便删除key为空（被回收了）的Entry，删除后会将该下标的下一个下标到下一个 entry 为空的下标之间的entry进行再hash

```
private Entry getEntry(ThreadLocal<?> key) {
    int i = key.threadLocalHashCode & (table.length - 1);
    Entry e = table[i];
    if (e != null && e.get() == key)
        return e;
    else
        return getEntryAfterMiss(key, i, e);
}

private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
    Entry[] tab = table;
    int len = tab.length;

    while (e != null) {
        ThreadLocal<?> k = e.get();
        if (k == key)
            return e;
        if (k == null)
            expungeStaleEntry(i);
        else
            i = nextIndex(i, len);
        e = tab[i];
    }
    return null;
}

private static int nextIndex(int i, int len) {
    return ((i + 1 < len) ? i + 1 : 0);
}
```

如果 ThreadLocalMap 为空或者取得的 Entry 为空则创建该对象并放入一个 key 为当前 ThreadLocal 的空值，并返回 null

Entry 是 ThreadLocalMap 的一个静态内部类，

```
/**
   * The entries in this hash map extend WeakReference, using
   * its main ref field as the key (which is always a
   * ThreadLocal object).  Note that null keys (i.e. entry.get()
   * == null) mean that the key is no longer referenced, so the
   * entry can be expunged from table.  Such entries are referred to
   * as "stale entries" in the code that follows.
   */
  static class Entry extends WeakReference<ThreadLocal<?>> {
      /** The value associated with this ThreadLocal. */
      Object value;

      Entry(ThreadLocal<?> k, Object v) {
          super(k);
          value = v;
      }
  }
```

该类继承自 WeakReference<ThreadLocal<?>> 防止有可能产生的 ThreadlLocal 内存泄漏，同时该类中还保存了对应的value

```
public void set(T value) {
    Thread t = Thread.currentThread();
    ThreadLocalMap map = getMap(t);
    if (map != null) {
        map.set(this, value);
    } else {
        createMap(t, value);
    }
}
```

set 方法本身也比较简单，这里主要看一下 map.set 方法

```
private void set(ThreadLocal<?> key, Object value) {

    // We don't use a fast path as with get() because it is at
    // least as common to use set() to create new entries as
    // it is to replace existing ones, in which case, a fast
    // path would fail more often than not.

    Entry[] tab = table;
    int len = tab.length;
    int i = key.threadLocalHashCode & (len-1);

    for (Entry e = tab[i];
         e != null;
         e = tab[i = nextIndex(i, len)]) {
        ThreadLocal<?> k = e.get();

        if (k == key) {
            e.value = value;
            return;
        }

        if (k == null) {
            replaceStaleEntry(key, value, i);
            return;
        }
    }

    tab[i] = new Entry(key, value);
    int sz = ++size;
    if (!cleanSomeSlots(i, sz) && sz >= threshold)
        rehash();
}
```

set 方法同样使用线性探测解决hash冲突

* 如果当前 Entry 为空，则直接赋值
* 如果 key 相同，则覆盖 value
* 如果该下表原本的 Entry 不为空，但是 key 为空，则替换


最后如果存储的下标到size+1之间存在key为空的值，并且 size+1 大于扩容阈值（数组长度的 2/3）,则进行全体再hash，删除所有 stale 节点（Entry不为空，key为空），完成后如果size仍然大于阈值的3/4 ，则进行扩容（翻倍）
