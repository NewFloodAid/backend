package com.example.flood_aid.utils;

import java.sql.Timestamp;
import java.util.Calendar;

public class DateUtils {  // Renamed to avoid conflict with java.util.Date
    public static Timestamp setEndOfDay(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return new Timestamp(cal.getTimeInMillis());
    }
}
