package com.example.integrated;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.data.Entry;
import com.example.integrated.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProgressTracking extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressAdapter adapter;
    private ArrayList<ProgressModel> progressList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_tracking);

        recyclerView = findViewById(R.id.rvProgressTracking);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProgressAdapter(progressList);
        recyclerView.setAdapter(adapter);

        fetchProgressData();
    }

    private void fetchProgressData() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");

        if (!userId.isEmpty()) {
            String url = " http://172.17.40.130:5000/get_user_progress/" + userId;

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                    this::parseResponse,
                    error -> Log.e("ProgressTracking", "Volley error: " + error.toString()));

            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
        }
    }

    private void parseResponse(JSONArray response) {
        try {
            Map<String, ProgressModel> activityModelsMap = new HashMap<>();

            for (int i = 0; i < response.length(); i++) {
                JSONArray item = response.getJSONArray(i);
                String activityName = item.getString(0);
                int levelNumber = item.getInt(1);
                float score = (float) item.getInt(2); // Convert score to float as Entry expects Y values to be float
                // Using i as the X value for simplicity, adjust as needed
                Entry entry = new Entry(i, score);

                // Check if the activity model already exists
                ProgressModel progressModel = activityModelsMap.get(activityName);
                if (progressModel == null) {
                    progressModel = new ProgressModel(activityName);
                    activityModelsMap.put(activityName, progressModel);
                }

                // Add the entry to the correct level within the model
                ArrayList<Entry> levelEntries = progressModel.getLevelEntriesMap().getOrDefault(levelNumber, new ArrayList<>());
                levelEntries.add(entry);
                progressModel.getLevelEntriesMap().put(levelNumber, levelEntries);
            }

            // Clear existing data and add all parsed data
            progressList.clear();
            progressList.addAll(activityModelsMap.values());
            adapter.notifyDataSetChanged();
        } catch (JSONException e) {
            Log.e("ProgressTracking", "JSON parsing error", e);
        }
    }

}
