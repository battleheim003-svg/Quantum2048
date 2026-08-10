package com.battleheim.quantum2048.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun formatNumber(value: Long): String =
    NumberFormat.getIntegerInstance(LocalContext.current.resources.configuration.locales[0]).format(value)

@Composable
fun formatNumber(value: Int): String =
    formatNumber(value.toLong())

@Composable
fun formatDecimal(value: Double, maximumFractionDigits: Int = 1): String =
    NumberFormat.getNumberInstance(LocalContext.current.resources.configuration.locales[0]).apply {
        this.maximumFractionDigits = maximumFractionDigits
        this.minimumFractionDigits = maximumFractionDigits
    }.format(value)

@Composable
fun formatPercent(value: Double): String =
    NumberFormat.getPercentInstance(LocalContext.current.resources.configuration.locales[0]).apply {
        maximumFractionDigits = 0
    }.format(value)

@Composable
fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(LocalContext.current.resources.configuration.locales[0]))
