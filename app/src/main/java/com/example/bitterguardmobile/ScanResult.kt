package com.example.bitterguardmobile

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class ScanResult(
    val id: String = UUID.randomUUID().toString(),
    val prediction: String,
    val confidence: String,
    val imageByteArray: ByteArray?,
    val imageUriString: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val location: String
) : Parcelable {
    val time: String
        get() = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    
    val date: String
        get() = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ScanResult

        if (id != other.id) return false
        if (prediction != other.prediction) return false
        if (confidence != other.confidence) return false
        if (timestamp != other.timestamp) return false
        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + prediction.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }
} 