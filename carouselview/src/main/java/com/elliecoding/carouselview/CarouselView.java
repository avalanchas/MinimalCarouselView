package com.elliecoding.carouselview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.elliecoding.carouselview.enums.IndicatorAnimationType;
import com.elliecoding.carouselview.enums.OffsetType;
import com.rd.PageIndicatorView;
import com.rd.animation.type.AnimationType;

@SuppressWarnings("unused") // This is a library, external consumers will use the public methods
public class CarouselView extends FrameLayout {

    private final Context context;
    private PageIndicatorView pageIndicatorView;
    private RecyclerView carouselRecyclerView;
    private CarouselLinearLayoutManager layoutManager;
    private CarouselViewListener carouselViewListener;
    private CarouselScrollListener carouselScrollListener;
    private IndicatorAnimationType indicatorAnimationType;
    private CarouselViewAdapter adapter;
    private OffsetType offsetType;
    private SnapHelper snapHelper;
    private boolean enableSnapping;
    private boolean enableAutoPlay;
    private int autoPlayDelay;
    private Handler autoPlayHandler;
    private boolean scaleOnScroll;
    private int resource;
    private int size;
    private int spacing;
    private int currentItem;
    private boolean isResourceSet = false;

    public CarouselView(@NonNull Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public CarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    private void init(AttributeSet attributeSet) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View carouselView = inflater.inflate(R.layout.view_carousel, this);
        this.carouselRecyclerView = carouselView.findViewById(R.id.carouselRecyclerView);
        this.pageIndicatorView = carouselView.findViewById(R.id.pageIndicatorView);
        this.autoPlayHandler = new Handler(Looper.myLooper());

        carouselRecyclerView.setHasFixedSize(false);
        this.initializeAttributes(attributeSet);
    }

    private void initializeAttributes(AttributeSet attributeSet) {
        if (attributeSet != null) {
            TypedArray attributes = this.context.getTheme().obtainStyledAttributes(attributeSet,
                    R.styleable.CarouselView, 0, 0);
            this.enableSnapping(attributes.getBoolean(R.styleable.CarouselView_enableSnapping, true));
            this.setScaleOnScroll(attributes.getBoolean(R.styleable.CarouselView_scaleOnScroll, false));
            this.setAutoPlay(attributes.getBoolean(R.styleable.CarouselView_setAutoPlay, false));
            this.setAutoPlayDelay(attributes.getInteger(R.styleable.CarouselView_setAutoPlayDelay, 2500));
            this.setCarouselOffset(this.getOffset(attributes.getInteger(R.styleable.CarouselView_carouselOffset, 0)));
            int resourceId = attributes.getResourceId(R.styleable.CarouselView_resource, 0);
            if (resourceId != 0) {
                this.setResource(resourceId);
            }
            int indicatorSelectedColorResourceId =
                    attributes.getColor(R.styleable.CarouselView_indicatorSelectedColor, 0);
            int indicatorUnselectedColorResourceId =
                    attributes.getColor(R.styleable.CarouselView_indicatorUnselectedColor, 0);
            if (indicatorSelectedColorResourceId != 0) {
                this.setIndicatorSelectedColor(indicatorSelectedColorResourceId);
            }
            if (indicatorUnselectedColorResourceId != 0) {
                this.setIndicatorUnselectedColor(indicatorUnselectedColorResourceId);
            }
            this.setIndicatorAnimationType(this.getAnimation(attributes.getInteger(R.styleable.CarouselView_indicatorAnimationType, 0)));
            this.setIndicatorRadius(attributes.getInteger(R.styleable.CarouselView_indicatorRadius, 5));
            this.setIndicatorPadding(attributes.getInteger(R.styleable.CarouselView_indicatorPadding, 5));
            this.setSize(attributes.getInteger(R.styleable.CarouselView_size, 0));
            this.setSpacing(attributes.getInteger(R.styleable.CarouselView_spacing, 0));
            attributes.recycle();
        }
    }

    public void enableSnapping(boolean enable) {
        this.enableSnapping = enable;
    }

    public void hideIndicator(boolean hide) {
        if (hide) {
            this.pageIndicatorView.setVisibility(GONE);
        } else {
            this.pageIndicatorView.setVisibility(VISIBLE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.setAutoPlay(false);
    }

    private void setAdapter() {
        this.layoutManager = new CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        this.layoutManager.isOffsetStart(this.getCarouselOffset() == OffsetType.START);
        if (this.getScaleOnScroll()) this.layoutManager.setScaleOnScroll(true);
        carouselRecyclerView.setLayoutManager(this.layoutManager);
        this.adapter = new CarouselViewAdapter(getCarouselViewListener(), getResource(), getSize(),
                carouselRecyclerView, this.getSpacing(), this.getCarouselOffset() == OffsetType.CENTER);
        this.carouselRecyclerView.setAdapter(adapter);
        if (this.enableSnapping) {
            this.snapHelper.attachToRecyclerView(this.carouselRecyclerView);
        }
        this.setScrollListener();
        this.enableAutoPlay();
    }

    public CarouselViewAdapter getAdapter() {
        return adapter;
    }

    private void setScrollListener() {
        this.carouselRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                View centerView = snapHelper.findSnapView(layoutManager);
                int position = layoutManager.getPosition(centerView);
                if (carouselScrollListener != null) {
                    carouselScrollListener.onScrollStateChanged(recyclerView, newState, position);
                }
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    pageIndicatorView.setSelection(position);
                    setCurrentItem(position);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (carouselScrollListener != null) {
                    carouselScrollListener.onScrolled(recyclerView, dx, dy);
                }
            }
        });
    }

    public boolean getAutoPlay() {
        return this.enableAutoPlay;
    }

    public void setAutoPlay(boolean enableAutoPlay) {
        this.enableAutoPlay = enableAutoPlay;
    }

    public int getAutoPlayDelay() {
        return this.autoPlayDelay;
    }

    public void setAutoPlayDelay(int autoPlayDelay) {
        this.autoPlayDelay = autoPlayDelay;
    }

    private void enableAutoPlay() {
        autoPlayHandler.postDelayed(new Runnable() {
            public void run() {
                if (getAutoPlay()) {
                    if (getSize() - 1 == getCurrentItem()) {
                        setCurrentItem(0);
                    } else {
                        setCurrentItem(getCurrentItem() + 1);
                    }
                    autoPlayHandler.postDelayed(this, getAutoPlayDelay());
                }
            }
        }, getAutoPlayDelay());
    }

    public OffsetType getCarouselOffset() {
        return this.offsetType;
    }

    public void setCarouselOffset(OffsetType offsetType) {
        this.offsetType = offsetType;
        switch (offsetType) {
            case CENTER:
                this.snapHelper = new LinearSnapHelper();
                break;
            case START:
                this.snapHelper = new CarouselSnapHelper();
                break;
        }
    }

    public int getCurrentItem() {
        return this.currentItem;
    }

    public void setCurrentItem(int item) {
        if (item < 0) {
            this.currentItem = 0;
        } else if (item >= this.getSize()) {
            this.currentItem = this.getSize() - 1;
        } else {
            this.currentItem = item;
        }
        this.carouselRecyclerView.smoothScrollToPosition(this.currentItem);
    }

    public IndicatorAnimationType getIndicatorAnimationType() {
        return this.indicatorAnimationType;
    }

    public void setIndicatorAnimationType(IndicatorAnimationType indicatorAnimationType) {
        this.indicatorAnimationType = indicatorAnimationType;
        switch (indicatorAnimationType) {
            case DROP:
                this.pageIndicatorView.setAnimationType(AnimationType.DROP);
                break;
            case FILL:
                this.pageIndicatorView.setAnimationType(AnimationType.FILL);
                break;
            case NONE:
                this.pageIndicatorView.setAnimationType(AnimationType.NONE);
                break;
            case SWAP:
                this.pageIndicatorView.setAnimationType(AnimationType.SWAP);
                break;
            case WORM:
                this.pageIndicatorView.setAnimationType(AnimationType.WORM);
                break;
            case COLOR:
                this.pageIndicatorView.setAnimationType(AnimationType.COLOR);
                break;
            case SCALE:
                this.pageIndicatorView.setAnimationType(AnimationType.SCALE);
                break;
            case SLIDE:
                this.pageIndicatorView.setAnimationType(AnimationType.SLIDE);
                break;
            case THIN_WORM:
                this.pageIndicatorView.setAnimationType(AnimationType.THIN_WORM);
                break;
            case SCALE_DOWN:
                this.pageIndicatorView.setAnimationType(AnimationType.SCALE_DOWN);
        }
    }

    public int getIndicatorRadius() {
        return this.pageIndicatorView.getRadius();
    }

    public void setIndicatorRadius(int radius) {
        this.pageIndicatorView.setRadius(radius);
    }

    public int getIndicatorPadding() {
        return this.pageIndicatorView.getPadding();
    }

    public void setIndicatorPadding(int padding) {
        this.pageIndicatorView.setPadding(padding);
    }

    public int getIndicatorSelectedColor() {
        return this.pageIndicatorView.getSelectedColor();
    }

    public void setIndicatorSelectedColor(int color) {
        this.pageIndicatorView.setSelectedColor(color);
    }

    public int getIndicatorUnselectedColor() {
        return this.pageIndicatorView.getUnselectedColor();
    }

    public void setIndicatorUnselectedColor(int color) {
        this.pageIndicatorView.setUnselectedColor(color);
    }

    public boolean getScaleOnScroll() {
        return this.scaleOnScroll;
    }

    public void setScaleOnScroll(boolean scaleOnScroll) {
        this.scaleOnScroll = scaleOnScroll;
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        this.size = size;
        this.pageIndicatorView.setCount(size);
    }

    public int getSpacing() {
        return this.spacing;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public int getResource() {
        return this.resource;
    }

    public void setResource(int resource) {
        this.resource = resource;
        this.isResourceSet = true;
    }

    public CarouselViewListener getCarouselViewListener() {
        return this.carouselViewListener;
    }

    public void setCarouselViewListener(CarouselViewListener carouselViewListener) {
        this.carouselViewListener = carouselViewListener;
    }

    public CarouselScrollListener getCarouselScrollListener() {
        return this.carouselScrollListener;
    }

    public void setCarouselScrollListener(CarouselScrollListener carouselScrollListener) {
        this.carouselScrollListener = carouselScrollListener;
    }

    private void validate() {
        if (!this.isResourceSet)
            throw new IllegalStateException("Please add a resource layout to populate the CarouselView");
    }

    private IndicatorAnimationType getAnimation(int value) {
        IndicatorAnimationType animationType;
        switch (value) {
            case 1:
                animationType = IndicatorAnimationType.FILL;
                break;
            case 2:
                animationType = IndicatorAnimationType.DROP;
                break;
            case 3:
                animationType = IndicatorAnimationType.SWAP;
                break;
            case 4:
                animationType = IndicatorAnimationType.WORM;
                break;
            case 5:
                animationType = IndicatorAnimationType.COLOR;
                break;
            case 6:
                animationType = IndicatorAnimationType.SCALE;
                break;
            case 7:
                animationType = IndicatorAnimationType.SLIDE;
                break;
            case 8:
                animationType = IndicatorAnimationType.THIN_WORM;
                break;
            case 9:
                animationType = IndicatorAnimationType.SCALE_DOWN;
                break;
            case 0:
            default:
                animationType = IndicatorAnimationType.NONE;
        }
        return animationType;
    }

    private OffsetType getOffset(int value) {
        OffsetType offset;
        switch (value) {
            case 1:
                offset = OffsetType.CENTER;
                break;
            case 0:
            default:
                offset = OffsetType.START;
        }
        return offset;
    }

    public void show() {
        this.validate();
        this.setAdapter();
    }

}
