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
                if (activity instanceof tv.megacubo.app.MainActivity) {
                    ((tv.megacubo.app.MainActivity) activity).setOnUserLeaveHintCallback(() -> {
                        Log.d(TAG, "onUserLeaveHint RECEIVED, autoPIP " + autoPIP);
                        if (autoPIP) {
                            enterPip(null);
                        }
                    });
                    Log.d(TAG, "onUserLeaveHint callback setup");
                } else {
                    Log.e(TAG, "Activity is not MainActivity, cannot set callback");
                    hasPIPMode = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up PIP", e);
                hasPIPMode = false;
            }
        }
    }

    @PluginMethod
    public void enter(PluginCall call) {
        if (autoPIP) {
            call.reject("autoPIP is enabled.");
            return;
        }
        if (call.hasOption("width") && call.hasOption("height")) {
            updatePipAspectRatio(call.getDouble("width"), call.getDouble("height"));
        }
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
        double width = call.getDouble("width");
        double height = call.getDouble("height");
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
                Rational aspectRatio = new Rational((int) width, (int) height);
                pictureInPictureParamsBuilder.setAspectRatio(aspectRatio);
                if (getActivity().isInPictureInPictureMode()) {
                    getActivity().setPictureInPictureParams(pictureInPictureParamsBuilder.build());
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
            if (activity.isInPictureInPictureMode()) {
                if (call != null) {
                    JSObject ret = new JSObject();
                    ret.put("value", "Already in picture-in-picture mode.");
                    call.resolve(ret);
                }
                return;
            }
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            if (!pm.isInteractive()) {
                if (call != null) {
                    JSObject ret = new JSObject();
                    ret.put("value", "Screen is off.");
                    call.resolve(ret);
                }
                return;
            }
            if (pictureInPictureParamsBuilder != null) {
                Context context = activity.getApplicationContext();
                Intent openMainActivity = new Intent(context, activity.getClass());
                openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivityIfNeeded(openMainActivity, 0);
                activity.enterPictureInPictureMode(pictureInPictureParamsBuilder.build());
                if (call != null) {
                    JSObject ret = new JSObject();
                    ret.put("value", "Picture-in-picture mode started.");
                    call.resolve(ret);
                }
            } else {
                throw new Exception("Picture-in-picture unavailable.");
            }
        } catch (Exception e) {
            Log.d(TAG, "enterPip ERR " + Log.getStackTraceString(e));
            if (call != null) {
                call.reject("Error: " + e.getMessage());
            }
        }
    }
}