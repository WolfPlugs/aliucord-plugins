package com.aliucord.plugins;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.SystemClock;
import android.util.LruCache;
import android.widget.ImageView;
import com.aliucord.Http;
import com.aliucord.Utils;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** View/cache state belongs to the main thread. Tokens prevent writes into recycled holders. */
final class GlobalBadgeImages {
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(2 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };
    private final Map<ImageView, Object> bindings = new WeakHashMap<>();
    private final Map<String, List<Target>> waiting = new HashMap<>();
    private final Map<String, Long> failures = new HashMap<>();
    private int generation;

    private static final class Target {
        final WeakReference<ImageView> view;
        final Object token;
        Target(ImageView view, Object token) { this.view = new WeakReference<>(view); this.token = token; }
    }

    void unbind(ImageView view) { bindings.remove(view); }

    void bind(ImageView view, String url) {
        Object token = new Object();
        bindings.put(view, token);
        view.setImageDrawable(null);
        Bitmap cached = cache.get(url);
        if (cached != null) { view.setImageBitmap(cached); return; }
        if (failures.getOrDefault(url, 0L) > SystemClock.elapsedRealtime()) return;
        Target target = new Target(view, token);
        List<Target> pending = waiting.get(url);
        if (pending != null) { pending.add(target); return; }
        pending = new ArrayList<>();
        pending.add(target);
        waiting.put(url, pending);
        int requestGeneration = generation;
        Utils.threadPool.execute(() -> {
            Bitmap bitmap = download(url);
            Utils.mainThread.post(() -> {
                if (generation != requestGeneration) return;
                if (bitmap != null) cache.put(url, bitmap);
                else {
                    if (failures.size() >= 128) failures.clear();
                    failures.put(url, SystemClock.elapsedRealtime() + 30_000L);
                }
                List<Target> targets = waiting.remove(url);
                if (targets == null) return;
                for (Target item : targets) {
                    ImageView image = item.view.get();
                    if (image != null && bindings.get(image) == item.token && bitmap != null) image.setImageBitmap(bitmap);
                }
            });
        });
    }

    private Bitmap download(String url) {
        try (var request = new Http.Request(url).setRequestTimeout(5000)) {
            var response = request.execute();
            if (!response.ok()) return null;
            byte[] bytes;
            try (var stream = response.stream(); var output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (output.size() + count > 1024 * 1024) return null;
                    output.write(buffer, 0, count);
                }
                bytes = output.toByteArray();
            }
            var bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1 || bounds.outWidth > 2048 || bounds.outHeight > 2048) return null;
            var options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (bounds.outWidth / options.inSampleSize > 128 || bounds.outHeight / options.inSampleSize > 128) options.inSampleSize *= 2;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Exception ignored) { return null; }
    }

    void clear() {
        generation++;
        bindings.clear();
        waiting.clear();
        failures.clear();
        cache.evictAll();
    }
}
