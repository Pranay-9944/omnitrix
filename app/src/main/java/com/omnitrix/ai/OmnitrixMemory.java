package com.omnitrix.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OmnitrixMemory {

    private static final String PREF_NAME = "omnitrix_memory";
    private final SharedPreferences prefs;

    // Memory keys
    private static final String KEY_NAME           = "user_name";
    private static final String KEY_LIKES          = "user_likes";
    private static final String KEY_DISLIKES       = "user_dislikes";
    private static final String KEY_GOALS          = "user_goals";
    private static final String KEY_ROUTINE        = "daily_routine";
    private static final String KEY_MOOD           = "mood_today";
    private static final String KEY_LAST_TALKED    = "last_talked";
    private static final String KEY_RECENT_TOPICS  = "recent_topics";
    private static final String KEY_CONVO_COUNT    = "convo_count";

    public OmnitrixMemory(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Set defaults on first launch
        if (!prefs.contains(KEY_NAME)) {
            prefs.edit()
                    .putString(KEY_NAME, "Bro")
                    .putString(KEY_LIKES, "tech, building apps")
                    .putString(KEY_DISLIKES, "nothing yet")
                    .putString(KEY_GOALS, "build Omnitrix")
                    .putString(KEY_ROUTINE, "wakes up and checks phone")
                    .putString(KEY_MOOD, "normal")
                    .putString(KEY_RECENT_TOPICS, "[]")
                    .putInt(KEY_CONVO_COUNT, 0)
                    .apply();
        }
    }

    // Build system prompt with memory
    public String buildSystemPrompt() {
        String name      = prefs.getString(KEY_NAME, "Bro");
        String likes     = prefs.getString(KEY_LIKES, "");
        String goals     = prefs.getString(KEY_GOALS, "");
        String mood      = prefs.getString(KEY_MOOD, "normal");
        String routine   = prefs.getString(KEY_ROUTINE, "");
        String lastTalked= prefs.getString(KEY_LAST_TALKED, "not recently");
        int convoCount   = prefs.getInt(KEY_CONVO_COUNT, 0);
        String topics    = prefs.getString(KEY_RECENT_TOPICS, "[]");
        String timeOfDay = getTimeOfDay();

        return "You are Omnitrix, the best friend of " + name + ". " +
                "Talk casually like a real friend, funny and cool. " +
                "Keep replies short — 1 to 3 sentences. " +
                "Never say you are an AI. Never be formal. " +
                "Here is what you know about " + name + ": " +
                "They like " + likes + ". " +
                "Their goals are: " + goals + ". " +
                "Their daily routine: " + routine + ". " +
                "Current mood: " + mood + ". " +
                "You last talked: " + lastTalked + ". " +
                "Recent topics discussed: " + topics + ". " +
                "Total conversations: " + convoCount + ". " +
                "Current time of day: " + timeOfDay + ". " +
                "Reference past topics naturally in conversation. " +
                "If it is morning say good morning. " +
                "If convoCount is 0 introduce yourself and ask their name.";
    }

    // Save what user said for memory
    public void updateMemory(String userMessage, String omnitrixReply) {
        // Update conversation count
        int count = prefs.getInt(KEY_CONVO_COUNT, 0);
        prefs.edit().putInt(KEY_CONVO_COUNT, count + 1).apply();

        // Save last talked time
        String time = new SimpleDateFormat("dd MMM hh:mm a",
                Locale.getDefault()).format(new Date());
        prefs.edit().putString(KEY_LAST_TALKED, time).apply();

        // Update recent topics
        updateRecentTopics(userMessage);

        // Auto detect mood from message
        detectAndSaveMood(userMessage);

        // Auto learn name if user says it
        learnName(userMessage);

        // Auto learn likes
        learnLikes(userMessage);

        // Auto learn goals
        learnGoals(userMessage);
    }

    private void updateRecentTopics(String message) {
        try {
            JSONArray topics = new JSONArray(
                    prefs.getString(KEY_RECENT_TOPICS, "[]"));

            // Add new topic
            topics.put(message.length() > 30
                    ? message.substring(0, 30) : message);

            // Keep only last 5 topics
            if (topics.length() > 5) {
                JSONArray newTopics = new JSONArray();
                for (int i = topics.length() - 5;
                     i < topics.length(); i++) {
                    newTopics.put(topics.get(i));
                }
                topics = newTopics;
            }

            prefs.edit().putString(KEY_RECENT_TOPICS,
                    topics.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void detectAndSaveMood(String message) {
        message = message.toLowerCase();
        String mood = "normal";

        if (message.contains("happy") || message.contains("great") ||
                message.contains("awesome") || message.contains("excited")) {
            mood = "happy";
        } else if (message.contains("sad") || message.contains("upset") ||
                message.contains("depressed") || message.contains("cry")) {
            mood = "sad";
        } else if (message.contains("tired") || message.contains("sleepy") ||
                message.contains("exhausted")) {
            mood = "tired";
        } else if (message.contains("angry") || message.contains("frustrated") ||
                message.contains("annoyed")) {
            mood = "angry";
        } else if (message.contains("stressed") || message.contains("anxious") ||
                message.contains("worried")) {
            mood = "stressed";
        }

        if (!mood.equals("normal")) {
            prefs.edit().putString(KEY_MOOD, mood).apply();
        }
    }

    private void learnName(String message) {
        message = message.toLowerCase();
        if (message.contains("my name is ")) {
            String name = message.split("my name is ")[1].trim();
            // Capitalize first letter
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            prefs.edit().putString(KEY_NAME, name).apply();
        } else if (message.contains("i am ") && message.length() < 20) {
            String name = message.split("i am ")[1].trim();
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            prefs.edit().putString(KEY_NAME, name).apply();
        }
    }

    private void learnLikes(String message) {
        message = message.toLowerCase();
        if (message.contains("i like ") || message.contains("i love ")) {
            String current = prefs.getString(KEY_LIKES, "");
            String keyword = message.contains("i like ")
                    ? message.split("i like ")[1].trim()
                    : message.split("i love ")[1].trim();
            if (!current.contains(keyword)) {
                prefs.edit().putString(KEY_LIKES,
                        current + ", " + keyword).apply();
            }
        }
    }

    private void learnGoals(String message) {
        message = message.toLowerCase();
        if (message.contains("i want to ") || message.contains("my goal is ")) {
            String current = prefs.getString(KEY_GOALS, "");
            String goal = message.contains("i want to ")
                    ? message.split("i want to ")[1].trim()
                    : message.split("my goal is ")[1].trim();
            if (!current.contains(goal)) {
                prefs.edit().putString(KEY_GOALS,
                        current + ", " + goal).apply();
            }
        }
    }

    private String getTimeOfDay() {
        int hour = Integer.parseInt(
                new SimpleDateFormat("HH", Locale.getDefault()).format(new Date()));
        if (hour >= 5 && hour < 12)  return "morning";
        if (hour >= 12 && hour < 17) return "afternoon";
        if (hour >= 17 && hour < 21) return "evening";
        return "night";
    }

    // Getters
    public String getUserName() {
        return prefs.getString(KEY_NAME, "Bro");
    }
}