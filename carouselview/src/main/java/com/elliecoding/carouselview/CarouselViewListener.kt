package com.elliecoding.carouselview

import android.view.View

fun interface CarouselViewListener {
    fun onBindView(view: View?, position: Int)
}
