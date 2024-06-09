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
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.integrated.R;

import org.json.JSONException;
import org.json.JSONObject;

public class SignUp extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";
    EditText editTextEmail, editTextName, editTextUsername, editTextAge, editTextPassword;
    TextView textViewLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Log.d(TAG, "onCreate: Activity started");

        editTextEmail = findViewById(R.id.eTEmail);
        editTextName = findViewById(R.id.eTName);
        editTextUsername = findViewById(R.id.eTUsername);
        editTextAge = findViewById(R.id.eTAge);
        editTextPassword = findViewById(R.id.eTPassword);
        textViewLogin = findViewById(R.id.goTologin);
        Log.d(TAG, "onCreate: Views initialized");

        Button signUpButton = findViewById(R.id.signUpButton);
        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString();
                String name = editTextName.getText().toString();
                String username = editTextUsername.getText().toString();
                int age = Integer.parseInt(editTextAge.getText().toString());
                String password = editTextPassword.getText().toString();
                Log.d(TAG, "onClick: Button clicked with email: " + email);

                sendSignUpRequest(email, name, username, age, password);
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUp.this, SignIn.class);
                startActivity(intent);
            }
        });
    }

    private void sendSignUpRequest(String email, String name, String username, int age, String password) {
        Log.d(TAG, "sendSignUpRequest: Preparing to send request");
        String url = " http://192.168.10.10:5000/signup";
        RequestQueue queue = Volley.newRequestQueue(this);

        JSONObject postData = new JSONObject();
        try {
            postData.put("email", email);
            postData.put("name", name);
            postData.put("username", username);
            postData.put("age", age);
            postData.put("password", password);
            Log.d(TAG, "sendSignUpRequest: JSON Object created");
        } catch (JSONException e) {
            Log.e(TAG, "sendSignUpRequest: JSON Exception", e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, postData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "onResponse: Received response from server");
                        Toast.makeText(SignUp.this, "Signup Successful!", Toast.LENGTH_LONG).show();

                        saveUserDetails(email, name, username, age);

                        Intent intent = new Intent(SignUp.this, Otp.class);
                        intent.putExtra("UserEmail", email);
                        startActivity(intent);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "onErrorResponse: Error during signup", error);
                Toast.makeText(SignUp.this, "Error during signup: " + error.toString(), Toast.LENGTH_LONG).show();
            }
        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,  // 10 seconds timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        Log.d(TAG, "sendSignUpRequest: Request added to queue");
        queue.add(jsonObjectRequest);
    }

    public void saveUserDetails(String email, String name, String username, int age) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("UserEmail", email);
        editor.putString("UserName", name);
        editor.putString("UserUsername", username);
        editor.putInt("UserAge", age);

        editor.apply();
        Log.d(TAG, "User details saved in Shared Preferences");
    }

}