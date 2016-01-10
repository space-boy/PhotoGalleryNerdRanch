package com.sunrise.ex.photogallery;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;

public class PhotoGalleryActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {

        return PhotoGalleryFragment.newInstance();
    }

    public static Intent newIntent(Context context){
        return new Intent(context,PhotoGalleryActivity.class);
    }

}
