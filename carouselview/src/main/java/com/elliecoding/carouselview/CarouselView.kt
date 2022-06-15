package com.elliecoding.carouselview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.elliecoding.carouselview.control.CarouselLinearLayoutManager
import com.elliecoding.carouselview.control.CarouselSnapHelper
import com.elliecoding.carouselview.control.CarouselViewAdapter
import com.elliecoding.carouselview.enums.IndicatorAnimationType
import com.elliecoding.carouselview.enums.OffsetType
import com.rd.PageIndicatorView
import com.rd.animation.type.AnimationType

// This is a library, external consumers will use the public methods
@Suppress("unused", "MemberVisibilityCanBePrivate")
class CarouselView : FrameLayout {
    private lateinit var indicatorView: PageIndicatorView
    private lateinit var carouselRecyclerView: RecyclerView
    private var layoutManager: CarouselLinearLayoutManager? = null
    private var offsetType: OffsetType? = null
    private var snapHelper: SnapHelper? = null
    private val autoPlayHandler: Handler by lazy {
        Handler(Looper.myLooper()!!)
    }

    var carouselViewListener: CarouselViewListener? = null
    var carouselScrollListener: CarouselScrollListener? = null

    /**
     * Animation type for the indicator below the carousel. Defaults to
     * [IndicatorAnimationType.SLIDE].
     */
    var indicatorAnimationType: IndicatorAnimationType = IndicatorAnimationType.SLIDE
        set(indicatorAnimationType) {
            field = indicatorAnimationType
            when (indicatorAnimationType) {
                IndicatorAnimationType.DROP -> indicatorView.setAnimationType(AnimationType.DROP)
                IndicatorAnimationType.FILL -> indicatorView.setAnimationType(AnimationType.FILL)
                IndicatorAnimationType.NONE -> indicatorView.setAnimationType(AnimationType.NONE)
                IndicatorAnimationType.SWAP -> indicatorView.setAnimationType(AnimationType.SWAP)
                IndicatorAnimationType.WORM -> indicatorView.setAnimationType(AnimationType.WORM)
                IndicatorAnimationType.COLOR -> indicatorView.setAnimationType(AnimationType.COLOR)
                IndicatorAnimationType.SCALE -> indicatorView.setAnimationType(AnimationType.SCALE)
                IndicatorAnimationType.SLIDE -> indicatorView.setAnimationType(AnimationType.SLIDE)
                IndicatorAnimationType.THIN_WORM -> indicatorView.setAnimationType(AnimationType.THIN_WORM)
                IndicatorAnimationType.SCALE_DOWN -> indicatorView.setAnimationType(AnimationType.SCALE_DOWN)
            }
        }

    var enableSnapping = false

    /**
     * Enables or disables autoplay. Changes to this field will be applied immediately, such that
     * you can toggle the feature on or off at any time.
     */
    var autoPlay = false
        set(value) {
            if (autoPlay != value) {
                field = value
                verifyAutoPlay()
            }
        }

    /**
     * Long millisecond value for the time it takes before the autoplay will continue to the next
     * carousel item. It is recommended that you use [java.util.concurrent.TimeUnit.toMillis] to set
     * this value for readability. Setting this to any value <= 0 will set `[autoPlay] = false`
     */
    var autoPlayDelayMillis = 0L
        set(value) {
            field = value
            if (value <= 0L) {
                autoPlay = false
            }
        }
    var scaleOnScroll = false
    var resource = 0
    var size = 0
        set(size) {
            field = size
            indicatorView.count = size
        }
    var spacing = 0
    var currentItem = 0
        set(item) {
            field = if (item < 0) {
                0
            } else if (item >= size) {
                size - 1
            } else {
                item
            }
            carouselRecyclerView.smoothScrollToPosition(currentItem)
        }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attributeSet: AttributeSet?) {
        val inflater = LayoutInflater.from(context)
        val carouselView = inflater.inflate(R.layout.view_carousel, this)
        carouselRecyclerView = carouselView.findViewById(R.id.carousel_recycler_view)
        indicatorView = carouselView.findViewById(R.id.page_indicator_view)
        carouselRecyclerView.setHasFixedSize(false)
        initializeAttributes(attributeSet)
    }

    private fun initializeAttributes(attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val attributes = context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.CarouselView, 0, 0
            )
            enableSnapping = attributes.getBoolean(R.styleable.CarouselView_enableSnapping, true)
            scaleOnScroll =
                attributes.getBoolean(R.styleable.CarouselView_scaleOnScroll, false)
            autoPlay = attributes.getBoolean(R.styleable.CarouselView_setAutoPlay, false)
            autoPlayDelayMillis =
                attributes.getInteger(R.styleable.CarouselView_setAutoPlayDelay, 2500).toLong()
            carouselOffset =
                getOffset(attributes.getInteger(R.styleable.CarouselView_carouselOffset, 0))
            val resourceId = attributes.getResourceId(R.styleable.CarouselView_resource, 0)
            if (resourceId != 0) {
                resource = resourceId
            }
            val indicatorSelectedColorResourceId =
                attributes.getColor(R.styleable.CarouselView_indicatorSelectedColor, 0)
            val indicatorUnselectedColorResourceId =
                attributes.getColor(R.styleable.CarouselView_indicatorUnselectedColor, 0)
            if (indicatorSelectedColorResourceId != 0) {
                indicatorSelectedColor = indicatorSelectedColorResourceId
            }
            if (indicatorUnselectedColorResourceId != 0) {
                indicatorUnselectedColor = indicatorUnselectedColorResourceId
            }
            indicatorAnimationType = this.getAnimation(
                attributes.getInteger(
                    R.styleable.CarouselView_indicatorAnimationType,
                    0
                )
            )
            indicatorRadius =
                attributes.getInteger(R.styleable.CarouselView_indicatorRadius, 5)
            indicatorPadding =
                attributes.getInteger(R.styleable.CarouselView_indicatorPadding, 5)
            size = attributes.getInteger(R.styleable.CarouselView_size, 0)
            spacing = attributes.getInteger(R.styleable.CarouselView_spacing, 0)
            attributes.recycle()
        }
    }

    fun hideIndicator(hide: Boolean) {
        if (hide) {
            indicatorView.visibility = GONE
        } else {
            indicatorView.visibility = VISIBLE
        }
    }

    override fun onDetachedFromWindow() {
        autoPlay = false
        carouselRecyclerView.clearOnScrollListeners()
        super.onDetachedFromWindow()
    }

    private fun setAdapter() {
        layoutManager = CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        layoutManager!!.isOffsetStart(carouselOffset == OffsetType.START)
        if (scaleOnScroll) layoutManager!!.setScaleOnScroll(true)
        carouselRecyclerView.layoutManager = layoutManager
        carouselRecyclerView.adapter = CarouselViewAdapter(
            carouselViewListener,
            resource,
            size,
            carouselRecyclerView,
            spacing,
            carouselOffset == OffsetType.CENTER
        )
        if (enableSnapping) {
            carouselRecyclerView.onFlingListener = null
            snapHelper!!.attachToRecyclerView(carouselRecyclerView)
        }
        setScrollListener()
    }

    private fun setScrollListener() {
        carouselRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (layoutManager != null && snapHelper != null) {
                    val centerView = snapHelper!!.findSnapView(layoutManager)
                    if (centerView != null) {
                        setPosition(centerView, recyclerView, newState)
                    }
                }
            }

            private fun setPosition(
                centerView: View,
                recyclerView: RecyclerView,
                newState: Int
            ) {
                val position = layoutManager!!.getPosition(centerView)
                if (carouselScrollListener != null) {
                    carouselScrollListener!!.onScrollStateChanged(
                        recyclerView,
                        newState,
                        position
                    )
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    indicatorView.selection = position
                    currentItem = position
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (carouselScrollListener != null) {
                    carouselScrollListener!!.onScrolled(recyclerView, dx, dy)
                }
            }
        })
    }

    private val autoPlayRunnable = object : Runnable {
        override fun run() {
            if (autoPlay) {
                currentItem = if (size - 1 == currentItem) {
                    0
                } else {
                    currentItem + 1
                }
                autoPlayHandler.postDelayed(this, autoPlayDelayMillis)
            }
        }
    }

    private fun verifyAutoPlay() {
        if (autoPlay) {
            enableAutoPlay()
        } else {
            disableAutoPlay()
        }
    }

    private fun enableAutoPlay() {
        autoPlayHandler.removeCallbacks(autoPlayRunnable)
        autoPlayHandler.postDelayed(autoPlayRunnable, autoPlayDelayMillis)
    }

    private fun disableAutoPlay() {
        autoPlayHandler.removeCallbacks(autoPlayRunnable)
    }

    private var carouselOffset: OffsetType?
        get() = offsetType
        set(offsetType) {
            this.offsetType = offsetType
            snapHelper = when (offsetType) {
                OffsetType.CENTER -> LinearSnapHelper()
                OffsetType.START -> CarouselSnapHelper()
                else -> throw IllegalStateException("Unknown offsetType $offsetType")
            }
        }
    private var indicatorRadius: Int
        get() = indicatorView.radius
        set(radius) {
            indicatorView.radius = radius
        }
    private var indicatorPadding: Int
        get() = indicatorView.padding
        set(padding) {
            indicatorView.padding = padding
        }
    private var indicatorSelectedColor: Int
        get() = indicatorView.selectedColor
        set(color) {
            indicatorView.selectedColor = color
        }
    private var indicatorUnselectedColor: Int
        get() = indicatorView.unselectedColor
        set(color) {
            indicatorView.unselectedColor = color
        }

    private fun validate() {
        check(resource != 0) { "Please add a resource layout to populate the CarouselView" }
    }

    private fun getAnimation(value: Int): IndicatorAnimationType {
        val animationType: IndicatorAnimationType = when (value) {
            1 -> IndicatorAnimationType.FILL
            2 -> IndicatorAnimationType.DROP
            3 -> IndicatorAnimationType.SWAP
            4 -> IndicatorAnimationType.WORM
            5 -> IndicatorAnimationType.COLOR
            6 -> IndicatorAnimationType.SCALE
            7 -> IndicatorAnimationType.SLIDE
            8 -> IndicatorAnimationType.THIN_WORM
            9 -> IndicatorAnimationType.SCALE_DOWN
            0 -> IndicatorAnimationType.NONE
            else -> IndicatorAnimationType.NONE
        }
        return animationType
    }

    private fun getOffset(value: Int): OffsetType {
        val offset: OffsetType = when (value) {
            1 -> OffsetType.CENTER
            0 -> OffsetType.START
            else -> OffsetType.START
        }
        return offset
    }

    fun show() {
        validate()
        setAdapter()
    }
}
