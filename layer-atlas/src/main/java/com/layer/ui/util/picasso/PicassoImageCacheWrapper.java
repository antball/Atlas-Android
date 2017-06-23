package com.layer.ui.util.picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.layer.ui.util.picasso.requesthandlers.MessagePartRequestHandler;
import com.layer.ui.util.picasso.transformations.CircleTransform;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;

import static com.layer.ui.util.Log.TAG;

public class PicassoImageCacheWrapper implements ImageCacheWrapper {
    protected final static CircleTransform SINGLE_TRANSFORM = new CircleTransform(TAG + ".single");
    protected final static CircleTransform MULTI_TRANSFORM = new CircleTransform(TAG + ".multi");
    protected final Picasso mPicasso;

    public PicassoImageCacheWrapper(MessagePartRequestHandler messagePartRequestHandler, Context context) {
        mPicasso = new Picasso.Builder(context)
                .addRequestHandler(messagePartRequestHandler)
                .build();
    }

    @Override
    public void load(String targetUrl, String tag, int size, int size1, ImageView imageView, Object... args) {
        boolean isMultiTransform = false;
        if (args != null && args.length > 0) {
            isMultiTransform = (boolean) args[0];
        }

        RequestCreator creator = mPicasso.load(targetUrl)
                .tag(tag)
                .noPlaceholder()
                .noFade()
                .centerCrop()
                .resize(size, size);

        creator.transform(isMultiTransform ? MULTI_TRANSFORM : SINGLE_TRANSFORM)
                .into(imageView);
    }

    @Override
    public void fetchBitmap(String url, String tag, int width, int height, final Callback callback,
                            Object... args) {

        boolean isMultiTransform = false;
        if (args != null && args.length > 0) {
            isMultiTransform = (boolean) args[0];
        }

        RequestCreator creator = mPicasso.load(url)
                .tag(tag)
                .noPlaceholder()
                .noFade()
                .centerCrop()
                .resize(width, height);

        creator.transform(isMultiTransform ? MULTI_TRANSFORM : SINGLE_TRANSFORM)
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        callback.onSuccess(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                        callback.onFailure();
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });

    }

    @Override
    public void cancelRequest(ImageView imageView) {
        if (imageView != null) {
            mPicasso.cancelRequest(imageView);
        }
    }

    @Override
    public void cancelRequest(String tag) {
        if (tag != null && !tag.isEmpty()) {
            mPicasso.cancelTag(tag);
        }
    }
}
