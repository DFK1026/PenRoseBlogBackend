package com.kirisamemarisa.blog.dto;

public class UserRegisterDTO {
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String background;
    private String gender;

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }
}
