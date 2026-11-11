package com.aisarathi.launcher.model

import android.graphics.drawable.Drawable

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)
