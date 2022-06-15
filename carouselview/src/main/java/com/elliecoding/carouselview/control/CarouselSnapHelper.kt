package com.elliecoding.carouselview.control

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CarouselSnapHelper : LinearSnapHelper() {
    private var context: Context? = null
    private var helper: OrientationHelper? = null
    private var scroller: Scroller? = null
    private var maxScrollDistance = 0
    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        return findFirstView(layoutManager, helper(layoutManager))
    }

    override fun createScroller(layoutManager: RecyclerView.LayoutManager): SmoothScroller? {
        if (layoutManager is ScrollVectorProvider) {
            return super.createScroller(layoutManager)
        }
        return if (context == null) {
            null
        } else object : LinearSmoothScroller(context) {
            override fun onTargetFound(
                targetView: View,
                state: RecyclerView.State,
                action: Action
            ) {
                val snapDistance = calculateDistanceToFinalSnap(layoutManager, targetView)
                val dx = snapDistance[0]
                val dy = snapDistance[1]
                val dt = calculateTimeForDeceleration(abs(dx))
                val time = max(1, min(MAX_SCROLL_ON_FLING_DURATION_MS, dt))
                action.update(dx, dy, time, mDecelerateInterpolator)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
            }
        }
    }

    override fun calculateDistanceToFinalSnap(
        layoutManager: RecyclerView.LayoutManager,
        targetView: View
    ): IntArray {
        val out = IntArray(2)
        out[0] = distanceStart(targetView, helper(layoutManager))
        return out
    }

    override fun calculateScrollDistance(velocityX: Int, velocityY: Int): IntArray {
        val out = IntArray(2)
        if (helper == null) {
            return out
        }
        if (maxScrollDistance == 0) {
            maxScrollDistance = (helper!!.endAfterPadding - helper!!.startAfterPadding) / 2
        }
        scroller!!.fling(0, 0, velocityX, velocityY, -maxScrollDistance, maxScrollDistance, 0, 0)
        out[0] = scroller!!.finalX
        out[1] = scroller!!.finalY
        return out
    }

    @Throws(IllegalStateException::class)
    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (recyclerView != null) {
            context = recyclerView.context
            scroller = Scroller(context, DecelerateInterpolator())
        } else {
            scroller = null
            context = null
        }
        super.attachToRecyclerView(recyclerView)
    }

    private fun findFirstView(
        layoutManager: RecyclerView.LayoutManager?,
        helper: OrientationHelper?
    ): View? {
        if (layoutManager == null) return null
        val childCount = layoutManager.childCount
        if (childCount == 0) return null
        var absClosest = Int.MAX_VALUE
        var closestView: View? = null
        val start = helper!!.startAfterPadding
        for (index in 0 until childCount) {
            val child = layoutManager.getChildAt(index)
            val childStart = helper.getDecoratedStart(child)
            val absDistanceToStart = Math.abs(childStart - start)
            if (absDistanceToStart < absClosest) {
                absClosest = absDistanceToStart
                closestView = child
            }
        }
        return closestView
    }

    private fun distanceStart(targetView: View, helper: OrientationHelper?): Int {
        val childStart = helper!!.getDecoratedStart(targetView)
        val containerStart = helper.startAfterPadding
        return childStart - containerStart
    }

    private fun helper(layoutManager: RecyclerView.LayoutManager): OrientationHelper? {
        if (helper == null) {
            helper = OrientationHelper.createHorizontalHelper(layoutManager)
        }
        return helper
    }

    companion object {
        private const val MAX_SCROLL_ON_FLING_DURATION_MS = 1000
        private const val MILLISECONDS_PER_INCH = 100f
    }
}
