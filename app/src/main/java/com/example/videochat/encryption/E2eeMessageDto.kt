package com.example.videochat.encryption

import com.google.gson.annotations.SerializedName


data class DhPublicKeyMessage @JvmOverloads constructor(
    @SerializedName("senderId") val senderId: String,
    @SerializedName("publicKeyBase64") val publicKeyBase64: String,
    @SerializedName("callId") val callId: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class WrappedGroupKeyMessage @JvmOverloads constructor(
    @SerializedName("inviterId") val inviterId: String,
    @SerializedName("recipientId") val recipientId: String,
    @SerializedName("wrappedKeyBase64") val wrappedKeyBase64: String,
    @SerializedName("nonceBase64") val nonceBase64: String,
    @SerializedName("keyIndex") val keyIndex: Int = 0,
    @SerializedName("callId") val callId: String,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

data class E2eeReadyMessage @JvmOverloads constructor(
    @SerializedName("userId") val userId: String,
    @SerializedName("callId") val callId: String,
    @SerializedName("ready") val ready: Boolean = true,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)