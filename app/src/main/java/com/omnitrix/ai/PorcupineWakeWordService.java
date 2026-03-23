package com.omnitrix.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineActivationException;
import ai.picovoice.porcupine.PorcupineActivationLimitException;
import ai.picovoice.porcupine.PorcupineActivationRefusedException;
import ai.picovoice.porcupine.PorcupineActivationThrottledException;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineInvalidArgumentException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

public class PorcupineWakeWordService extends Service {

    private static final String TAG = "PorcupineWake";
    private static final String CHANNEL_ID = "omnitrix_channel";
    private static final String ACCESS_KEY = "RVMrAVUfTVdPeKMsUhVAdAIiSkS3VsRTt7R5u/WItyRnjvhYEbrf8A==";
    private static final String KEYWORD_FILE =
            "matrix_en_android_v4_0_0.ppn";
    private PorcupineManager porcupineManager;

    @Override
    public void onCreate() {
        Log.d(TAG, "=== Service onCreate ===");
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
        initPorcupine();
    }

    private void initPorcupine() {
        Log.d(TAG, "=== Initializing Porcupine ===");
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(ACCESS_KEY)
                    .setKeywordPath(KEYWORD_FILE)
                    .setSensitivity(0.7f)
                    .build(getApplicationContext(),
                            keywordIndex -> {
                                Log.d(TAG, "=== MATRIX WAKE WORD DETECTED! ===");
                                wakeUpOmnitrix();
                            });

            porcupineManager.start();
            Log.d(TAG, "=== Porcupine started successfully! ===");

        } catch (PorcupineException e) {
            Log.e(TAG, "Porcupine error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }

    private void wakeUpOmnitrix() {
        Log.d(TAG, "=== Waking up Omnitrix! ===");
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("WAKE_WORD_TRIGGERED", true);
        startActivity(intent);
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Omnitrix Active")
                .setContentText("Say 'Matrix' to wake me up")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Omnitrix Wake Word",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager =
                getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                Log.e(TAG, "Stop error: " + e.getMessage());
            }
        }
    }
}