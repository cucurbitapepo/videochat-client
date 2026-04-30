package com.example.videochat.dto;

import java.util.List;

public class PageResponse<T> {
  private List<T> content;
  private boolean empty;
  private boolean first;
  private boolean last;
  private int number;
  private int numberOfElements;
  private Pageable pageable;
  private int size;
  private Sort sort;
  private int totalElements;
  private int totalPages;

  public List<T> getContent() { return content; }
  public boolean isEmpty() { return empty; }
  public boolean isFirst() { return first; }
  public boolean isLast() { return last; }
  public int getNumber() { return number; }
  public int getNumberOfElements() { return numberOfElements; }
  public Pageable getPageable() { return pageable; }
  public int getSize() { return size; }
  public Sort getSort() { return sort; }
  public int getTotalElements() { return totalElements; }
  public int getTotalPages() { return totalPages; }
}