package com.gwb.superrecycleview.ui.wedgit

import android.animation.*
import android.content.Context
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.gwb.superrecycleview.R

/**
 * @author: yuhaibo
 * @time: 2018/12/2 上午10:42.
 * projectName: SuperRecycleView-master-master.
 * Description:
 */
class AddOrReduceView : FrameLayout {
    private val TIME: Long = 300   // 动画的执行时间
    var reduceLeft = 0
    var addLeft = 0
    var count = 0 //单个商品的数量
    var allCount = 0 //总共的商品数量
    var ivAdd: ImageView? = null
    // 贝塞尔曲线中间过程点坐标
    private val mCurrentPosition = FloatArray(2)

    constructor(context: Context) : this(context, null) {}

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView()
    }

    /**
     * 初始化view
     */
    private fun initView() {
        val view = View.inflate(context, R.layout.add_reduce_view, this)
        val ivReduce = view.findViewById<ImageView>(R.id.iv_goods_reduce)
        val tvCount = view.findViewById<TextView>(R.id.tv_goods_count)
        ivAdd = view.findViewById<ImageView>(R.id.iv_goods_add)
        //获取加号的位置
        ivAdd?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 获取增加图标的位置
                addLeft = ivAdd!!.left
                ivAdd!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        //获取减号的位置
        ivReduce.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // 获取减少图标的位置
                reduceLeft = ivReduce.left
                ivReduce.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        //加号点击事件
        ivAdd?.setOnClickListener {
            if (mAddListener != null) {
                count++
                allCount++
                if (count == 1) {
                    ivReduce.visibility = View.VISIBLE
                    animOpen(ivReduce)
                }
                tvCount.text = count.toString()
                mAddListener!!.onAddClick()
            }
        }
        //减号点击事件
        ivReduce.setOnClickListener {
            if (mReduceListener != null) {
                count--
                // 防止过快点击出现多个关闭动画
                when {
                    count == 0 -> {
                        animClose(ivReduce)
                        tvCount.text = ""
                        // 考虑到用户点击过快
                        allCount--
                    }
                    count < 0 -> // 防止过快点击出现商品数为负数
                        count = 0
                    else -> {
                        allCount--
                        tvCount.text = count.toString()
                    }
                }
                mReduceListener!!.onReduceClick()
            }
        }
    }

    /**
     * 打开动画
     */
    private fun animOpen(imageView: ImageView) {
        val animatorSet = AnimatorSet()
        val translationAnim = ObjectAnimator.ofFloat(imageView, "translationX", (addLeft - reduceLeft).toFloat(), 0f)
        val rotationAnim = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 180f)
        animatorSet.play(translationAnim).with(rotationAnim)
        animatorSet.setDuration(TIME).start()
    }

    /**
     * 关闭动画
     */
    private fun animClose(imageView: ImageView) {
        val animatorSet = AnimatorSet()
        val translationAnim = ObjectAnimator.ofFloat(imageView, "translationX", 0f, (addLeft - reduceLeft).toFloat())
        val rotationAnim = ObjectAnimator.ofFloat(imageView, "rotation", 0f, 180f)
        animatorSet.play(translationAnim).with(rotationAnim)
        animatorSet.setDuration(TIME).start()
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //  因为属性动画会改变位置,所以当结束的时候,要回退的到原来的位置,同时用补间动画的位移不好控制
                val oa = ObjectAnimator.ofFloat(imageView, "translationX", (addLeft - reduceLeft).toFloat(), 0f)
                oa.duration = 0
                oa.start()
                imageView.visibility = View.INVISIBLE
            }
        })
    }

    /**
     * 贝塞尔曲线动画
     */
    fun addGoods2CartAnim(context: Context, mCoordinatorLayout: ViewGroup, mIvShoppingCart: ImageView) {
        val goods = ImageView(context)
        goods.setImageResource(R.mipmap.icon_goods_add)
        val size = dp2px(context, 24f)
        val lp = ViewGroup.LayoutParams(size, size)
        goods.layoutParams = lp
        mCoordinatorLayout.addView(goods)
        // 控制点的位置
        val recyclerLocation = IntArray(2)
        mCoordinatorLayout.getLocationInWindow(recyclerLocation)
        // 加入点的位置起始点
        val startLocation = IntArray(2)
        ivAdd?.getLocationInWindow(startLocation)
        // 购物车的位置终点
        val endLocation = IntArray(2)
        mIvShoppingCart.getLocationInWindow(endLocation)
        // TODO: 2018/5/21 0021 考虑到状态栏的问题，不然会往下偏移状态栏的高度
        val startX = startLocation[0] - recyclerLocation[0]
        val startY = startLocation[1] - recyclerLocation[1]
        // TODO: 2018/5/21 0021 和上面一样
        val endX = endLocation[0] - recyclerLocation[0]
        val endY = endLocation[1] - recyclerLocation[1]
        // 开始绘制贝塞尔曲线
        val path = Path()
        // 移动到起始点位置(即贝塞尔曲线的起点)
        path.moveTo(startX.toFloat(), startY.toFloat())
        // 使用二阶贝塞尔曲线：注意第一个起始坐标越大，贝塞尔曲线的横向距离就会越大，一般按照下面的式子取即可
        path.quadTo(((startX + endX) / 2).toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())
        // mPathMeasure用来计算贝塞尔曲线的曲线长度和贝塞尔曲线中间插值的坐标，如果是true，path会形成一个闭环
        val pathMeasure = PathMeasure(path, false)
        // 属性动画实现（从0到贝塞尔曲线的长度之间进行插值计算，获取中间过程的距离值）
        val valueAnimator = ValueAnimator.ofFloat(0f, pathMeasure.length)
        // 计算距离
        val tempX = Math.abs(startX - endX)
        val tempY = Math.abs(startY - endY)
        // 根据距离计算时间
        val time = (0.3 * Math.sqrt((tempX * tempX + tempY * tempY).toDouble())).toInt()
        valueAnimator.duration = time.toLong()
        valueAnimator.start()
        valueAnimator.interpolator = AccelerateInterpolator()
        valueAnimator.addUpdateListener { animation ->
            // 当插值计算进行时，获取中间的每个值，
            // 这里这个值是中间过程中的曲线长度（下面根据这个值来得出中间点的坐标值）
            val value = animation.animatedValue as Float
            // 获取当前点坐标封装到mCurrentPosition
            // boolean getPosTan(float distance, float[] pos, float[] tan) ：
            // 传入一个距离distance(0<=distance<=getLength())，然后会计算当前距离的坐标点和切线，pos会自动填充上坐标，这个方法很重要。
            // mCurrentPosition此时就是中间距离点的坐标值
            pathMeasure.getPosTan(value, mCurrentPosition, null)
            // 移动的商品图片（动画图片）的坐标设置为该中间点的坐标
            goods.translationX = mCurrentPosition[0]
            goods.translationY = mCurrentPosition[1]
        }
        valueAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // 移除图片
                mCoordinatorLayout.removeView(goods)
                // 购物车数量增加
//                mTvShoppingCartCount.setText(allCount.toString())
            }
        })
    }

    /**
     * dp2px 转换
     */
    fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private var mAddListener: AddListener? = null
    private var mReduceListener: ReduceListener? = null

    fun setReduceListener(reduceListener: ReduceListener?) {
        this.mReduceListener = reduceListener
    }

    fun setAddListener(addListener: AddListener?) {
        this.mAddListener = addListener
    }

    /**
     * 加号的点击事件
     */
    interface AddListener {
        fun onAddClick()
    }

    /**
     * 减号的点击事件
     */
    interface ReduceListener {
        fun onReduceClick()
    }

}