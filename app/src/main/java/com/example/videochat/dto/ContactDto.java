package com.example.videochat.dto;

import java.io.Serializable;

public class ContactDto implements Serializable {
  private Long id;
  private UserDto contact;
  private String alias;
  private boolean blocked;
  private String createdAt;
  private String updatedAt;

  public ContactDto() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public UserDto getContact() { return contact; }
  public void setContact(UserDto contact) { this.contact = contact; }

  public String getAlias() { return alias; }
  public void setAlias(String alias) { this.alias = alias; }

  public boolean isBlocked() { return blocked; }
  public void setBlocked(boolean blocked) { this.blocked = blocked; }

  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

  public String getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
