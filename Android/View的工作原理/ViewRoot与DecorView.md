ViewRoot对应于ViewRootImpl类，是连接WindowManager和DecorView的纽带，View的三大流程都是由ViewRoot完成。

在ActivtiyThread中，Activity对象被创建完毕后，会将DecorView添加到Window中，同时会创建ViewRootImpl对象，并将该对象与DecorView建立关联

![](/Assets/view_flow.jpg)

* measure过程决定了View的宽/高，完成后可获取到测量宽高
* layout决定了View 的上下左右边界和实际宽高
* draw之后View才能显示


 <center> <img src ="/Assets/decorView.jpg"> </center>
