package com.amitmatth.iqbooster.model;

public class Badge {
    private String name;
    private String description;
    private int earnedDrawableResId;
    private int lockedDrawableResId;
    private int requiredCorrectAnswers;
    private boolean isUnlocked;
    private int currentProgress;

    public Badge(String name, String description, int earnedDrawableResId, int lockedDrawableResId, int requiredCorrectAnswers) {
        this.name = name;
        this.description = description;
        this.earnedDrawableResId = earnedDrawableResId;
        this.lockedDrawableResId = lockedDrawableResId;
        this.requiredCorrectAnswers = requiredCorrectAnswers;
        this.isUnlocked = false;
        this.currentProgress = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEarnedDrawableResId() {
        return earnedDrawableResId;
    }

    public void setEarnedDrawableResId(int earnedDrawableResId) {
        this.earnedDrawableResId = earnedDrawableResId;
    }

    public int getLockedDrawableResId() {
        return lockedDrawableResId;
    }

    public void setLockedDrawableResId(int lockedDrawableResId) {
        this.lockedDrawableResId = lockedDrawableResId;
    }

    public int getRequiredCorrectAnswers() {
        return requiredCorrectAnswers;
    }

    public void setRequiredCorrectAnswers(int requiredCorrectAnswers) {
        this.requiredCorrectAnswers = requiredCorrectAnswers;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }
}