package com.example.petretiandrea.gpsreceiver.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

import com.example.petretiandrea.gpsreceiver.R;


/**
 * Project Sensors
 * Package it.petretiandrea.sensors.utils
 * Created by Petreti Andrea petretiandrea@gmail.com.
 * Created at 20/11/17.
 */

public class NotificationHelper extends ContextWrapper{

    private NotificationManager _notificationManager;

    public static final String CHANNEL_ONE_ID = "it.petretiandrea.gpsreceiver.foregroundnotification";
    public static final String CHANNEL_ONE_NAME = "Notifica Servizio Bluetooth Service";

    public NotificationHelper(Context base) {
        super(base);
        createChannels();
    }

    private void createChannels()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            getManager().createNotificationChannel(notificationChannel);
        }
    }

    public NotificationCompat.Builder getNotificationOne(String title, String body)
    {
        return new NotificationCompat.Builder(this, CHANNEL_ONE_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setAutoCancel(true);
    }

    public void notify(int id, NotificationCompat.Builder notificationBuilder)
    {
        getManager().notify(id, notificationBuilder.build());
    }

    private NotificationManager getManager() {
        if (_notificationManager == null)
            _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        return _notificationManager;
    }

}
