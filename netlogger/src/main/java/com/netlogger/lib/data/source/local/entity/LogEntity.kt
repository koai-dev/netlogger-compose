package com.netlogger.lib.data.source.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "netlogger_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "API" or "GENERAL"
    val timestamp: Long,
    val tag: String,
    // General
    val message: String?,
    val level: String?,
    // API
    val method: String?,
    val url: String?,
    val requestHeaders: String?,
    val requestBody: String?,
    val responseHeaders: String?,
    val responseBody: String?,
    val statusCode: Int?,
    val requestTime: Long?,
    val responseTime: Long?,
    val totalDuration: Long?
)
