package com.omnitrix.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroqApiClient {



    private static final String MODEL = "llama-3.3-70b-versatile";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ResponseCallback {
        void onResponse(String reply);
        void onError(String error);
    }

    public void sendMessageWithContext(String systemPrompt,
                                       String userMessage,
                                       ResponseCallback callback) {
        try {
            JSONObject message1 = new JSONObject();
            message1.put("role", "system");
            message1.put("content", systemPrompt);

            JSONObject message2 = new JSONObject();
            message2.put("role", "user");
            message2.put("content", userMessage);

            JSONArray messages = new JSONArray();
            messages.put(message1);
            messages.put(message2);

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("max_tokens", 150);
            body.put("temperature", 0.9);

            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("GROQ", "Network error: " + e.getMessage());
                    mainHandler.post(() ->
                            callback.onError(e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response)
                        throws IOException {
                    String responseBody = response.body().string();
                    Log.d("GROQ", "Raw response: " + responseBody);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        String reply = json
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");
                        mainHandler.post(() ->
                                callback.onResponse(reply.trim()));
                    } catch (Exception e) {
                        Log.e("GROQ", "Parse error: " + e.getMessage());
                        Log.e("GROQ", "Response: " + responseBody);
                        mainHandler.post(() ->
                                callback.onError("Parse error"));
                    }
                }
            });

        } catch (Exception e) {
            Log.e("GROQ", "Exception: " + e.getMessage());
            callback.onError(e.getMessage());
        }
    }
}