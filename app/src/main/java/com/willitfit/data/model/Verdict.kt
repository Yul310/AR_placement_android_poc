package com.willitfit.data.model

import androidx.compose.ui.graphics.Color

sealed class Verdict(val reason: String) {
    class Pass(reason: String) : Verdict(reason)
    class Fail(reason: String) : Verdict(reason)
    class NotSure(reason: String) : Verdict(reason)

    val title: String
        get() = when (this) {
            is Pass -> "WILL FIT"
            is Fail -> "WON'T FIT"
            is NotSure -> "NOT SURE"
        }

    val color: Color
        get() = when (this) {
            is Pass -> Color(0xFF4CAF50)
            is Fail -> Color(0xFFF44336)
            is NotSure -> Color(0xFFFF9800)
        }

    val icon: String
        get() = when (this) {
            is Pass -> "check_circle"
            is Fail -> "cancel"
            is NotSure -> "help"
        }
}
