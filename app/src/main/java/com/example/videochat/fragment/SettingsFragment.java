package com.example.videochat.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.videochat.R;
import com.example.videochat.util.SettingsManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

  private SwitchMaterial switchMuteMic;
  private SwitchMaterial switchMuteCam;
  private SettingsManager settingsManager;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_settings, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    settingsManager = SettingsManager.getInstance(requireContext());

    initViews(view);
    loadSettings();
    setupListeners();
  }

  private void initViews(View view) {
    switchMuteMic = view.findViewById(R.id.switch_mute_mic);
    switchMuteCam = view.findViewById(R.id.switch_mute_cam);
  }

  private void loadSettings() {
    switchMuteMic.setChecked(settingsManager.shouldMuteMicOnJoin());
    switchMuteCam.setChecked(settingsManager.shouldMuteCamOnJoin());
  }

  private void setupListeners() {
    switchMuteMic.setOnCheckedChangeListener((buttonView, isChecked) -> {
      settingsManager.setMuteMicOnJoin(isChecked);
      Log.d("Settings", "Mute mic on join: " + isChecked);
    });

    switchMuteCam.setOnCheckedChangeListener((buttonView, isChecked) -> {
      settingsManager.setMuteCamOnJoin(isChecked);
      Log.d("Settings", "Mute cam on join: " + isChecked);
    });
  }
}