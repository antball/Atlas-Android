package com.layer.ui.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;

import com.layer.sdk.messaging.Identity;
import com.layer.ui.util.Util;
import com.layer.ui.util.imagecache.BitmapWrapper;
import com.layer.ui.util.imagecache.ImageCacheWrapper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


public class AvatarViewModel implements Avatar.ViewModel  {

    private Set<Identity> mParticipants = new LinkedHashSet<>();
    private final Map<Identity, String> mInitials = new HashMap<>();
    private final Map<Identity, BitmapWrapper> mImageTargets = new HashMap<>();
    private final List<BitmapWrapper> mPendingLoads = new ArrayList<>();
    private AvatarInitials mAvatarInitials;
    private Handler mMainHandler;



    private WeakReference<Avatar.View> mViewWeakReference;

    private int mMaxAvatar = 3;
    private ImageCacheWrapper mImageCacheWrapper;

    public AvatarViewModel(ImageCacheWrapper imageCacheWrapper) {
        mImageCacheWrapper = imageCacheWrapper;
    }

    @Override
    public void setAvatarInitials(AvatarInitials avatarInitials) {
        mAvatarInitials = avatarInitials;
    }

    @Override
    public void update() {
        // Limit to mMaxAvatar valid avatars, prioritizing participants with avatars.
        if (mParticipants.size() > mMaxAvatar) {
            Queue<Identity> withAvatars = new LinkedList<>();
            Queue<Identity> withoutAvatars = new LinkedList<>();
            for (Identity participant : mParticipants) {
                if (participant == null) continue;
                if (!TextUtils.isEmpty(participant.getAvatarImageUrl())) {
                    withAvatars.add(participant);
                } else {
                    withoutAvatars.add(participant);
                }
            }

            mParticipants = new LinkedHashSet<>();
            int numWithout = Math.min(mMaxAvatar - withAvatars.size(), withoutAvatars.size());
            for (int i = 0; i < numWithout; i++) {
                mParticipants.add(withoutAvatars.remove());
            }
            int numWith = Math.min(mMaxAvatar, withAvatars.size());
            for (int i = 0; i < numWith; i++) {
                mParticipants.add(withAvatars.remove());
            }
        }

        Diff diff = diff(mInitials.keySet(), mParticipants);
        List<BitmapWrapper> toLoad = new ArrayList<>();

        List<BitmapWrapper> recyclableTargets = new ArrayList<>();
        for (Identity removed : diff.removed) {
            mInitials.remove(removed);
            BitmapWrapper target = mImageTargets.remove(removed);
            if (target != null) {
                mImageCacheWrapper.cancelRequest(target.getUrl());
                recyclableTargets.add(target);
            }
        }

        for (Identity added : diff.added) {
            if (added == null) return;
            mInitials.put(added, getInitialsForAvatarView(added));

            final BitmapWrapper target;
            if (recyclableTargets.isEmpty()) {
                target = new BitmapWrapper(added.getAvatarImageUrl());
            } else {
                target = recyclableTargets.remove(0);
            }
            target.setUrl(added.getAvatarImageUrl());
            mImageTargets.put(added, target);
            toLoad.add(target);
        }

        // Cancel existing in case the size or anything else changed.
        // TODO: make caching intelligent wrt sizing
        for (Identity existing : diff.existing) {
            if (existing == null) continue;
            mInitials.put(existing, getInitialsForAvatarView(existing));

            BitmapWrapper existingTarget = mImageTargets.get(existing);
            mImageCacheWrapper.cancelRequest(existingTarget.getUrl());
            toLoad.add(existingTarget);
        }

        for (BitmapWrapper bitmapWrapper : mPendingLoads) {
            mImageCacheWrapper.cancelRequest(bitmapWrapper.getUrl());
        }
        mPendingLoads.clear();
        mPendingLoads.addAll(toLoad);

        if (mViewWeakReference != null) {
            synchronized (mViewWeakReference) {
                Avatar.View view = mViewWeakReference.get();
                if (view != null) {
                    view.setClusterSizes(mInitials,mPendingLoads);
                    view.revalidateView();
                }
            }
        }
    }

    private String getInitialsForAvatarView(Identity added) {
        return mAvatarInitials != null ? mAvatarInitials.getInitials(added) : Util.getInitials(added);
    }

    @Override
    public void setParticipants(Identity[] participants) {
        mParticipants.clear();
        mParticipants.addAll(Arrays.asList(participants));
        update();
    }

    @Override
    public void setParticipants(Set<Identity> participants) {
        mParticipants.clear();
        mParticipants.addAll(participants);
        update();
    }

    @Override
    public void setMaximumAvatar(int maximumAvatar) {
        mMaxAvatar = maximumAvatar;
    }

    @Override
    public Set<Identity> getParticipants() {
        return new LinkedHashSet<>(mParticipants);
    }

    @Override
    public int getInitialSize() {
        return mInitials.size();
    }

    @Override
    public Set<Map.Entry<Identity, String>> getEntrySet() {
        return mInitials.entrySet();
    }

    @Override
    public BitmapWrapper getImageTarget(Identity key) {
        return  mImageTargets.get(key);
    }

    @Override
    public void setClusterSizes() {
        final Avatar.View view = mViewWeakReference != null ? mViewWeakReference.get() : null;
        if (view != null) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    view.setClusterSizes(mInitials,mPendingLoads);
                }
            });
        }
    }

    @Override
    public void loadImage(String url, String tag, int width, int height, final BitmapWrapper bitmapWrapper, Object... args) {

        mImageCacheWrapper.fetchBitmap(url, tag, width, height,
                new ImageCacheWrapper.Callback() {
                    @Override
                    public void onSuccess(final Bitmap bitmap) {
                        final Avatar.View view = mViewWeakReference != null ? mViewWeakReference.get() : null;
                        if (view != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    bitmapWrapper.setBitmap(bitmap);
                                    view.revalidateView();                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure() {
                        final Avatar.View view = mViewWeakReference != null ? mViewWeakReference.get() : null;
                        if (view != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    bitmapWrapper.setBitmap(null);
                                    view.revalidateView();                              }
                            });
                        }
                    }

                    @Override
                    public void onPrepareLoad() {
                        final Avatar.View view = mViewWeakReference != null ? mViewWeakReference.get() : null;
                        if (view != null) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    bitmapWrapper.setBitmap(null);
                                    view.revalidateView();                              }
                            });
                        }
                    }
                }, args);
    }

    @Override
    public void setView(Avatar.View avatar, Context context) {
        mMainHandler= new Handler(context.getMainLooper());
        mViewWeakReference = new WeakReference<>(avatar);
    }

    private static Diff diff(Set<Identity> oldSet, Set<Identity> newSet) {
        Diff diff = new Diff();
        for (Identity old : oldSet) {
            if (newSet.contains(old)) {
                diff.existing.add(old);
            } else {
                diff.removed.add(old);
            }
        }
        for (Identity newItem : newSet) {
            if (!oldSet.contains(newItem)) {
                diff.added.add(newItem);
            }
        }
        return diff;
    }

    private static class Diff {
        public List<Identity> existing = new ArrayList<>();
        public List<Identity> added = new ArrayList<>();
        public List<Identity> removed = new ArrayList<>();
    }
}
