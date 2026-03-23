package com.omnitrix.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import android.util.Log;

public class PhoneController {

    public static boolean isPhoneCommand(String input) {
        input = input.toLowerCase();
        return input.startsWith("call ") ||
                input.startsWith("open ") ||
                input.startsWith("send message") ||
                input.startsWith("send sms") ||
                input.contains("turn on wifi") ||
                input.contains("turn off wifi");
    }

    public static void handleCommand(Context context,
                                     String input, OmnitrixMemory memory) {
        input = input.toLowerCase().trim();

        if (input.startsWith("call ")) {
            String name = input.replace("call ", "").trim();
            makeCall(context, name);

        } else if (input.startsWith("open ")) {
            String appName = input.replace("open ", "").trim();
            openApp(context, appName);

        } else if (input.startsWith("send message to ") ||
                input.startsWith("send sms to ")) {
            String[] parts = input.split(" to | saying ");
            if (parts.length >= 3) {
                String number = parts[1].trim();
                String message = parts[2].trim();
                sendSms(number, message);
            }
        }
    }

    private static void makeCall(Context context, String name) {
        // Check if it's a number or a name
        if (name.matches("[0-9+\\-\\s]+")) {
            // It's a phone number — call directly
            callNumber(context, name);
        } else {
            // It's a name — search contacts first
            String number = getNumberFromContacts(context, name);
            if (number != null) {
                Log.d("OMNITRIX", "Found contact: " + name + " = " + number);
                callNumber(context, number);
            } else {
                Log.e("OMNITRIX", "Contact not found: " + name);
                // Speak back that contact not found
                android.widget.Toast.makeText(context,
                        "Contact '" + name + "' not found",
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static void callNumber(Context context, String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("OMNITRIX", "Call failed: " + e.getMessage());
            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
            dialIntent.setData(Uri.parse("tel:" + number));
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(dialIntent);
        }
    }

    private static String getNumberFromContacts(Context context, String name) {
        String number = null;
        try {
            android.database.Cursor cursor = context.getContentResolver().query(
                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    },
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                            + " LIKE ?",
                    new String[]{"%" + name + "%"},
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                number = cursor.getString(cursor.getColumnIndexOrThrow(
                        android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER));
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("OMNITRIX", "Contact search error: " + e.getMessage());
        }
        return number;
    }

    private static void openApp(Context context, String appName) {
        appName = appName.toLowerCase().trim();
        String packageName = getPackageName(appName);

        Log.d("OMNITRIX", "Trying to open: " + appName +
                " | package: " + packageName);

        if (packageName != null) {
            Intent intent = context.getPackageManager()
                    .getLaunchIntentForPackage(packageName);

            if (intent != null) {
                Log.d("OMNITRIX", "Intent found! Launching...");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            } else {
                Log.e("OMNITRIX", "Intent NULL for package: " + packageName);
            }
        }

        // Fallback — open as website
        Log.d("OMNITRIX", "Using web fallback for: " + appName);
        try {
            String url = getWebUrl(appName);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e("OMNITRIX", "Fallback failed: " + e.getMessage());
        }
    }

    private static void sendSms(String number, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number,
                    null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getPackageName(String appName) {
        switch (appName.trim()) {
            case "youtube":      return "com.google.android.youtube";
            case "whatsapp":     return "com.whatsapp";
            case "instagram":    return "com.instagram.android";
            case "camera":       return "com.mediatek.camera";
            case "chrome":       return "com.android.chrome";
            case "maps":
            case "google maps":  return "com.google.android.apps.maps";
            case "spotify":      return "com.spotify.music";
            case "twitter":
            case "x":            return "com.twitter.android";
            case "facebook":     return "com.facebook.katana";
            case "telegram":     return "org.telegram.messenger";
            case "settings":     return "com.android.settings";
            case "gmail":        return "com.google.android.gm";
            case "photos":       return "com.google.android.apps.photos";
            case "play store":   return "com.android.vending";
            case "calculator":   return "com.android.calculator2";
            case "clock":        return "com.android.deskclock";
            case "snapchat":     return "com.snapchat.android";
            default:             return null;
        }
    }

    private static String getWebUrl(String appName) {
        switch (appName.trim()) {
            case "youtube":    return "https://www.youtube.com";
            case "whatsapp":   return "https://web.whatsapp.com";
            case "instagram":  return "https://www.instagram.com";
            case "facebook":   return "https://www.facebook.com";
            case "twitter":
            case "x":          return "https://www.twitter.com";
            case "gmail":      return "https://mail.google.com";
            case "maps":       return "https://maps.google.com";
            default:           return "https://www.google.com/search?q=" + appName;
        }
    }
}