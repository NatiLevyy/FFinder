package com.locationsharing.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.debounceClickable(delayMs: Long = 500L, onClick: () -> Unit): Modifier = composed {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    this.clickable {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= delayMs) {
            lastClickTime = currentTime
            onClick()
        }
    }
}