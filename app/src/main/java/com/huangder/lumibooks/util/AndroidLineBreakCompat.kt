package com.huangder.lumibooks.util

import android.graphics.text.LineBreaker
import android.os.Build
import android.text.Layout

/**
 * Android 29 moved the TextView/StaticLayout @IntDef contracts from Layout to
 * LineBreaker without changing the underlying numerical values. API 26–28
 * still need the pre-29 Layout constants, while newer SDKs should use the
 * actual LineBreaker constants required by the public setter contracts.
 *
 * Properties, rather than inlined compile-time constants, keep this genuinely
 * API-gated. No lint rules or call sites are suppressed.
 */
internal object AndroidLineBreakCompat {
    val SIMPLE: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        LineBreaker.BREAK_STRATEGY_SIMPLE else Layout.BREAK_STRATEGY_SIMPLE

    val HIGH_QUALITY: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        LineBreaker.BREAK_STRATEGY_HIGH_QUALITY else Layout.BREAK_STRATEGY_HIGH_QUALITY

    val JUSTIFICATION_NONE: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        LineBreaker.JUSTIFICATION_MODE_NONE else Layout.JUSTIFICATION_MODE_NONE

    val JUSTIFICATION_INTER_WORD: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        LineBreaker.JUSTIFICATION_MODE_INTER_WORD else Layout.JUSTIFICATION_MODE_INTER_WORD
}
