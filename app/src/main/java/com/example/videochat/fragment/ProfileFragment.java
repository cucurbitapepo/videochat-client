package com.example.videochat.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.videochat.R;
import com.example.videochat.activity.SplashActivity;

public class ProfileFragment extends Fragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_profile, container, false);

    Button logoutButton = view.findViewById(R.id.logout_button);
    logoutButton.setOnClickListener(v -> showLogoutDialog());

    return view;
  }

  private void showLogoutDialog() {
    new AlertDialog.Builder(requireContext())
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти", (d, w) -> performLogout())
            .setNegativeButton("Отмена", null)
            .show();
  }

  private void performLogout() {
    requireContext().getSharedPreferences("app_data", 0)
            .edit()
            .remove("auth_token")
            .apply();

    Intent intent = new Intent(requireContext(), SplashActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
    requireActivity().finish();
  }
}