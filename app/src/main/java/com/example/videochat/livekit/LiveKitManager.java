package com.example.videochat.livekit;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import io.livekit.android.renderer.SurfaceViewRenderer;
import io.livekit.android.room.Room;
import io.livekit.android.room.participant.LocalParticipant;
import io.livekit.android.room.participant.Participant;
import io.livekit.android.room.participant.RemoteParticipant;
import io.livekit.android.room.track.LocalVideoTrack;
import io.livekit.android.room.track.RemoteVideoTrack;
import io.livekit.android.room.track.Track;
import io.livekit.android.room.track.TrackException;
import io.livekit.android.room.track.TrackPublication;

import java.lang.ref.WeakReference;
import java.util.List;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.Job;
import kotlinx.coroutines.JobKt;

public class LiveKitManager implements LiveKitEventCallback {

  private static final String TAG = "LiveKitManager";

  private final Context context;
  private final MutableLiveData<Room> roomLiveData = new MutableLiveData<>();
  private final MutableLiveData<Participant> activeSpeakerLiveData = new MutableLiveData<>();
  private final MutableLiveData<Exception> connectionErrorLiveData = new MutableLiveData<>();

  private WeakReference<SurfaceViewRenderer> remoteVideoViewRef;
  private WeakReference<SurfaceViewRenderer> localVideoViewRef;
  private Room room;
  private boolean isMicrophoneEnabled = true;
  private boolean isCameraEnabled = true;
  private boolean isLocalRendererAttached = false;
  private boolean isReconnecting = false;

  private final Job job = JobKt.Job(null);
  private final CoroutineScope scope = new CoroutineScope() {
    @NonNull
    @Override
    public CoroutineContext getCoroutineContext() {
      return job.plus(Dispatchers.getMain());
    }
  };

  public interface AvatarCallback {
    void onAvatarStateChanged(String participantIdentity, boolean show);

    void onLocalAvatarStateChanged(boolean show);
  }

  private AvatarCallback avatarCallback;

  public interface UiCallback {
    void onLocalVideoReady();

    void onRemoteVideoReady(String participantIdentity);

    void onError(String message);
  }

  private UiCallback uiCallback;

  public void setUiCallback(UiCallback callback) {
    this.uiCallback = callback;
  }

  public interface CameraStateCallback {
    void onRemoteCameraMuted(String participantIdentity, boolean muted);
  }

  private CameraStateCallback cameraStateCallback;

  public void setCameraStateCallback(CameraStateCallback callback) {
    this.cameraStateCallback = callback;
  }

  public LiveKitManager(Context context) {
    this.context = context.getApplicationContext();
    io.livekit.android.LiveKit.INSTANCE.init(context);
  }


  public void setRemoteVideoView(SurfaceViewRenderer renderer) {
    this.remoteVideoViewRef = new WeakReference<>(renderer);
  }

  public void setLocalVideoView(SurfaceViewRenderer renderer) {
    this.localVideoViewRef = new WeakReference<>(renderer);
  }

  public void clearRemoteVideoView() {
    if (remoteVideoViewRef != null) {
      SurfaceViewRenderer renderer = remoteVideoViewRef.get();
      if (renderer != null && room != null) {
        for (RemoteParticipant participant : room.getRemoteParticipants().values()) {
          for (kotlin.Pair<TrackPublication, Track> pair : participant.getVideoTrackPublications()) {
            if (pair.getSecond() instanceof RemoteVideoTrack) {
              ((RemoteVideoTrack) pair.getSecond()).removeRenderer(renderer);
            }
          }
        }
        remoteVideoViewRef.clear();
      }
    }
  }

  public void clearLocalVideoView() {
    if (localVideoViewRef != null) {
      SurfaceViewRenderer renderer = localVideoViewRef.get();
      if (renderer != null && room != null) {
        LocalVideoTrack localTrack = getLocalVideoTrack();
        if (localTrack != null) {
          localTrack.removeRenderer(renderer);
          Log.d(TAG, "Local renderer removed");
        }
        localVideoViewRef.clear();
      }
    }
    isLocalRendererAttached = false;
  }

  public void connectToRoom(String serverUrl, String token) {
    Log.d(TAG, "connectToRoom called: url=" + serverUrl);

    room = LiveKitRoomConnector.connectAndCollectEvents(
            context,
            serverUrl,
            token,
            this,
            scope
    );
  }

  public void enableLocalTracks() {
    if (room == null) {
      Log.w(TAG, "Room not connected, cannot enable tracks");
      return;
    }

    LiveKitRoomConnector.enableLocalTracks(
            room,
            isMicrophoneEnabled,
            isCameraEnabled,
            scope
    );
  }

  public void toggleMicrophone() throws TrackException.PublishException {
    isMicrophoneEnabled = !isMicrophoneEnabled;
    if (room != null && room.getLocalParticipant() != null) {
      room.getLocalParticipant().setMicrophoneEnabled(isMicrophoneEnabled, EMPTY_CONTINUATION);
    }
  }

  public void toggleCamera() throws TrackException.PublishException {
    isCameraEnabled = !isCameraEnabled;
    if (room != null && room.getLocalParticipant() != null) {
      room.getLocalParticipant().setCameraEnabled(isCameraEnabled, EMPTY_CONTINUATION);
    }
  }

  public void disconnect() {
    LiveKitRoomConnector.disconnect(room);
    room = null;

    Job job = (Job) scope.getCoroutineContext().get(Job.Key);
    if (job != null && !job.isCancelled()) {
      job.cancel(null);
    }

    roomLiveData.postValue(null);
    isLocalRendererAttached = false;
  }


  public MutableLiveData<Room> getRoomLiveData() {
    return roomLiveData;
  }

  public MutableLiveData<Participant> getActiveSpeakerLiveData() {
    return activeSpeakerLiveData;
  }

  public LocalParticipant getLocalParticipant() {
    return room != null ? room.getLocalParticipant() : null;
  }

  public LocalVideoTrack getLocalVideoTrack() {
    if (room == null || room.getLocalParticipant() == null) return null;

    for (kotlin.Pair<TrackPublication, Track> pair : room.getLocalParticipant().getVideoTrackPublications()) {
      Track track = pair.getSecond();
      if (track instanceof LocalVideoTrack) {
        return (LocalVideoTrack) track;
      }
    }
    return null;
  }

  public boolean isMicrophoneEnabled() {
    return isMicrophoneEnabled;
  }

  public boolean isCameraEnabled() {
    return isCameraEnabled;
  }


  @Override
  public void onRoomConnected(Room room) {
    Log.d(TAG, "onRoomConnected: " + room.getName());
    isReconnecting = false;
    roomLiveData.postValue(room);

    Log.d(TAG, "LiveData updated, enabling local tracks");
    enableLocalTracks();

    processExistingParticipants();
  }

  @Override
  public void onRoomDisconnected() {
    Log.d(TAG, "onRoomDisconnected");
    if (!isReconnecting) {
      roomLiveData.postValue(null);
    }
  }

  @Override
  public void onReconnecting() {
    Log.d(TAG, "onReconnecting");
    isReconnecting = true;
  }

  @Override
  public void onReconnected() {
    Log.d(TAG, "onReconnected. artificially calling onRoomConnected()");
    isReconnecting = false;
    onRoomConnected(room);
  }

  @Override
  public void onActiveSpeakersChanged(List<Participant> speakers) {
    if (!speakers.isEmpty()) {
      activeSpeakerLiveData.postValue(speakers.get(0));
    }
  }

  @Override
  public void onTrackPublished(Participant participant, TrackPublication publication) {
    Log.d(TAG, "onTrackPublished: " + publication.getName() + " by " + participant.getParticipantInfo().getIdentity());

    if (publication.getTrack() instanceof RemoteVideoTrack &&
        publication.getSubscribed()) {
      onTrackSubscribed(participant, publication.getTrack());
    }
  }

  @Override
  public void onTrackSubscribed(Participant participant, Track track) {
    Log.d(TAG, "onTrackSubscribed: " + track.getName() +
               " by " + participant.getParticipantInfo().getIdentity() +
               " (isLocal=" + (participant instanceof LocalParticipant) + ")");

    if (participant instanceof LocalParticipant) {
      Log.d(TAG, "Skipping local track in onTrackSubscribed (handled elsewhere)");
    }
    if (track instanceof RemoteVideoTrack) {
      onRemoteVideoTrackReady((RemoteVideoTrack) track);
    }
  }

  @Override
  public void onTrackUnsubscribed(Track track) {
    Log.d(TAG, "onTrackUnsubscribed: " + track.getName());

    if (track instanceof RemoteVideoTrack) {
      removeRemoteVideoRenderer((RemoteVideoTrack) track);
    }
  }

  @Override
  public void onTrackMuted(Participant participant, TrackPublication publication) {
    String identity = participant.getParticipantInfo().getIdentity();
    Log.d(TAG, "onTrackMuted: " + publication.getKind().getValue() + " by " + identity);

    if (Track.Kind.VIDEO.equals(publication.getKind())) {
      if (participant instanceof RemoteParticipant) {
        if (cameraStateCallback != null) {
          cameraStateCallback.onRemoteCameraMuted(identity, true);
        }
//        showAvatarForParticipant(identity, true);
      } else if (participant instanceof LocalParticipant) {
//        showLocalAvatar(true);
      }
    }
  }

  @Override
  public void onTrackUnmuted(Participant participant, TrackPublication publication) {
    String identity = participant.getParticipantInfo().getIdentity();
    Log.d(TAG, "onTrackUnmuted: " + publication.getKind().getValue() + " by " + identity);

    if (Track.Kind.VIDEO.equals(publication.getKind())) {
      if (participant instanceof RemoteParticipant) {
        if (cameraStateCallback != null) {
          cameraStateCallback.onRemoteCameraMuted(identity, false);
        }
//        showAvatarForParticipant(identity, false);
      } else if (participant instanceof LocalParticipant) {
//        showLocalAvatar(false);
      }
    }
  }

  @Override
  public void onLocalTrackSubscribed(LocalVideoTrack track) {
    Log.d(TAG, "onLocalTrackSubscribed: attaching renderer to localVideoView");

    if (isLocalRendererAttached) {
      Log.d(TAG, "Local renderer already attached, skipping");
      return;
    }

    if (track == null) {
      Log.w(TAG, "track is null");
      return;
    }

    if (localVideoViewRef == null) {
      Log.w(TAG, "localVideoViewRef is null");
      return;
    }

    SurfaceViewRenderer renderer = localVideoViewRef.get();
    if (renderer == null) {
      Log.w(TAG, "localVideoView was garbage collected");
      return;
    }

    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      attachRendererToTrack(track, renderer);
    }, 150);
  }

  private void attachRendererToTrack(LocalVideoTrack track, SurfaceViewRenderer renderer) {
    Log.d(TAG, "View size: " + renderer.getWidth() + "x" + renderer.getHeight());

    if (renderer.getWidth() == 0 || renderer.getHeight() == 0) {
      Log.w(TAG, "View has 0x0 size, retrying in 200ms");
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (renderer.getWidth() > 0 && renderer.getHeight() > 0) {
          track.addRenderer(renderer);
          renderer.requestLayout();
          isLocalRendererAttached = true;
          Log.d(TAG, "Local video renderer attached (retry after layout)");
        }
      }, 200);
      return;
    }

    track.addRenderer(renderer);
    renderer.requestLayout();
    isLocalRendererAttached = true;
    Log.d(TAG, "Local video renderer attached via onLocalTrackSubscribed");

    if (uiCallback != null) {
      uiCallback.onLocalVideoReady();
    }
  }

  private void showAvatarForParticipant(String identity, boolean show) {
    if (avatarCallback != null) {
      avatarCallback.onAvatarStateChanged(identity, show);
    }
  }

  private void showLocalAvatar(boolean show) {
    if (avatarCallback != null) {
      avatarCallback.onLocalAvatarStateChanged(show);
    }
  }

  public void setAvatarCallback(AvatarCallback avatarCallback) {
    this.avatarCallback = avatarCallback;
  }

  @Override
  public void onParticipantConnected(Participant participant) {
    Log.d(TAG, "onParticipantConnected: " + participant.getParticipantInfo().getIdentity());
  }

  @Override
  public void onParticipantDisconnected(Participant participant) {
    Log.d(TAG, "onParticipantDisconnected: " + participant.getParticipantInfo().getIdentity());
  }

  @Override
  public void onError(Throwable error) {
    Log.e(TAG, "LiveKit error", error);
    if (error instanceof Exception) {
      connectionErrorLiveData.postValue((Exception) error);
    } else {
      connectionErrorLiveData.postValue(new RuntimeException(error));
    }
  }


  private void onRemoteVideoTrackReady(RemoteVideoTrack track) {
    Log.d(TAG, "Remote video track ready: " + track.getName());

    if (remoteVideoViewRef == null) {
      Log.w(TAG, "Remote video view not set");
      return;
    }

    SurfaceViewRenderer renderer = remoteVideoViewRef.get();
    if (renderer != null) {
      track.addRenderer(renderer);
      Log.d(TAG, "Renderer added to remote video track");
    } else {
      Log.w(TAG, "Remote video view was garbage collected");
    }
  }

  private void removeRemoteVideoRenderer(RemoteVideoTrack track) {
    if (remoteVideoViewRef == null) return;

    SurfaceViewRenderer renderer = remoteVideoViewRef.get();
    if (renderer != null) {
      track.removeRenderer(renderer);
      Log.d(TAG, "Renderer removed from remote video track");
    }
  }

  private void processExistingParticipants() {
    if (room == null) return;

    Log.d(TAG, "Processing existing remote participants");
    for (RemoteParticipant participant : room.getRemoteParticipants().values()) {
      Log.d(TAG, "Existing participant: " + participant.getParticipantInfo().getIdentity());
      for (kotlin.Pair<TrackPublication, Track> pair : participant.getVideoTrackPublications()) {
        TrackPublication publication = pair.getFirst();
        Track track = pair.getSecond();
        if (track instanceof RemoteVideoTrack && publication.getSubscribed()) {
          Log.d(TAG, "Existing track: " + publication.getTrack().getName());
          onRemoteVideoTrackReady((RemoteVideoTrack) track);
        }
      }
    }
  }

  private static final Continuation<Object> EMPTY_CONTINUATION = new Continuation<>() {
    @Override
    public CoroutineContext getContext() {
      return EmptyCoroutineContext.INSTANCE;
    }

    @Override
    public void resumeWith(Object o) {
    }
  };
}
