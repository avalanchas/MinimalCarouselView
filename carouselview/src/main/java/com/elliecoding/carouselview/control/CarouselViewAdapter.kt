package com.elliecoding.carouselview.control

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.elliecoding.carouselview.CarouselViewListener
import com.elliecoding.carouselview.control.CarouselViewAdapter.CarouselAdapterViewHolder

class CarouselViewAdapter internal constructor(
    private val carouselViewListener: CarouselViewListener?,
    private val resource: Int,
    private val size: Int,
    private val recyclerView: RecyclerView,
    private val spacing: Int,
    private val isOffsetStart: Boolean
) :
    RecyclerView.Adapter<CarouselAdapterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(resource, parent, false)
        return CarouselAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarouselAdapterViewHolder, position: Int) {
        carouselViewListener?.onBindView(holder.itemView, position)
        initOffset(recyclerView, holder.itemView, spacing, isOffsetStart)
    }

    private fun initOffset(
        recyclerView: RecyclerView,
        view: View,
        spacing: Int,
        isOffsetStart: Boolean
    ) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (recyclerView.itemDecorationCount > 0) {
                    recyclerView.removeItemDecorationAt(0)
                }
                if (isOffsetStart) {
                    recyclerView.addItemDecoration(CarouselItemDecoration(view.width, spacing), 0)
                } else {
                    recyclerView.addItemDecoration(CarouselItemDecoration(0, spacing), 0)
                }
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun getItemCount(): Int {
        return size
    }

    class CarouselAdapterViewHolder(itemView: View) :
        ViewHolder(itemView)
}
