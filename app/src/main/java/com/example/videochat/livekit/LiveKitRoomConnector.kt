package com.example.videochat.livekit

import android.content.Context
import android.util.Log
import io.livekit.android.ConnectOptions
import io.livekit.android.LiveKit
import io.livekit.android.RoomOptions
import io.livekit.android.e2ee.BaseKeyProvider
import io.livekit.android.e2ee.E2EEOptions
import io.livekit.android.e2ee.E2EEState
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.RemoteVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.TrackPublication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import livekit.LivekitModels
import java.util.Base64;

object LiveKitRoomConnector {

    private const val TAG = "LiveKitRoomConnector"

    @JvmStatic
    fun connectAndCollectEvents(
        context: Context,
        serverUrl: String,
        token: String,
        callback: LiveKitEventCallback,
        scope: CoroutineScope
    ): Room {

        Log.d(TAG, "connectAndCollectEvents: url=$serverUrl")

        val room = LiveKit.create(
            context,
            RoomOptions(
                dynacast = true,
                adaptiveStream = true,
                e2eeOptions = E2EEOptions(
                    keyProvider = BaseKeyProvider(
                        enableSharedKey = true
                    ),
                    encryptionType = LivekitModels.Encryption.Type.GCM
                )
            )
        )

        Log.d(TAG, "Starting event collection coroutine...")

        scope.launch(Dispatchers.Main) {
            Log.d(TAG, "Event collection coroutine started")
            try {
                room.events.events
                    .onStart { Log.d(TAG, "Event collection started") }
                    .collect { event ->
                        Log.d(TAG, "Event received: ${event::class.simpleName}")
                        handleRoomEvent(event, callback, room)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Event collection failed", e)
                callback.onError(e)
            }
            Log.d(TAG, "Event collection coroutine ended")
        }

        scope.launch(Dispatchers.Main) {
            delay(500)
            Log.d(TAG, "Calling room.connect()...")
            try {
                room.connect(serverUrl, token, ConnectOptions())
                Log.d(TAG, "room.connect() completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                callback.onError(e)
            }
        }

        return room
    }

    @JvmStatic
    fun setGroupKey(room: Room, keyIndex: Int, groupKey: ByteArray) {
        val keyProvider = room.e2eeManager?.keyProvider as? BaseKeyProvider
        if (keyProvider != null) {
            Log.d("LiveKitRoomConnector", "Setting E2EE key: index=$keyIndex, keyLength=${groupKey.size}, first8=${groupKey.take(8).joinToString("") { "%02x".format(it) }}")

            val keyBase64 = Base64.getEncoder().encodeToString(groupKey)
            keyProvider.setSharedKey(
                keyBase64,
                keyIndex
            )
            keyProvider.enableSharedKey = true;
            Log.d("LiveKitRoomConnector", "E2EE key set successfully")
        } else {
            Log.e("LiveKitRoomConnector", "Failed to set E2EE key: keyProvider is null or not BaseKeyProvider")
        }
    }

    private fun handleRoomEvent(
        event: RoomEvent,
        callback: LiveKitEventCallback,
        room: Room
    ) {
        Log.d(TAG, "Event: ${event::class.simpleName}")

        when (event) {
            is RoomEvent.Connected -> {
                Log.d(TAG, "RoomEvent.Connected")
                callback.onRoomConnected(room)
            }

            is RoomEvent.Reconnecting -> {
                Log.d(TAG, "reconnecting, idk what to do here")
            }

            is RoomEvent.Reconnected -> {
                Log.d(TAG, "reconnected, idk what to do here")
            }

            is RoomEvent.Disconnected -> {
                Log.d(TAG, "RoomEvent.Disconnected")
                callback.onRoomDisconnected()
            }

            is RoomEvent.FailedToConnect -> {
                Log.e(TAG, "ConnectFailed: ${event.error.message}")
                callback.onError(event.error)
                callback.onRoomDisconnected()
            }

            is RoomEvent.ActiveSpeakersChanged -> {
                Log.d(TAG, "ActiveSpeakersChanged: ${event.speakers.size} speakers")
                callback.onActiveSpeakersChanged(event.speakers)
            }

            is RoomEvent.TrackPublished -> {
                val participant = event.participant
                val publication = event.publication
                Log.d(TAG, "TrackPublished: ${publication.kind} by ${participant.identity}")

                callback.onTrackPublished(participant, publication)

                if (publication.track is RemoteVideoTrack && publication.subscribed) {
                    callback.onTrackSubscribed(participant, publication.track)
                }
            }

            is RoomEvent.TrackPublicationFailed -> {
                Log.e(TAG, "TrackPublicationFailed: for ${event.participant?.identity}")
                callback.onError(Exception("Track publication failed"))
            }

            is RoomEvent.TrackSubscribed -> {
                val participant = event.participant
                val track = event.track
                Log.d(
                    TAG,
                    "TrackSubscribed: ${event.publication.name} by ${participant.identity}"
                )

                callback.onTrackSubscribed(participant, track)
            }

            is RoomEvent.TrackUnsubscribed -> {
                Log.d(TAG, "TrackUnsubscribed: ${event.track.name}")
                callback.onTrackUnsubscribed(event.track)
            }

            is RoomEvent.TrackMuted -> {
                Log.d(
                    TAG,
                    "TrackMuted: ${event.publication?.name} by ${event.participant?.identity}"
                )
                callback.onTrackMuted(event.participant, event.publication)
            }

            is RoomEvent.TrackUnmuted -> {
                Log.d(
                    TAG,
                    "TrackUnmuted: ${event.publication?.name} by ${event.participant?.identity}"
                )
                callback.onTrackUnmuted(event.participant, event.publication)
            }

            is RoomEvent.LocalTrackSubscribed -> {
                val track = event.publication.track
                Log.d(TAG,"LocalTrackSubscribed: track=${track?.name}, kind=${event.publication.track?.kind}")

                if (track is LocalVideoTrack) {
                    callback.onLocalTrackSubscribed(track)
                } else {
                    Log.w(TAG, "Track is not LocalVideoTrack or is null: ${track?.javaClass?.simpleName}")
                }
            }


            is RoomEvent.ParticipantConnected -> {
                val participant = event.participant
                Log.d(TAG, "ParticipantConnected: ${participant.identity}")

                callback.onParticipantConnected(participant)

                for ((publication, track) in participant.videoTrackPublications) {
                    if (track is RemoteVideoTrack && publication.subscribed) {
                        callback.onTrackSubscribed(participant, track)
                    }
                }
            }

            is RoomEvent.ParticipantDisconnected -> {
                Log.d(TAG, "ParticipantDisconnected: ${event.participant.identity}")
                callback.onParticipantDisconnected(event.participant)
            }

            is RoomEvent.TrackE2EEStateEvent -> {
                Log.d(
                    "LiveKitRoomConnector",
                    "TrackE2EEStateEvent: track=${event.track?.sid}, state=${event.state}, participant=${event.participant?.identity}"
                )

                when (event.state) {
                    E2EEState.MISSING_KEY -> {
                        Log.w(
                            "LiveKitRoomConnector",
                            "Track ${event.track?.sid} is MISSING_KEY — check if setSharedKey was called"
                        )
                    }
                    E2EEState.OK -> {
                        Log.d(
                            "LiveKitRoomConnector",
                            "Track ${event.track?.sid} is now OK (encrypted)"
                        )
                    }
                    E2EEState.ENCRYPTION_FAILED -> {
                        Log.e(
                            "LiveKitRoomConnector",
                            "Track ${event.track?.sid} encryption failed"
                        )
                    }
                    else -> {
                        Log.d("LiveKitRoomConnector", "Track ${event.track?.sid} E2EE state: ${event.state}")
                    }
                }
            }

            else -> {
                Log.d(TAG, "Unhandled event: ${event::class.simpleName}")
            }
        }
    }


    @JvmStatic
    fun enableLocalTracks(
        room: Room,
        microphoneEnabled: Boolean,
        cameraEnabled: Boolean,
        scope: CoroutineScope
    ) {
        scope.launch {
            try {
                room.localParticipant?.setMicrophoneEnabled(microphoneEnabled)
                room.localParticipant?.setCameraEnabled(cameraEnabled)
                Log.d(TAG, "Local tracks enabled: mic=$microphoneEnabled, cam=$cameraEnabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable local tracks", e)
                throw e
            }
        }
    }

    @JvmStatic
    fun disconnect(room: Room?) {
        room?.disconnect()
        Log.d(TAG, "Disconnected from room")
    }
}