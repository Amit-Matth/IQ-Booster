package com.amitmatth.iqbooster.data;

import android.media.Image;
import android.widget.ImageView;

public class Users {
    public String photoUrl;
    public String name;

    private int id;
    public int score;
    public int rank;

    public int totalQuestions;

    public Users(String photoUrl, String name, int id, int score, int rank, int totalQuestions) {
        this.name = name;
        this.photoUrl = photoUrl;
        this.id = id;
        this.score = score;
        this.rank = rank;
        this.totalQuestions = totalQuestions;
    }

    public Users() {}

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScore() {
        return this.score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getRank() {
        return this.rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getProfilePicUrl() {
        return photoUrl;
    }

    public void setProfilePicUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }


    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

}
