package com.neverreader.util.android;

/**
 * Any classes implementing this interface will be kept by Proguard.  Provided the proguard.cfg file has the following definitions
 * 
 * -keep public class com.neverreader.util.android.NoObfuscation
 * -keep public class * implements com.neverreader.util.android.NoObfuscation
 * -keepclassmembers class * implements com.neverreader.util.android.NoObfuscation {
 *   <methods>;
 * } 
 * 
 * 
 * @author max
 *
 */
public interface NoObfuscation {

}
