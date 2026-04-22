package com.example.freshmilk.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cloudinary image upload helper using unsigned upload preset via REST API.
 *
 * Setup steps:
 * 1. Create a Cloudinary account at https://cloudinary.com
 * 2. Go to Settings → Upload → Upload Presets
 * 3. Create an unsigned upload preset (e.g., "freshmilk_payments")
 * 4. Copy your Cloud Name from the Dashboard
 * 5. Update CLOUD_NAME and UPLOAD_PRESET below
 */
public class CloudinaryHelper {

    private static final String TAG = "CloudinaryHelper";

    // ========== CONFIGURE THESE VALUES ==========

    private static final String CLOUD_NAME = "demnvdjgk";
    private static final String UPLOAD_PRESET = "Fresh Milk";
    // =============================================

    private static final String UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/demnvdjgk/image/upload";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Callback interface for upload results.
     */
    public interface UploadCallback {
        void onProgress(int percentage);
        void onSuccess(String secureUrl);
        void onError(String errorMessage);
    }

    /**
     * Upload an image to Cloudinary using unsigned upload preset.
     * Runs on a background thread, callbacks are dispatched on the main thread.
     *
     * @param context  Android context for content resolver access
     * @param imageUri URI of the image to upload (from gallery or camera)
     * @param folder   Cloudinary folder to organize images (e.g., "payment_screenshots")
     * @param callback Callback for progress, success, and error events
     */
    public static void uploadImage(Context context, Uri imageUri, String folder,
                                   UploadCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                // Read image bytes from URI
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    postError(callback, "Failed to read image file");
                    return;
                }

                byte[] imageBytes = readBytes(inputStream);
                inputStream.close();

                // Validate size (10MB limit)
                if (imageBytes.length > 10 * 1024 * 1024) {
                    postError(callback, "Image too large. Max 10MB allowed.");
                    return;
                }

                postProgress(callback, 10);

                // Build multipart form data
                String boundary = "----CloudinaryBoundary" + System.currentTimeMillis();
                String lineEnd = "\r\n";
                String twoHyphens = "--";

                URL url = new URL(UPLOAD_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestMethod("POST");
                connection.setUseCaches(false);
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);

                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());

                // Add upload_preset field
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + lineEnd);
                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(UPLOAD_PRESET + lineEnd);

                // Add folder field
                if (folder != null && !folder.isEmpty()) {
                    outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    outputStream.writeBytes("Content-Disposition: form-data; name=\"folder\"" + lineEnd);
                    outputStream.writeBytes(lineEnd);
                    outputStream.writeBytes(folder + lineEnd);
                }

                postProgress(callback, 20);

                // Add image file
                outputStream.writeBytes(twoHyphens + boundary + lineEnd);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"screenshot.jpg\"" + lineEnd);
                outputStream.writeBytes("Content-Type: image/jpeg" + lineEnd);
                outputStream.writeBytes(lineEnd);

                // Write image data in chunks for progress tracking
                int totalBytes = imageBytes.length;
                int chunkSize = 8192;
                int bytesWritten = 0;

                for (int i = 0; i < totalBytes; i += chunkSize) {
                    int len = Math.min(chunkSize, totalBytes - i);
                    outputStream.write(imageBytes, i, len);
                    bytesWritten += len;

                    // Progress: 20% (prep) + 60% (upload) + 20% (response) = 100%
                    int progress = 20 + (int) ((bytesWritten * 60.0) / totalBytes);
                    postProgress(callback, progress);
                }

                outputStream.writeBytes(lineEnd);
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                outputStream.flush();
                outputStream.close();

                postProgress(callback, 85);

                // Read response
                int responseCode = connection.getResponseCode();
                InputStream responseStream = (responseCode == 200)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                String responseBody = responseBuilder.toString();
                Log.d(TAG, "Cloudinary response: " + responseBody);

                if (responseCode == 200) {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    String secureUrl = jsonResponse.getString("secure_url");
                    postProgress(callback, 100);
                    postSuccess(callback, secureUrl);
                } else {
                    // Parse error
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        JSONObject error = errorJson.optJSONObject("error");
                        String message = error != null ? error.getString("message") : "Upload failed";
                        postError(callback, "Cloudinary error: " + message);
                    } catch (Exception e) {
                        postError(callback, "Upload failed with code: " + responseCode);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Upload failed", e);
                postError(callback, "Upload failed: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    /**
     * Convenience method using default folder "payment_screenshots".
     */
    public static void uploadPaymentScreenshot(Context context, Uri imageUri,
                                               String customerId, UploadCallback callback) {
        String folder = "payment_screenshots/" + customerId;
        uploadImage(context, imageUri, folder, callback);
    }

    private static byte[] readBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    private static void postProgress(UploadCallback callback, int percentage) {
        if (callback != null) {
            mainHandler.post(() -> callback.onProgress(percentage));
        }
    }

    private static void postSuccess(UploadCallback callback, String url) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(url));
        }
    }

    private static void postError(UploadCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
}
