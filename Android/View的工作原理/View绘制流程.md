## Measure

### MeasureSpec

MeasureSpec 代表一个 32 位 int 值，高 2 位 代表 SpecMode，低 30 位代表 SpecSize

SpecMode 有三种：

* UNSPECIFIED：父容器不对 View 有任何限制，一半用于系统内部目标是一种测量状态
* EXACTLY：精确大小，对应于match_parent和具体数值
* AT_MOST: 父容器制定了一个 SpecSize，View 的大小不能大于这个值，对应于 wrap_content

decorView 根据LayoutParams的参数生成MeasureSpec

* match_parent：精确模式，大小就是窗口大小
* wrap_content：大小不定，不能超过窗口大小
* 固定大小：大小为设置大小

### View 的 measure

View 的 measure 由其 measure 方法完成，这是一个 final 方法，在其中调用了 onMeasure 方法，Measure 由其 parent 传来

```
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
            getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
}
```

setMeasuredDimension 方法会设置 View 的测量大小

```
public static int getDefaultSize(int size, int measureSpec) {
    int result = size;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);

    switch (specMode) {
    case MeasureSpec.UNSPECIFIED:
        result = size;
        break;
    case MeasureSpec.AT_MOST:
    case MeasureSpec.EXACTLY:
        result = specSize;
        break;
    }
    return result;
}
```

**getDefaultSize**

AT_MOST 和 EXACTLY 两种模式都是直接返回 specSize 作为测量大小
Ps：View 的大小在 layout 阶段确定，但是一般情况下测量大小都和最终大小相等

UNSPECIFIED 模式返回 size ，getSuggestedMinimumWidth() 的返回值

* 如果 View 没有背景，返回 android:minWidth 指定的值，如果没有指定则为 0
* 如果设置了背景， 则返回 max(mMinWidth, mBackground.getMinimumWidth())
  * getMinimumWidth 返回 Drawable 的原始宽度

**所以自定义 View 时需要重写 onMeasure 方法，否则 wrap_content 和 match_parent 的作用相同**

### ViewGroup 的 measure

ViewGroup 是一个抽象类，没有重写 onMeasure 方法，不同的 ViewGroup 具有不同的测量行为

ViewGroup 除了测量自己，还需要调用其子元素的测量方法

```
protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
    final int size = mChildrenCount;
    final View[] children = mChildren;
    for (int i = 0; i < size; ++i) {
        final View child = children[i];
        if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
    }
}
```
只要可见性不是 GONE 的子元素都需要测量其大小

```
protected void measureChild(View child, int parentWidthMeasureSpec,
        int parentHeightMeasureSpec) {
    final LayoutParams lp = child.getLayoutParams();

    final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
            mPaddingLeft + mPaddingRight, lp.width);
    final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
            mPaddingTop + mPaddingBottom, lp.height);

    child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
}
```
获取子类的 MeasureSpec 然后调用子类的 measure 方法

这个方法没有计算 Margin ，有对应的 measureChildWithMargins 方法处理 margin

getChildMeasureSpec() 逻辑如下图所示：
parentSize为父容器的剩余空间大小

![](/Assets/普通View的MeasureSpec创建规则.jpg)

## Layout

layout 首先调用 setFrame 设置 l,t,r,b 四个边的位置，然后调用 onLayout 对子元素进行布局

## Draw

Draw 分为 4 个过程：
1. 绘制背景 background.draw
2. 绘制自己 onDraw
3. 绘制子元素 dispatchDraw
4. 绘制装饰 onDrawScrollBars
