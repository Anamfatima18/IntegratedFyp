package com.example.integrated;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
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

import java.io.IOException;

public class WordHarmony extends AppCompatActivity {
    private TextView levelNumberTextView, taskNumberTextView, scoreTextView;
    private Button[] wordButtons = new Button[6];
    private Button playAudioButton;
    private int currentLevel = 1, currentTask = 1;
    private final int maxTasksPerLevel = 5;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    private int currentLevelScore = 0, currentLevelTries = 0, cumulativeScore = 0, totalTries = 0;
    private String correctWord, soundPath;

    private MediaPlayer successSound, failureSound;

    private LottieAnimationView animationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_harmony);
        initializeUIComponents();

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
            resetGameOrNavigate(); // Reset game state and UI
        } else {

            // Load progress from SharedPreferences
            SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
            currentLevel = prefs.getInt("currentLevel", 1);
            currentTask = prefs.getInt("currentTask", 1);
            currentLevelScore = prefs.getInt("currentLevelScore", 0);
            currentLevelTries = prefs.getInt("currentLevelTries", 0);
            cumulativeScore = prefs.getInt("cumulativeScore", 0);
            totalTries = prefs.getInt("totalTries", 0);
            // Regular game startup logic

            updateUIForNewLevel();
            updateScoreDisplay();
            fetchWordHarmonyTask(currentLevel);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Saving state when the activity is paused
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

    private void resetGameOrNavigate() {
        // Reset all game state
        currentLevel = 1;
        currentTask = 1;
        currentLevelScore = 0;
        currentLevelTries = 0;
        cumulativeScore = 0;
        totalTries = 0;

        // Clear any saved preferences if used for state persistence
        SharedPreferences prefs = getSharedPreferences("WordHarmonyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Reset UI components to initial state
        initializeUIComponents();

        // Start the game at the first level
        fetchWordHarmonyTask(currentLevel);
    }


    private void initializeUIComponents() {
        levelNumberTextView = findViewById(R.id.levelNumberTextView);
        taskNumberTextView = findViewById(R.id.taskNumberTextView);
        scoreTextView = findViewById(R.id.scoreTextView); // Assume you have added this TextView for score display

        playAudioButton = findViewById(R.id.playAudioButton);
        playAudioButton.setOnClickListener(v -> playSound(soundPath));

        // Initialize word buttons
        for (int i = 0; i < wordButtons.length; i++) {
            String buttonID = "wordButton" + (i + 1);
            int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
            wordButtons[i] = findViewById(resID);
        }
        setupWordButtons();
        loadSounds();
    }

    private void loadSounds() {
        successSound = MediaPlayer.create(this, R.raw.success);
        failureSound = MediaPlayer.create(this, R.raw.failure);
    }

    private void setupWordButtons() {
        View.OnClickListener wordClickListener = view -> {
            Button clickedButton = (Button) view;
            String selectedWord = clickedButton.getText().toString();
            validateOption(selectedWord, clickedButton);
        };

        for (Button wordButton : wordButtons) {
            wordButton.setOnClickListener(wordClickListener);
        }
    }

    private void validateOption(String selectedWord, Button clickedButton) {
        totalTries++;
        currentLevelTries++;
        boolean isCorrect = selectedWord.equalsIgnoreCase(correctWord);

        if (isCorrect) {
            currentLevelScore++;
            cumulativeScore++;
            // Change to correct answer color
            ViewCompat.setBackgroundTintList(clickedButton, ColorStateList.valueOf(ContextCompat.getColor(WordHarmony.this, R.color.correct_answer)));
        } else {
            // Change to incorrect answer color
            ViewCompat.setBackgroundTintList(clickedButton, ColorStateList.valueOf(ContextCompat.getColor(WordHarmony.this, R.color.incorrect_answer)));
        }

        // Check for level completion after a brief delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (currentTask == maxTasksPerLevel) {
                checkLevelCompletion();
            } else {
                advanceTask();
            }
            // Reset button color for all outcomes after delay
            ViewCompat.setBackgroundTintList(clickedButton, ColorStateList.valueOf(ContextCompat.getColor(WordHarmony.this, R.color.button_default)));
        }, 1000);
    }

    private void submitScore(int userId, int level, int score) {
        String url = " http://172.17.40.130:5000/submit_word_harmony_score";
        JSONObject postData = new JSONObject();
        try {
            postData.put("user_id", userId);
            postData.put("level", level);
            postData.put("score", score);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                    response -> Log.d("WordHarmony", "Score submitted successfully: " + response),
                    error -> Log.e("WordHarmony", "Error submitting score: " + error.toString())
            );

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.e("WordHarmony", "JSON Exception: " + e.getMessage());
        }
    }

    private void fetchWordHarmonyTask(int level) {
        String url = "  http://172.17.40.130:5000/get_word_harmony_task/" + level;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        correctWord = response.getString("word");
                        soundPath = response.getString("sound_path");
                        JSONArray optionsArray = response.getJSONArray("options");
                        for (int i = 0; i < optionsArray.length(); i++) {
                            wordButtons[i].setText(optionsArray.getString(i));
                        }
                        playSound(soundPath);
                    } catch (JSONException e) {
                        Log.e("WordHarmony", "JSON parsing error", e);
                    }
                }, error -> Log.e("WordHarmony", "Volley error: " + error.toString()));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(jsonObjectRequest);
    }

    private void playSound(String soundPath) {
        if (soundPath == null || soundPath.trim().isEmpty()) {
            Log.e("WordHarmonyActivity", "Sound path is invalid");
            Toast.makeText(this, "Sound path is invalid", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            mediaPlayer.reset();
            // Extract the resource name from the sound path
            String resourceName = soundPath.substring(soundPath.lastIndexOf('/') + 1, soundPath.lastIndexOf('.'));
            int resourceId = getResources().getIdentifier(resourceName, "raw", getPackageName());

            if (resourceId == 0) {
                Log.e("WordHarmonyActivity", "Resource not found: " + resourceName);
                Toast.makeText(this, "Unable to find audio resource", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + resourceId);
            mediaPlayer.setDataSource(this, soundUri);
            mediaPlayer.prepareAsync(); // Prepare async to not block the main thread

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d("WordHarmonyActivity", "MediaPlayer prepared, starting playback");
                mp.start();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e("WordHarmonyActivity", "MediaPlayer error occurred: What: " + what + ", Extra: " + extra);
                Toast.makeText(WordHarmony.this, "Error playing sound", Toast.LENGTH_SHORT).show();
                return true; // Indicates that we have handled the error
            });

        } catch (IOException e) {
            Log.e("WordHarmonyActivity", "IOException setting data source", e);
            Toast.makeText(this, "Unable to play audio", Toast.LENGTH_SHORT).show();
        }
    }


    private void advanceTask() {
        currentTask++;
        if (currentTask <= maxTasksPerLevel) {
            fetchWordHarmonyTask(currentLevel); // Fetch the next task within the current level
            taskNumberTextView.setText(String.valueOf(currentTask));
            updateScoreDisplay(); // Update the displayed score
        } else {
            // If all tasks within the level are completed, check for level advancement
            checkLevelCompletion();
        }
    }

    private void checkLevelCompletion() {
        if ((double) currentLevelScore / maxTasksPerLevel >= 0.8) {
            if (currentLevel < 3) {
                showLevelScoreAndDecideNextStep(true);
            } else {
                showFinalResults();
            }
        } else {
            showLevelScoreAndDecideNextStep(false);
        }
    }


    private void showLevelScoreAndDecideNextStep(boolean isSuccess) {
        // Submit the score regardless of being correct or incorrect
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        if (!userId.isEmpty()) {
            int parsedUserId = Integer.parseInt(userId); // Ensure userId is correctly parsed as an int
            submitScore(parsedUserId, currentLevel, currentLevelScore);
        }

        double scoreRatio = (double) currentLevelScore / maxTasksPerLevel;
        if (isSuccess) {
            playSoundAndAnimation(true);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Level " + currentLevel + " completed.\nYour score: " + currentLevelScore + "/" + maxTasksPerLevel);
            builder.setPositiveButton("Continue", (dialog, which) -> advanceToNextLevel());
            builder.create().show();
        } else {
            playSoundAndAnimation(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Level " + currentLevel + " completed.\nYour score: " + currentLevelScore + "/" + maxTasksPerLevel);
            builder.setPositiveButton("Retry", (dialog, which) -> retryLevel());
            builder.create().show();
        }

        // Reset level-specific variables
        currentLevelScore = 0;
        currentLevelTries = 0;
    }

    private void advanceToNextLevel() {
        currentLevel++;
        resetForNewLevel();
        updateUIForNewLevel();
    }

    private void retryLevel() {
        Toast.makeText(this, "Try this level again!", Toast.LENGTH_SHORT).show();
        resetForNewLevel();
        updateUIForNewLevel(); // This should also handle fetching new tasks
    }

    private void resetForNewLevel() {
        currentTask = 1;
        currentLevelScore = 0;
        currentLevelTries = 0;
        updateScoreDisplay(); // Reset the score display
        fetchWordHarmonyTask(currentLevel); // Fetch tasks for the new/current level
    }

    private void updateUIForNewLevel() {
        LinearLayout rootLayout = findViewById(R.id.rootLayout); // Assuming this is your root layout
        Drawable backgroundDrawable;

        switch (currentLevel) {
            case 2:
                backgroundDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.level_two_word_harmony);
                break;
            case 3:
                backgroundDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.level_three_word_harmony);
                break;
            default:
                backgroundDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.level_one_word_harmony);
                break;
        }

        if (backgroundDrawable != null) {
            rootLayout.setBackground(backgroundDrawable);
        }

        levelNumberTextView.setText(String.valueOf(currentLevel));
        taskNumberTextView.setText(String.valueOf(currentTask));
        scoreTextView.setText("Score: 0/0"); // Reset score display for the new level
    }

    private void updateScoreDisplay() {
        String scoreText = "Score: " + currentLevelScore + "/" + currentLevelTries;
        scoreTextView.setText(scoreText);
    }

    private void showFinalResults() {
        clearSavedState(); // Clear the saved state before navigating to FinalScore
        Intent finalScoreIntent = new Intent(WordHarmony.this, FinalScore.class);
        finalScoreIntent.putExtra("activityClass", WordHarmony.class.getName());
        finalScoreIntent.putExtra("cumulativeScore", cumulativeScore);
        finalScoreIntent.putExtra("totalTries", totalTries);
        startActivity(finalScoreIntent);
        finish(); // Optionally, close the current activity
    }

    private void clearSavedState() {
        SharedPreferences prefs = getSharedPreferences("RhymeActivityPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // This will clear the saved state
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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

}
