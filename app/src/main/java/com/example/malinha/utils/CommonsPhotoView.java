package com.example.malinha.utils;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.malinha.R;
import com.example.malinha.services.TravelSearchService;
import com.example.malinha.services.WikimediaImageService;
import java.lang.ref.WeakReference;

/** Binds one card, keeping images and credits paired when rows are recycled. */
public final class CommonsPhotoView {
    private CommonsPhotoView() {}

    public static void bindDestination(View row, String destination, int fallback) {
        String name = destination == null ? "" : destination.split(",", 2)[0];
        bind(row, name, destination, fallback);
    }

    public static void bind(View row, String name, String destination, int fallback) {
        ImageView image = row.findViewById(R.id.commons_photo);
        TextView credit = row.findViewById(R.id.commons_credit);
        RequestManager requests = Glide.with(image);
        requests.clear(image);
        image.setImageResource(fallback);
        credit.setOnClickListener(null);
        credit.setVisibility(View.GONE);
        Object binding = new Object();
        image.setTag(R.id.commons_photo, binding);

        WeakReference<ImageView> imageRef = new WeakReference<>(image);
        WeakReference<TextView> creditRef = new WeakReference<>(credit);
        WeakReference<RequestManager> requestsRef = new WeakReference<>(requests);
        WikimediaImageService.get().find(name, destination, photo -> {
            ImageView target = imageRef.get();
            TextView caption = creditRef.get();
            RequestManager manager = requestsRef.get();
            if (photo == null || target == null || caption == null || manager == null
                    || target.getTag(R.id.commons_photo) != binding) return;
            GlideUrl url = new GlideUrl(photo.thumbnail, new LazyHeaders.Builder()
                .setHeader("User-Agent", WikimediaImageService.USER_AGENT).build());
            manager.load(url).fitCenter().diskCacheStrategy(DiskCacheStrategy.DATA)
                .placeholder(fallback).error(fallback).listener(new RequestListener<Drawable>() {
                    @Override public boolean onLoadFailed(GlideException error, Object model,
                            Target<Drawable> ignored, boolean first) {
                        if (target.getTag(R.id.commons_photo) == binding) {
                            caption.setVisibility(View.GONE);
                            android.util.Log.w("MalinhaPhotos", "O download da miniatura do Commons falhou.");
                        }
                        return false;
                    }

                    @Override public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> ignored, DataSource source, boolean first) {
                        if (target.getTag(R.id.commons_photo) != binding) return true;
                        caption.setText(caption.getContext().getString(R.string.commons_credit, photo.author, photo.license));
                        caption.setOnClickListener(v -> showCredits(v, photo));
                        caption.setVisibility(View.VISIBLE);
                        return false;
                    }
                }).into(target);
        });
    }

    public static void clear(View row) {
        ImageView image = row.findViewById(R.id.commons_photo);
        image.setTag(R.id.commons_photo, null);
        Glide.with(image.getContext().getApplicationContext()).clear(image);
        TextView credit = row.findViewById(R.id.commons_credit);
        credit.setVisibility(View.GONE);
        credit.setOnClickListener(null);
    }

    private static void showCredits(View view, WikimediaImageService.Photo photo) {
        android.content.Context context = view.getContext();
        new AlertDialog.Builder(context).setTitle(photo.title)
            .setMessage(context.getString(R.string.commons_credit, photo.author, photo.license))
            .setNeutralButton(R.string.commons_source,
                (dialog, which) -> TravelSearchService.open(context, Uri.parse(photo.source)))
            .setPositiveButton(context.getString(R.string.commons_license, photo.license),
                (dialog, which) -> TravelSearchService.open(context, Uri.parse(photo.licenseUrl)))
            .setNegativeButton(R.string.close_dialog, null).show();
    }
}
