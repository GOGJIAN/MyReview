#### 简介
LinkedHashMap，顾名思义，是链表加HashMap的结构，这里采用了双向链表

在 java.util 包下，继承于HashMap，对比其增加了按照访问顺序排序的能力

#### 源码解析


map最常用的就是put，get两个方法

首先从put入手，LinkedHashMap 没有实现自己的 put 方法，而直接使用了 HashMap 的 put 方法，但是 HashMap 是无序的，那么LinkedHashMap如何维护插入顺序呢

有一个不起眼的方法，在 list 中 尾插一个节点

```
// link at the end of list
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;
        tail = p;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
    }
```
调用在 newNode 方法中
```
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }
```

而这个方法是重写了 HashMap 的 同名方法，在 HashMap 的 put 方法中会进行调用，也就是说在插入时直接默认就是按照插入顺序排序的，这里是尾插

官方注释中也指明了这个方法也是专门为了LinkedHashMap而设计的，而不是其他子类


然后时 get 方法
```
public V get(Object key) {
    Node<K,V> e;
    if ((e = getNode(key)) == null)
        return null;
    if (accessOrder)
        afterNodeAccess(e);
    return e.value;
}
```

这个方法本身代码不多，getNode(k) 是HashMap中的方法，这里不做讨论，就是通过k获取v的方法

afterNodeAccess(e) 方法是实现访问顺序排序的精髓，源码如下

```
void afterNodeAccess(Node<K,V> e) { // move node to last
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a != null)
            a.before = b;
        else
            last = b;
        if (last == null)
            head = p;
        else {
            p.before = last;
            last.after = p;
        }
        tail = p;
        ++modCount;
    }
}
```
这个方法官方也有给一句注释，“move node to last”，这里使用的也是典型的链表移动节点的方法，需要考虑到链表为空，当前节点在头节点的特殊情况，因为创建节点时是尾插，所以这里要移动到尾部，保证尾部是最新插入或者最新访问的


下面这个方法是HashMap的一个回调方法，在插入某个节点时调用，有可能会删除最旧的一个节点
```
void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }
```
判断条件里的第一个 evict 只要不是在初始化 map 时均为true
第三个默认返回false，也就是说在直接使用该类时，永远都不会删除旧节点

removeEldestEntry 方法可以被子类重写，如果仅修改这个方法，那在不修改sizeOf方法时，也可以实现LRU，只需要在这个方法中判断当前大小是否大于maxSize即可，但如果修改了sizeOf方法就不再可行，因为这时候每次不一定删除多少个节点，而该方法只会删除一个节点

```
在删除一个节点后将其从双向链表中删除
void afterNodeRemoval(Node<K,V> e) { // unlink
    LinkedHashMap.Entry<K,V> p =
        (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
    p.before = p.after = null;
    if (b == null)
        head = a;
    else
        b.after = a;
    if (a == null)
        tail = b;
    else
        a.before = b;
}
```

Ps：这里学习的时候有一个疑问，如果只需要尾插头删的话，保存头尾节点的单链表也可以做到，为什么需要使用双链表，因为在删除尾结点的时候无法找到上一个节点，无法对链表进行维护，删除中间节点时有办法可以在 O(1) 时间复杂度做到，只需将需要删除的节点中的数据替换为下一个节点，然后再删除下一个节点即可
