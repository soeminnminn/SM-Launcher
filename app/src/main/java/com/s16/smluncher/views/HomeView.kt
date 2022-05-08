package com.s16.smluncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout

@SuppressLint("ClickableViewAccessibility")
class HomeView : FrameLayout {

    private var timeSincePress = 0L
    private var onGestureListener: OnGestureListener? = null
    private lateinit var gestureDetector: GestureDetector

    constructor(context: Context)
            : super(context) {
        initialize(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {
        initialize(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
        initialize(context, attrs, 0)
    }

    @SuppressLint("CustomViewStyleable")
    private fun initialize(context: Context, attrs: AttributeSet?, defStyle: Int) {
        gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                timeSincePress = System.currentTimeMillis()
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                onGestureListener?.onDoubleTap(e)
                return true
            }

            override fun onLongPress(e: MotionEvent?) {
                onGestureListener?.onLongPress(e)
                super.onLongPress(e)
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1?.pointerCount == 1 && e2?.pointerCount == 1) {
                    if (System.currentTimeMillis() - timeSincePress < 240) {
                        if (distanceY < 0) {
                            onGestureListener?.onTopOverScroll(e1)
                        } else {
                            onGestureListener?.onBottomOverScroll(e1)
                        }
                    }
                }
                return true
            }
        })
    }

    interface OnGestureListener {
        fun onDoubleTap(event: MotionEvent?)
        fun onLongPress(event: MotionEvent?)
        fun onTopOverScroll(event: MotionEvent?)
        fun onBottomOverScroll(event: MotionEvent?)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (this::gestureDetector.isInitialized) {
            return gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    fun setOnGestureListener(l: OnGestureListener) {
        onGestureListener = l
    }
}
