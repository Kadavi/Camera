package org.schoolsfirstfcu.mobile.plugin.checkcapture;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class Camera extends CordovaPlugin {
	private static final String TAG = Camera.class.getSimpleName();
	private CallbackContext callbackContext;

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if (!hasRearFacingCamera()) {
			String message = "No rear camera detected";
			callbackContext.error(message);
			Log.d(TAG, message);
			return false;
		}

		this.callbackContext = callbackContext;
		try {
			Log.d(TAG, "Launching camera plugin...");
			Context context = cordova.getActivity().getApplicationContext();
			Intent intent = new Intent(context, CameraActivity.class);
			intent.putExtra(CameraActivity.TITLE, args.getString(0));
			intent.putExtra(CameraActivity.QUALITY, args.getInt(1));
			intent.putExtra(CameraActivity.TARGET_WIDTH, args.getInt(2));
			intent.putExtra(CameraActivity.TARGET_HEIGHT, args.getInt(3));
			intent.putExtra(CameraActivity.LOGO_FILENAME, args.getString(4));
			intent.putExtra(CameraActivity.DESCRIPTION, args.getString(5));
			
			cordova.startActivityForResult(this, intent, 0);

			return true;
		} catch (IllegalArgumentException e) {
			callbackContext.error("Illegal Argument Exception");
			PluginResult r = new PluginResult(PluginResult.Status.ERROR);
			callbackContext.sendPluginResult(r);
			return true;
		}
	}

	private boolean hasRearFacingCamera() {
		Context context = cordova.getActivity().getApplicationContext();
		return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		
		if (resultCode == Activity.RESULT_OK) {
			callbackContext.success(intent.getExtras().getString(CameraActivity.IMAGE_DATA));
		} else if (resultCode == CameraActivity.RESULT_ERROR) {
			String errorMessage = intent.getExtras().getString(CameraActivity.ERROR_MESSAGE);
			if (errorMessage != null) {
				callbackContext.error(errorMessage);
			} else {
				callbackContext.error("Failed to take picture");
			}
		}
		 
	}

}
