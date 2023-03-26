package io.github.album.ui;

import androidx.viewpager.widget.ViewPager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.annotation.SuppressLint;

import android.util.AttributeSet;
import android.content.Context;
import android.view.MotionEvent;

public class PreviewViewPager extends ViewPager {

    public PreviewViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // In case of out of range.
        // https://stackoverflow.com/questions/16459196/java-lang-illegalargumentexception-pointerindex-out-of-range-exception-dispat
        try {
            return (super.onTouchEvent(ev));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return (super.onInterceptTouchEvent(ev));
        } catch (Exception e) {
            return false;
        }
    }

    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }
}
