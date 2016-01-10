package com.sunrise.ex.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "";
    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getrecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";

    //for now, we have a generic address for the endpoint, we will attach the method we want
    //and any applicable parameters later
    private static final Uri ENDPOINT =  Uri.parse("https://api.flickr.com/services/rest/")
                                            .buildUpon()
                                            .appendQueryParameter("method", "flickr.photos.getRecent")
                                            .appendQueryParameter("api_key", API_KEY)
                                            .appendQueryParameter("format", "json")
                                            .appendQueryParameter("nojsoncallback", "1")
                                            .appendQueryParameter("extras", "url_s")
                                            .build();
   // .appendQueryParameter("page", String.valueOf(page))

    public byte[] getUrlBytes(String urlSpec) throws IOException{

        URL url = new URL(urlSpec);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage()
                                      + ": with "
                                      + urlSpec);
            }

            int bytesRead; // = 0
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) > 0){
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();

        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException{
        return new String(getUrlBytes(urlSpec));
    }

    private String buildUri(String method, String query,int page){

        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                                      .appendQueryParameter("method",method)
                                      .appendQueryParameter("page",String.valueOf(page));

        if(method.equals(SEARCH_METHOD)){
            uriBuilder.appendQueryParameter("text",query);
        }

        return uriBuilder.build().toString();

    }

    public List<GalleryItem> fetchRecentPhotos(int page){
        String url = buildUri(FETCH_RECENTS_METHOD,null,page);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query, int page){
        String url = buildUri(SEARCH_METHOD,query,page);
        return downloadGalleryItems(url);
    }

    //int page
    public List<GalleryItem> downloadGalleryItems(String url){


        List<GalleryItem> items = new ArrayList<>();

        try {

            String jsonString = getUrlString(url);
            //Log.i(TAG, "Received Json: " + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items,jsonBody);

        }catch (JSONException je){
            Log.e(TAG,"Failed to parse json: " + je);
        } catch (IOException ioe){
            Log.e(TAG,"Failed to fetch Items: " + ioe);
        }

        return items;

    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException, IOException{

        //we know that what we got back from the URL was json
        //we know that there is a JSON object returned called "photos", which has meta data about the photos we got returned
        //we know that there is a second array within "photos", called "photo", which are individual photos
        //which is why we are getting objects and then getting objects within objects

        JSONObject photoJsonOBject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photoJsonOBject.getJSONArray("photo");

        for(int i = 0;i < photoJsonArray.length();i++){
            photoJsonOBject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setMid(photoJsonOBject.getString("id"));
            item.setCaption(photoJsonOBject.getString("title"));

            //wait till working then re-arrange. why bother assigning the above stuff
            //if we later abandon the object without adding it to the array
            //N.B...counts for url small...maybe it does not need moving?
            if(!photoJsonOBject.has("url_s")){
                continue;
            }
            item.setUrl(photoJsonOBject.getString("url_s"));
            items.add(item);
        }
    }

}
