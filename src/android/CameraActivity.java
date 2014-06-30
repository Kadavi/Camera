package org.schoolsfirstfcu.mobile.plugin.checkcapture;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.cordova.LOG;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import static android.hardware.Camera.Parameters.FLASH_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_AUTO;
import static android.hardware.Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;

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

	private static final int HEADER_HEIGHT = 54;
	private static final int FRAME_BORDER_SIZE = 34;

	private Camera camera;
	private RelativeLayout layout;
	private FrameLayout cameraPreviewView;
	private ImageView spinner;
    private TextView headerText;
    private TextView titleText;
    private TextView cancelText;
    private TextView captureText;
	private ImageButton captureButton;
    private ProgressDialog progress;
	private Bitmap lightButton, darkButton;

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
		cameraSettings.setFlashMode(FLASH_MODE_AUTO);
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
        createFrame();
        createCaptureButton();

        setContentView(layout);
	}

    private void createCameraPreview() {
        cameraPreviewView = new FrameLayout(this);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT) - (pixelsToDp(FRAME_BORDER_SIZE)*2),
                screenHeightInPixels() - (pixelsToDp(FRAME_BORDER_SIZE)*3));
        cameraPreviewView.setLayoutParams(layoutParams);
        cameraPreviewView.setX(pixelsToDp(FRAME_BORDER_SIZE));
        cameraPreviewView.setY(pixelsToDp(FRAME_BORDER_SIZE));
        layout.addView(cameraPreviewView);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void createFrame() {
        // Header
        RelativeLayout.LayoutParams headerLayoutParams = new RelativeLayout.LayoutParams(
                pixelsToDp(HEADER_HEIGHT), LayoutParams.MATCH_PARENT);
        headerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        headerLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        View headerView = new View(this);
        headerView.setBackgroundColor(0xFF2D4452);
        headerView.setLayoutParams(headerLayoutParams);
        layout.addView(headerView);

        // Header Message
        RelativeLayout.LayoutParams logoLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        logoLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        logoLayoutParams.rightMargin = pixelsToDp(6);
        headerText = new VerticalTextView(this);
        headerText.setGravity(Gravity.CENTER);
        headerText.setText(getIntent().getStringExtra(DESCRIPTION));
        headerText.setLayoutParams(logoLayoutParams);
        layout.addView(headerText);

        // Left Pane
        RelativeLayout.LayoutParams leftPaneLayoutParams = new RelativeLayout.LayoutParams(
                screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT), pixelsToDp(FRAME_BORDER_SIZE));
        View leftPaneView = new View(this);
        leftPaneView.setBackgroundColor(0xFFB9C7D4);
        leftPaneView.setLayoutParams(leftPaneLayoutParams);
        layout.addView(leftPaneView);

        // Right Pane
        RelativeLayout.LayoutParams rightPaneLayoutParams = new RelativeLayout.LayoutParams(
                screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT), pixelsToDp(FRAME_BORDER_SIZE)*2);
        rightPaneLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        View rightPaneView = new View(this);
        rightPaneView.setBackgroundColor(0xFFB9C7D4);
        rightPaneView.setLayoutParams(rightPaneLayoutParams);
        layout.addView(rightPaneView);

        // Bottom Pane
        RelativeLayout.LayoutParams bottomPaneLayoutParams = new RelativeLayout.LayoutParams(
                pixelsToDp(FRAME_BORDER_SIZE), screenHeightInPixels());
        View bottomPaneView = new View(this);
        bottomPaneView.setBackgroundColor(0xFFB9C7D4);
        bottomPaneView.setLayoutParams(bottomPaneLayoutParams);
        layout.addView(bottomPaneView);

        // Top Pane
        RelativeLayout.LayoutParams topPaneLayoutParams = new RelativeLayout.LayoutParams(
                pixelsToDp(FRAME_BORDER_SIZE), screenHeightInPixels());
        topPaneLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        topPaneLayoutParams.rightMargin = pixelsToDp(HEADER_HEIGHT);
        View topPaneView = new View(this);
        topPaneView.setBackgroundColor(0xFFB9C7D4);
        topPaneView.setLayoutParams(topPaneLayoutParams);
        layout.addView(topPaneView);

        // Front/Back Title
        RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        titleLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        titleLayoutParams.rightMargin = pixelsToDp(HEADER_HEIGHT + 6);
        titleLayoutParams.topMargin = pixelsToDp(FRAME_BORDER_SIZE);
        titleText = new VerticalTextView(this);
        titleText.setTextColor(Color.parseColor("#000000"));
        titleText.setText(getIntent().getStringExtra(TITLE));
        titleText.setLayoutParams(titleLayoutParams);
        layout.addView(titleText);

        // Cancel Button
        RelativeLayout.LayoutParams cancelLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        cancelLayoutParams.leftMargin = pixelsToDp(FRAME_BORDER_SIZE);
        cancelText = new TextView(this);
        cancelText.setY(screenHeightInPixels() - (pixelsToDp(FRAME_BORDER_SIZE)*2) + pixelsToDp(6));
        cancelText.setTextColor(Color.parseColor("#FFFFFF"));
        cancelText.setTextSize(18);
        cancelText.setText("Cancel");
        cancelText.setLayoutParams(cancelLayoutParams);
        cancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        layout.addView(cancelText);

        CropMarks cropMarks = new CropMarks(this);
        layout.addView(cropMarks);

        // Capture Button
        /*RelativeLayout.LayoutParams captureLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        captureLayoutParams.leftMargin = pixelsToDp(FRAME_BORDER_SIZE) + pixelsToDp(75);
        captureText = new TextView(this);
        captureText.setY(screenHeightInPixels() - (pixelsToDp(FRAME_BORDER_SIZE)*2) + pixelsToDp(6));
        captureText.setTextColor(Color.parseColor("#FFFFFF"));
        captureText.setTextSize(18);
        captureText.setText("Take Picture");
        captureText.setLayoutParams(captureLayoutParams);
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Please wait...");
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        captureText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!progress.isShowing()) {
                    captureText.setOnClickListener(null);
                    progress.show();
                    takePictureWithAutoFocus();
                }
            }
        });
        layout.addView(captureText);
        */

        // How to add an image
        /*
        RelativeLayout.LayoutParams spinnerLayoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        spinner = new ImageView(this);
        setBitmap(spinner, "www/img/logo.png");
        spinner.setLayoutParams(spinnerLayoutParams);
        layout.addView(spinner);
        */
    }

    public class CropMarks extends View {
        public CropMarks(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Paint paint = new Paint();
            paint.setARGB(255, 255, 255, 255);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setPathEffect(new DashPathEffect(new float[]{20, 20}, 0));

            int borderLength = pixelsToDp(30);
            Point topLeftPts = new Point(pixelsToDp(FRAME_BORDER_SIZE), pixelsToDp(FRAME_BORDER_SIZE));
            Point topRightPts = new Point(screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT) - pixelsToDp(FRAME_BORDER_SIZE),
                    pixelsToDp(FRAME_BORDER_SIZE));
            Point bottomLeftPts = new Point(pixelsToDp(FRAME_BORDER_SIZE), screenHeightInPixels() - pixelsToDp(FRAME_BORDER_SIZE)*2);
            Point bottomRightPts = new Point(screenWidthInPixels() - pixelsToDp(HEADER_HEIGHT) - pixelsToDp(FRAME_BORDER_SIZE),
                    screenHeightInPixels() - pixelsToDp(FRAME_BORDER_SIZE)*2);

            Path path = new Path();
            path.moveTo(topLeftPts.x, topLeftPts.y + borderLength);
            path.lineTo(topLeftPts.x, topLeftPts.y);
            path.lineTo(topLeftPts.x + borderLength, topLeftPts.y);

            path.moveTo(topRightPts.x - borderLength, topRightPts.y);
            path.lineTo(topRightPts.x, topRightPts.y);
            path.lineTo(topRightPts.x, topRightPts.y + borderLength);

            path.moveTo(bottomRightPts.x, bottomRightPts.y - borderLength);
            path.lineTo(bottomRightPts.x, bottomRightPts.y);
            path.lineTo(bottomRightPts.x - borderLength, bottomRightPts.y);

            path.moveTo(bottomLeftPts.x + borderLength, bottomLeftPts.y);
            path.lineTo(bottomLeftPts.x, bottomLeftPts.y);
            path.lineTo(bottomLeftPts.x, bottomLeftPts.y - borderLength);

            canvas.drawPath(path, paint);
        }
    }

    public class VerticalTextView extends TextView
    {
        final boolean topDown;

        public VerticalTextView(Context context) {
            super(context);
            final int gravity = getGravity();
            if ( Gravity.isVertical(gravity)
                    && ( gravity & Gravity.VERTICAL_GRAVITY_MASK )
                    == Gravity.BOTTOM )
            {
                setGravity(
                        ( gravity & Gravity.HORIZONTAL_GRAVITY_MASK )
                                | Gravity.TOP );
                topDown = false;
            }
            else
            {
                topDown = true;
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec,
                                 int heightMeasureSpec)
        {
            super.onMeasure(heightMeasureSpec, widthMeasureSpec);
            setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
        }

        @Override
        protected void onDraw(Canvas canvas) {
            TextPaint textPaint = getPaint();
            textPaint.setColor( getCurrentTextColor() );
            textPaint.drawableState = getDrawableState();

            canvas.save();

            if ( topDown )
            {
                canvas.translate( getWidth(), 0 );
                canvas.rotate( 90 );
            }
            else
            {
                canvas.translate( 0, getHeight() );
                canvas.rotate( -90 );
            }

            canvas.translate( getCompoundPaddingLeft(),
                    getExtendedPaddingTop() );

            getLayout().draw( canvas );
            canvas.restore();
        }
    }

	private void createCaptureButton() {
        try {
            InputStream inputStream = getAssets().open("www/img/buttonup.png");
            lightButton = BitmapFactory.decodeStream(inputStream);
            inputStream = getAssets().open("www/img/buttondown.png");
            darkButton = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
        } catch (Exception e) {
            LOG.e(ERROR_MESSAGE, "Button image(s) not found.");
        }

		captureButton = new ImageButton(this);
		captureButton.setImageBitmap(lightButton);
		captureButton.setBackgroundColor(Color.TRANSPARENT);
        captureButton.setX((screenWidthInPixels() - lightButton.getWidth() - pixelsToDp(HEADER_HEIGHT)) / 2);
        captureButton.setY(screenHeightInPixels() - lightButton.getHeight() - pixelsToDp(6));

        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Please wait...");
        progress.setIndeterminate(true);
        progress.setCancelable(false);

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
                if (!progress.isShowing()) {
                    captureButton.setOnClickListener(null);
                    progress.show();
                    takePictureWithAutoFocus();
                }
			}
		});

		layout.addView(captureButton);
	}

    private void setCaptureButtonImageForEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            captureButton.setImageBitmap(darkButton);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            captureButton.setImageBitmap(lightButton);
        }
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
				scaleBitmap.compress(Bitmap.CompressFormat.JPEG, 20, stream);
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
			Log.e(TAG, "Couldn't load image", e);
		}
	}

}
