package com.example.videochat.encryption

import android.util.Log
import androidx.lifecycle.Observer
import com.example.videochat.livekit.LiveKitRoomConnector
import com.example.videochat.websocket.StompClientManager
import io.livekit.android.room.Room
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import java.security.KeyPair
import java.security.PublicKey
import java.util.Base64
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class E2eeKeyManager(
    private val stompClient: StompClientManager,
    private val room: Room,
    private val callId: String,
    private val localUserId: String,
    private val isInitiator: Boolean,
    private val preReceivedInitiatorPublicKey: String? = null,
    private val onE2eeReady: () -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val TAG = "E2eeKeyManager"

    companion object {
        private val initiatorKeyCache = mutableMapOf<String, java.security.KeyPair>()

        @JvmStatic
        fun generateInitiatorPublicKeyForInvite(callId: String, username: String): String {
            val cacheKey = "$callId:$username"
            val (keyPair, publicKeyBase64) = SecureKeyExchange.generateDhKeyPairAndEncodePublic()
            initiatorKeyCache[cacheKey] = keyPair
            Log.d("E2eeKeyManager", "Cached initiator key pair for cacheKey='$cacheKey'")
            return publicKeyBase64
        }

        @JvmStatic
        fun takeInitiatorKeyPair(callId: String, username: String): java.security.KeyPair? {
            val cacheKey = "$callId:$username"
            val keyPair = initiatorKeyCache[cacheKey]
            if (keyPair != null) {
                initiatorKeyCache.remove(cacheKey)
                Log.d("E2eeKeyManager", "Retrieved initiator key pair for cacheKey='$cacheKey'")
            } else {
                Log.w("E2eeKeyManager", "Initiator key pair NOT FOUND for cacheKey='$cacheKey'")
            }
            return keyPair
        }
    }

    private var dhKeyPair: KeyPair? = null
    private var groupKey: ByteArray? = null
    private var keyIndex: Int = 0
    private var remotePublicKey: PublicKey? = null
    private var bufferedWrappedKey: Triple<ByteArray, ByteArray, Int>? = null
    private var isKeySet: Boolean = false
    private var isE2eeCompleted: Boolean = false
    private var connectionObserver: Observer<Boolean>? = null

    private val disposables = CompositeDisposable()

    private val KEY_EXCHANGE_TIMEOUT_SEC = 15L

    fun start() {
        Log.d(TAG, "Starting E2EE key exchange for call $callId (isInitiator=$isInitiator)")

        if (!stompClient.isConnected()) {
            Log.d(TAG, "WebSocket not connected yet, waiting for connection...")

            connectionObserver = Observer<Boolean> { connected ->
                if (connected) {
                    Log.d(TAG, "WebSocket connected, proceeding with E2EE key exchange")

                    connectionObserver?.let {
                        stompClient.connectionState.removeObserver(it)
                        connectionObserver = null
                    }

                    performKeyExchange()
                }
            }

            connectionObserver?.let {
                stompClient.connectionState.observeForever(it)
            }
        } else {
            performKeyExchange()
        }
    }

    private fun performKeyExchange() {
        Log.d(TAG, "performKeyExchange() called")

        if (isInitiator) {
            dhKeyPair = takeInitiatorKeyPair(callId, localUserId)
            if (dhKeyPair == null) {
                Log.w(TAG, "Initiator key pair not found in cache, generating new one")
                dhKeyPair = SecureKeyExchange.generateDhKeyPair()
            }
        } else {
            if (preReceivedInitiatorPublicKey != null) {
                try {
                    remotePublicKey = SecureKeyExchange.decodePublicKey(preReceivedInitiatorPublicKey)
                    Log.d(TAG, "Pre-received initiator public key decoded")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode pre-received public key", e)
                    onError(e)
                    return
                }
            }
            dhKeyPair = SecureKeyExchange.generateDhKeyPair()
        }

        val dhSubscription = stompClient.subscribeToDhKeys(
            callId,
            { msg -> handleDhPublicKeyReceived(msg) },
            { e -> onError(e) }
        )
        if (dhSubscription != null) {
            disposables.add(dhSubscription)
            Log.d(TAG, "Subscribed to DH keys topic")
        } else {
            Log.e(TAG, "Failed to subscribe to DH keys — WebSocket disconnected?")
            onError(IllegalStateException("Cannot subscribe to DH keys"))
            return
        }

        if (!isInitiator) {
            subscribeToWrappedKeys()
        }

        val readySubscription = stompClient.subscribeToE2eeReady(
            callId,
            { msg -> checkBothReady() },
            { e -> onError(e) }
        )
        if (readySubscription != null) {
            disposables.add(readySubscription)
            Log.d(TAG, "Subscribed to E2EE ready topic")
        }

        val publicKeyBase64 = SecureKeyExchange.encodePublicKey(dhKeyPair!!.public)
        disposables.add(
            stompClient.sendDhPublicKey(callId, localUserId, publicKeyBase64)
                .subscribe(
                    { Log.d(TAG, "DH public key sent") },
                    { e -> onError(e) }
                )
        )

        startKeyExchangeTimeout()
    }

    private fun handleDhPublicKeyReceived(msg: DhPublicKeyMessage) {
        Log.d(TAG, "DEBUG: Received DH key | sender=${msg.senderId}, local=$localUserId, isInitiator=$isInitiator")

        if (msg.senderId == localUserId) {
            Log.d(TAG, "Ignoring own DH key")
            return
        }

        Log.d(TAG, "Processing DH key from ${msg.senderId}")

        try {
            val publicKey = SecureKeyExchange.decodePublicKey(msg.publicKeyBase64)
            this.remotePublicKey = publicKey
            Log.d(TAG, "Remote public key stored from ${msg.senderId}")

            if (isInitiator && groupKey == null && dhKeyPair != null) {
                val sharedSecret = SecureKeyExchange.deriveSharedSecret(dhKeyPair!!.private, publicKey)
                Log.d(TAG, "DEBUG: Initiator shared secret (first 8 bytes): ${sharedSecret.take(8).joinToString("") { "%02x".format(it) }}")
            }

            bufferedWrappedKey?.let { (wrappedKey, nonce, keyIndex) ->
                Log.d(TAG, "Processing buffered wrapped key...")
                bufferedWrappedKey = null
                attemptUnwrap(wrappedKey, nonce, keyIndex)
            }

            if (isInitiator && groupKey == null) {
                Log.d(TAG, "Initiator: generating group key and wrapping...")
                groupKey = SecureKeyExchange.generateGroupKey()

                val sharedSecret = SecureKeyExchange.deriveSharedSecret(
                    dhKeyPair!!.private,
                    publicKey
                )

                Log.d(TAG, "DEBUG: Initiator shared secret (first 8 bytes): ${sharedSecret.take(8).joinToString("") { "%02x".format(it) }}")

                val (wrappedKey, nonce) = SecureKeyExchange.wrapGroupKey(sharedSecret, groupKey!!)
                val wrappedMsg = WrappedGroupKeyMessage(
                    inviterId = localUserId,
                    recipientId = msg.senderId,
                    wrappedKeyBase64 = Base64.getEncoder().encodeToString(wrappedKey),
                    nonceBase64 = Base64.getEncoder().encodeToString(nonce),
                    keyIndex = keyIndex,
                    callId = callId
                )

                disposables.add(
                    stompClient.sendWrappedGroupKey(callId, wrappedMsg)
                        .subscribe(
                            {
                                Log.d(TAG, "Wrapped group key sent to ${msg.senderId}")
                                LiveKitRoomConnector.setGroupKey(room, keyIndex, groupKey!!)
                                isKeySet = true
                                Log.d(TAG, "E2EE key set in LiveKit (initiator), index=$keyIndex")
                                setLocalGroupKey()
                            },
                            { e ->
                                Log.e(TAG, "Failed to send wrapped key", e)
                                onError(e)
                            }
                        )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing DH key", e)
            onError(e)
        }
    }

    private fun subscribeToWrappedKeys() {
        Log.d(TAG, "Subscribing to /user/queue/e2ee/wrapped-key for user $localUserId")

        val subscription = stompClient.subscribeToWrappedKeys(
            { msg ->
                Log.d(TAG, "Received wrapped key message: inviter=${msg.inviterId}, recipient=${msg.recipientId}")
                if (msg.recipientId != localUserId) {
                    Log.w(TAG, "Wrapped key not for me (expected=$localUserId, got=${msg.recipientId}), ignoring")
                    return@subscribeToWrappedKeys
                }

                try {
                    val wrappedKey = Base64.getDecoder().decode(msg.wrappedKeyBase64)
                    val nonce = Base64.getDecoder().decode(msg.nonceBase64)
                    val keyIndex = msg.keyIndex

                    if (remotePublicKey == null) {
                        Log.w(TAG, "remotePublicKey is NULL - buffering wrapped key")
                        bufferedWrappedKey = Triple(wrappedKey, nonce, keyIndex)
                        return@subscribeToWrappedKeys
                    }

                    if (dhKeyPair == null) {
                        Log.e(TAG, "dhKeyPair is NULL — this should not happen!")
                        onError(IllegalStateException("Local DH key pair not generated"))
                        return@subscribeToWrappedKeys
                    }

                    attemptUnwrap(wrappedKey, nonce, msg.keyIndex)

                } catch (e: javax.crypto.AEADBadTagException) {
                    Log.e(TAG, "GCM tag verification failed — possible tampering or key mismatch", e)
                    onError(SecurityException("E2EE key unwrapping failed: invalid authentication tag"))
                } catch (e: Exception) {
                    Log.e(TAG, "Error unwrapping group key", e)
                    onError(e)
                }
            },
            { e -> onError(e) }
        )
        disposables.add(subscription)
    }

    private fun attemptUnwrap(wrappedKey: ByteArray, nonce: ByteArray, keyIndexFromMsg: Int) {
        Log.d(TAG, "Attempting to unwrap group key...")

        if (remotePublicKey == null || dhKeyPair == null) {
            Log.e(TAG, "Still not ready: remotePublicKey=${remotePublicKey != null}, dhKeyPair=${dhKeyPair != null}")
            onError(IllegalStateException("DH keys not ready for unwrapping after retry"))
            return
        }

        try {
            val sharedSecret = SecureKeyExchange.deriveSharedSecret(
                dhKeyPair!!.private,
                remotePublicKey!!
            )
            Log.d(TAG, "DEBUG: Callee shared secret (first 8 bytes): ${sharedSecret.take(8).joinToString("") { "%02x".format(it) }}")


            groupKey = SecureKeyExchange.unwrapGroupKey(sharedSecret, wrappedKey, nonce)
            keyIndex = keyIndexFromMsg

            Log.d(TAG, "Group key unwrapped successfully")
            LiveKitRoomConnector.setGroupKey(room, keyIndex, groupKey!!)
            isKeySet = true
            Log.d(TAG, "E2EE key set in LiveKit, index=$keyIndex")

            setLocalGroupKey()

        } catch (e: javax.crypto.AEADBadTagException) {
            Log.e(TAG, "GCM tag verification failed — possible tampering or key mismatch", e)
            onError(SecurityException("E2EE key unwrapping failed: invalid authentication tag"))
        } catch (e: Exception) {
            Log.e(TAG, "Error during unwrap attempt", e)
            onError(e)
        }
    }

    private fun setLocalGroupKey() {
        if (groupKey == null) {
            Log.w(TAG, "groupKey is null, cannot send ready signal")
            return
        }

        try {
            disposables.add(
                stompClient.sendE2eeReady(callId, localUserId)
                    .subscribe(
                        {
                            Log.d(TAG, "E2EE ready signal sent")
                        },
                        { e -> onError(e) }
                    )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set E2EE key in LiveKit", e)
            onError(e)
        }
    }

    private fun checkBothReady() {
        if (isE2eeCompleted) {
            Log.d(TAG, "E2EE already completed, skipping duplicate checkBothReady()")
            return
        }

        if (!isKeySet) {
            Log.w(TAG, "checkBothReady() called but key not set yet")
            return
        }

        isE2eeCompleted = true
        Log.d(TAG, "checkBothReady() called — E2EE exchange complete!")
        onE2eeReady()
    }

    private fun startKeyExchangeTimeout() {
        disposables.add(
            Completable.timer(KEY_EXCHANGE_TIMEOUT_SEC, TimeUnit.SECONDS)
                .subscribe({
                    if (!isKeySet) {
                        onError(TimeoutException("E2EE key exchange timed out after $KEY_EXCHANGE_TIMEOUT_SEC seconds"))
                    }
                }, { /* ignore */ })
        )
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up E2eeKeyManager")

        connectionObserver?.let {
            stompClient.connectionState.removeObserver(it)
            connectionObserver = null
        }

        disposables.clear()
        groupKey?.fill(0)
        groupKey = null
        dhKeyPair = null
        remotePublicKey = null
    }

    private fun ByteArray.fill(value: Byte) {
        for (i in indices) this[i] = value
    }
}