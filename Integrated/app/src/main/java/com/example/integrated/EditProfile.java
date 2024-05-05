package com.example.integrated;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {
    private static final String TAG = "EditProfile";

    private EditText eTName, eTUserName, eTAge, eTEmail, eTOldPassword, eTNewPassword;
    private TextView saveChanges;

    private ImageView backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize views
        eTName = findViewById(R.id.eTName);
        eTUserName = findViewById(R.id.eTUserName);
        eTAge = findViewById(R.id.eTAge);
        eTEmail = findViewById(R.id.eTEmail);
        eTOldPassword = findViewById(R.id.eTOldPassword);
        eTNewPassword = findViewById(R.id.eTNewPassword);
        saveChanges = findViewById(R.id.saveChanges);
        backBtn = findViewById(R.id.backArrow);

        // Load user data
        loadUserData();

        // Setup save changes button
        saveChanges.setOnClickListener(view -> {
            updateProfile();
        });

        backBtn.setOnClickListener(view -> {
            finish();
        });

        // Email field should not be editable
        eTEmail.setEnabled(false);
    }

    private void loadUserData() {
        // Fetch user data from shared preferences or directly from your backend
        SharedPreferences prefs = getSharedPreferences(SignIn.MY_PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId != null) {
            // Make a request to your backend to fetch the user data
            fetchUserProfile(userId);
        } else {
            Log.e(TAG, "User ID not found in SharedPreferences.");
            Toast.makeText(this, "Error loading profile.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchUserProfile(String userId) {
        String url = " http://172.17.40.130:5000/get_user_profile?userId=" + userId;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Assuming your backend response matches the keys
                        eTName.setText(response.getString("name"));
                        eTUserName.setText(response.getString("username"));
                        eTAge.setText(String.valueOf(response.getInt("age")));
                        eTEmail.setText(response.getString("email"));
                        // Passwords are not fetched for security reasons
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing user profile JSON", e);
                        Toast.makeText(EditProfile.this, "Error loading profile data.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error fetching user profile", error);
                    Toast.makeText(EditProfile.this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void updateProfile() {
        // Validate input data
        String name = eTName.getText().toString();
        String username = eTUserName.getText().toString();
        String age = eTAge.getText().toString();
        String oldPassword = eTOldPassword.getText().toString();
        String newPassword = eTNewPassword.getText().toString();

        if (name.isEmpty() || username.isEmpty() || age.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Assuming you've a user ID stored in SharedPreferences
        SharedPreferences prefs = getSharedPreferences(SignIn.MY_PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        // Prepare your request payload
        JSONObject payload = new JSONObject();
        try {
            payload.put("userId", userId);
            payload.put("name", name);
            payload.put("username", username);
            payload.put("age", age);
            payload.put("oldPassword", oldPassword);
            payload.put("newPassword", newPassword);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating update profile payload", e);
        }

        // Send request to your backend
        String url = " http://172.17.40.130:5000/update_user_profile";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, payload,
                response -> {
                    try {
                        // Check for a successful update flag in your response
                        boolean success = response.getBoolean("success");
                        if (success) {
                            Toast.makeText(EditProfile.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            // Optionally, return to the profile page or update SharedPreferences with new data
                            finish(); // Close the activity, assuming you navigate back to the profile page
                        } else {
                            // Handle failure
                            String errorMessage = response.has("error") ? response.getString("error") : "Update failed";
                            Toast.makeText(EditProfile.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing update profile response", e);
                        Toast.makeText(EditProfile.this, "Error updating profile.", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error updating profile", error);
                    Toast.makeText(EditProfile.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                // Add your headers here if needed, e.g., authorization token
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }
}

