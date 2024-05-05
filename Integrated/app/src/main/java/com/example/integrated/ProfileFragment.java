package com.example.integrated;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.integrated.R;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ProfileFragment extends Fragment {

    private TextView textViewName, textViewUsername, textViewAge;
    private Button btnEditProfile, btnLogout;
    private ImageView imageViewProfile, uploadImage;
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final String TAG = ProfileFragment.class.getSimpleName();
    private static final String UPLOAD_URL = " http://172.17.40.130:5000/upload_profile_picture";

    private RequestQueue requestQueue;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        textViewName = view.findViewById(R.id.textViewName);
        textViewUsername = view.findViewById(R.id.textViewUsername);
        textViewAge = view.findViewById(R.id.textViewAge);
        btnEditProfile = view.findViewById(R.id.buttonEditProfile);
        btnLogout = view.findViewById(R.id.logoutButton);
        uploadImage = view.findViewById(R.id.imageViewProfile);
        imageViewProfile = view.findViewById(R.id.imageViewProfile);

        requestQueue = Volley.newRequestQueue(getContext());

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EditProfile.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());

        uploadImage.setOnClickListener(v -> openFileChooser());

        fetchUserProfile();
        fetchProfilePicture();
        return view;
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageViewProfile.setImageBitmap(bitmap);
                uploadProfilePicture(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchUserProfile() {
        SharedPreferences prefs = getActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        String url = " http://172.17.40.130:5000/get_user_profile?userId=" + userId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        textViewName.setText(response.getString("name"));
                        textViewUsername.setText(response.getString("username"));
                        textViewAge.setText("Age: " + response.getInt("age")); // Add "Age: " prefix
                        // Assuming profile picture URL is also returned in the response
                        String profilePictureUrl = response.getString("profile_picture_url");
                        // Load and display profile picture using your preferred image loading library (e.g., Picasso, Glide)
                        // Example:
                        // Picasso.get().load(profilePictureUrl).into(imageViewProfile);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Handle error
                    Toast.makeText(getContext(), "Error fetching user profile", Toast.LENGTH_SHORT).show();
                });

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(jsonObjectRequest);
    }

    private void fetchProfilePicture() {
        SharedPreferences prefs = getActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        String url = " http://172.17.40.130:5000/fetch_profile_picture/" + userId;
        new DownloadImageTask().execute(url);
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            String urlDisplay = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(urlDisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(TAG, "Error downloading image: " + e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                // Set the downloaded image as the profile picture
                imageViewProfile.setImageBitmap(result);
            } else {
                //make toast error fetching profile picture
                Toast.makeText(getContext(), "Error fetching profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadProfilePicture(Bitmap bitmap) {
        final String UPLOAD_URL = "  http://172.17.40.130:5000/upload_profile_picture";
        SharedPreferences prefs = getActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        MultipartRequest multipartRequest = new MultipartRequest(UPLOAD_URL,
                response -> Toast.makeText(getContext(), "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(getContext(), "Failed to upload profile picture", Toast.LENGTH_SHORT).show());

        try {
            multipartRequest.addFormField("user_id", userId); // Add user ID field
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();
            multipartRequest.addFilePart("image", "profile_picture.png", imageBytes, "image/png"); // Add image file part
        } catch (IOException e) {
            e.printStackTrace();
        }

        RequestQueue queue = Volley.newRequestQueue(getContext());
        queue.add(multipartRequest);
    }



    private void logout() {
        // Clear the login state
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isLoggedIn");
        editor.remove("userId");
        editor.apply();

        // Redirect to SignIn activity
        Intent intent = new Intent(getActivity(), SignIn.class);
        startActivity(intent);
        getActivity().finish();
    }
}
