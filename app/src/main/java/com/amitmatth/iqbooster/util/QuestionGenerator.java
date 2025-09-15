package com.amitmatth.iqbooster.util;

import com.amitmatth.iqbooster.data.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuestionGenerator {

    private static final Random random = new Random();

    public static List<Question> generateQuestions(String category, int numberOfQuestions, String difficulty) {
        List<Question> generatedQuestions = new ArrayList<>();

        for (int i = 0; i < numberOfQuestions; i++) {
            Question question = null;
            int questionId = i + 1;

            switch (category.toLowerCase()) {
                case "arithmetic":
                    question = generateArithmeticQuestion(questionId, difficulty);
                    break;
                case "unit_conversion":
                    question = generateUnitConversionQuestion(questionId, difficulty);
                    break;
                case "square_root":
                    question = generateSquareRootQuestion(questionId, difficulty);
                    break;
                default:
                    question = new Question(questionId, "Unsupported category: " + category, "N/A", "N/A", "N/A", "N/A", "N/A", "Please select a valid category.");
                    break;
            }
            if (question != null) {
                generatedQuestions.add(question);
            }
        }
        return generatedQuestions;
    }

    private static Question generateArithmeticQuestion(int id, String difficulty) {
        int num1, num2;
        String operationSymbol;
        int correctAnswer;
        String questionText;
        String explanation;

        int maxOperand = 10;
        if ("Medium".equalsIgnoreCase(difficulty)) {
            maxOperand = 50;
        } else if ("Hard".equalsIgnoreCase(difficulty)) {
            maxOperand = 100;
        }

        num1 = random.nextInt(maxOperand) + 1;
        num2 = random.nextInt(maxOperand) + 1;

        int operationType = random.nextInt(4);

        switch (operationType) {
            case 0:
                operationSymbol = "+";
                correctAnswer = num1 + num2;
                questionText = String.format("What is %d + %d?", num1, num2);
                explanation = String.format("%d plus %d equals %d.", num1, num2, correctAnswer);
                break;
            case 1:
                operationSymbol = "-";
                if (num1 < num2 && !"Hard".equalsIgnoreCase(difficulty)) {
                    int temp = num1;
                    num1 = num2;
                    num2 = temp;
                }
                correctAnswer = num1 - num2;
                questionText = String.format("What is %d - %d?", num1, num2);
                explanation = String.format("%d minus %d equals %d.", num1, num2, correctAnswer);
                break;
            case 2:
                operationSymbol = "*";
                if ("Easy".equalsIgnoreCase(difficulty)) {
                    num1 = random.nextInt(10) + 1;
                    num2 = random.nextInt(10) + 1;
                } else if ("Medium".equalsIgnoreCase(difficulty)) {
                    num1 = random.nextInt(20) + 1;
                    num2 = random.nextInt(12) + 1;
                }
                correctAnswer = num1 * num2;
                questionText = String.format("What is %d * %d?", num1, num2);
                explanation = String.format("%d multiplied by %d equals %d.", num1, num2, correctAnswer);
                break;
            case 3:
            default:
                operationSymbol = "÷";
                num2 = random.nextInt(maxOperand / 2 > 0 ? maxOperand / 2 : 1) + 1; // num2 is not too large and > 0
                if ("Easy".equalsIgnoreCase(difficulty)) num2 = random.nextInt(10) + 1;

                correctAnswer = random.nextInt(10) + 1;
                num1 = correctAnswer * num2;
                questionText = String.format("What is %d ÷ %d?", num1, num2);
                explanation = String.format("%d divided by %d equals %d.", num1, num2, correctAnswer);
                break;
        }

        List<String> options = generateDistractorOptions(String.valueOf(correctAnswer), correctAnswer - (maxOperand / 2), correctAnswer + (maxOperand / 2));
        return new Question(id, questionText, options.get(0), options.get(1), options.get(2), options.get(3), String.valueOf(correctAnswer), explanation);
    }

    private static Question generateUnitConversionQuestion(int id, String difficulty) {
        String[] fromUnits = {"m", "kg", "L", "hr"};
        String[] toUnits = {"cm", "g", "mL", "min"};
        double[] factors = {100, 1000, 1000, 60};

        int type = random.nextInt(fromUnits.length);
        int value = random.nextInt("Hard".equalsIgnoreCase(difficulty) ? 50 : ("Medium".equalsIgnoreCase(difficulty) ? 20 : 10)) + 1;

        String questionText = String.format("Convert %d %s to %s.", value, fromUnits[type], toUnits[type]);
        int correctAnswer = (int) (value * factors[type]);
        String explanation = String.format("%d %s is %d %s. (1 %s = %.0f %s)", value, fromUnits[type], correctAnswer, toUnits[type], fromUnits[type], factors[type], toUnits[type]);

        List<String> options = generateDistractorOptions(String.valueOf(correctAnswer), correctAnswer / 2, correctAnswer * 2);
        return new Question(id, questionText, options.get(0), options.get(1), options.get(2), options.get(3), String.valueOf(correctAnswer), explanation);
    }

    private static Question generateSquareRootQuestion(int id, String difficulty) {
        int baseMax = "Hard".equalsIgnoreCase(difficulty) ? 25 : ("Medium".equalsIgnoreCase(difficulty) ? 15 : 10);
        int base = random.nextInt(baseMax - 2 + 1) + 2;
        int number = base * base;

        String questionText = String.format("What is the square root of %d?", number);
        String correctAnswer = String.valueOf(base);
        String explanation = String.format("The square root of %d is %s, because %s * %s = %d.", number, correctAnswer, correctAnswer, correctAnswer, number);

        List<String> options = generateDistractorOptions(correctAnswer, base - 3, base + 3);
        return new Question(id, questionText, options.get(0), options.get(1), options.get(2), options.get(3), correctAnswer, explanation);
    }

    private static List<String> generateDistractorOptions(String correctAnswer, int minRange, int maxRange) {
        List<String> options = new ArrayList<>();
        options.add(correctAnswer);

        if (minRange <= 0) minRange = 1;
        if (maxRange <= Integer.parseInt(correctAnswer))
            maxRange = Integer.parseInt(correctAnswer) + 10;
        if (minRange >= maxRange) minRange = maxRange - 5 > 0 ? maxRange - 5 : 1;


        while (options.size() < 4) {
            int distractorValue = random.nextInt(maxRange - minRange + 1) + minRange;
            String distractor = String.valueOf(distractorValue);
            if (!options.contains(distractor) && distractorValue > 0) {
                options.add(distractor);
            }
        }
        java.util.Collections.shuffle(options);
        return options;
    }
}