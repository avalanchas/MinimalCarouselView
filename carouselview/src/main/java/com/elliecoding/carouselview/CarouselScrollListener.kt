package com.elliecoding.carouselview

import androidx.recyclerview.widget.RecyclerView

interface CarouselScrollListener {
    fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int, position: Int)

    fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int)
}
