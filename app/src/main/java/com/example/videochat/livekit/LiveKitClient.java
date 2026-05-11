package com.example.videochat.livekit;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import io.livekit.android.renderer.SurfaceViewRenderer;
import io.livekit.android.room.Room;
import io.livekit.android.room.participant.LocalParticipant;
import io.livekit.android.room.participant.Participant;
import io.livekit.android.room.track.LocalVideoTrack;
import io.livekit.android.room.track.TrackException;

public class LiveKitClient {
  private static LiveKitClient instance;
  private final LiveKitManager liveKitManager;

  public void setRemoteVideoView(SurfaceViewRenderer renderer) {
    liveKitManager.setRemoteVideoView(renderer);
  }

  public void setLocalVideoView(SurfaceViewRenderer renderer) {
    liveKitManager.setLocalVideoView(renderer);
  }

  public void clearRemoteVideoView() {
    liveKitManager.clearRemoteVideoView();
  }

  public void clearLocalVideoView() {
    liveKitManager.clearLocalVideoView();
  }

  private LiveKitClient(Context context) {
    liveKitManager = new LiveKitManager(context.getApplicationContext());
  }

  public static synchronized LiveKitClient getInstance(Context context) {
    if (instance == null) {
      instance = new LiveKitClient(context);
    }
    return instance;
  }

  public void connectToRoom(String serverUrl, String token) {
    liveKitManager.connectToRoom(serverUrl, token);
  }

  public void disconnect() {
    liveKitManager.disconnect();
  }

  public MutableLiveData<Room> getRoomLiveData() {
    return liveKitManager.getRoomLiveData();
  }

  public MutableLiveData<Participant> getActiveSpeakerLiveData() {
    return liveKitManager.getActiveSpeakerLiveData();
  }

  public void toggleMicrophone() throws TrackException.PublishException {
    liveKitManager.toggleMicrophone();
  }

  public void toggleCamera() throws TrackException.PublishException {
    liveKitManager.toggleCamera();
  }

  public boolean isMicrophoneEnabled() {
    return liveKitManager.isMicrophoneEnabled();
  }

  public boolean isCameraEnabled() {
    return liveKitManager.isCameraEnabled();
  }

  public LocalVideoTrack getLocalVideoTrack() {
    return liveKitManager.getLocalVideoTrack();
  }

  public LocalParticipant getLocalParticipant() {
    return liveKitManager.getLocalParticipant();
  }

  public void setAvatarCallback(LiveKitManager.AvatarCallback callback) {
    liveKitManager.setAvatarCallback(callback);
  }

  public void setCameraStateCallback(LiveKitManager.CameraStateCallback callback) {
    liveKitManager.setCameraStateCallback(callback);
  }

  public void setUiCallback(LiveKitManager.UiCallback callback) {
    liveKitManager.setUiCallback(callback);
  }

  public static synchronized void resetInstance() {
    if (instance != null) {
      instance.liveKitManager.disconnect();
      instance = null;
      Log.d("LiveKitClient", "Instance reset");
    }
  }
}