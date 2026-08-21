package com.battleheim.quantum2048.ui

import android.content.Context
import androidx.annotation.StringRes

data class GameMessage(
    @StringRes val resId: Int,
    val args: List<String> = emptyList(),
) {
    fun resolve(context: Context): String =
        context.getString(resId, *args.toTypedArray())
}

fun message(@StringRes resId: Int, vararg args: Any): GameMessage =
    GameMessage(resId, args.map { it.toString() })
