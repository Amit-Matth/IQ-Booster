package com.amitmatth.iqbooster.helper;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.amitmatth.iqbooster.activities.QuestionsActivity;
import com.amitmatth.iqbooster.data.Question;
import com.amitmatth.iqbooster.databinding.ActivityQuestionsGeneratorBinding;
import com.amitmatth.iqbooster.network.RemoteConfigHelper;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class QuestionsGeneratorActivity extends AppCompatActivity {

    private ActivityQuestionsGeneratorBinding binding;

    List<Question> list = new ArrayList<>();

    private ListenableFuture<GenerateContentResponse> response;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionsGeneratorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Intent intent = getIntent();
        String topic = intent.getStringExtra("topic");
        String questionFormat = intent.getStringExtra("questionFormat");

        RemoteConfigHelper remoteConfigHelper = new RemoteConfigHelper();
        remoteConfigHelper.fetchApiKey(apiKey -> {

        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash",
                apiKey);

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        int numberOfQuestions = 5; // This can be user-selected

        Content content = new Content.Builder()
                .addText("Generate an array named 'list' containing five" +
                        " unique " + topic + " in JSON format. Each question should have an 'id', " +
                        "'questionText', four answer choices ('option1', 'option2', 'option3', 'option4'), and a 'correctAnswer'. Example:\n" +
                        "{\n" +
                        "  \"list\": [\n" +
                        "    {\n" +
                        "      \"id\": 1,\n" +
                        "      \"questionText\": \"" + questionFormat + "\",\n" +
                        "      \"option1\": \"18\",\n" +
                        "      \"option2\": \"20\",\n" +
                        "      \"option3\": \"22\",\n" +
                        "      \"option4\": \"24\",\n" +
                        "      \"correctAnswer\": \"20\"\n" +
                        "    },\n" +
                        "    ... (Generate " + (numberOfQuestions - 1) + " more unique questions that can be solved in five seconds)\n" +
                        "  ]\n" +
                        "}")
                .build();



        response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                if (result != null && result.getText() != null) {

                    try {
                        // Log raw response for debugging
                        Log.d("gemini JSON Debug", "Raw Response: " + result.getText());

                        // Step 1: Find first { and last }
                        int startIndex = result.getText().indexOf("{");
                        int endIndex = result.getText().lastIndexOf("}");

                        // Validate positions to avoid errors
                        if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                            Log.e("gemini JSON Error", "Invalid JSON format - Missing curly braces!");
                            return;
                        }

                        // Step 2: Extract valid JSON part
                        String jsonString = result.getText().substring(startIndex, endIndex + 1).trim();

//                        Log.d("gemini response JSON Debug", "Cleaned JSON: " + jsonString);

                        Gson gson = new Gson();

                        // Step 3: Convert JSON string to JsonObject
                        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

                        // Check if "list" exists in JSON
                        if (!jsonObject.has("list")) {
                            Log.e("gemini JSON Error", "Missing 'list' key in JSON!");
                            return;
                        }

                        // Step 4: Extract "list" array
                        Type listType = new TypeToken<List<Question>>() {
                        }.getType();
                        List<Question> questionList = gson.fromJson(jsonObject.getAsJsonArray("list"), listType);

                        // Step 5: Add questions to the list
                        if (questionList == null || questionList.isEmpty()) {
                            Log.e("gemini JSON Error", "Parsed list is null or empty!");
                            return;
                        }

                        for (Question q : questionList) {
                            if (q == null) {
                                Log.e("gemini JSON Error", "Parsed question is null!");
                                continue;
                            }

//                            // Log each field to check if they are properly extracted
//                            Log.d("gemini response Parsed Question", "ID: " + q.getId());
//                            Log.d("gemini response Parsed Question", "Question: " + (q.getQuestionText() != null ? q.getQuestionText() : "NULL"));
//                            Log.d("gemini responseParsed Question", "Option1: " + q.getOption1());
//                            Log.d("gemini response Parsed Question", "Option2: " + q.getOption2());
//                            Log.d("gemini response Parsed Question", "Option3: " + q.getOption3());
//                            Log.d("gemini response Parsed Question", "Option4: " + q.getOption4());
//                            Log.d("gemini response Parsed Question", "Correct Answer: " + (q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "NULL"));

                            list.add(q);  // Assuming 'list' is your global question list
                        }

//                        Log.d("gemini response", "the list is " + new Gson().toJson(questionList));

                        Intent intent = new Intent(QuestionsGeneratorActivity.this, QuestionsActivity.class);
                        intent.putExtra("questions", new Gson().toJson(questionList)); // Convert list to JSON string

                        binding.startQuizBtn.setVisibility(View.VISIBLE);
                        binding.preparingQuizProgressBar.setVisibility(View.GONE);
                        binding.preparingQuizTxt.setText("Your quiz prepared successfully...");

                        binding.startQuizBtn.setOnClickListener(v->{
                            startActivity(intent);
                            finish();
                        });

                    } catch (Exception e) {
                        Log.e("gemini JSON Error", "Unexpected error while parsing JSON", e);
                    }

                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d("gemini error", "Error" + t.getMessage());
            }
        }, getExecutor());

        });

    }
    private static Executor getExecutor() {
        return Runnable::run; // Use a proper Executor (like a UI thread executor)
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (response != null && !response.isDone()) {
            response.cancel(true);
            Log.d("gemini response", "Request cancelled as activity is destroyed.");
        }

    }


}