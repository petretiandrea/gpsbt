package com.example.petretiandrea.gpsreceiver.util;

import android.icu.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Utils {

    public static Date localDateFromUTC(long utc) {
        Date utcDate = new Date(utc);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
        calendar.setTime(utcDate);
        return calendar.getTime();
    }

    public static String formatDateTime(Date datetime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(datetime);
    }

}
