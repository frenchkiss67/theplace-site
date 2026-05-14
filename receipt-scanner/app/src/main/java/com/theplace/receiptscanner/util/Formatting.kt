package com.theplace.receiptscanner.util

import android.text.format.Formatter
import java.text.DateFormat
import java.util.Date
import android.content.Context

fun formatSize(context: Context, bytes: Long): String =
    Formatter.formatShortFileSize(context, bytes)

fun formatDate(timestampMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestampMs))
