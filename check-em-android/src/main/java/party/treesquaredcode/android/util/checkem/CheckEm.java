package party.treesquaredcode.android.util.checkem;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class CheckEm {
    private static final String TAG = CheckEm.class.getCanonicalName();

	public static String getCheckEmString() {
		return "Check Em";
	}

	private static CheckEm sharedInstance;

	String routingNumberResult;
	String accountNumberResult;
    LayoutStrategy layoutStrategy;

	public static CheckEm getSharedInstance() {
		return sharedInstance = (sharedInstance == null ? new CheckEm() : sharedInstance);
	}

	private CheckEm() {}

	public void checkEm(Activity activity) {
        if (activity == null) {
            log("Null activity.");
            return;
        }
		if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            log("No camera.");
            return;
        }
        if (!TesseractDataManager.ensureTesseractData(activity)) {
            log("Failed to write Tesseract data.");
            return;
        }
        Intent intent = new Intent(activity, MicrScanActivity.class);
        activity.startActivity(intent);
    }

	public String getRoutingNumberResult() {
		return routingNumberResult;
	}

	public String getAccountNumberResult() {
		return accountNumberResult;
	}

    public void setLayoutStrategy(LayoutStrategy layoutStrategy) {
        this.layoutStrategy = layoutStrategy;
    }

	public void clearResults() {
		routingNumberResult = null;
		accountNumberResult = null;
	}

    private boolean checkCameraHardware(Activity activity) {
        if (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void log(String string) {
        Log.d(TAG, string);
    }

    interface LayoutStrategy {
		int getLayoutResId();
        int getPreviewFrameLayoutId();
		int getRoutingNumberTextViewId();
		int getAccountNumberTextViewId();
		int getOkButtonId();
        double getScanWidthRatio();
        double getScanHeightRatio();
        int getMaskColor();
	}
}
