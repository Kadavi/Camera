package org.schoolsfirstfcu.mobile.plugin.checkcapture;

import static android.hardware.Camera.Parameters.FLASH_MODE_OFF;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
public class CameraActivity extends Activity {

	private static final String TAG = CameraActivity.class.getSimpleName();

	public static String TITLE = "Title";
	public static String QUALITY = "Quality";
	public static String TARGET_WIDTH = "TargetWidth";
	public static String TARGET_HEIGHT = "TargetHeight";
	public static String LOGO_FILENAME = "LogoFilename";
	public static String DESCRIPTION = "TargetHeight";
	public static String IMAGE_DATA = "ImageData";
	public static String ERROR_MESSAGE = "ErrorMessage";
	public static int RESULT_ERROR = 2;

	private static final int HEADER_HEIGHT = 56;
	private static final int FRAME_BORDER_SIZE = 28;

	private Camera camera;
	private RelativeLayout layout;
	private FrameLayout cameraPreviewView;
	private ImageView logo;
	private ImageButton captureButton;
	private Bitmap darkPictureButton;
	private Bitmap lightPictureButton;

	@Override
	protected void onResume() {
		super.onResume();
		try {
			camera = Camera.open();
			configureCamera();
			displayCameraPreview();
		} catch (Exception e) {
			finishWithError("Camera is not accessible");
		}
	}

	private void configureCamera() {
		Camera.Parameters cameraSettings = camera.getParameters();
		cameraSettings.setJpegQuality(100);
		List<String> supportedFocusModes = cameraSettings
				.getSupportedFocusModes();
		if (supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
			cameraSettings.setFocusMode(FOCUS_MODE_CONTINUOUS_PICTURE);
		} else if (supportedFocusModes.contains(FOCUS_MODE_AUTO)) {
			cameraSettings.setFocusMode(FOCUS_MODE_AUTO);
		}
		cameraSettings.setFlashMode(FLASH_MODE_OFF);
		camera.setParameters(cameraSettings);
	}

	private void displayCameraPreview() {
		cameraPreviewView.removeAllViews();
		cameraPreviewView.addView(new CameraPreview(this, camera));
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseCamera();
	}

	private void releaseCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		layout = new RelativeLayout(this);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		layout.setLayoutParams(layoutParams);
		createCameraPreview();
		createBackground();
		darkPictureButton = createCustomButton(true);
		lightPictureButton = createCustomButton(false);
		createCaptureButton(lightPictureButton);
		setContentView(layout);
	}

	private Bitmap createCustomButton(boolean isDark) {
		int width = pixelsToDp(70);
		int height = pixelsToDp(40);
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();

		if (!isDark) {
			paint.setARGB(255, 255, 255, 255);
		} else {
			paint.setARGB(255, 0, 0, 0);
		}

		paint.setStyle(Style.FILL);
		canvas.drawRoundRect(new RectF(0, 0, width, height), pixelsToDp(15),
				pixelsToDp(15), paint);

		paint.setARGB(255, 255, 0, 0);
		// canvas.drawCircle(0, 0, 30, paint);

		return bitmap;
	}

	private void createCameraPreview() {
		cameraPreviewView = new FrameLayout(this);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		cameraPreviewView.setLayoutParams(layoutParams);
		layout.addView(cameraPreviewView);
	}

	private void createBackground() {
		String filename = getIntent().getStringExtra(LOGO_FILENAME);
		if (!filename.isEmpty()) {
			layout.addView(new FrameBackgroundView(this));

			logo = new ImageView(this);
			logo.setScaleType(ScaleType.FIT_XY);
			setBitmap(logo, filename);
			RelativeLayout.LayoutParams layoutParams;
			layoutParams = new RelativeLayout.LayoutParams(pixelsToDp(35),
					pixelsToDp(260));
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
			layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			layoutParams.rightMargin = pixelsToDp(10);

			logo.setLayoutParams(layoutParams);
			layout.addView(logo);

			TextView side = new TextView(this);
			side.setText(getIntent().getStringExtra(TITLE));
			side.setTextSize(24);
			side.setEllipsize(null);

			int color = Color.parseColor("#F5DC49");
			side.setTextColor(color);

			int leg = (screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT)) / 2;

			int pivotX = 0;
			int pivotY = leg;

			int posX = leg;
			int posY = -leg + pixelsToDp(FRAME_BORDER_SIZE); // + pivotY;

			side.setPivotX(pivotX);
			side.setPivotY(pivotY);
			side.setX(posX);
			side.setY(posY);
			side.setRotation(90);

			layout.addView(side);
		}

	}

	public class FrameBackgroundView extends View {

		public FrameBackgroundView(Context context) {
			super(context);
		}

		@Override
		public void draw(Canvas canvas) {

			// Draw border
			Path borderRect = new Path();
			int gap = pixelsToDp(FRAME_BORDER_SIZE);

			borderRect
					.addRect(
							pixelsToDp(FRAME_BORDER_SIZE) / 2,
							pixelsToDp(FRAME_BORDER_SIZE) / 2,
							(screenWidthInPixels() - pixelsToDp(FRAME_BORDER_SIZE) / 2)
									- pixelsToDp(HEADER_HEIGHT),
							(screenHeightInPixels() - pixelsToDp(FRAME_BORDER_SIZE) / 2)
									- gap, Path.Direction.CW);
			Paint borderPaint = new Paint();
			borderPaint.setARGB(255, 185, 199, 212);
			borderPaint.setAlpha(150);
			borderPaint.setStyle(Paint.Style.STROKE);
			borderPaint.setStrokeWidth(pixelsToDp(FRAME_BORDER_SIZE));
			canvas.drawPath(borderRect, borderPaint);

			Path buttonRect = new Path();
			int x = 0;
			int y = (screenHeightInPixels() - gap);
			int width = screenWidthInPixels();
			int height = screenHeightInPixels();
			buttonRect.addRect(x, y, width, height, Path.Direction.CW);
			Paint buttonPaint = new Paint();
			buttonPaint.setARGB(255, 185, 199, 212);
			buttonPaint.setAlpha(150);
			buttonPaint.setStyle(Style.FILL);

			canvas.drawPath(buttonRect, buttonPaint);

			// Draw top header
			Path headerRect = new Path();
			headerRect.addRect(screenWidthInPixels()
					- pixelsToDp(HEADER_HEIGHT), 0, screenWidthInPixels(),
					screenHeightInPixels(), Path.Direction.CW);

			Paint headerPaint = new Paint();
			headerPaint.setARGB(255, 45, 68, 82);
			headerPaint.setAlpha(255);
			headerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
			canvas.drawPath(headerRect, headerPaint);
		}

	}

	private void createCaptureButton(Bitmap buttonPicture) {
		captureButton = new ImageButton(getApplicationContext());
		captureButton.setImageBitmap(buttonPicture);
		captureButton.setBackgroundColor(Color.TRANSPARENT);

		RelativeLayout.LayoutParams layoutParams = null;

		layoutParams = new RelativeLayout.LayoutParams(
				buttonPicture.getWidth(), buttonPicture.getHeight());
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		layoutParams.topMargin = (screenHeightInPixels() - pixelsToDp(FRAME_BORDER_SIZE))
				- (buttonPicture.getHeight() / 2);
		layoutParams.leftMargin = (screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT))
				/ 2 - buttonPicture.getWidth() / 2;

		captureButton.setLayoutParams(layoutParams);
		captureButton.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				setCaptureButtonImageForEvent(event);
				return false;
			}
		});
		captureButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				takePictureWithAutoFocus();
			}
		});

		layout.addView(captureButton);
	}

	private int screenWidthInPixels() {
		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		return size.x;
	}

	private int screenHeightInPixels() {
		Point size = new Point();
		getWindowManager().getDefaultDisplay().getSize(size);
		return size.y;
	}

	private void setCaptureButtonImageForEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			captureButton.setImageBitmap(darkPictureButton);
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			captureButton.setImageBitmap(lightPictureButton);
		}
	}

	private void takePictureWithAutoFocus() {
		if (getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
			camera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					takePicture();
				}
			});
		} else {
			takePicture();
		}
	}

	private void takePicture() {
		try {
			camera.takePicture(null, null, new PictureCallback() {
				@Override
				public void onPictureTaken(byte[] jpegData, Camera camera) {
					new OutputCapturedImageTask().execute(jpegData);
				}
			});
		} catch (Exception e) {
			finishWithError("Failed to take image");
		}
	}

	private class OutputCapturedImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... jpegData) {
			try {
				Bitmap scaleBitmap = getScaledBitmap(jpegData[0]);
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				scaleBitmap.compress(Bitmap.CompressFormat.JPEG, 30, stream);
				byte[] byteArray = stream.toByteArray();

				String imageData = Base64.encodeToString(byteArray,
						Base64.DEFAULT);

				Intent data = new Intent();
				data.putExtra(IMAGE_DATA, imageData);

				setResult(RESULT_OK, data);
				finish();
			} catch (Exception e) {
				finishWithError("Failed to take picture.");
			}
			return null;
		}

	}

	private Bitmap getScaledBitmap(byte[] jpegData) {
		int targetWidth = getIntent().getIntExtra(TARGET_WIDTH, 600);
		int targetHeight = getIntent().getIntExtra(TARGET_HEIGHT, 400);
		if (targetWidth <= 0 && targetHeight <= 0) {
			return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
		}

		// get dimensions of image without scaling
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);

		// decode image as close to requested scale as possible
		options.inJustDecodeBounds = false;
		options.inSampleSize = calculateInSampleSize(options, targetWidth,
				targetHeight);
		Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0,
				jpegData.length, options);

		// set missing width/height based on aspect ratio
		float aspectRatio = ((float) options.outHeight) / options.outWidth;
		if (targetWidth > 0 && targetHeight <= 0) {
			targetHeight = Math.round(targetWidth * aspectRatio);
		} else if (targetWidth <= 0 && targetHeight > 0) {
			targetWidth = Math.round(targetHeight / aspectRatio);
		}

		// make sure we also
		Matrix matrix = new Matrix();
		matrix.postRotate(90);
		return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight,
				true);
	}

	private int calculateInSampleSize(BitmapFactory.Options options,
			int requestedWidth, int requestedHeight) {
		int originalHeight = options.outHeight;
		int originalWidth = options.outWidth;
		int inSampleSize = 1;
		if (originalHeight > requestedHeight || originalWidth > requestedWidth) {
			int halfHeight = originalHeight / 2;
			int halfWidth = originalWidth / 2;
			while ((halfHeight / inSampleSize) > requestedHeight
					&& (halfWidth / inSampleSize) > requestedWidth) {
				inSampleSize *= 2;
			}
		}
		return inSampleSize;
	}

	private void finishWithError(String message) {
		Intent data = new Intent().putExtra(ERROR_MESSAGE, message);
		setResult(RESULT_ERROR, data);
		finish();
	}

	private int pixelsToDp(int pixels) {
		float density = getResources().getDisplayMetrics().density;
		return Math.round(pixels * density);
	}

	private void setBitmap(ImageView imageView, String imageName) {
		try {
			InputStream imageStream = getAssets().open(imageName);
			Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
			imageView.setImageBitmap(bitmap);
			imageStream.close();
		} catch (Exception e) {
			Log.e(TAG, "Could load image", e);
		}
	}

}