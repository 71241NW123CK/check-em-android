package party.treesquaredcode.android.util.checkem;

import android.graphics.Bitmap;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rht on 5/10/16.
 */
class MicrOcrUtil {

    private static final int ROUTING_NUMBER_HISTORY_LENGTH = 8;
    private static final int ROUTING_NUMBER_THRESHOLD = 3;

    private static final int ACCOUNT_NUMBER_HISTORY_LENGTH = 8;
    private static final int ACCOUNT_NUMBER_THRESHOLD = 3;

    private static final Pattern ROUTING_NUMBER_PATTERN = Pattern.compile("a[0-9]{9}a");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("a[0-9d]{6,20}c");

    TessBaseAPI tessBaseAPI = new TessBaseAPI();
    String currentRoutingNumber;
    List<String> routingNumberHistory = new ArrayList<>();
    String currentAccountNumber;
    List<String> accountNumberHistory = new ArrayList<>();

    private boolean isRunning;

    public MicrOcrUtil() {
        tessBaseAPI.setDebug(true);
        tessBaseAPI.init(TesseractDataManager.getDataPath(), TesseractDataManager.getLanguage());
    }

    public MicrOcrResult processBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return produceExternalResult(new MicrOcrResult(null, null));
        }
        isRunning = true;
        tessBaseAPI.setImage(bitmap);
        MicrOcrResult result = produceExternalResult(produceInternalResult(tessBaseAPI.getUTF8Text()));
        isRunning = false;
        return result;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void destroy() {
        tessBaseAPI.end();
        currentRoutingNumber = null;
        routingNumberHistory.clear();
        currentAccountNumber = null;
        accountNumberHistory.clear();
    }

    private MicrOcrResult produceInternalResult(String rawScan) {
        String routingNumber = null;
        String accountNumber = null;
        String strippedScan = rawScan.replaceAll("\\s", "");
        Matcher routingNumberMatcher = ROUTING_NUMBER_PATTERN.matcher(strippedScan);
        if (routingNumberMatcher.find()) {
            routingNumber = routingNumberMatcher.group();
            routingNumber = routingNumber.substring(1, 10);
        }
        Matcher accountNumberMatcher = ACCOUNT_NUMBER_PATTERN.matcher(strippedScan);
        if (accountNumberMatcher.find()) {
            accountNumber = accountNumberMatcher.group();
            accountNumber = accountNumber.substring(1, accountNumber.length() - 1);
            accountNumber = accountNumber.replace("d", "-");
            if (accountNumber.length() - accountNumber.replace("-", "").length() > 1) {//only one dash!
                accountNumber = null;
            }
        }
        return new MicrOcrResult(routingNumber, accountNumber);
    }

    private MicrOcrResult produceExternalResult(MicrOcrResult internalResult) {
        String newRoutingNumber = null;
        String newAccountNumber = null;
        if (internalResult.routingNumber != null) {
            if (routingNumberHistory.isEmpty()) {
                newRoutingNumber = internalResult.routingNumber;
            } else {
                int matchCount = 0;
                for (String pastRoutingNumber : routingNumberHistory) {
                    if (internalResult.routingNumber.equals(pastRoutingNumber)) {
                        matchCount++;
                    }
                }
                if (matchCount >= ROUTING_NUMBER_THRESHOLD) {
                    newRoutingNumber = internalResult.routingNumber;
                }
                if (routingNumberHistory.size() >= ROUTING_NUMBER_HISTORY_LENGTH) {
                    routingNumberHistory.remove(0);
                }
            }
            routingNumberHistory.add(internalResult.routingNumber);
            if (currentRoutingNumber == null || newRoutingNumber != null) {
                currentRoutingNumber = internalResult.routingNumber;
            }
        }
        if (internalResult.accountNumber != null) {
            if (accountNumberHistory.isEmpty()) {
                newAccountNumber = internalResult.accountNumber;
            } else {
                int matchCount = 0;
                for (String pastAccountNumber : accountNumberHistory) {
                    if (internalResult.accountNumber.equals(pastAccountNumber)) {
                        matchCount++;
                    }
                }
                if (matchCount >= ACCOUNT_NUMBER_THRESHOLD) {
                    newAccountNumber = internalResult.accountNumber;
                }
                if (accountNumberHistory.size() >= ACCOUNT_NUMBER_HISTORY_LENGTH) {
                    accountNumberHistory.remove(0);
                }
            }
            accountNumberHistory.add(internalResult.accountNumber);
            if (currentAccountNumber == null || newAccountNumber != null) {
                currentAccountNumber = internalResult.accountNumber;
            }
        }
        return new MicrOcrResult(currentRoutingNumber, currentAccountNumber);
    }

    static class MicrOcrResult {
        public String routingNumber;
        public String accountNumber;

        public MicrOcrResult(String routingNumber, String accountNumber) {
            this.routingNumber = routingNumber;
            this.accountNumber = accountNumber;
        }
    }
}
