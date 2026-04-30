package com.example.videochat.dto;

public class Pageable {
  private int offset;
  private int pageNumber;
  private int pageSize;
  private boolean paged;
  private Sort sort;
  private boolean unpaged;

  public int getOffset() { return offset; }
  public int getPageNumber() { return pageNumber; }
  public int getPageSize() { return pageSize; }
  public boolean isPaged() { return paged; }
  public Sort getSort() { return sort; }
  public boolean isUnpaged() { return unpaged; }
}
