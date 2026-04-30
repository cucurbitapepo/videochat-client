package com.example.videochat.livekit;

import io.livekit.android.room.Room;
import io.livekit.android.room.participant.Participant;
import io.livekit.android.room.track.LocalVideoTrack;
import io.livekit.android.room.track.RemoteVideoTrack;
import io.livekit.android.room.track.TrackPublication;

import java.util.List;

public interface LiveKitEventCallback {

  void onRoomConnected(Room room);

  void onRoomDisconnected();

  void onReconnecting();

  void onReconnected();

  void onActiveSpeakersChanged(List<Participant> speakers);

  void onTrackPublished(Participant participant,
                        io.livekit.android.room.track.TrackPublication publication);

  void onTrackSubscribed(Participant participant,
                         io.livekit.android.room.track.Track track);

  void onTrackUnsubscribed(io.livekit.android.room.track.Track track);

  void onTrackMuted(Participant participant, TrackPublication publication);
  void onTrackUnmuted(Participant participant, TrackPublication publication);

  void onLocalTrackSubscribed(LocalVideoTrack track);
  void onParticipantConnected(Participant participant);

  void onParticipantDisconnected(Participant participant);

  void onError(Throwable error);
}