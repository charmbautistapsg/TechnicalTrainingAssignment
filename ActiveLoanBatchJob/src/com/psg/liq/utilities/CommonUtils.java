package com.psg.liq.utilities;

public class CommonUtils {
    public static int exitErrorCode = 8;
    public static int exitWarnCode = 4;
    public static int exitSuccessCode = 0;
    public static boolean exitError = false;

    public static int getExitErrorCode() {
        return exitErrorCode;
    }

    public static boolean isExitError() {
        return exitError;
    }
    public static void setExitError(boolean exitError) {
        CommonUtils.exitError = exitError;
    }

    public static int getExitWarnCode() {
        return exitWarnCode;
    }

    public static int getExitSuccessCode() {
        return exitSuccessCode;
    }
}
