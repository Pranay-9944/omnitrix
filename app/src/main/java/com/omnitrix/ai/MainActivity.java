package com.omnitrix.ai;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnMic;
    private TextView tvChat, tvStatus;
    private ScrollView scrollView;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean isListening = false;
    private GroqApiClient groqClient = new GroqApiClient();
    private OmnitrixMemory memory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMic     = findViewById(R.id.btnMic);
        tvChat     = findViewById(R.id.tvChat);
        tvStatus   = findViewById(R.id.tvStatus);
        scrollView = findViewById(R.id.scrollView);

        memory = new OmnitrixMemory(this);

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                speak("Yo! Omnitrix online. What's up bro?");
                appendChat("Omnitrix", "Yo! Omnitrix online. What's up bro?");
            }
        });

        setupSpeechRecognizer();

        btnMic.setOnClickListener(v -> {
            if (!isListening) startListening();
        });

        // Request permissions
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS
        };

        boolean needsPermission = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                needsPermission = true;
                break;
            }
        }
        if (needsPermission) {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }

        // Start Vosk wake word service AFTER super.onCreate
        try {
            Log.d("OMNITRIX_MAIN", "Starting Vosk service...");
            Intent serviceIntent = new Intent(this, PorcupineWakeWordService.class);
            startForegroundService(serviceIntent);
        } catch (Exception e) {
            Log.e("OMNITRIX_MAIN", "Service start failed: " + e.getMessage());
        }

        // Check if launched from wake word
        if (getIntent().getBooleanExtra("WAKE_WORD_TRIGGERED", false)) {
            appendChat("Omnitrix", "Yeah bro? What's up?");
            speak("Yeah bro? What's up?");
            new android.os.Handler().postDelayed(
                    () -> startListening(), 1500);
        }
    }
    private void wakeUpOmnitrix() {
        Log.d(TAG, "=== Waking up Omnitrix! ===");

        // If app is in background bring it to front
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        );
        intent.putExtra("WAKE_WORD_TRIGGERED", true);
        startActivity(intent);
    }

    private void setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle params) {
                tvStatus.setText("Listening...");
                isListening = true;
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                tvStatus.setText("Thinking...");
                ArrayList<String> matches = results
                        .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String userText = matches.get(0);
                    appendChat("You", userText);
                    sendToOmnitrix(userText);
                }
            }

            @Override
            public void onError(int error) {
                isListening = false;
                tvStatus.setText("Tap mic to talk");
                appendChat("Omnitrix", "Didn't catch that bro, try again!");
            }

            @Override public void onBeginningOfSpeech() {}
            @Override public void onEndOfSpeech() {
                tvStatus.setText("Processing...");
            }
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int type, Bundle b) {}
            @Override public void onRmsChanged(float v) {}
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.startListening(intent);
        tvStatus.setText("Listening...");
    }

    private void sendToOmnitrix(String userText) {
        tvStatus.setText("Thinking...");
        btnMic.setEnabled(false);

        if (PhoneController.isPhoneCommand(userText)) {
            PhoneController.handleCommand(this, userText, memory);
            if (userText.toLowerCase().startsWith("call ")) {
                String name = userText.toLowerCase()
                        .replace("call ", "").trim();
                appendChat("Omnitrix", "Calling " + name + " bro!");
                speak("Calling " + name);
            } else {
                appendChat("Omnitrix", "On it bro!");
                speak("On it bro!");
            }
            tvStatus.setText("Tap mic to talk");
            btnMic.setEnabled(true);
            return;
        }

        String systemPrompt = memory.buildSystemPrompt();
        groqClient.sendMessageWithContext(systemPrompt, userText,
                new GroqApiClient.ResponseCallback() {
                    @Override
                    public void onResponse(String reply) {
                        appendChat("Omnitrix", reply);
                        speak(reply);
                        tvStatus.setText("Tap mic to talk");
                        btnMic.setEnabled(true);
                        memory.updateMemory(userText, reply);
                    }

                    @Override
                    public void onError(String error) {
                        appendChat("Omnitrix", "Bro my brain lagged, try again!");
                        speak("Bro my brain lagged, try again!");
                        tvStatus.setText("Tap mic to talk");
                        btnMic.setEnabled(true);
                    }
                });
    }

    private void speak(String text) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void appendChat(String sender, String message) {
        String current = tvChat.getText().toString();
        if (current.equals("Omnitrix: Initializing...")) current = "";
        tvChat.setText(current + "\n" + sender + ": " + message);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
}