package com.xlythe.calculator.holo;

import android.app.Notification;
import android.content.Intent;

import com.xlythe.view.floating.OpenShortcutActivity;

/**
 * When the shortcut icon is pressed, use this Activity to launch the overlay Service
 */
public class FloatingCalculatorOpenShortCutActivity extends OpenShortcutActivity {
    @Override
    public Intent createServiceIntent() {
        return new Intent(this, FloatingCalculator.class);
    }

    @Override
    public Intent createActivityIntent() {
        return new Intent(this, FloatingCalculator.class);
    }

    @Override
    protected Notification createNotification() {
        return new Notification();
    }
}
