# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\hnoct\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class project.kitchen.ui.FragmentSearch.MyJavaScriptInterface {
   public *;
}
-ignorewarnings

-keep class * {
    public private *;
}

## Butterknife
#-dontwarn butterknife.internal.**
#-keep class butterknife.** { *; }
#-keep class **$$ViewInjector { *; }
#-keepclasseswithmembernames class * {
#    @butterknife.InjectView <fields>;
#}
#-keepclasseswithmembernames class * {
#    @butterknife.OnClick <methods>;
#    @butterknife.OnEditorAction <methods>;
#    @butterknife.OnItemClick <methods>;
#    @butterknife.OnItemLongClick <methods>;
#    @butterknife.OnLongClick <methods>;
#}