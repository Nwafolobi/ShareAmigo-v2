package com.eduvos.sharamigo.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

// Shrinks a photo from the phone before upload so students don't burn their data.
// Call from a background thread: decoding and compressing a camera photo takes a moment.
public final class ImageCompressor {

    private static final int MAX_SIDE = 1280;   // pixels on the longest side
    private static final int JPEG_QUALITY = 80; // usually lands around 150-400 KB

    private ImageCompressor() {}

    public static byte[] compress(Context context, Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        // 1. Read only the size, so a 12 MP photo is never fully loaded into memory.
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open photo");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("That file is not a photo");
        }

        // 2. Decode at a reduced size.
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= MAX_SIDE || bounds.outHeight / (sample * 2) >= MAX_SIDE) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Cannot open photo");
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) {
            throw new IOException("Could not read the photo");
        }

        // 3. Scale to the final size and turn it the right way up.
        Matrix matrix = new Matrix();
        float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (scale < 1f) {
            matrix.postScale(scale, scale);
        }
        int rotation = readRotation(resolver, uri);
        if (rotation != 0) {
            matrix.postRotate(rotation);
        }
        if (!matrix.isIdentity()) {
            Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (transformed != bitmap) {
                bitmap.recycle();
                bitmap = transformed;
            }
        }

        // 4. Save as JPEG.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        bitmap.recycle();
        return out.toByteArray();
    }

    // Builds the "photo" form field the backend expects.
    public static MultipartBody.Part photoPart(Context context, Uri uri) throws IOException {
        byte[] jpeg = compress(context, uri);
        RequestBody body = RequestBody.create(jpeg, MediaType.parse("image/jpeg"));
        return MultipartBody.Part.createFormData("photo", "photo.jpg", body);
    }

    public static RequestBody textPart(String value) {
        return RequestBody.create(value, MediaType.parse("text/plain"));
    }

    private static int readRotation(ContentResolver resolver, Uri uri) {
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) return 0;
            int orientation = new ExifInterface(in).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
