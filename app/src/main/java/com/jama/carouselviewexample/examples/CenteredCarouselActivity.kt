package com.jama.carouselviewexample.examples

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jama.carouselview.enums.IndicatorAnimationType
import com.jama.carouselview.enums.OffsetType
import com.jama.carouselviewexample.R
import kotlinx.android.synthetic.main.activity_centered_carousel.*

class CenteredCarouselActivity : AppCompatActivity() {

    private val images = arrayListOf(R.drawable.boardwalk_by_the_ocean, R.drawable.journal_and_coffee_at_table, R.drawable.tying_down_tent_fly)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_centered_carousel)

        carouselView.apply {
            size = images.size
            autoPlay = false
            autoPlayDelay = 3000
            hideIndicator(true)
            indicatorAnimationType = IndicatorAnimationType.THIN_WORM
            carouselOffset = OffsetType.CENTER
            resource = R.layout.center_carousel_item
            setCarouselViewListener { view, position ->
                val imageView = view.findViewById<ImageView>(R.id.imageView)
                imageView.setImageDrawable(ContextCompat.getDrawable(context, images[position]))
            }
            show()
        }
    }
}
