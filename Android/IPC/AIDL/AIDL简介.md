### Binder
  1. 直观来说，Binder 是 Android 中的一个类，它实现了IBinder接口。
  2. 从 IPC 角度来说，Binder 是 Android 中的一种跨进程通信方式，Binder 还可以理解为一种虚拟的物理设备，它的驱动是 /dev/binder
  3. 从 Android Framework 角度来说，Binder 是 ServiceManager 链接各种 Manager(AM,WM等)，和相应MS的桥梁
  4. 从 Android 应用层来说， Binder 是客户端和服务端进行通信的媒介，当 bindService 的时候，会返回一个 Binder 对象，使用该对象可以获取服务端提供的服务或者数据，包括普通服务和基于 AIDL 的服务

### AIDL（Android Interface Definition Language）

使用时通过 aidl 文件生成所需要的的 java 接口类
