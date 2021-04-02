**LiveData**

典型用法

```java
xLiveData.observe(this, Observer { doSomeThing(it) })
```

源码解析

```java
@MainThread
public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
    assertMainThread("observe");
    if (owner.getLifecycle().getCurrentState() == DESTROYED) {
        // ignore
        return;
    }

    LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);

    ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
    if (existing != null && !existing.isAttachedTo(owner)) {
        throw new IllegalArgumentException("Cannot add the same observer"
                + " with different lifecycles");
    }
    if (existing != null) {
        return;
    }
    owner.getLifecycle().addObserver(wrapper);
}

public interface Observer<T> {
    void onChanged(T t);
}

```

**LifecycleOwner**

可提供 LifecycleRegistry 的接口，在Activity ，Fragment ，LifeCycleService 中都有实现，用来获取当前的状态等

**LifecycleBoundObserver**

用来观察生命周期边界，判断当前页面是否处在活动状态等

RESUMED 和 STARTED是活动状态

继承自 **ObserverWrapper**

**ObserverWrapper** LiveData的内部类

主要是做一些状态改变时的回调操作，比如重新赋值 ```dispatchingValue```

下面看一下LiveData是如何的数据流是怎样的

```java
protected void setValue(T value) {
    assertMainThread("setValue");
    mVersion++;
    mData = value;
    dispatchingValue(null);
}

void dispatchingValue(@Nullable ObserverWrapper initiator) {
    if (mDispatchingValue) {
        mDispatchInvalidated = true;
        return;
    }
    mDispatchingValue = true;
    do {
        mDispatchInvalidated = false;
        if (initiator != null) {
            considerNotify(initiator);
            initiator = null;
        } else {
            for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                    mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                considerNotify(iterator.next().getValue());
                if (mDispatchInvalidated) {
                    break;
                }
            }
        }
    } while (mDispatchInvalidated);
    mDispatchingValue = false;
}

private void considerNotify(ObserverWrapper observer) {
    if (!observer.mActive) {
        return;
    }

    if (!observer.shouldBeActive()) {
        observer.activeStateChanged(false);
        return;
    }
    if (observer.mLastVersion >= mVersion) {
        return;
    }
    observer.mLastVersion = mVersion;
    observer.mObserver.onChanged((T) mData);
}

//ObserverWrapper
void activeStateChanged(boolean newActive) {
    if (newActive == mActive) {
        return;
    }
    // immediately set active state, so we'd never dispatch anything to inactive
    // owner
    mActive = newActive;
    boolean wasInactive = LiveData.this.mActiveCount == 0;
    LiveData.this.mActiveCount += mActive ? 1 : -1;
    if (wasInactive && mActive) {
        onActive();
    }
    if (LiveData.this.mActiveCount == 0 && !mActive) {
        onInactive();
    }
    if (mActive) {
        dispatchingValue(this);
    }
}
```

主要就是遍历mObservers，如果当前Activity处于活动状态，则调用其onChanged方法

否则这里将状态进行切换，然后什么都不做

等待下一次生命周期改变时再进行赋值和回调
