package com.huangyangwei.iknow.api.dto.auth;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户信息：基础资料 + 角色列表。
 */
public class UserInfo implements Serializable {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private List<String> roles;
    private LocalDateTime createdAt;

    public UserInfo() {
    }

    public UserInfo(Long id, String username, String email, String nickname, List<String> roles, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
