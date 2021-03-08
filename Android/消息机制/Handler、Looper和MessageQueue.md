#### MessageQueue

MessageQueue 有两个主要功能 插入和读取，读取伴随着删除

通过单链表来存储数据，因为在插入和删除上有优势

```
boolean enqueueMessage(Message msg, long when) {
    if (msg.target == null) {
        throw new IllegalArgumentException("Message must have a target.");
    }
    if (msg.isInUse()) {
        throw new IllegalStateException(msg + " This message is already in use.");
    }

    synchronized (this) {
        if (mQuitting) {
            IllegalStateException e = new IllegalStateException(
                    msg.target + " sending message to a Handler on a dead thread");
            Log.w(TAG, e.getMessage(), e);
            msg.recycle();
            return false;
        }

        msg.markInUse();
        msg.when = when;
        Message p = mMessages;
        boolean needWake;
        if (p == null || when == 0 || when < p.when) {
            // New head, wake up the event queue if blocked.
            msg.next = p;
            mMessages = msg;
            needWake = mBlocked;
        } else {
            // Inserted within the middle of the queue.  Usually we don't have to wake
            // up the event queue unless there is a barrier at the head of the queue
            // and the message is the earliest asynchronous message in the queue.
            needWake = mBlocked && p.target == null && msg.isAsynchronous();
            Message prev;
            for (;;) {
                prev = p;
                p = p.next;
                if (p == null || when < p.when) {
                    break;
                }
                if (needWake && p.isAsynchronous()) {
                    needWake = false;
                }
            }
            msg.next = p; // invariant: p == prev.next
            prev.next = msg;
        }

        // We can assume mPtr != 0 because mQuitting is false.
        if (needWake) {
            nativeWake(mPtr);
        }
    }
    return true;
}
```
这个方法在 Handler 中调用，发送消息最后都会调用到这个方法
逻辑比较简单，如果 when 为 0 ，则插在第一条，否则插入到第一个比 when 大的 Message 之前


next 内部是一个死循环，如果没有消息，则一直阻塞，如果有消息，则返回这条消息

#### Looper
Looper 会不断的查看 MessageQueue 中是否有消息，如果有则立刻处理，如果没有则阻塞

```

private static void prepare(boolean quitAllowed) {
    if (sThreadLocal.get() != null) {
        throw new RuntimeException("Only one Looper may be created per thread");
    }
    sThreadLocal.set(new Looper(quitAllowed));
}

private Looper(boolean quitAllowed) {
    mQueue = new MessageQueue(quitAllowed);
    mThread = Thread.currentThread();
}
```
Looper 的构造方法中会创建一个 MessageQueue，并将当前线程保存起来，Looper 在 Looper.prepare 方法中创建，调用 Looper.loop() 开启循环

Looper 提供了quit 和 quitSafety 方法，quit 会立即退出，quitSafety 会等待消息处理完毕后退出，Looper 退出后线程会立即结束，否则线程会等待

子线程应该及时终止 Looper

Looper.loop 方法是一个死循环，唯一的退出条件是 next 返回空，而只有Looper 调用 quit 或者 quitSafety 方法 MessageQueue 才会返回空，也就是说 Looper 必须退出，loop 方法才会停止

在获取到 msg 后，使用 msg.target.dispatchMessage(msg); 来处理，msg.target 就是发送 msg的 Handler ，这样就又交给 Handler 处理了，而这个方法是在创建 Handler 时绑定的 Looper 中执行的，就起到了切换线程的作用

#### Handler
Handler 主要功能也就是发送消息和处理消息

发送消息主要是 post 和 send 方法，最终都会调用到 MessageQueue.enqueueMessage 方法

处理消息就是 dispatchMessage(msg) 方法

```
public void dispatchMessage(Message msg) {
    if (msg.callback != null) {
        handleCallback(msg);
    } else {
        if (mCallback != null) {
            if (mCallback.handleMessage(msg)) {
                return;
            }
        }
        handleMessage(msg);
    }
}

private static void handleCallback(Message message) {
    message.callback.run();
}

public interface Callback {
    /**
     * @param msg A {@link android.os.Message Message} object
     * @return True if no further handling is desired
     */
    public boolean handleMessage(Message msg);
}
```

优先级
1. msg.callback  就是post方法中的runnable参数
2. mCallback Handler(Callback callback, boolean async) 构造函数中传入的Callback参数
3. handleMessage 方法，重写Handler时重写的方法

![](/Assets/handler消息处理.jpg)


#### 内存泄漏

Message 持有 Handler  ，Handler 持有  Activity

如果存在未处理完的Message，就会导致Activity无法回收

使用静态类和弱引用进行处理
