package com.elliecoding.carouselview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.elliecoding.carouselview.enums.IndicatorAnimationType
import com.elliecoding.carouselview.enums.OffsetType
import com.rd.PageIndicatorView
import com.rd.animation.type.AnimationType


@Suppress("unused") // This is a library, external consumers will use the public methods
class CarouselView : FrameLayout {
    private var pageIndicatorView: PageIndicatorView? = null
    private var carouselRecyclerView: RecyclerView? = null
    private var layoutManager: CarouselLinearLayoutManager? = null
    private var carouselViewListener: CarouselViewListener? = null
    var carouselScrollListener: CarouselScrollListener? = null
    private var indicatorAnimationType: IndicatorAnimationType? = null
        set(indicatorAnimationType) {
            field = indicatorAnimationType
            when (indicatorAnimationType) {
                IndicatorAnimationType.DROP -> pageIndicatorView!!.setAnimationType(
                    AnimationType.DROP
                )
                IndicatorAnimationType.FILL -> pageIndicatorView!!.setAnimationType(AnimationType.FILL)
                IndicatorAnimationType.NONE -> pageIndicatorView!!.setAnimationType(AnimationType.NONE)
                IndicatorAnimationType.SWAP -> pageIndicatorView!!.setAnimationType(AnimationType.SWAP)
                IndicatorAnimationType.WORM -> pageIndicatorView!!.setAnimationType(AnimationType.WORM)
                IndicatorAnimationType.COLOR -> pageIndicatorView!!.setAnimationType(AnimationType.COLOR)
                IndicatorAnimationType.SCALE -> pageIndicatorView!!.setAnimationType(AnimationType.SCALE)
                IndicatorAnimationType.SLIDE -> pageIndicatorView!!.setAnimationType(AnimationType.SLIDE)
                IndicatorAnimationType.THIN_WORM -> pageIndicatorView!!.setAnimationType(
                    AnimationType.THIN_WORM
                )
                IndicatorAnimationType.SCALE_DOWN -> pageIndicatorView!!.setAnimationType(
                    AnimationType.SCALE_DOWN
                )
                else -> throw IllegalArgumentException("Unknown indicatorAnimationType $indicatorAnimationType")
            }
        }
    private var adapter: CarouselViewAdapter? = null
    private var offsetType: OffsetType? = null
    private var snapHelper: SnapHelper? = null
    private var enableSnapping = false
    var autoPlay = false
    var autoPlayDelay = 0
    private var autoPlayHandler: Handler? = null
    private var scaleOnScroll = false
    private var resource = 0
        set(resource) {
            field = resource
            isResourceSet = true
        }
    var size = 0
        set(size) {
            field = size
            pageIndicatorView!!.count = size
        }
    private var spacing = 0
    var currentItem = 0
        set(item) {
            field = if (item < 0) {
                0
            } else if (item >= size) {
                size - 1
            } else {
                item
            }
            carouselRecyclerView!!.smoothScrollToPosition(currentItem)
        }
    private var isResourceSet = false

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    private fun init(attributeSet: AttributeSet?) {
        val inflater = LayoutInflater.from(context)
        val carouselView = inflater.inflate(R.layout.view_carousel, this)
        autoPlayHandler = Handler(Looper.myLooper()!!)

        carouselRecyclerView = carouselView.findViewById(R.id.carousel_recycler_view)
        pageIndicatorView = carouselView.findViewById(R.id.page_indicator_view)
        carouselRecyclerView!!.setHasFixedSize(false)
        initializeAttributes(attributeSet)
    }

    private fun initializeAttributes(attributeSet: AttributeSet?) {
        if (attributeSet != null) {
            val attributes = context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.CarouselView, 0, 0
            )
            enableSnapping(attributes.getBoolean(R.styleable.CarouselView_enableSnapping, true))
            scaleOnScroll =
                attributes.getBoolean(R.styleable.CarouselView_scaleOnScroll, false)
            autoPlay = attributes.getBoolean(R.styleable.CarouselView_setAutoPlay, false)
            autoPlayDelay =
                attributes.getInteger(R.styleable.CarouselView_setAutoPlayDelay, 2500)
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

    private fun enableSnapping(enable: Boolean) {
        enableSnapping = enable
    }

    fun hideIndicator(hide: Boolean) {
        if (hide) {
            pageIndicatorView!!.visibility = GONE
        } else {
            pageIndicatorView!!.visibility = VISIBLE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        autoPlay = false
    }

    private fun setAdapter() {
        layoutManager = CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        layoutManager!!.isOffsetStart(carouselOffset == OffsetType.START)
        if (scaleOnScroll) layoutManager!!.setScaleOnScroll(true)
        carouselRecyclerView!!.layoutManager = layoutManager
        adapter = CarouselViewAdapter(
            carouselViewListener, resource, size,
            carouselRecyclerView, spacing, carouselOffset == OffsetType.CENTER
        )
        carouselRecyclerView!!.adapter = adapter
        if (enableSnapping) {
            snapHelper!!.attachToRecyclerView(carouselRecyclerView)
        }
        setScrollListener()
        enableAutoPlay()
    }

    private fun setScrollListener() {
        carouselRecyclerView!!.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val centerView = snapHelper!!.findSnapView(layoutManager)
                val position = layoutManager!!.getPosition(centerView!!)
                if (carouselScrollListener != null) {
                    carouselScrollListener!!.onScrollStateChanged(recyclerView, newState, position)
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    pageIndicatorView!!.selection = position
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

    private fun enableAutoPlay() {
        autoPlayHandler!!.postDelayed(object : Runnable {
            override fun run() {
                if (autoPlay) {
                    currentItem = if (size - 1 == currentItem) {
                        0
                    } else {
                        currentItem + 1
                    }
                    autoPlayHandler!!.postDelayed(this, autoPlayDelay.toLong())
                }
            }
        }, autoPlayDelay.toLong())
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
        get() = pageIndicatorView!!.radius
        set(radius) {
            pageIndicatorView!!.radius = radius
        }
    private var indicatorPadding: Int
        get() = pageIndicatorView!!.padding
        set(padding) {
            pageIndicatorView!!.padding = padding
        }
    private var indicatorSelectedColor: Int
        get() = pageIndicatorView!!.selectedColor
        set(color) {
            pageIndicatorView!!.selectedColor = color
        }
    private var indicatorUnselectedColor: Int
        get() = pageIndicatorView!!.unselectedColor
        set(color) {
            pageIndicatorView!!.unselectedColor = color
        }

    private fun validate() {
        check(isResourceSet) { "Please add a resource layout to populate the CarouselView" }
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
