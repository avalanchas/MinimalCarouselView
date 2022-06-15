package com.elliecoding.carouselview.control

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlin.math.abs
import kotlin.math.min

class CarouselLinearLayoutManager internal constructor(
    context: Context?,
    orientation: Int,
    reverseLayout: Boolean
) :
    LinearLayoutManager(context, orientation, reverseLayout) {
    private var isOffsetStart = false
    private var scaleOnScroll = false
    override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
        super.onLayoutChildren(recycler, state)
        scrollHorizontallyBy(0, recycler, state)
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: RecyclerView.State): Int {
        val scrolled = super.scrollHorizontallyBy(dx, recycler, state)
        if (scaleOnScroll) {
            for (index in 0 until childCount) {
                val child = getChildAt(index) ?: continue
                val childWidth = child.right - child.left
                val childWidthHalf = childWidth / 2f
                val childCenter = child.left + childWidthHalf
                val parentWidth = if (isOffsetStart) childWidth else width
                val parentWidthHalf = parentWidth / 2f
                val d0 = 0f
                val mShrinkDistance = .75f
                val d1 = mShrinkDistance * parentWidthHalf
                val s0 = 1f
                val mShrinkAmount = 0.15f
                val s1 = 1f - mShrinkAmount
                val delta = min(d1, abs(parentWidthHalf - childCenter))
                val position = s0 + (s1 - s0) * (delta - d0) / (d1 - d0)
                child.scaleX = position
                child.scaleY = position
            }
        }
        return scrolled
    }

    fun isOffsetStart(isOffsetStart: Boolean) {
        this.isOffsetStart = isOffsetStart
    }

    fun setScaleOnScroll(scaleOnScroll: Boolean) {
        this.scaleOnScroll = scaleOnScroll
    }
}
