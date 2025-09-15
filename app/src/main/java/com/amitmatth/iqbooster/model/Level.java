package com.amitmatth.iqbooster.model;

public class Level {
    private int levelNumber;
    private String name;
    private int requiredScore;
    private int earnedDrawableResId;
    private int lockedDrawableResId;
    private boolean isUnlocked;
    private int userScoreForThisLevel;
    private int previousLevelScoreRequirement;

    public Level(int levelNumber, String name, int requiredScore, int earnedDrawableResId, int lockedDrawableResId) {
        this.levelNumber = levelNumber;
        this.name = name;
        this.requiredScore = requiredScore;
        this.earnedDrawableResId = earnedDrawableResId;
        this.lockedDrawableResId = lockedDrawableResId;
        this.isUnlocked = false;
        this.userScoreForThisLevel = 0;
        this.previousLevelScoreRequirement = 0;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public String getName() {
        return name;
    }

    public int getRequiredScore() {
        return requiredScore;
    }

    public int getEarnedDrawableResId() {
        return earnedDrawableResId;
    }

    public int getLockedDrawableResId() {
        return lockedDrawableResId;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public int getUserScoreForThisLevel() {
        return userScoreForThisLevel;
    }

    public void setUserScoreForThisLevel(int userScoreForThisLevel) {
        this.userScoreForThisLevel = userScoreForThisLevel;
    }

    public int getPreviousLevelScoreRequirement() {
        return previousLevelScoreRequirement;
    }

    public void setPreviousLevelScoreRequirement(int previousLevelScoreRequirement) {
        this.previousLevelScoreRequirement = previousLevelScoreRequirement;
    }
}