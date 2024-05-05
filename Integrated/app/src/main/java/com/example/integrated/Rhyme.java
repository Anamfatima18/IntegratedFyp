package com.example.integrated;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Rhyme extends AppCompatActivity {
    private TextView levelNumberTextView, taskNumberTextView, wordTextView, scoreTextView;
    private Button option1Button, option2Button, option3Button, option4Button;
    private MediaPlayer successSound, failureSound;

    private LottieAnimationView animationView;

    private int currentLevel = 1;
    private int currentTask = 1;
    private final int maxTasksPerLevel = 5;
    private int currentLevelScore = 0;
    private int currentLevelTries = 0;
    private int cumulativeScore = 0;
    private int totalTries = 0;

    @Override
    protected void onPause() {
        super.onPause();
        saveGameState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGameState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rhyme);
        initializeUI();

        // Initialize media players
        successSound = MediaPlayer.create(this, R.raw.success);
        failureSound = MediaPlayer.create(this, R.raw.failure);

        // Initialize the animation view from your layout
        animationView = findViewById(R.id.animation_view);

        // Check if the activity is started with the intention to reset
        boolean shouldReset = getIntent().getBooleanExtra("reset", false);

        if (shouldReset) {
            // Optionally clear SharedPreferences
            SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            // Reset all progress to the initial state
            resetGameOrNavigate(); // Use your existing method to reset the game
        } else {
            // Load progress from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
            currentLevel = prefs.getInt("currentLevel", 1);
            currentTask = prefs.getInt("currentTask", 1);
            currentLevelScore = prefs.getInt("currentLevelScore", 0);
            currentLevelTries = prefs.getInt("currentLevelTries", 0);
            cumulativeScore = prefs.getInt("cumulativeScore", 0);
            totalTries = prefs.getInt("totalTries", 0);
        }

        updateUIForNewLevel();
        updateScoreDisplay();
        fetchRhymingTask(currentLevel);
    }

    private void resetGameOrNavigate() {
// Clear the saved state
        SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        // Navigate to the first level
        currentLevel = 1;
        currentTask = 1;
        currentLevelScore = 0;
        currentLevelTries = 0;
        cumulativeScore = 0;
        totalTries = 0;
    }


    private void saveGameState() {
        SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("currentLevel", currentLevel);
        editor.putInt("currentTask", currentTask);
        editor.putInt("currentLevelScore", currentLevelScore);
        editor.putInt("currentLevelTries", currentLevelTries);
        editor.putInt("cumulativeScore", cumulativeScore);
        editor.putInt("totalTries", totalTries);
        editor.apply();
    }

    private void loadGameState() {
        SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
        currentLevel = prefs.getInt("currentLevel", 1);
        currentTask = prefs.getInt("currentTask", 1);
        currentLevelScore = prefs.getInt("currentLevelScore", 0);
        currentLevelTries = prefs.getInt("currentLevelTries", 0);
        cumulativeScore = prefs.getInt("cumulativeScore", 0);
        totalTries = prefs.getInt("totalTries", 0);
    }

    private void initializeUI() {
        levelNumberTextView = findViewById(R.id.levelNumberTextView);
        taskNumberTextView = findViewById(R.id.taskNumberTextView);
        wordTextView = findViewById(R.id.wordTextView);
        scoreTextView = findViewById(R.id.scoreTextView);
        option1Button = findViewById(R.id.option1Button);
        option2Button = findViewById(R.id.option2Button);
        option3Button = findViewById(R.id.option3Button);
        option4Button = findViewById(R.id.option4Button);

        setupOptionButtons();
        loadSounds();
    }

    private void setupOptionButtons() {
        View.OnClickListener optionClickListener = view -> {
            Button clickedButton = (Button) view;
            String selectedOption = clickedButton.getText().toString();
            validateOption(selectedOption, clickedButton);
        };

        option1Button.setOnClickListener(optionClickListener);
        option2Button.setOnClickListener(optionClickListener);
        option3Button.setOnClickListener(optionClickListener);
        option4Button.setOnClickListener(optionClickListener);
    }

    private void validateOption(String selectedOption, Button clickedButton) {
        currentLevelTries++;
        totalTries++;
        String url = " http://172.17.40.130:5000/submit_rhyming_answer";

        try {
            JSONObject postData = new JSONObject();
            postData.put("word", wordTextView.getText().toString());
            postData.put("chosen_option", selectedOption);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                    response -> {
                        runOnUiThread(() -> {
                            try {
                                String result = response.getString("result");
                                if ("correct".equals(result)) {
                                    currentLevelScore++;
                                    cumulativeScore++;
                                    ViewCompat.setBackgroundTintList(clickedButton, ColorStateList.valueOf(ContextCompat.getColor(Rhyme.this, R.color.correct_answer)));
                                } else {
                                    ViewCompat.setBackgroundTintList(clickedButton, ColorStateList.valueOf(ContextCompat.getColor(Rhyme.this, R.color.incorrect_answer)));
                                }
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    advanceTaskOrShowScore();
                                    ViewCompat.setBackgroundTintList(clickedButton, ColorStateList.valueOf(ContextCompat.getColor(Rhyme.this, R.color.button_default)));
                                }, 5000);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });

                    },
                    error -> Toast.makeText(Rhyme.this, "Error validating answer", Toast.LENGTH_SHORT).show());

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void advanceTaskOrShowScore() {
        currentTask++;
        updateScoreDisplay();
        if (currentTask > maxTasksPerLevel) {
            showLevelScoreAndDecideNextStep();
        } else {
            taskNumberTextView.setText(String.valueOf(currentTask));
            fetchRhymingTask(currentLevel);
        }
    }

    private void showLevelScoreAndDecideNextStep() {
        double scoreRatio = (double) currentLevelScore / currentLevelTries;
        if (currentLevel == 3) {
            submitScore();
            if (scoreRatio >= 0.8) {
                playSoundAndAnimation(true);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Level " + currentLevel + " completed.\nYour score: " + currentLevelScore + "/" + currentLevelTries);
                builder.setPositiveButton("OK", (dialog, which) -> showFinalResults());
                builder.create().show();
            } else {
                playSoundAndAnimation(false);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Level " + currentLevel + " completed.\nYour score: " + currentLevelScore + "/" + currentLevelTries);
                builder.setPositiveButton("Retry", (dialog, which) -> restartCurrentLevel());
                builder.create().show();
            }
            currentLevelScore = 0;
            currentLevelTries = 0;
        } else {
            submitScore();
            if (scoreRatio >= 0.8) {
                playSoundAndAnimation(true);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Level " + currentLevel + " completed.\nYour score: " + currentLevelScore + "/" + currentLevelTries);
                builder.setPositiveButton("Continue", (dialog, which) -> {
                    advanceToNextLevel();
                });
                builder.create().show();
            } else {
                playSoundAndAnimation(false);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Level " + currentLevel + " completed.\nYour score: " + currentLevelScore + "/" + currentLevelTries);
                builder.setPositiveButton("Retry", (dialog, which) -> {
                    restartCurrentLevel();
                });
                builder.create().show();
            }
            currentLevelScore = 0;
            currentLevelTries = 0;
        }
    }




    private void advanceToNextLevel() {
        currentLevel++;
        currentTask = 1;
        currentLevelScore = 0;
        currentLevelTries = 0;

        if (currentLevel > 3) {
            showFinalResults();
        } else {
            updateUIForNewLevel();
        }
    }

    private void restartCurrentLevel() {
        currentTask = 1;
        updateUIForNewLevel();
    }

    private void updateUIForNewLevel() {
        LinearLayout rootLayout = findViewById(R.id.rootLayout);
        Drawable backgroundDrawable;
        switch (currentLevel) {
            case 2:
                backgroundDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.level_two_rhyming);
                break;
            case 3:
                backgroundDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.level_three_rhyming);
                break;
            default:
                backgroundDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.level_one_rhyming);
                break;
        }

        if (backgroundDrawable != null) {
            rootLayout.setBackground(backgroundDrawable);
        }

        levelNumberTextView.setText(String.valueOf(currentLevel));
        taskNumberTextView.setText(String.valueOf(currentTask));
        fetchRhymingTask(currentLevel);
        updateScoreDisplay();
    }

    private void updateScoreDisplay() {
        String scoreText = "Score: " + currentLevelScore + "/" + currentLevelTries;
        scoreTextView.setText(scoreText);
    }

    private void showFinalResults() {
        clearSavedState();
        Intent finalScoreIntent = new Intent(Rhyme.this, FinalScore.class);
        finalScoreIntent.putExtra("activityClass", Rhyme.class.getName());
        finalScoreIntent.putExtra("cumulativeScore", cumulativeScore);
        finalScoreIntent.putExtra("totalTries", totalTries);
        startActivity(finalScoreIntent);
        finish();
    }

    private void clearSavedState() {
        SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void fetchRhymingTask(int level) {
        String url = " http://172.17.40.130:5000/get_rhyming_task/" + level; // Ensure this URL matches your server

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String word = response.getString("word");
                        JSONArray optionsArray = response.getJSONArray("options");
                        ArrayList<String> options = new ArrayList<>();
                        for (int i = 0; i < optionsArray.length(); i++) {
                            options.add(optionsArray.getString(i));
                        }

                        wordTextView.setText(word); // Set the word for the current task
                        // Assign options to buttons
                        option1Button.setText(options.get(0));
                        option2Button.setText(options.get(1));
                        option3Button.setText(options.get(2));
                        option4Button.setText(options.get(3));
                    } catch (JSONException e) {
                        Toast.makeText(Rhyme.this, "Failed to parse task data.", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }, error -> Toast.makeText(Rhyme.this, "Error fetching the task.", Toast.LENGTH_SHORT).show());

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private void loadSounds() {
        successSound = MediaPlayer.create(this, R.raw.success);
        failureSound = MediaPlayer.create(this, R.raw.failure);
    }

    private void playSoundAndAnimation(boolean isSuccess) {
        MediaPlayer mediaPlayer = isSuccess ? successSound : failureSound;

        if (isSuccess) {
            mediaPlayer.start();
            String animationFile = isSuccess ? "success_animation.json" : "failure_animation.json";
            animationView.setAnimation(animationFile);
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();

            new Handler().postDelayed(() -> {
                animationView.setVisibility(View.GONE);
            }, 5000);
        } else {
            mediaPlayer.start();
            String animationFile = isSuccess ? "success_animation.json" : "failure_animation.json";
            animationView.setAnimation(animationFile);
            animationView.setVisibility(View.VISIBLE);
            animationView.playAnimation();

            new Handler().postDelayed(() -> {
                animationView.setVisibility(View.GONE);
            }, 5000);
        }
    }

    private void submitScore() {
        // Replace the placeholders with your actual user ID and server URL
        int userId = getUserIdFromSharedPreferences(); // Retrieve the user ID
        String url = "  http://172.17.40.130:5000/update_score"; // Replace with your server URL
        JSONObject postData = new JSONObject();
        try {
            postData.put("user_id", userId);
            postData.put("level", currentLevel);
            postData.put("score", currentLevelScore);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                    response -> {
                        // Handle response
                        Log.d("SubmitScore", "Score submitted successfully");
                    },
                    error -> {
                        // Handle error
                        Log.e("SubmitScore", "Error submitting score: " + error.getMessage());
                    });

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("SubmitScore", "Error creating JSON object for submitting score", e);
        }
    }

    private int getUserIdFromSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userIdString = prefs.getString("userId", ""); // Retrieve as String
        int userId = -1; // Default to -1 or any other sentinel value indicating "not found" or "invalid"
        try {
            userId = Integer.parseInt(userIdString); // Convert String to Integer
        } catch (NumberFormatException e) {
            // Handle the case where userId is not a valid integer
            Log.e("Rhyme", "Invalid userId in SharedPreferences", e);
        }
        return userId;
    }

}
