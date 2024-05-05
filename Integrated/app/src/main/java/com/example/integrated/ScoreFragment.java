package com.example.integrated;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.integrated.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ScoreFragment extends Fragment {

    private RecyclerView scoresRecyclerView;
    private ScoreAdapter scoreAdapter;
    private List<ActivityModel> activitiesList;

    private Button progressButton;

    public ScoreFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_score, container, false);

        progressButton = view.findViewById(R.id.analyzeProgressButton);
        scoresRecyclerView = view.findViewById(R.id.scoresRecyclerView);
        activitiesList = new ArrayList<>();
        scoreAdapter = new ScoreAdapter(activitiesList);
        scoresRecyclerView.setAdapter(scoreAdapter);
        scoresRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        progressButton.setOnClickListener(v -> {
            // Open progress tracking activity
            Intent intent = new Intent(getContext(), ProgressTracking.class);
            startActivity(intent);
        });

        return view;

    }

    @Override
    public void onResume() {
        super.onResume();
        initializeScoreData();
    }

    private void initializeScoreData() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", getActivity().MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        Log.d("ScoreFragment", "User ID: " + userId); // Log user ID

        if (!userId.isEmpty()) {
            String url = " http://172.17.40.130:5000/get_max_scores?user_id=" + userId;
            Log.d("ScoreFragment", "Fetching scores from: " + url); // Log URL

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    response -> {
                        Log.d("ScoreFragment", "Response received: " + response.toString()); // Log raw response
                        parseActivities(response);
                    },
                    error -> {
                        Log.e("ScoreFragment", "Error fetching scores: ", error); // Log error
                    });

            RequestQueue queue = Volley.newRequestQueue(requireContext());
            queue.add(request);
        } else {
            Log.e("ScoreFragment", "User ID not found"); // Log missing user ID
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
        }
    }


    private void parseActivities(JSONArray response) {
        activitiesList.clear(); // Clear existing data
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject activityObject = response.getJSONObject(i);
                String activityName = activityObject.getString("activity_name");
                JSONArray levelsArray = activityObject.getJSONArray("levels");
                List<LevelModel> levelsList = new ArrayList<>();

                for (int j = 0; j < levelsArray.length(); j++) {
                    JSONObject levelObject = levelsArray.getJSONObject(j);
                    int levelNumber = levelObject.getInt("level_number");
                    // Use optInt for optional "score" field
                    int score = levelObject.optInt("max_score", 0);
                    levelsList.add(new LevelModel(levelNumber, score));
                }
                activitiesList.add(new ActivityModel(activityName, levelsList));
            }
            scoreAdapter.notifyDataSetChanged(); // Notify adapter to update the data
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
