package org.hascoapi.utils;

public class Utils {

    public static int random(final int max) {
        return (int) (Math.random() * (double) max);
    }

    public static void wait(final int millis) {
        try {
            Thread.sleep(millis);
        } catch (final Exception e) {}
    }

    public static int block() {
        final int wait = 10 + random(20);
        wait(wait);
        return wait;
    }

    public static String adjustedPriority(String priority, int totContainerSlots) {
        int digits = 0;
        if (totContainerSlots < 10) {
            digits = 1;
        } else if (totContainerSlots < 100) {
            digits = 2;
        } else if (totContainerSlots < 1000) {
            digits = 3;
        } else {
            digits = 4;
        }
        String auxstr = String.valueOf(priority);
        for (int filler = auxstr.length(); filler < digits; filler++) {
            auxstr = "0" + auxstr;
        }
        return auxstr;
    }


}
