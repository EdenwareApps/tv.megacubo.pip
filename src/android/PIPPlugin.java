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
	
	private Window window;
	private Activity activity;

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
		activity = cordova.getActivity();
		window = activity.getWindow();
        hasPIPMode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O; //>= SDK 26 Oreo
		if(hasPIPMode){
			try {
				Class.forName("android.app.PictureInPictureParams");
                pictureInPictureParamsBuilder = new PictureInPictureParams.Builder();
                Class<?> mainActivityClass = Class.forName("tv.megacubo.app.MainActivity");
                Method setOnUserLeaveHintCallback = mainActivityClass.getMethod("setOnUserLeaveHintCallback", Runnable.class);
                setOnUserLeaveHintCallback.invoke(activity, new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "onUserLeaveHint RECEIVED, autoPIP "+ autoPIP);
                        if(autoPIP) {
                            enterPip(null);
                        }
                    }
                });
                Log.d(TAG, "onUserLeaveHint callback setup");
			} catch(Exception e) {
				hasPIPMode = false;
                e.printStackTrace();
			}
		}
    }
            
    @Override    
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("enter")){
            if(args.length() > 1) {
                updatePipAspectRatio(args.getDouble(0), args.getDouble(1));
            }
            if(!autoPIP) {
                enterPip(callbackContext);
            } else {
                callbackContext.error("autoPIP is enabled.");
            }
            return true;
        } else if(action.equals("isPip")){
            this.isPip(callbackContext);
            return true;
        } else if(action.equals("autoPIP")){
            autoPIP = args.getBoolean(0);
            if(autoPIP && args.length() > 1) {
                updatePipAspectRatio(args.getDouble(1), args.getDouble(2));
            }
            callbackContext.success("autoPIP set to " + autoPIP);
            return true;
        } else if(action.equals("aspectRatio")){
            updatePipAspectRatio(args.getDouble(0), args.getDouble(1));
            callbackContext.success("aspectRatio set to " + args.getDouble(0) + ":" + args.getDouble(1));
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

    private void updatePipAspectRatio(Double width, Double height) {
        try{
            if(pictureInPictureParamsBuilder != null) {
                Rational aspectRatio = new Rational(width.intValue(), height.intValue());
                pictureInPictureParamsBuilder.setAspectRatio(aspectRatio);
                if(activity.isInPictureInPictureMode()){
                    activity.setPictureInPictureParams(pictureInPictureParamsBuilder.build());
                }
            } else {
                throw new Exception("Picture-in-picture unavailable.");
            }
        } catch(Exception e){                                         
            String stackTrace = Log.getStackTraceString(e);
            Log.d(TAG, "updatePipAspectRatio ERR " + stackTrace);
        }
    }
    
    private void enterPip(CallbackContext callbackContext) {
        try{
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
                    Context context = activity.getApplicationContext();
                    Intent openMainActivity = new Intent(context, activity.getClass());
                    openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    activity.startActivityIfNeeded(openMainActivity, 0);
                    activity.enterPictureInPictureMode(pictureInPictureParamsBuilder.build());
					if(callbackContext != null) callbackContext.success("Picture-in-picture mode started.");
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
    
    public void isPip(CallbackContext callbackContext) {
		String ret = "false";
		if(hasPIPMode && pictureInPictureParamsBuilder != null && activity.isInPictureInPictureMode()){
			ret = "true";
		}
		callbackContext.success(ret);
    }
    
}