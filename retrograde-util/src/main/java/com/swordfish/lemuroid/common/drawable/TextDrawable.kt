package com.swordfish.lemuroid.common.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint

/**
 * The tile shown for a game with no cover picture: white, a black edge, black letters. A grey or
 * coloured fill is a smear on this panel; ink on white is the one thing it draws well.
 */
class TextDrawable(private val text: String) : Drawable() {
    private val mTextBounds = Rect()
    private val mPaint: TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

    init {
        mPaint.color = Color.BLACK
        mPaint.textAlign = Paint.Align.CENTER
        mPaint.typeface = Typeface.MONOSPACE
        mPaint.getTextBounds(text, 0, text.length, mTextBounds)
    }

    override fun draw(canvas: Canvas) {
        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.WHITE
        canvas.drawRect(bounds, mPaint)

        val edge = maxOf(2f, bounds.height() * 0.015f)
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = edge
        mPaint.color = Color.BLACK
        canvas.drawRect(
            bounds.left + edge / 2,
            bounds.top + edge / 2,
            bounds.right - edge / 2,
            bounds.bottom - edge / 2,
            mPaint,
        )

        mPaint.style = Paint.Style.FILL
        mPaint.color = Color.BLACK
        mPaint.textSize = bounds.height() * 0.3f
        val xPos = bounds.width().toFloat() / 2
        val yPos = (bounds.height().toFloat() / 2 - (mPaint.descent() + mPaint.ascent()) / 2)
        canvas.drawText(text, xPos, yPos, mPaint)
    }

    override fun getOpacity(): Int = mPaint.alpha

    override fun getIntrinsicWidth(): Int = -1

    override fun getIntrinsicHeight(): Int = -1

    override fun setAlpha(alpha: Int) {
        mPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(p0: ColorFilter?) {
        mPaint.colorFilter = p0
        invalidateSelf()
    }
}
