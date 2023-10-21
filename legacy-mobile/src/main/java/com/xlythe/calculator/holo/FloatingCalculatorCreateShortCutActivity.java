package com.xlythe.calculator.holo;

import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.annotation.DrawableRes;

import com.xlythe.view.floating.CreateShortcutActivity;

/**
 * Creates the shortcut icon
 */
public class FloatingCalculatorCreateShortCutActivity extends CreateShortcutActivity {
    @Override
    public CharSequence getShortcutName() {
        return getString(R.string.app_name);
    }

    @DrawableRes
    @Override
    public int getShortcutIcon() {
        return R.drawable.ic_launcher_floating;
    }

    @Override
    public Intent getOpenShortcutActivityIntent() {
        return new Intent(this, FloatingCalculatorOpenShortCutActivity.class);
    }

    protected NotificationChannel createNotificationChannel() {
        return new NotificationChannel(new String("xxx"), "FIXME", NotificationManager.IMPORTANCE_DEFAULT);
    }
}
