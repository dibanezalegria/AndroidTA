package com.pbluedotsoft.pcarstimeattackadfree;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Created by daniel on 7/03/18.
 *
 */
public class Laptime {

    public int lapNum;
    public float time, s1, s2, s3;
    public boolean invalid;

    public Laptime(int num) {
        lapNum = num;
        invalid = false;
        time = 0;
        s1 = 0;
        s2 = 0;
        s3 = 0;
    }

    public Laptime(int num, float[] laptime, boolean novalid) {
        // Split array in public accessible variables to reduce bloated code
        lapNum = num;
        time = laptime[0];
        s1 = laptime[1];
        s2 = laptime[2];
        s3 = laptime[3];
        invalid = novalid;
    }

    /**
     * Formats given time to string.
     *
     * @return string '--:--.---'
     */
    public static String format(float time) {
        if (time <= 0 || time == Float.MAX_VALUE) {
            return "--:--.---";
        }

        time = BigDecimal.valueOf(time).setScale(3, BigDecimal.ROUND_HALF_UP)
                .floatValue();  // truncate decimal part to 3 decimals

        int minutes = (int) time / 60;
        int seconds = (int) time % 60;
        int msec = ((int) (time * 1000)) % 1000;
        return String.format(Locale.ENGLISH, "%02d", minutes) +
                ":" + String.format(Locale.ENGLISH,"%02d", seconds) + "." +
                String.format(Locale.ENGLISH,"%03d", msec);
    }

    /**
     * Calculates difference between slow time and fast.
     *
     * @return string with format '+00:00.000'
     */
    public static String getGapStr(float slow, float fast) {
        float time = BigDecimal.valueOf(slow - fast).setScale(3, BigDecimal.ROUND_HALF_UP)
                .floatValue();  // truncate decimal part to 3 decimals

        // Plus or minus
        String sign = "+";
        if (time < 0)
            sign = "-";

        // Absolute
        time = Math.abs(time);

        if (time == 0) {
            return String.format(Locale.ENGLISH, "%10s", "+0.000");
        }

        int min = (int) time / 60;
        int sec = (int) time % 60;
        int ms = ((int) (time * 1000)) % 1000;

        String minStr = (min == 0) ? "" : String.format(Locale.ENGLISH, "%s:", min);
        String secStr = (sec == 0 && min == 0) ? "0." : String.format(Locale.ENGLISH, "%02d.",
                sec);
        String msStr = String.format(Locale.ENGLISH, "%03d", ms);

        return String.format(Locale.ENGLISH, "%s%s%s%s", sign, minStr, secStr, msStr);
    }

    /**
     * Calculates differences between slow and fast time.
     *
     * @return float - gap
     */
    public static float getGap(float slow, float fast) {
        return BigDecimal.valueOf(slow - fast).setScale(3, BigDecimal.ROUND_HALF_UP)
                .floatValue();  // truncate decimal part to 3 decimals
    }

    /**
     * Check that all times are more than zero and also invalid flag is zero.
     */
    public boolean isGood() {
        return s1 > 0 && s2 > 0 && s3 > 0 && time > 0 && !invalid;
    }

//    /**
//     * Overriding equals() to compare Laptimes
//     *
//     * @param obj - Laptime object
//     * @return - true if equal
//     */
//    public boolean equals(Object obj) {
//        if (obj == this)
//            return true;
//
//        if (!(obj instanceof Laptime))
//            return false;
//
//        if (lapNum == ((Laptime) obj).lapNum &&
//                time == ((Laptime) obj).time &&
//                s1 == ((Laptime) obj).s1 &&
//                s2 == ((Laptime) obj).s2 &&
//                s3 == ((Laptime) obj).s3 &&
//                invalid == ((Laptime) obj).invalid)
//            return true;
//
//        return false;
//    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "lap: %s s1: %s s2: %s s3: %s time: %s invalid: %s",
                lapNum, format(s1), format(s2), format(s3), format(time), invalid);
    }
}
