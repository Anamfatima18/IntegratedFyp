package com.example.integrated;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.integrated.R;

import org.json.JSONException;
import org.json.JSONObject;

public class SignIn extends AppCompatActivity {
    private static final String TAG = "SignInActivity";
    public static final String MY_PREFS_NAME = "MyAppPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if the user is already logged in
        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        if (!userId.isEmpty()) {
            // User is already logged in, navigate to MainActivity
            Intent intent = new Intent(SignIn.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_sign_in);

        EditText editTextEmail = findViewById(R.id.eTEmailAddress);
        EditText editTextPassword = findViewById(R.id.eTPassword);
        Button loginButton = findViewById(R.id.loginButton);
        TextView goToRegistration = findViewById(R.id.signUp);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();
                Log.d(TAG, "Login button clicked with email: " + email);
                signIn(email, password);
            }
        });

        goToRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignIn.this, SignUp.class);
                startActivity(intent);
            }
        });
    }
    private void saveToken(String token) {
        // Store the token in SharedPreferences or other storage mechanism
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("token", token);
        editor.apply();
    }
    private void signIn(String email, String password) {
        Log.d(TAG, "Preparing to sign in with email: " + email);
        String url = " http://172.17.40.130:5000/signin"; // Replace with your server URL
        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("password", password);
            Log.d(TAG, "JSON Object for sign in created");
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception in signIn", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(@NonNull JSONObject response) {
                        String token = null ;
                        try {

                            Log.d(TAG, "Sign in successful: " + response.toString());
                            String userId = response.getString("userId"); // Assuming response contains userId
                            // Save user info in SharedPreferences
                            token = response.getString("token");
                            SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putString("userId", userId);
                            editor.apply();

                            // Navigate to HomeActivity or similar after successful login
                            Intent intent = new Intent(SignIn.this, MainActivity.class); // Assuming you have a HomeActivity
                            startActivity(intent);
                            finish();
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON parsing error", e);
                        }
                        saveToken(token);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(@NonNull VolleyError error) {
                Log.e(TAG, "Error during sign in", error);
                Toast.makeText(SignIn.this, "Error during sign in", Toast.LENGTH_LONG).show();
            }
        });

        Log.d(TAG, "Sign in request added to queue");
        queue.add(jsonObjectRequest);
    }
}