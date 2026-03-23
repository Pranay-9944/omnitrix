package com.omnitrix.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class WakeWordService extends Service {

    private static final String CHANNEL_ID = "omnitrix_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, buildNotification());
    }

    private Notification buildNotification() {
        // Tap notification to open Omnitrix
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("WAKE_WORD_TRIGGERED", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE |
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Omnitrix is ready")
                .setContentText("Tap here to talk to Omnitrix")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Omnitrix",
                NotificationManager.IMPORTANCE_HIGH
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
    public void onDestroy() { super.onDestroy(); }
}