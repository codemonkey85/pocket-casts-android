package au.com.shiftyjelly.pocketcasts.view

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.ViewPager2AwareBottomSheetBehavior
import kotlinx.parcelize.Parcelize

// StackOverflow: https://stackoverflow.com/questions/35794264/disabling-user-dragging-on-bottomsheet

@Suppress("unused")
class LockableBottomSheetBehavior<V : View> : ViewPager2AwareBottomSheetBehavior<V> {
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var swipeEnabled = false

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent,
    ): Boolean {
        return if (swipeEnabled) {
            super.onInterceptTouchEvent(parent, child, event)
        } else {
            false
        }
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        return if (swipeEnabled) {
            super.onTouchEvent(parent, child, event)
        } else {
            false
        }
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int,
    ): Boolean {
        return if (swipeEnabled) {
            super.onStartNestedScroll(
                coordinatorLayout,
                child,
                directTargetChild,
                target,
                axes,
                type,
            )
        } else {
            false
        }
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int,
    ) {
        if (swipeEnabled) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        }
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int,
    ) {
        if (swipeEnabled) {
            super.onStopNestedScroll(coordinatorLayout, child, target, type)
        }
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        return if (swipeEnabled) {
            super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
        } else {
            false
        }
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): Parcelable {
        return SavedState(
            isSwipeEnabled = swipeEnabled,
            parentState = super.onSaveInstanceState(parent, child),
        )
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        val savedState = state as SavedState
        swipeEnabled = savedState.isSwipeEnabled
        super.onRestoreInstanceState(parent, child, savedState.parentState)
    }

    @Parcelize
    private class SavedState(
        val isSwipeEnabled: Boolean,
        val parentState: Parcelable,
    ) : Parcelable
}
