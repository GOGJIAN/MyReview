**ViewModelProvider**

用来获取 ViewModel 的类

典型用法：
```java
ViewModelProvider(
        activity,
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )[XXXViewModel::class.java]
```

内部实现：

```java
public ViewModelProvider(@NonNull ViewModelStoreOwner owner, @NonNull Factory factory) {
        this(owner.getViewModelStore(), factory);
    }

public ViewModelProvider(@NonNull ViewModelStore store, @NonNull Factory factory) {
    mFactory = factory;
    mViewModelStore = store;
}
```



```java
public <T extends ViewModel> T get(@NonNull Class<T> modelClass) {
    String canonicalName = modelClass.getCanonicalName();
    if (canonicalName == null) {
        throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
    }
    return get(DEFAULT_KEY + ":" + canonicalName, modelClass);
}
```
```java
public <T extends ViewModel> T get(@NonNull String key, @NonNull Class<T> modelClass) {
    ViewModel viewModel = mViewModelStore.get(key);

    if (modelClass.isInstance(viewModel)) {
        if (mFactory instanceof OnRequeryFactory) {
            ((OnRequeryFactory) mFactory).onRequery(viewModel);
        }
        return (T) viewModel;
    } else {
        //noinspection StatementWithEmptyBody
        if (viewModel != null) {
            // TODO: log a warning.
        }
    }
    if (mFactory instanceof KeyedFactory) {
        viewModel = ((KeyedFactory) mFactory).create(key, modelClass);
    } else {
        viewModel = mFactory.create(modelClass);
    }
    mViewModelStore.put(key, viewModel);
    return (T) viewModel;
}
```

通过get方法进行获取 ViewModel,如果在 ViewModelStore 中存在该 ViewModel 的实例则返回，否则根据不同的 Factory 调用不同的方法进行创建然后添加到 ViewModelStore 中

**ViewModelStoreOwner** 接口


```java
public interface ViewModelStoreOwner {
    @NonNull
    ViewModelStore getViewModelStore();
}

```
实现了该接口的类具有提供 ViewModelStore 的能力，Activity 和 Fragment 都实现了该接口

**ViewModelStore**

ViewModel的存储类，内部使用一个HashMap来保存ViewModel

在 Activity 和 Fragment 销毁时（onDestory方法，在不是设置改变导致重启的情况下）会调用其clear方法进行清空

该类的clear会遍历去调用其中ViewModel的**clear**方法，然后清空map

在 put 时如果 key 存在对应的 ViewModel ，则会调用 ViewModel 的 onCleared 方法

**Factory**

ViewModelProvider 的内部类（接口）

```java
public interface Factory {
    @NonNull
    <T extends ViewModel> T create(@NonNull Class<T> modelClass);
}

static class OnRequeryFactory {
    void onRequery(@NonNull ViewModel viewModel) {
    }
}
```
ViewModel的生产工厂

常用的 **AndroidViewModelFactory**

是一个单例，继承于 **NewInstanceFactory** ，额外保存了Application

**SavedStateViewModelFactory**

创建时需要查询是否有对应的以 SavedStateHandle 为参数的构造方法

通过反射进行 ViewModel 的创建

**ViewModel**

**SavedStateHandle**
