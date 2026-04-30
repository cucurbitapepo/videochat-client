package com.example.videochat.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.videochat.R;
import com.example.videochat.adapter.ContactAdapter;
import com.example.videochat.api.ApiClient;
import com.example.videochat.dialog.UserProfileDialog;
import com.example.videochat.dto.ContactDto;
import com.example.videochat.dto.UserSearchResultDto;
import com.example.videochat.search.SearchManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//public class ContactsFragment extends Fragment implements SearchManager.SearchCallback {
//
//  private TextInputEditText searchInput;
//  private RecyclerView resultsRecycler;
//  private ProgressBar progressBar;
//  private TextView emptyView;
//  private SearchManager searchManager;
//  private UserSearchAdapter adapter;
//
//  @Override
//  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//    View view = inflater.inflate(R.layout.fragment_contacts, container, false);
//
//    searchInput = view.findViewById(R.id.search_input);
//    resultsRecycler = view.findViewById(R.id.results_recycler);
//    progressBar = view.findViewById(R.id.progress_bar);
//    emptyView = view.findViewById(R.id.empty_view);
//
//    resultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
//    adapter = new UserSearchAdapter();
//    resultsRecycler.setAdapter(adapter);
//
//    searchManager = new SearchManager();
//    searchManager.setSearchCallback(this);
//
//    searchInput.addTextChangedListener(new TextWatcher() {
//      @Override
//      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//      @Override
//      public void onTextChanged(CharSequence s, int start, int before, int count) {}
//
//      @Override
//      public void afterTextChanged(Editable s) {
//        searchManager.query(s.toString());
//      }
//    });
//
//    return view;
//  }
//
//  @Override
//  public void onResults(List<UserSearchResultDto> results) {
//    if (results == null || results.isEmpty()) {
//      adapter.setUsers(null);
//      emptyView.setVisibility(View.VISIBLE);
//    } else {
//      adapter.setUsers(results);
//      emptyView.setVisibility(View.GONE);
//    }
//  }
//
//  @Override
//  public void onLoading(boolean isLoading) {
//    progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
//  }
//
//  @Override
//  public void onError(String message) {
//    emptyView.setText(message);
//    emptyView.setVisibility(View.VISIBLE);
//  }
//
//  @Override
//  public void onEmptyQuery() {
//    adapter.setUsers(null);
//    emptyView.setText("Введите минимум 3 символа");
//    emptyView.setVisibility(View.VISIBLE);
//  }
//}


public class ContactsFragment extends Fragment implements SearchManager.SearchCallback {

  private TextInputEditText searchInput;
  private RecyclerView resultsRecycler;
  private ProgressBar progressBar;
  private TextView emptyView;
  private SearchManager searchManager;
  private ContactAdapter adapter;
  private boolean isSearching = false;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_contacts, container, false);

    // Инициализация UI
    searchInput = view.findViewById(R.id.search_input);
    resultsRecycler = view.findViewById(R.id.results_recycler);
    progressBar = view.findViewById(R.id.progress_bar);
    emptyView = view.findViewById(R.id.empty_view);

    // Настройка RecyclerView
    resultsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
    adapter = new ContactAdapter(user -> {
      showUserProfile(user);
      return null;
    });
    resultsRecycler.setAdapter(adapter);

    // Инициализация SearchManager
    searchManager = new SearchManager();
    searchManager.setSearchCallback(this);

    // Настройка TextWatcher
    searchInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        String query = s.toString().trim();
        if (query.isEmpty()) {
          isSearching = false;
          loadContacts();
        } else {
          isSearching = true;
          searchManager.query(query);
        }
      }
    });

    // Загрузка контактов при первом открытии
    loadContacts();

    return view;
  }

  private void loadContacts() {
    progressBar.setVisibility(View.VISIBLE);
    emptyView.setVisibility(View.GONE);

    ApiClient.getContactsApi().getContacts().enqueue(new Callback<>() {
      @Override
      public void onResponse(Call<List<ContactDto>> call, Response<List<ContactDto>> response) {
        progressBar.setVisibility(View.GONE);

        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
          // Преобразуем ContactDto в UserSearchResultDto
          List<UserSearchResultDto> contacts = response.body().stream()
                  .map(contact -> {
                    UserSearchResultDto dto = new UserSearchResultDto();
                    dto.setId(contact.getContact().getId());
                    dto.setUsername(contact.getContact().getUsername());
                    dto.setOnline(false);
                    dto.setIsContact(true);
                    return dto;
                  })
                  .collect(Collectors.toList());

          adapter.setContacts(contacts);
          emptyView.setVisibility(View.GONE);
        } else {
          adapter.setContacts(new ArrayList<>());
          emptyView.setText("Контактов нет. Начните поиск пользователей");
          emptyView.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onFailure(Call<List<ContactDto>> call, Throwable t) {
        progressBar.setVisibility(View.GONE);
        emptyView.setText("Не удалось загрузить контакты");
        emptyView.setVisibility(View.VISIBLE);
      }
    });
  }

  private void showUserProfile(UserSearchResultDto user) {
    UserProfileDialog dialog = UserProfileDialog.newInstance(user);
    dialog.show(getParentFragmentManager(), "user_profile");
  }

  // Реализация SearchCallback
  @Override
  public void onResults(List<UserSearchResultDto> results) {
    if (results == null || results.isEmpty()) {
      adapter.setSearchResults(new ArrayList<>(), new ArrayList<>());
      emptyView.setText("Пользователи не найдены");
      emptyView.setVisibility(View.VISIBLE);
    } else {
      // Разделяем результаты на контакты и не-контакты
      List<UserSearchResultDto> contacts = new ArrayList<>();
      List<UserSearchResultDto> others = new ArrayList<>();

      for (UserSearchResultDto user : results) {
        if (user.isContact()) {
          contacts.add(user);
        } else {
          others.add(user);
        }
      }

      adapter.setSearchResults(contacts, others);
      emptyView.setVisibility(View.GONE);
    }
  }

  @Override
  public void onLoading(boolean isLoading) {
    if (isSearching) {
      progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
  }

  @Override
  public void onError(String message) {
    progressBar.setVisibility(View.GONE);
    emptyView.setText(message);
    emptyView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onEmptyQuery() {
    if (!isSearching) {
      loadContacts();
    }
  }
}