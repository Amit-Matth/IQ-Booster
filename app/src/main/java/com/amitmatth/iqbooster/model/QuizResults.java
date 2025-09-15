package com.amitmatth.iqbooster.model;

public class QuizResults {
    public int totalQuestions;
    public int correct;
    public int skipped;
    public int wrong;
    public int totalQuizzes;

    public QuizResults() {
    }

    public QuizResults(int totalQuestions, int correct, int skipped, int wrong, int totalQuizzes) {
        this.totalQuestions = totalQuestions;
        this.correct = correct;
        this.skipped = skipped;
        this.wrong = wrong;
        this.totalQuizzes = totalQuizzes;
    }
}