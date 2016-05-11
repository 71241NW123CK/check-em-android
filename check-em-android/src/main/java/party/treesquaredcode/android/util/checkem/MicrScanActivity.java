package party.treesquaredcode.android.util.checkem;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

/**
 * Created by rht on 5/10/16.
 */
public class MicrScanActivity extends Activity implements TextureView.SurfaceTextureListener {
    private CheckEm.LayoutStrategy layoutStrategy;

    private FrameLayout previewFrameLayout;
    private TextView routingNumberTextView;
    private TextView accountNumberTextView;
    private View okButton;

    private Camera camera;
    private TextureView textureView;
    double scanWidthRatio;
    double scanHeightRatio;
    MicrOcrUtil micrOcrUtil;
    MicrOcrUtil.MicrOcrResult result;

    ScanAsyncTask scanAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layoutStrategy = CheckEm.getSharedInstance().layoutStrategy;
        if (layoutStrategy == null) {
            layoutStrategy = produceDefaultLayoutStrategy();
        }
        setContentView(layoutStrategy.getLayoutResId());
        previewFrameLayout = (FrameLayout) findViewById(layoutStrategy.getPreviewFrameLayoutId());
        routingNumberTextView = (TextView) findViewById(layoutStrategy.getRoutingNumberTextViewId());
        accountNumberTextView = (TextView) findViewById(layoutStrategy.getAccountNumberTextViewId());
        okButton = findViewById(layoutStrategy.getOkButtonId());

        textureView = new TextureView(this);
        textureView.setSurfaceTextureListener(this);
        previewFrameLayout.addView(textureView);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (result != null) {
                    if (result.routingNumber != null) {
                        CheckEm.getSharedInstance().routingNumberResult = result.routingNumber;
                    }
                    if (result.accountNumber != null) {
                        CheckEm.getSharedInstance().accountNumberResult = result.accountNumber;
                    }
                }
                finish();
            }
        });

        scanWidthRatio = layoutStrategy.getScanWidthRatio();
        if (scanWidthRatio > 1.0 || scanWidthRatio <= 0.0) {
            scanWidthRatio = 1.0;
        }
        scanHeightRatio = layoutStrategy.getScanHeightRatio();
        if (scanHeightRatio > 1.0 || scanHeightRatio <= 0.0) {
            scanHeightRatio = 1.0;
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        int verticalBorderHeight = (int) (screenHeight * 0.5 * (1.0 - scanHeightRatio));
        int scanHeight = screenHeight - 2 * verticalBorderHeight;
        int horizontalBorderWidth = (int) (screenWidth * 0.5 * (1.0 - scanWidthRatio));

        int maskColor = layoutStrategy.getMaskColor();

        View topBorder = new View(this);
        topBorder.setBackgroundColor(maskColor);
        FrameLayout.LayoutParams topBorderLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, verticalBorderHeight, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        previewFrameLayout.addView(topBorder, topBorderLayoutParams);

        View leftBorder = new View(this);
        leftBorder.setBackgroundColor(maskColor);
        FrameLayout.LayoutParams leftBorderLayoutParams = new FrameLayout.LayoutParams(horizontalBorderWidth, scanHeight, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        previewFrameLayout.addView(leftBorder, leftBorderLayoutParams);

        View rightBorder = new View(this);
        rightBorder.setBackgroundColor(maskColor);
        FrameLayout.LayoutParams rightBorderLayoutParams = new FrameLayout.LayoutParams(horizontalBorderWidth, scanHeight, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        previewFrameLayout.addView(rightBorder, rightBorderLayoutParams);

        View bottomBorder = new View(this);
        bottomBorder.setBackgroundColor(maskColor);
        FrameLayout.LayoutParams bottomBorderLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, verticalBorderHeight, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        previewFrameLayout.addView(bottomBorder, bottomBorderLayoutParams);

//        View view = new View(this);
//        view.setBackgroundColor(0x80FF0000);
//        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int) (screenWidth * scanWidthRatio), (int) (screenHeight * scanHeightRatio), Gravity.CENTER);
//        previewFrameLayout.addView(view, layoutParams);



        clear();
        micrOcrUtil = new MicrOcrUtil();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (scanAsyncTask != null) {
            scanAsyncTask.shouldKill = true;
        }
        super.onStop();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size previewSize = getOptimalPreviewSize(parameters.getSupportedPreviewSizes(), width, height);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(parameters);
        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (IOException ioe) {
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //no-op
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        camera.stopPreview();
        camera.release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (micrOcrUtil != null && !micrOcrUtil.isRunning()) {
            Bitmap bitmap = textureView.getBitmap();
            if (bitmap == null) {
                return;
            }
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            int halfBitmapWidth = bitmapWidth / 2;
            int halfScanWidth = (int) (halfBitmapWidth * scanWidthRatio);
            int halfBitmapHeight = bitmapHeight / 2;
            int halfScanHeight = (int) (halfBitmapHeight * scanHeightRatio);
            int startX = halfBitmapWidth - halfScanWidth;
            int endX = halfBitmapWidth + halfScanWidth;
            int startY = halfBitmapHeight - halfScanHeight;
            int endY = halfBitmapHeight + halfScanHeight;
            if (startX < 0) {
                startX = 0;
            }
            if (endX > bitmapWidth) {
                endX = bitmapWidth;
            }
            if (startY < 0) {
                startY = 0;
            }
            if (endY > bitmapHeight) {
                endY = bitmapHeight;
            }
            int scanWidth = endX - startX;
            int scanHeight = endY - startY;
            bitmap = Bitmap.createBitmap(bitmap, startX, startY, scanWidth, scanHeight);
            scanAsyncTask = new ScanAsyncTask();
            scanAsyncTask.execute(bitmap);
        }
    }

    private CheckEm.LayoutStrategy produceDefaultLayoutStrategy() {
        return new CheckEm.LayoutStrategy() {
            @Override
            public int getLayoutResId() {
                return R.layout.activity__micr_scan_activity;
            }

            @Override
            public int getPreviewFrameLayoutId() {
                return R.id.micr_scan_activity__preview_frame;
            }

            @Override
            public int getRoutingNumberTextViewId() {
                return R.id.micr_scan_activity__routing_text;
            }

            @Override
            public int getAccountNumberTextViewId() {
                return R.id.micr_scan_activity__account_text;
            }

            @Override
            public int getOkButtonId() {
                return R.id.micr_scan_activity__ok_button;
            }

            @Override
            public double getScanWidthRatio() {
                return 0.875;
            }

            @Override
            public double getScanHeightRatio() {
                return 0.25;
            }

            @Override
            public int getMaskColor() {
                return 0x40000000;
            }
        };
    }

    private void clear() {
        routingNumberTextView.setText(R.string.micr_scan__empty_routing);
        accountNumberTextView.setText(R.string.micr_scan__empty_account);
        okButton.setVisibility(View.GONE);
    }

    private void setRoutingNumberResult(String routingNumberResult) {
        routingNumberTextView.setText(getString(R.string.micr_scan__routing_format, routingNumberResult));
    }

    private void setAccountNumberResult(String accountNumberResult) {
        accountNumberTextView.setText(getString(R.string.micr_scan__account_format, accountNumberResult));
    }

    //from the interwebs
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;

        if (sizes==null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    class ScanAsyncTask extends AsyncTask<Bitmap, Long, MicrOcrUtil.MicrOcrResult> {
        boolean shouldKill;

        @Override
        protected MicrOcrUtil.MicrOcrResult doInBackground(Bitmap... params) {
            return micrOcrUtil.processBitmap(params[0]);
        }

        @Override
        protected void onPostExecute(MicrOcrUtil.MicrOcrResult micrOcrResult) {
            super.onPostExecute(micrOcrResult);
            if (shouldKill) {
                micrOcrUtil.destroy();
                return;
            }
            result = micrOcrResult;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (result.routingNumber != null) {
                        setRoutingNumberResult(result.routingNumber);
                    }
                    if (result.accountNumber != null) {
                        setAccountNumberResult(result.accountNumber);
                    }
                    if (result.routingNumber != null && result.accountNumber != null) {
                        okButton.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }
}
