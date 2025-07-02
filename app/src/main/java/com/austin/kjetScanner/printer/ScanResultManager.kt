package com.austin.kjetScanner.printer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ScanResultManager {
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult

    fun updateScanResult(type: String, value: String) {
        _scanResult.value = ScanResult(type, value, System.currentTimeMillis())
    }

    fun clearScanResult() {
        _scanResult.value = null
    }
}

data class ScanResult(
    val type: String,
    val value: String,
    val timestamp: Long
)