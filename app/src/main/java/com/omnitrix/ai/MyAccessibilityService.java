package com.omnitrix.ai; // change to your package name

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.content.Intent;

public class MyAccessibilityService extends AccessibilityService {

    private long lastPressTime = 0;

    @Override
    protected boolean onKeyEvent(KeyEvent event) {

        // Only act when button is PRESSED (not released)
        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {

                long currentTime = System.currentTimeMillis();

                // Check double tap (within 300 ms)
                if (currentTime - lastPressTime < 300) {

                    // 🔥 Launch your Omnitrix (MainActivity)
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }

                lastPressTime = currentTime;
            }
        }

        return super.onKeyEvent(event);
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
        // Not needed for this feature
    }

    @Override
    public void onInterrupt() {
        // Not needed
    }
}