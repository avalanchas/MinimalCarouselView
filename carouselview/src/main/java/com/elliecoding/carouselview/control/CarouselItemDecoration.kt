package com.elliecoding.carouselview.control

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

internal class CarouselItemDecoration internal constructor(
    private val width: Int,
    private val spacing: Int
) :
    ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.right = if (width > 0) spacing / 2 else spacing
        outRect.left = if (width > 0) spacing / 2 else 0
        if (state.itemCount - 1 == parent.getChildLayoutPosition(view)) {
            outRect.right = if (width > 0) parent.measuredWidth / 2 - width / 2 else 0
        }
        if (parent.getChildLayoutPosition(view) == 0) {
            outRect.left = if (width > 0) parent.measuredWidth / 2 - width / 2 else 0
        }
    }
}
