###简介
Messenger是较为简单的进程间通信方式，底层是AIDL，单线程处理，发送Message类型的消息

###通信模型

![messenger_construct](/Assets/messenger_construct.jpg)

###实例

用一个例子来演示Messenger的使用，客户端发送消息，服务端收到后返回一条消息

####服务端代码

```java
class MessengerService : Service() {

  companion object{
      var TAG = "MessengerService"

      class MessengerHandler : Handler(){
          override fun handleMessage(msg: Message) {
              when(msg.what){
                  Constants.MSG_FROM_CLIENT -> {
                      Log.d(TAG,"receive msg from client:${msg.data.getString("msg")}")
                      //从msg中获取到relpyTo，本身也是个Messenger，使用这个实例进行消息的发送
                      val messenger = msg.replyTo
                      val m = Message.obtain(null,Constants.MSG_FROM_SERVER)
                      val data = Bundle()
                      data.putString("msg","收到！")
                      m.data = data

                      try {
                          messenger.send(m)
                      }catch (e: Exception){
                          e.printStackTrace()
                      }
                  }
                  else -> super.handleMessage(msg)
              }
          }
      }
  }

  private val mMessenger = Messenger(MessengerHandler())

  override fun onBind(intent: Intent?): IBinder? {
      return mMessenger.binder
  }
}
```

####客户端代码
```java
class MainActivity : AppCompatActivity() {
  companion object{
      const val TAG = "MessengerActivity"

      class MessengerHandler : Handler(){
          override fun handleMessage(msg: Message) {
              when(msg.what){
                  Constants.MSG_FROM_SERVER -> {
                      Log.d(TAG, "receive msg from server:${msg.data.getString("msg")}")
                  }
                  else -> super.handleMessage(msg)
              }
          }
      }
  }

  private lateinit var mService:Messenger

  //传递给服务端使用的Messenger
  private val mGetReplyMessenger = Messenger(MessengerHandler())

  //绑定服务的回调
  private val mConnection:ServiceConnection = object :ServiceConnection{
      override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
          mService = Messenger(service)
          sendMessage("msg", "hello,this is client")
      }

      override fun onServiceDisconnected(name: ComponentName?) {

      }
  }

  fun sendMessage(key: String, value: String){
      val msg = Message.obtain(null, Constants.MSG_FROM_CLIENT)
      val data = Bundle()
      data.putString(key, value)
      msg.data = data
      msg.replyTo = mGetReplyMessenger
      try {
          mService.send(msg)
      }catch (e: Exception){
          e.printStackTrace()
      }
  }


  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.layout.activity_main)

      val intent = Intent()
      intent.setClass(this,MessengerService::class.java)
      //如果需要使用跨应用的多进程，原理上是一样的
//        intent.component = ComponentName("第一个应用的包名", "第一个应用的包名+Service名称")
      bindService(intent, mConnection, Context.BIND_AUTO_CREATE)

      //点击发送
      tv_hello.setOnClickListener {
          sendMessage("msg", "hello,this is client")
      }
  }
}
```

###结语
Messenger的简单使用就是这样，在实际使用时还需要注意处理服务端的鉴权，binder死亡的处理等，而且Messenger本身是对于AIDL的封装，如果需要处理更复杂的功能还是需要直接使用AIDL
