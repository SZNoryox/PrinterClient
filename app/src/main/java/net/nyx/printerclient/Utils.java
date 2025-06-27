package net.nyx.printerclient;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Utils {

    private static final String TAG = "Utils";

    private static Application sApplication;
    private static Utils sUtils;

    public static Utils getInstance() {
        if (sUtils == null)
            sUtils = new Utils();
        return sUtils;
    }

    public static Utils init(final Application app) {
        getInstance();
        if (sApplication == null) {
            if (app == null) {
                sApplication = getApplicationByReflect();
            } else {
                sApplication = app;
            }
        } else {
            if (app != null && app.getClass() != sApplication.getClass()) {
                sApplication = app;
            }
        }
        return sUtils;
    }

    public static Application getApp() {
        if (sApplication != null)
            return sApplication;
        Application app = getApplicationByReflect();
        init(app);
        return app;
    }

    private static Application getApplicationByReflect() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            Object app = activityThread.getMethod("getApplication").invoke(thread);
            if (app == null) {
                throw new NullPointerException("u should init first");
            }
            return (Application) app;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new NullPointerException("u should init first");
    }

    public static String getRandomStr(int len) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static Bitmap createQRCode(String content, int width, int height) {
        if (content == null || "".equals(content)) {
            return null;
        }
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);

        Bitmap bitmap;
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            width = Math.max(width, bitMatrix.getWidth());
            height = Math.max(height, bitMatrix.getHeight());
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    pixels[y * width + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (WriterException e) {
            throw new IllegalArgumentException("CreateQRCode failed", e);
        }
        return bitmap;
    }

    public static byte[] printTwoColumn(String title, String content) {
        int iNum = 0;
        byte[] buf = new byte[100];
        byte[] tmp = title.getBytes();

        System.arraycopy(tmp, 0, buf, iNum, tmp.length);
        iNum += tmp.length;

        tmp = setPosition(384 - content.length() * 12);
        System.arraycopy(tmp, 0, buf, iNum, tmp.length);
        iNum += tmp.length;

        tmp = content.getBytes();
        System.arraycopy(tmp, 0, buf, iNum, tmp.length);

        iNum += tmp.length;
        buf[iNum] = '\n';
        return buf;
    }

    private static byte[] setPosition(int offset) {
        byte[] bs = new byte[4];
        bs[0] = 0x1B;
        bs[1] = 0x24;
        bs[2] = (byte) (offset % 256);
        bs[3] = (byte) (offset / 256);
        return bs;
    }
}
