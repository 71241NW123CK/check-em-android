package party.treesquaredcode.android.util.checkem;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by rht on 5/10/16.
 */
public class TesseractDataManager {
    private static final String ASSET_SUBDIR = "tessdata";
    private static final String EXTERNAL_DATA_DIR = "CheckEm";
    private static final String LANGUAGE = "mcr";
    private static final String OCR_TRAINED_DATA = LANGUAGE + ".traineddata";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + EXTERNAL_DATA_DIR + File.separator;

    static String getLanguage() {
        return LANGUAGE;
    }

    static String getDataPath() {
        return DATA_PATH;
    }

    public static boolean ensureTesseractData(Activity activity) {
        String dataSubdir = DATA_PATH + ASSET_SUBDIR + File.separator;
        return activity != null && ensureDirectoryExists(DATA_PATH) && ensureDirectoryExists(dataSubdir) && ensureTrainedDataInAssetDirectory(activity.getAssets());
    }

    private static boolean ensureDirectoryExists(String path) {
        File directory = new File(path);
        return directory.exists() || directory.mkdirs();
    }

    private static boolean ensureTrainedDataInAssetDirectory(AssetManager assetManager) {
        String internalPath = OCR_TRAINED_DATA;
        String externalPath = DATA_PATH + ASSET_SUBDIR + File.separator + internalPath;
        if (!new File(externalPath).exists()) {
            try {
                InputStream inputStream = assetManager.open(internalPath);
                OutputStream outputStream = new FileOutputStream(externalPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
