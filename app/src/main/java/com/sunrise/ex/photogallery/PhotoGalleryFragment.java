package com.sunrise.ex.photogallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

    private RecyclerView mPhotoRecylerView;
    private ProgressBar mProgBar;
    private static final String TAG = "PhotoGalleryFragment";
    private List<GalleryItem> mItems = new ArrayList<>();
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mPage;
    private int mCols = 3;

    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
        updateItems(0);

        //Intent i = PollService.newIntent(getActivity());
        //getActivity().startService(i);

        //PollService.setServiceAlarm(getActivity(),true);


        //new FetchItemsTask().execute(mPage++);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setTThumbnailDownloaderListener(new ThumbnailDownloader.ThumbnailDowloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDowloaded(PhotoHolder target, Bitmap bitmap) {

                if(isAdded()) {
                    Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                    target.bindDrawable(drawable);
                    }

                }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem =  menu.findItem(R.id.menu_search_item);

        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                QueryPreferences.setStoredQuery(getActivity(), query);
                mProgBar.setVisibility(View.VISIBLE);
                mPhotoRecylerView.setVisibility(View.INVISIBLE);
                updateItems(0);

                searchView.setQuery("", false);
                searchView.setIconified(true);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new SearchView.OnClickListener() {

            @Override
            public void onClick(View v) {

                searchView.setQuery(QueryPreferences.getStoredQuery(getActivity()), false);

            }
        });

        MenuItem menuToggle = menu.findItem(R.id.menu_item_toggle_polling);

        if(PollService.isServiceAlarmOn(getActivity())){
            menuToggle.setTitle(R.string.stop_polling);
        } else {
            menuToggle.setTitle(R.string.start_polling);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){

            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(), null);
                mProgBar.setVisibility(View.VISIBLE);
                mPhotoRecylerView.setVisibility(View.INVISIBLE);
                updateItems(0);
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    private void updateItems(int page){
        String query = QueryPreferences.getStoredQuery(getActivity());

        if(page == 0)
            page = 1;

        new FetchItemsTask(query,mProgBar).execute(page);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "killed background thread");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_photo_gallery,container, false);

        mPhotoRecylerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecylerView.setLayoutManager(new GridLayoutManager(getActivity(), mCols));

        mProgBar = (ProgressBar) v.findViewById(R.id.prog_bar);
        mProgBar.setVisibility(View.VISIBLE);

        mPhotoRecylerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {

                int mNewColumns;
                DisplayMetrics metrics = new DisplayMetrics();
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);


                if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE){
                     mNewColumns = (int) Math.floor(metrics.widthPixels * 3 / metrics.heightPixels);
                } else {
                    mNewColumns = (int) Math.floor(metrics.heightPixels * 3 / metrics.widthPixels);
                }

                if (mNewColumns != mCols) {
                    GridLayoutManager layoutManager = (GridLayoutManager) mPhotoRecylerView.getLayoutManager();
                    layoutManager.setSpanCount(mNewColumns);
                }

            }
        });

        mPhotoRecylerView.addOnScrollListener(new RecyclerView.OnScrollListener(){

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                //if scrolled downwards
                //up scrolling would be more difficult
                if(dy > 0){
                    PhotoAdapter adapter = (PhotoAdapter)recyclerView.getAdapter();
                    GridLayoutManager layoutManager = (GridLayoutManager)recyclerView.getLayoutManager();

                    if(layoutManager.findLastCompletelyVisibleItemPosition() >= (adapter.getItemCount() - 1)){

                        mPage++;
                        if(mPage == 100)
                            mPage = 1;

                        new FetchItemsTask(QueryPreferences.getStoredQuery(getActivity()),mProgBar).execute(mPage);

                    }
                }
            }
        });

        setupAdapter();

        return v;
    }

    private void setupAdapter(){
        if(isAdded()){
            mPhotoRecylerView.setAdapter(new PhotoAdapter(mItems));
        }
    }



    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView){
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }

    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());

            View v = layoutInflater.inflate(R.layout.gallery_item, parent, false);

            return new PhotoHolder(v);

        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {

            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = ContextCompat.getDrawable(getActivity(),R.drawable.no_img);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());

        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>>{

        private String mQuery;


        public FetchItemsTask(String query, ProgressBar progressBar){
            mQuery = query;
            mProgBar = progressBar;
        }

        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {

            //query = QueryPreferences.getStoredQuery(getActivity());

            if(mQuery == null){
                return new FlickrFetchr().fetchRecentPhotos(params[0]);
            } else {
                return new FlickrFetchr().searchPhotos(mQuery,params[0]);
            }
            //return new FlickrFetchr().FetchItems(params[0]);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            mProgBar.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems = galleryItems;
            mProgBar.setVisibility(View.GONE);
            mPhotoRecylerView.setVisibility(View.VISIBLE);
            setupAdapter();
        }
    }
}
