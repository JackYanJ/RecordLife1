

package com.test.recordlife.util

import android.content.Context

/**
 * 根据手机的分辨率将dp转成为px。
 */
fun dp2px(dp: Float, context: Context): Int {
    val scale = context.resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

/**
 * 根据手机的分辨率将px转成dp。
 */
fun px2dp(px: Float, context: Context): Int {
    val scale = context.resources.displayMetrics.density
    return (px / scale + 0.5f).toInt()
}
