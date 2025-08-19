package com.example.alarmamusical;

import android.content.Context;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.alarmamusical.TokenRefreshWorker;

import java.util.concurrent.TimeUnit;

public class WorkManagerSetup {

    public static void setupTokenRefresh(Context context) {
        PeriodicWorkRequest tokenRefreshRequest = new PeriodicWorkRequest.Builder(
                TokenRefreshWorker.class, 55, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(context).enqueue(tokenRefreshRequest);
    }
}