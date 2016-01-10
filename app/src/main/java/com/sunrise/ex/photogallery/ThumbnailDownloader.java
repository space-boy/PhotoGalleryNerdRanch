package com.sunrise.ex.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static int MESSAGE_DOWNLOAD = 0;
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private ThumbnailDowloadListener<T> mTThumbnailDownloaderListener;

    public interface ThumbnailDowloadListener<T>{
        void onThumbnailDowloaded(T target, Bitmap bitmap);
    }

    public void setTThumbnailDownloaderListener(ThumbnailDowloadListener<T> listener){
        mTThumbnailDownloaderListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    public void queueThumbnail(T target, String url){
        //Log.i(TAG, "Got a  url: " + url);

        if(url == null){
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target,url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
    }

    private void handleRequest(final T target){

        try {
            final String url = mRequestMap.get(target);

            if(url == null)
                return;

            byte[] bitmapbytes = new FlickrFetchr().getUrlBytes(url);

            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapbytes, 0, bitmapbytes.length);

            //Log.i(TAG, "bitmap successfully created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {

                    if(mRequestMap.get(target) !=  url){
                        return;
                    }

                    mRequestMap.remove(target);

                    mTThumbnailDownloaderListener.onThumbnailDowloaded(target,bitmap);
                }
            });

        } catch (IOException ioe){
            Log.e(TAG, "error downloading images: " + ioe);
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    @Override
    protected void onLooperPrepared() {
        super.onLooperPrepared();

        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T) msg.obj; //the object we attached earlier, we know it's an instance of PhotoHolder
                    //Log.i(TAG, "got a request for target: " + mRequestMap.get(target));

                    handleRequest(target);
                }
            }
        };
    }
}

