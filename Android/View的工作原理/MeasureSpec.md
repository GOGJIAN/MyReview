MeasureSpec 代表一个 32 位 int 值，高 2 位 代表 SpecMode，低 30 位代表 SpecSize

SpecMode 有三种：

* UNSPECIFIED：父容器不对 View 有任何限制，一半用于系统内部目标是一种测量状态
* EXACTLY：精确大小，对应于match_parent和具体数值
* AT_MOST: 父容器制定了一个 SpecSize，View 的大小不能大于这个值，对应于 wrap_content

decorView 根据LayoutParams的参数生成MeasureSpec

* match_parent：精确模式，大小就是窗口大小
* wrap_content：大小不定，不能超过窗口大小
* 固定大小：大小为设置大小


parentSize为父容器的剩余空间大小

![](/Assets/普通View的MeasureSpec创建规则.jpg)
