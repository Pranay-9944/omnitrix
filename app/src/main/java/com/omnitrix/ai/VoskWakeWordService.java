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

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VoskWakeWordService extends Service {

    private static final String TAG = "VoskWakeWord";
    private static final String CHANNEL_ID = "omnitrix_channel";

    private static final String MODEL_NAME = "vosk-model-small-en-us-0.15";

    private Model model;
    private SpeechService speechService;

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d(TAG,"Service started");

        createNotificationChannel();
        startForeground(1, buildNotification());

        initVosk();
    }

    private void initVosk(){

        new Thread(() -> {

            try {

                copyModelFromAssets();

                String modelPath = getFilesDir()+"/"+MODEL_NAME;

                model = new Model(modelPath);

                Log.d(TAG,"Model loaded");

                Recognizer recognizer = new Recognizer(
                        model,
                        16000.0f,
                        "[\"omni\",\"tricks\",\"activate\",\"hello\"]"
                );

                speechService = new SpeechService(recognizer,16000.0f);

                speechService.startListening(new RecognitionListener() {

                    @Override
                    public void onPartialResult(String hypothesis) {

                        checkWakeWord(hypothesis);
                    }

                    @Override
                    public void onResult(String hypothesis) {

                        checkWakeWord(hypothesis);
                    }

                    @Override
                    public void onFinalResult(String hypothesis) {

                        Log.d(TAG,"Final: "+hypothesis);
                    }

                    @Override
                    public void onError(Exception e) {

                        Log.e(TAG,"Vosk error: "+e.getMessage());
                    }

                    @Override
                    public void onTimeout() {

                        Log.d(TAG,"Timeout");
                    }
                });

                Log.d(TAG,"Listening started");

            }catch(Exception e){

                Log.e(TAG,"Error "+e.getMessage());
            }

        }).start();
    }

    private void checkWakeWord(String hypothesis){

        String text = extractText(hypothesis).toLowerCase();

        Log.d(TAG,"Heard: "+text);

        if(text.contains("omni") && text.contains("tricks")){

            wakeUpOmnitrix();
        }
    }

    private String extractText(String json){

        try{

            JSONObject obj = new JSONObject(json);

            if(obj.has("partial"))
                return obj.getString("partial");

            if(obj.has("text"))
                return obj.getString("text");

        }catch(Exception ignored){}

        return "";
    }

    private void wakeUpOmnitrix(){

        Log.d(TAG,"OMNITRIX WAKE WORD DETECTED");

        if(speechService!=null){
            speechService.stop();
        }

        Intent intent = new Intent(this, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        intent.putExtra("WAKE_WORD_TRIGGERED",true);

        startActivity(intent);
    }

    private void copyModelFromAssets() throws IOException {

        File modelDir = new File(getFilesDir(),MODEL_NAME);

        if(modelDir.exists()) return;

        modelDir.mkdirs();

        copyAssetFolder(MODEL_NAME,modelDir.getAbsolutePath());
    }

    private void copyAssetFolder(String assetPath,String destPath) throws IOException {

        String[] files = getAssets().list(assetPath);

        if(files==null) return;

        new File(destPath).mkdirs();

        for(String file:files){

            String subAsset = assetPath+"/"+file;
            String subDest = destPath+"/"+file;

            String[] subFiles = getAssets().list(subAsset);

            if(subFiles!=null && subFiles.length>0){

                copyAssetFolder(subAsset,subDest);

            }else{

                InputStream in = getAssets().open(subAsset);
                FileOutputStream out = new FileOutputStream(subDest);

                byte[] buffer = new byte[4096];
                int read;

                while((read=in.read(buffer))!=-1){
                    out.write(buffer,0,read);
                }

                in.close();
                out.close();
            }
        }
    }

    private Notification buildNotification(){

        Intent notificationIntent = new Intent(this,MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle("Omnitrix Listening")
                .setContentText("Say 'Omni Tricks' to wake me up")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel(){

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Omnitrix Wake Word",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationManager manager = getSystemService(NotificationManager.class);

        manager.createNotificationChannel(channel);
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        return START_STICKY;
    }

    @Override
    public void onDestroy(){

        super.onDestroy();

        if(speechService!=null)
            speechService.stop();

        if(model!=null)
            model.close();
    }
}