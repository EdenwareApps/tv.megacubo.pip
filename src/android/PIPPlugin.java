package tv.megacubo.pip;

import android.view.Window;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.util.Rational;
import android.util.Log;
import android.os.PowerManager;
import android.os.Bundle;
import android.os.Build;

import java.lang.reflect.Method;
import java.lang.Exception;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PIPPlugin extends CordovaPlugin {
    private PictureInPictureParams.Builder pictureInPictureParamsBuilder = null;
    private CallbackContext callback = null;
	private String TAG = "PIPPlugin";
	private boolean hasPIPMode = false;
	private boolean autoPIP = false;
	
    Double autoPipWidth = 320.00;
    Double autoPipHeight = 240.00;

	private Window window;
	private Activity activity;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
		activity = cordova.getActivity();
		window = activity.getWindow();
        hasPIPMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O; //>= SDK 26 //Oreo
		if(hasPIPMode){
			try{
				Class.forName("android.app.PictureInPictureParams");
                try {
                    Class<?> mainActivityClass = Class.forName("tv.megacubo.app.MainActivity");
                    Method setOnUserLeaveHintCallback = mainActivityClass.getMethod("setOnUserLeaveHintCallback", Runnable.class);
                    setOnUserLeaveHintCallback.invoke(activity, new Runnable() {
                        @Override
                        public void run() {
                            if(autoPIP) {
                                enterPip(autoPipWidth, autoPipHeight, null);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
			} catch(Exception e) {
				hasPIPMode = false;
                e.printStackTrace();
			}            
		}
    }
    
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        Log.d(TAG, "autoPip " + autoPIP);
        if(autoPIP) {
            enterPip(autoPipWidth, autoPipHeight, null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "autoPip " + autoPIP);
        if(autoPIP) {
            enterPip(autoPipWidth, autoPipHeight, null);
        }
    }
        
    @Override    
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("enter")){
            Double width = args.getDouble(0);
            Double height = args.getDouble(1);
            enterPip(width, height, callbackContext);
            return true;
        } else if(action.equals("isPip")){
            this.isPip(callbackContext);
            return true;
        } else if(action.equals("autoPip")){
            autoPIP = args.getBoolean(0);
            if(autoPIP) {
                autoPipWidth = args.getDouble(1);
                autoPipHeight = args.getDouble(2);
            }
            return true;
        } else if(action.equals("onPipModeChanged")){
            if(callback == null){
                callback = callbackContext; //save global callback for later callbacks
                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT); //send no result to execute the callbacks later
                result.setKeepCallback(true); // Keep callback
            }
            return true;
        } else if(action.equals("isPipModeSupported")){
            this.isPipModeSupported(callbackContext);
            return true;
        }
        return false;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
        if(callback != null && hasPIPMode){
            try{
                boolean active = activity.isInPictureInPictureMode(); //>= SDK 26 //Oreo
                Log.d(TAG, "pipChanged " + active);
                if(active){
                    this.callbackFunction(true, "true");
                } else {
                    this.callbackFunction(true, "false");
                }
            } catch(Exception e){
                String stackTrace = Log.getStackTraceString(e);
                Log.d(TAG, "pipChanged ERR " + stackTrace);
                this.callbackFunction(false, stackTrace);
            }
        }
    }
    
    public void callbackFunction(boolean op, String str){
        if(op){
            PluginResult result = new PluginResult(PluginResult.Status.OK, str);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        } else {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, str);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }
    
    private void enterPip(Double width, Double height, CallbackContext callbackContext) {
        try{
            this.initializePip();
            if(activity.isInPictureInPictureMode()) {
                if(callbackContext != null) callbackContext.success("Already in picture-in-picture mode.");
                return;
            }
            PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isInteractive();
            if(!isScreenOn) {
                if(callbackContext != null) callbackContext.success("Screen is off.");
                return;
            }            
            if(pictureInPictureParamsBuilder != null) {
				boolean active = activity.isInPictureInPictureMode(); //>= SDK 26 //Oreo
				Log.d(TAG, "enterPip " + active);
				if(active){
					if(callbackContext != null) callbackContext.success("Already in picture-in-picture mode.");
				} else {
					if(width != null && width > 0 && height != null && height > 0){
						Context context = activity.getApplicationContext();
						Intent openMainActivity = new Intent(context, activity.getClass());
						openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
						activity.startActivityIfNeeded(openMainActivity, 0);
						Rational aspectRatio = new Rational(Integer.valueOf(width.intValue()), Integer.valueOf(height.intValue()));
						pictureInPictureParamsBuilder.setAspectRatio(aspectRatio).build();
						activity.enterPictureInPictureMode(pictureInPictureParamsBuilder.build());
						if(callbackContext != null) callbackContext.success("Scaled picture-in-picture mode started.");
					} else {
						activity.enterPictureInPictureMode();
						if(callbackContext != null) callbackContext.success("Default picture-in-picture mode started.");
					}
				}
            } else {
				throw new Exception("Picture-in-picture unavailable.");
            }
        } catch(Exception e){
            String stackTrace = Log.getStackTraceString(e);
			Log.d(TAG, "enterPip ERR " + stackTrace);
            if(callbackContext != null) callbackContext.error(stackTrace);
        }             
    }
    
    private void isPipModeSupported(CallbackContext callbackContext) {
		if(hasPIPMode){
			callbackContext.success("true");
		} else {
			callbackContext.success("false");
		}
    }
    
    private void initializePip() {
        if(pictureInPictureParamsBuilder == null) {
			if(hasPIPMode) {
				try {
					pictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
				} catch(Exception e) {
					pictureInPictureParamsBuilder = null;
					String stackTrace = Log.getStackTraceString(e);
					Log.d(TAG, stackTrace);
				}
			} else {
				Log.d(TAG, "PIP unavailable.");
			}
        }
    }
    
    public void isPip(CallbackContext callbackContext) {
		String ret = "false";
		if(hasPIPMode && pictureInPictureParamsBuilder != null && activity.isInPictureInPictureMode()){
			ret = "true";
		}
		callbackContext.success(ret);
    }
    
}