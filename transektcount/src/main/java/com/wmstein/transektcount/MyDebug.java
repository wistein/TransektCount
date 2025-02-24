package com.wmstein.transektcount;

/**
 * Global constant to control logcat logging.
 * <p>
 * Should always be set to false in released versions,
 * should only temporarily set to true for debugging.
 * <p>
 * Put back MyDebug.kt to MyDebug.java as in Kotlin2 "object MyDebug" produces Errors when called
 * from java modules.
 * Last edited on 2024-12-15
 */
public class MyDebug
{
    /** Un-comment one of the 2 code lines as described.
     *    dLog = false for release version
     *    dLog = true for debug version
     */
    public static boolean DLOG = false;
//    public static boolean DLOG = true;
}
