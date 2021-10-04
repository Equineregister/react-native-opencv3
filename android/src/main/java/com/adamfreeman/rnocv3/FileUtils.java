// @author Adam G. Freeman - adamgf@gmail.com, 04/07/2019
package com.adamfreeman.rnocv3;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableNativeMap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.MatOfPoint;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    private static FileUtils fileUtils = null;
	
    private FileUtils() {
    }
	
    // static method to create instance of Singleton class
    public static FileUtils getInstance() {
        if (fileUtils == null)
            fileUtils = new FileUtils();

        return fileUtils;
    }
	
    private static void reject(Promise promise, String filepath, Exception ex) {
        if (ex instanceof FileNotFoundException) {
            rejectFileNotFound(promise, filepath);
            return;
        }

        promise.reject(null, ex.getMessage());
    }

    private static void rejectFileNotFound(Promise promise, String filepath) {
        promise.reject("ENOENT", "ENOENT: no such file or directory, open '" + filepath + "'");
    }

    private static void rejectFileIsDirectory(Promise promise, String filepath) {
        promise.reject("EISDIR", "EISDIR: illegal operation on a directory, open '" + filepath + "'");
    }

    private static void rejectInvalidParam(Promise promise, String param) {
        promise.reject("EINVAL", "EINVAL: invalid parameter, read '" + param + "'");
    }
	
    public static void imageToMat(final String inPath, final Promise promise) {
        try {
            if (inPath == null || inPath.length() == 0) {
                rejectInvalidParam(promise, inPath);
                return;
            }

            File inFileTest = new File(inPath);
            if(!inFileTest.exists()) {
                rejectFileNotFound(promise, inPath);
                return;
            }
            if (inFileTest.isDirectory()) {
                rejectFileIsDirectory(promise, inPath);
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(inPath);
            if (bitmap == null) {
                throw new IOException("Decoding error unable to decode: " + inPath);
            }			
            Mat img = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(bitmap, img);
            int matIndex = MatManager.getInstance().addMat(img);

            WritableNativeMap result = new WritableNativeMap();
            result.putInt("cols", img.cols());
            result.putInt("rows", img.rows());
            result.putInt("matIndex", matIndex);
            promise.resolve(result);
        }
        catch (Exception ex) {
            reject(promise, "EGENERIC", ex);
        }
    }

    public static void matToImage(final Mat mat, final String outPath, final Promise promise) {
        try {
            if (outPath == null || outPath.length() == 0) {
                // TODO: if no path sent in then auto-generate??!!!?
                rejectInvalidParam(promise, outPath);
                return;
            }

            Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bm);

            int width = bm.getWidth();
            int height = bm.getHeight();

            FileOutputStream file = new FileOutputStream(outPath);

            if (file != null) {
                String fileType = "";
                int i = outPath.lastIndexOf('.');
                if (i > 0) {
                    fileType = outPath.substring(i+1).toLowerCase();
                }
                else {
                    rejectInvalidParam(promise, outPath);
                    file.close();
                    return;
                }

                if (fileType.equals("png")) {
                    bm.compress(Bitmap.CompressFormat.PNG, 100, file);
                }
                else if (fileType.equals("jpg") || fileType.equals("jpeg")) {
                    bm.compress(Bitmap.CompressFormat.JPEG, 80, file);
                }
                else {
                    rejectInvalidParam(promise, outPath);
                    file.close();
                    return;
                }
                file.close();
            }
            else {
                rejectFileNotFound(promise, outPath);
                return;
            }

            WritableNativeMap result = new WritableNativeMap();
            result.putInt("width", width);
            result.putInt("height", height);
            result.putString("uri", outPath);
            promise.resolve(result);
        }
        catch (Exception ex) {
            reject(promise, "EGENERIC", ex);
        }
    }

    public static void demoOpencvMethod(final Mat mat, final String outPath, final String cannyPath,final int gaussian,final int min,final int max, final Promise promise) {
        try {
            if (outPath == null || outPath.length() == 0) {
                // TODO: if no path sent in then auto-generate??!!!?
                rejectInvalidParam(promise, outPath);
                return;
            }
            Mat backup = mat.clone();

            Imgproc.cvtColor(backup,backup,Imgproc.COLOR_RGB2GRAY);
            Imgproc.GaussianBlur(backup, backup, new Size(gaussian, gaussian), 0);

            Mat detectedEdges = backup;
            Imgproc.Canny(backup, detectedEdges, min, max, 3, false);

            // save canny image *
            Mat cannyMat = backup.clone();
            Imgproc.threshold(cannyMat, cannyMat, 1, 255, Imgproc.THRESH_BINARY_INV);
            Bitmap cannyBm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cannyMat, cannyBm);
            // end *

            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(detectedEdges, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

            for (int i = 0; i < contours.size(); i++) {
                Scalar color = new Scalar(0, 255, 0);
                Imgproc.drawContours(mat, contours, i, color, -1, Imgproc.LINE_8, hierarchy, 0, new Point());
            }

            Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);

            Utils.matToBitmap(mat, bm);

            int width = bm.getWidth();
            int height = bm.getHeight();

            FileOutputStream file = new FileOutputStream(outPath);
            // write canny image to fs
            FileOutputStream cannyFile = new FileOutputStream(cannyPath);

            if (file != null) {
                String fileType = "";
                int i = outPath.lastIndexOf('.');
                if (i > 0) {
                    fileType = outPath.substring(i+1).toLowerCase();
                }
                else {
                    rejectInvalidParam(promise, outPath);
                    file.close();
                    cannyFile.close();
                    return;
                }

                if (fileType.equals("png")) {
                    bm.compress(Bitmap.CompressFormat.PNG, 100, file);
                    cannyBm.compress(Bitmap.CompressFormat.PNG, 100, cannyFile);
                }
                else if (fileType.equals("jpg") || fileType.equals("jpeg")) {
                    bm.compress(Bitmap.CompressFormat.JPEG, 80, file);
                    cannyBm.compress(Bitmap.CompressFormat.JPEG, 80, cannyFile);
                }
                else {
                    rejectInvalidParam(promise, outPath);
                    file.close();
                    cannyFile.close();
                    return;
                }
                file.close();
                cannyFile.close();
            }
            else {
                rejectFileNotFound(promise, outPath);
                return;
            }
            WritableNativeMap result = new WritableNativeMap();
            result.putInt("width", width);
            result.putInt("height", height);
            result.putString("uri", outPath);
            result.putString("cannyUri", cannyPath);
            // cannyMat putArray
            promise.resolve(result);
        }
        catch (Exception ex) {
            reject(promise, "EGENERIC", ex);
        }
    }

    public static void ROCrop(final String imagePath,
        final String outPath,
        final int x,
        final int y,
        final int width,
        final int height,
        final Promise promise) {
        try {
            if (outPath == null || outPath.length() == 0) {
                // TODO: if no path sent in then auto-generate??!!!?
                rejectInvalidParam(promise, outPath);
                return;
            }

            Bitmap bitmapImage = BitmapFactory.decodeFile(imagePath);
            Bitmap crop = Bitmap.createBitmap(bitmapImage, x, y, width, height);
            FileOutputStream file = new FileOutputStream(outPath);
            crop.compress(Bitmap.CompressFormat.JPEG, 100, file);

            WritableNativeMap result = new WritableNativeMap();
            result.putString("uri", outPath);
            promise.resolve(result);
        }
        catch (Exception ex) {
            reject(promise, "EGENERIC", ex);
        }
    }

    public static void ROCombain(final String firstImage, final String secondImage,final String outPath, final Promise promise) {
        try {

            Bitmap firstImageBitmap = BitmapFactory.decodeFile(firstImage);
            Bitmap secondImageBitmap = BitmapFactory.decodeFile(secondImage);
            Bitmap scaledSecondImage = Bitmap.createScaledBitmap(secondImageBitmap, 170, 510, true);

            Bitmap bmOverlay = Bitmap.createBitmap(firstImageBitmap.getWidth(), firstImageBitmap.getHeight(), firstImageBitmap.getConfig());
            Canvas canvas = new Canvas(bmOverlay);
            canvas.drawColor(0xffffffff);
            float xOffset = (firstImageBitmap.getWidth() / 2) - (scaledSecondImage.getWidth() / 2);
            float yOffset = (float) ((firstImageBitmap.getHeight() / 2) - (scaledSecondImage.getHeight() / 2.5));
            canvas.drawBitmap(scaledSecondImage, xOffset, yOffset, null);
            canvas.drawBitmap(firstImageBitmap, 0, 0, null);
            firstImageBitmap.recycle();
            secondImageBitmap.recycle();
            scaledSecondImage.recycle();

            FileOutputStream file = new FileOutputStream(outPath);
            bmOverlay.compress(Bitmap.CompressFormat.JPEG, 100, file);

            WritableNativeMap result = new WritableNativeMap();
            result.putString("uri", outPath);
            promise.resolve(result);

        }
        catch (Exception ex) {
            reject(promise, "EGENERIC", ex);
        }
    }
}
