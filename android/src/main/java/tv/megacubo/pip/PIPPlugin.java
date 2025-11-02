package tv.megacubo.pip;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import android.util.Rational;
import android.view.View;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PIP")
public class PIPPlugin extends Plugin {
    private static final String TAG = "PIPPlugin";
    private PictureInPictureParams.Builder pictureInPictureParamsBuilder = null;
    private boolean hasPIPMode = false;
    private boolean autoPIP = false;

    @Override
    public void load() {
        hasPIPMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O; // >= SDK 26 Oreo
        if (hasPIPMode) {
            try {
                pictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
                Activity activity = getActivity();

                // Use reflection to avoid hardcoding MainActivity
                try {
                    Class<?> mainActivityClass = activity.getClass();
                    java.lang.reflect.Method setCallbackMethod = mainActivityClass.getMethod("setOnUserLeaveHintCallback", Runnable.class);
                    setCallbackMethod.invoke(activity, (Runnable) () -> {
                        // Prevent multiple PiP entries - check if already in PiP
                        if (autoPIP && !activity.isInPictureInPictureMode()) {
                            Log.d(TAG, "onUserLeaveHint: Entering PiP (autoPIP enabled)");
                            enterPip(null);
                        } else if (activity.isInPictureInPictureMode()) {
                            Log.d(TAG, "onUserLeaveHint: Already in PiP, skipping");
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "AutoPIP setup error: " + e.getMessage());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error setting up PIP", e);
                hasPIPMode = false;
            }
        }
    }

    @PluginMethod
    public void enter(PluginCall call) {
        Log.d(TAG, "enter() called from JavaScript");
        if (autoPIP) {
            Log.d(TAG, "enter() rejected: autoPIP is enabled");
            call.reject("autoPIP is enabled.");
            return;
        }
        if (call.hasOption("width") && call.hasOption("height")) {
            updatePipAspectRatio(call.getDouble("width"), call.getDouble("height"));
        }
        Log.d(TAG, "enter() calling enterPip()");
        enterPip(call);
    }

    @PluginMethod
    public void isPip(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", hasPIPMode && pictureInPictureParamsBuilder != null && getActivity().isInPictureInPictureMode());
        call.resolve(ret);
    }

    @PluginMethod
    public void autoPIP(PluginCall call) {
        autoPIP = call.getBoolean("value", false);
        if (autoPIP && call.hasOption("width") && call.hasOption("height")) {
            updatePipAspectRatio(call.getDouble("width"), call.getDouble("height"));
        }
        JSObject ret = new JSObject();
        ret.put("value", "autoPIP set to " + autoPIP);
        call.resolve(ret);
    }

    @PluginMethod
    public void aspectRatio(PluginCall call) {
        if (!call.hasOption("width") || !call.hasOption("height")) {
            call.reject("Missing width or height parameters.");
            return;
        }

        Double width = call.getDouble("width");
        Double height = call.getDouble("height");

        if (width == null || height == null) {
            call.reject("Invalid width or height values.");
            return;
        }

        if (width <= 0 || height <= 0) {
            call.reject("Width and height must be positive numbers.");
            return;
        }

        updatePipAspectRatio(width, height);
        JSObject ret = new JSObject();
        ret.put("value", "aspectRatio set to " + width + ":" + height);
        call.resolve(ret);
    }

    @PluginMethod
    public void onPipModeChanged(PluginCall call) {
        call.setKeepAlive(true); // Keep callback for future events
        saveCall(call); // Store for configuration change events
    }

    @PluginMethod
    public void isPipModeSupported(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", hasPIPMode);
        call.resolve(ret);
    }

    @Override
    protected void handleOnConfigurationChanged(Configuration newConfig) {
        PluginCall savedCall = getSavedCall();
        if (savedCall != null && hasPIPMode) {
            try {
                boolean active = getActivity().isInPictureInPictureMode();
                Log.d(TAG, "pipChanged " + active);
                JSObject ret = new JSObject();
                ret.put("value", active);
                savedCall.resolve(ret);
            } catch (Exception e) {
                Log.d(TAG, "pipChanged ERR " + Log.getStackTraceString(e));
                savedCall.reject("Error: " + e.getMessage());
            }
        }
    }

    private void updatePipAspectRatio(double width, double height) {
        try {
            if (pictureInPictureParamsBuilder != null) {
                int w = (int) width;
                int h = (int) height;
                
                // Validate aspect ratio limits (Android requires 1:2.39 to 2.39:1)
                // Also ensure values are within reasonable bounds
                double ratio = (double) w / h;
                if (ratio < 0.4184) { // Less than 1:2.39
                    Log.w(TAG, "Aspect ratio too small: " + ratio + ", clamping to minimum");
                    h = (int) (w / 0.4184);
                } else if (ratio > 2.39) { // Greater than 2.39:1
                    Log.w(TAG, "Aspect ratio too large: " + ratio + ", clamping to maximum");
                    w = (int) (h * 2.39);
                }
                
                // Ensure minimum size
                if (w <= 0) w = 240;
                if (h <= 0) h = 240;
                
                Rational aspectRatio = new Rational(w, h);
                pictureInPictureParamsBuilder.setAspectRatio(aspectRatio);
                
                // Update params if already in PiP mode
                if (getActivity().isInPictureInPictureMode()) {
                    getActivity().setPictureInPictureParams(pictureInPictureParamsBuilder.build());
                    Log.d(TAG, "Updated PiP aspect ratio: " + w + ":" + h);
                } else {
                    Log.d(TAG, "Prepared PiP aspect ratio: " + w + ":" + h);
                }
            } else {
                throw new Exception("Picture-in-picture unavailable.");
            }
        } catch (Exception e) {
            Log.d(TAG, "updatePipAspectRatio ERR " + Log.getStackTraceString(e));
        }
    }

    private void enterPip(PluginCall call) {
        try {
            Activity activity = getActivity();
            
            // Double check if already in PiP (prevent multiple entries)
            if (activity.isInPictureInPictureMode()) {
                Log.d(TAG, "enterPip: Already in PiP mode, skipping");
                if (call != null) {
                    JSObject ret = new JSObject();
                    ret.put("value", "Already in picture-in-picture mode.");
                    call.resolve(ret);
                }
                return;
            }
            
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (!pm.isInteractive()) {
                Log.d(TAG, "enterPip: Screen is off, cannot enter PiP");
                if (call != null) {
                    JSObject ret = new JSObject();
                    ret.put("value", "Screen is off.");
                    call.resolve(ret);
                }
                return;
            }
            
            if (pictureInPictureParamsBuilder != null) {
                // Ensure we have valid aspect ratio before entering
                PictureInPictureParams params = pictureInPictureParamsBuilder.build();
                if (params.getAspectRatio() == null) {
                    Log.e(TAG, "enterPip: Aspect ratio is null, setting default");
                    pictureInPictureParamsBuilder.setAspectRatio(new Rational(16, 9));
                    params = pictureInPictureParamsBuilder.build();
                }
                
                Log.d(TAG, "enterPip: Entering PiP mode");
                
                // Check Activity state to determine if we need to reorder
                // In Overview mode, even if Activity appears active, we should reorder
                // to ensure it's properly positioned before entering PiP
                boolean shouldReorder = true;
                try {
                    View decorView = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
                    boolean isResumed = activity.isResumed();
                    boolean hasFocus = activity.hasWindowFocus();
                    boolean isVisible = decorView != null && decorView.getVisibility() == View.VISIBLE;
                    
                    Log.d(TAG, "enterPip: Activity state - resumed: " + isResumed + ", hasFocus: " + hasFocus + ", visible: " + isVisible);
                    
                    // More conservative approach: Only skip reordering if Activity is clearly
                    // the topmost active Activity (not just resumed/focused/visible, but actually in foreground)
                    // In Overview, the Activity may appear active but isn't truly in foreground
                    if (isResumed && hasFocus && isVisible) {
                        // Additional check: verify Activity is actually topmost by checking if it's finishing
                        // or if window is attached (truly in foreground)
                        if (!activity.isFinishing() && activity.getWindow() != null && 
                            activity.getWindow().getDecorView() != null &&
                            activity.getWindow().getDecorView().isAttachedToWindow()) {
                            Log.d(TAG, "enterPip: Activity is truly active, attempting to skip reorder");
                            // Even if appears active, still reorder if called during pause/leaveHint
                            // This prevents issues when entering from Overview
                            shouldReorder = false;
                        } else {
                            Log.d(TAG, "enterPip: Activity appears active but window not properly attached, will reorder");
                            shouldReorder = true;
                        }
                    } else {
                        Log.d(TAG, "enterPip: Activity not fully active, will reorder");
                        shouldReorder = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "enterPip: Error checking Activity state, will reorder: " + e.getMessage());
                    shouldReorder = true;
                }
                
                if (shouldReorder) {
                    Log.d(TAG, "enterPip: Reordering Activity to front before entering PiP");
                    Context context = activity.getApplicationContext();
                    Intent openMainActivity = new Intent(context, activity.getClass());
                    openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    try {
                        int result = activity.startActivityIfNeeded(openMainActivity, 0);
                        Log.d(TAG, "enterPip: startActivityIfNeeded result: " + result + " (0=not needed, 1=started, -1=error)");
                        
                        // Always add delay after reordering to allow Activity to settle
                        // This is especially important when coming from Overview mode
                        new android.os.Handler().postDelayed(() -> {
                            try {
                                Activity currentActivity = getActivity();
                                if (currentActivity == null || currentActivity.isFinishing()) {
                                    Log.e(TAG, "enterPip: Activity is null or finishing after delay");
                                    if (call != null) {
                                        call.reject("Activity is not available");
                                    }
                                    return;
                                }
                                
                                Log.d(TAG, "enterPip: Calling enterPictureInPictureMode after reorder delay");
                                currentActivity.enterPictureInPictureMode(params);
                                
                                if (call != null) {
                                    JSObject ret = new JSObject();
                                    ret.put("value", "Picture-in-picture mode started.");
                                    call.resolve(ret);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "enterPip: Error calling enterPictureInPictureMode after delay: " + e.getMessage());
                                if (call != null) {
                                    call.reject("Error entering PiP: " + e.getMessage());
                                }
                            }
                        }, 150); // Increased delay to 150ms to ensure Activity is ready
                        
                        // Return early since we're using delayed execution
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "enterPip: Error reordering Activity: " + e.getMessage());
                        // Continue to try entering PiP directly
                    }
                } else {
                    Log.d(TAG, "enterPip: Skipping reorder, Activity is active");
                }
                
                Log.d(TAG, "enterPip: Calling enterPictureInPictureMode directly");
                activity.enterPictureInPictureMode(params);
                
                if (call != null) {
                    JSObject ret = new JSObject();
                    ret.put("value", "Picture-in-picture mode started.");
                    call.resolve(ret);
                }
            } else {
                throw new Exception("Picture-in-picture unavailable.");
            }
        } catch (Exception e) {
            Log.e(TAG, "enterPip ERR " + Log.getStackTraceString(e));
            if (call != null) {
                call.reject("Error: " + e.getMessage());
            }
        }
    }
}