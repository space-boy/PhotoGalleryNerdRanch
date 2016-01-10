package com.sunrise.ex.photogallery;

public class GalleryItem {

    private String mCaption;
    private String mid;
    private String mUrl;

    @Override
    public String toString(){
        return mCaption;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public String getMid() {

        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getCaption() {

        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }
}
