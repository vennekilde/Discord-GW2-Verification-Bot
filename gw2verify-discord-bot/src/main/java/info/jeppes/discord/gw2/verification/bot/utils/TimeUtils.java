/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.jeppes.discord.gw2.verification.bot.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jeppe Boysen Vennekilde
 */
public class TimeUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile("((?<years>\\d+)Y)?((?<months>\\d+)MM)?((?<days>\\d+)D)?((?<hours>\\d+)H)?((?<minutes>\\d+)M)?((?<seconds>\\d+)S)?", Pattern.CASE_INSENSITIVE);

    private static final int SECONDS_IN_MINUTE = 60;
    private static final int MINUTES_IN_HOUR = 60;
    private static final int HOURS_IN_DAY = 24;
    private static final int DAYS_IN_MONTH = 31; //ish
    private static final int DAYS_IN_YEAR = 365; //ish
    private static final long MILLIS_IN_SECOND = 1000;
    private static final long MILLIS_IN_MINUTE = (long) MILLIS_IN_SECOND * SECONDS_IN_MINUTE;
    private static final long MILLIS_IN_HOUR = (long) MILLIS_IN_MINUTE * MINUTES_IN_HOUR;
    private static final long MILLIS_IN_DAY = (long) MILLIS_IN_HOUR * HOURS_IN_DAY;
    private static final long MILLIS_IN_MONTH = (long) MILLIS_IN_DAY * DAYS_IN_MONTH;
    private static final long MILLIS_IN_YEAR = (long) MILLIS_IN_DAY * DAYS_IN_YEAR;

    public static int[] getTimeWWDDHHMMSS(long time) {
        int weeks = (int) time / 604800;
        int days = (int) (time % 604800) / 86400;
        int hours = (int) ((time % 604800) % 86400) / 3600;
        int minutes = (int) (((time % 604800) % 86400) % 3600) / 60;
        int seconds = (int) (((time % 604800) % 86400) % 3600) % 60;
        return new int[]{weeks, days, hours, minutes, seconds};
    }

    public static String getTimeWWDDHHMMSSStringShort(long time) {
        int[] timeWWDDHHMMSS = getTimeWWDDHHMMSS(time);
        boolean useWeeks = timeWWDDHHMMSS[0] != 0;
        boolean useDays = timeWWDDHHMMSS[1] != 0;
        boolean useHours = timeWWDDHHMMSS[2] != 0;
        boolean useMinuts = timeWWDDHHMMSS[3] != 0;
        boolean useSeconds = timeWWDDHHMMSS[4] != 0;
        String timeString = (useWeeks ? (timeWWDDHHMMSS[0] + " week" + (timeWWDDHHMMSS[0] == 1 ? " " : "s ")) : "")
                + (useDays ? (timeWWDDHHMMSS[1] + " day" + (timeWWDDHHMMSS[1] == 1 ? " " : "s ")) : "")
                + (useHours ? (timeWWDDHHMMSS[2] + " hour" + (timeWWDDHHMMSS[2] == 1 ? " " : "s ")) : "")
                + (useMinuts ? (timeWWDDHHMMSS[3] + " minute" + (timeWWDDHHMMSS[3] == 1 ? " " : "s ")) : "")
                + ((useWeeks || useDays || useHours || useMinuts) && useSeconds ? "and " : "")
                + (useSeconds ? (timeWWDDHHMMSS[4] + " second" + (timeWWDDHHMMSS[4] == 1 ? "" : "s")) : "");
        if (timeString.isEmpty()) {
            return "0 seconds";
        }
        return timeString;
    }

    public static long getDurationMillisFromString(String durationString) {
        long duration = 0;
        Matcher matcher = DURATION_PATTERN.matcher(durationString);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Could not parse provided input");
        }
        String years = matcher.group("years");
        String months = matcher.group("months");
        String days = matcher.group("days");
        String hours = matcher.group("hours");
        String minutes = matcher.group("minutes");
        String seconds = matcher.group("seconds");
        if (years != null) {
            duration += Integer.parseInt(years) * MILLIS_IN_YEAR;
        }
        if (months != null) {
            duration += Integer.parseInt(months) * MILLIS_IN_MONTH;
        }
        if (days != null) {
            duration += Integer.parseInt(days) * MILLIS_IN_DAY;
        }
        if (hours != null) {
            duration += Integer.parseInt(hours) * MILLIS_IN_HOUR;
        }
        if (minutes != null) {
            duration += Integer.parseInt(minutes) * MILLIS_IN_MINUTE;
        }
        if (seconds != null) {
            duration += Integer.parseInt(seconds) * MILLIS_IN_SECOND;
        }

        return duration;
    }
}
