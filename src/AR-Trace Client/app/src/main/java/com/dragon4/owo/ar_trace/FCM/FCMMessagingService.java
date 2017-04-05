package com.dragon4.owo.ar_trace.FCM;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dragon4.owo.ar_trace.ARCore.Activity.GoogleSignActivity;
import com.dragon4.owo.ar_trace.ARCore.MixView;
import com.dragon4.owo.ar_trace.MainActivity;
import com.dragon4.owo.ar_trace.Model.User;
import com.dragon4.owo.ar_trace.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by joyeongje on 2017. 1. 10..
 */

public class FCMMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final int NOTIFICATION_ID = 2229393;
    private static HashMap<String, HashMap<String, String>> tracePushHashMap = new HashMap<>();

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            sendNotification(remoteMessage.getData().get("message"));
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            //sendNotification(remoteMessage.getNotification().getBody());
        }
    }

    private void sendNotification(String messageBody) {
        try {
            JSONObject jsonObj = new JSONObject(messageBody);

            Intent intent;
            if(isAppOnForeground(getApplicationContext()))
                intent = new Intent(this, MixView.class);
            else
                intent = new Intent(this, GoogleSignActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | intent.FLAG_ACTIVITY_SINGLE_TOP);
            Bundle bundle = new Bundle();
            bundle.putString("userName", jsonObj.getString("userName"));
            bundle.putString("buildingID", jsonObj.getString("buildingID"));
            bundle.putString("traceID", jsonObj.getString("traceID"));
            bundle.putString("title", jsonObj.getString("title"));
            intent.putExtras(bundle);

            String notificationString;
            HashMap<String, String> likeUserHashMap = tracePushHashMap.get(jsonObj.getString("traceID"));
            if(likeUserHashMap == null) {
                likeUserHashMap = new HashMap<>();
                likeUserHashMap.put(jsonObj.getString("userID"), jsonObj.getString("userName"));
                tracePushHashMap.put(jsonObj.getString("traceID"), likeUserHashMap);
                notificationString = jsonObj.getString("userName") + "님이 회원님이 남긴 흔적을 좋아합니다.";
            }
            else {
                String userName = likeUserHashMap.get(jsonObj.getString("userID"));
                if(userName != null)
                    return;

                likeUserHashMap.put(jsonObj.getString("userID"), jsonObj.getString("userName"));
                notificationString = jsonObj.getString("userName") + "님 외 " + (likeUserHashMap.size() - 1) + "명이 회원님이 남긴 흔적을 좋아합니다.";
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                    PendingIntent.FLAG_ONE_SHOT);
            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.icon_convenience_store)
                    .setContentTitle("AR-Trace")
                    .setContentText(notificationString)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);
            Notification n = notificationBuilder.build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(jsonObj.getString("traceID"), NOTIFICATION_ID /* ID of notification */, n);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void clear(String traceID) {
        tracePushHashMap.remove(traceID);
    }

    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}