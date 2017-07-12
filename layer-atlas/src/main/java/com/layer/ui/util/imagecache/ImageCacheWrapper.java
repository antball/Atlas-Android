package com.layer.ui.util.imagecache;

import android.graphics.Bitmap;

public interface ImageCacheWrapper {

    /**
     * Fetch Bitmap from any Image Caching Library
     * Set the Bitmap on the BitmapWrapper in your implementation
     * @see BitmapWrapper#setBitmap(Bitmap)
     * Pass the BitmapWrapper to the onSuccess of Callback
     * @see PicassoImageCacheWrapper for sample implementation
     * @param bitmapWrapper
     */
    void fetchBitmap(BitmapWrapper bitmapWrapper, Callback callback);

    /**
     * Makes call to cancel BitMap request
     * @see PicassoImageCacheWrapper for sample implementation
     * @param bitmapWrapper
     */
    void cancelBitmap(BitmapWrapper bitmapWrapper);

    /**
     * Callback on the ViewModel when the Bitmap is loaded from the ImageCache Library
     * @see com.layer.ui.avatar.AvatarViewModel#fetchBitmap(BitmapWrapper)
     *
     */
    interface Callback {
        void onSuccess();
        void onFailure();
    }

}
