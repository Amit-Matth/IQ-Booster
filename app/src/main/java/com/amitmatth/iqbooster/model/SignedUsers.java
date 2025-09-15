package com.amitmatth.iqbooster.model;

import com.google.gson.annotations.SerializedName;

public class SignedUsers implements Comparable<SignedUsers> {

    @SerializedName("name")
    public String name;
    @SerializedName("email")
    public String email;
    @SerializedName("photoUrl")
    public String photoUrl;
    @SerializedName("score")
    public int score;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public SignedUsers() {
    }

    public SignedUsers(String name, String email, String photoUrl, int score) {
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
        this.score = score;
    }

    @Override
    public int compareTo(SignedUsers other) {
        return Integer.compare(other.score, this.score);
    }
}