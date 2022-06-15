package com.elliecoding.carouselview

import android.view.View

interface CarouselViewListener {
    fun onBindView(view: View?, position: Int)
}
