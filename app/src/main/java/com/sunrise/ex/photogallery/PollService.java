package com.sunrise.ex.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.List;

public class PollService extends IntentService {

    private static final String TAG ="PollService";
    public static final String PERM_PRIVATE = "com.sunrise.ex.photogallery.PRIVATE";
    public static final String ACTION_SHOW_NOTIFICATION = "com.sunrise.ex.photogallery.SHOW_NOTIFICATION";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";
    //private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    private static final long POLL_INTERVAL = 60 * 1000;

    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }

    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context,0,i,PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }

    public static void setServiceAlarm(Context context, boolean isOn){

        //set the alarm so that the intent is handeled (in this service, in the onHandleIntent method)
        //which as we can see, uses the network stuff which i've built
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        //if we're turning the alarm on, the below line describes when to start, what to do and the intervals
        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),POLL_INTERVAL,pi);

        } else {
            //we want to turn the alarm off, so we use the same type of intent to cancel the alarm, and the intent itself
            alarmManager.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context,isOn);
    }

    private void showBackgroundNotification(int requestCode, Notification notification){

        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE,requestCode);
        i.putExtra(NOTIFICATION,notification);

        sendOrderedBroadcast(i,PERM_PRIVATE,null,null, Activity.RESULT_OK,null,null);

    }

    public PollService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(!isNetworkAvailableAndConnected())
            return;

        String query = QueryPreferences.getStoredQuery(this);
        String lastResultId = QueryPreferences.getLastResultId(this);

        List<GalleryItem> items;

        if(query == null){
            //does paging matter?
            items = new FlickrFetchr().fetchRecentPhotos(1);
        } else {
            items = new FlickrFetchr().searchPhotos(query,1);
        }

        if(items.size() == 0)
            return;

        String resultId = items.get(0).getMid();

        if(resultId.equals(lastResultId)){
            Log.i(TAG,"Fetched old result: " + lastResultId);
        } else {
            Log.i(TAG,"Fetched new result: " + resultId);

            Resources resource = getResources();
            //get an intent that would start photoActivity
            Intent i = PhotoGalleryActivity.newIntent(this);
            //wrap it in a pending intent, to use with notifications
            PendingIntent pi = PendingIntent.getActivity(this,0,i,0);

            Notification notification = new NotificationCompat.Builder(this)
                                        .setTicker(resource.getString(R.string.new_pictures_title))
                                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                                        .setContentTitle(resource.getString(R.string.new_pictures_title))
                                        .setContentText(resource.getString(R.string.new_pictures_text))
                                        .setContentIntent(pi)
                                        .setAutoCancel(true).build();

            //NotificationManagerCompat notificationMaanger = NotificationManagerCompat.from(this);

            //notificationMaanger.notify(0,notification);

           // sendBroadcast(new Intent(ACTION_SHOW_NOTIFICATION),PERM_PRIVATE);

            showBackgroundNotification(0,notification);

        }

        QueryPreferences.setLastResultId(this,resultId);

    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvail = cm.getActiveNetworkInfo() != null;

        boolean isNetworkConnected = isNetworkAvail && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

}
