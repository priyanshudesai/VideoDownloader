package com.pd.videodownloader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.SparseArray;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pd.videodownloader.R;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class DownloadActivity extends AppCompatActivity {


    TextView t,t1;
    Button rm;
    WebView webView;
    static String youtubeLink;
    LinearLayout mainLayout;
    ProgressBar mainProgressBar,progressBar1;
    DownloadManager manager;
    private long downloadID;//
//
    private static final int PROGRESS_DELAY = 1000;
    Handler handler = new Handler();
    private boolean isProgressCheckerRunning = false;

    BroadcastReceiver onComplete=new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
                DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor cursor = manager.query(query);
                if (cursor.moveToFirst()) {
                    if (cursor.getCount() > 0) {
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            String file = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                            // So something here on success
                            new DownloadActivity().stopProgressChecker();
                            progressBar1.setProgress(100);
                            t.setText("Downloaded Successfully");
                            rm.setVisibility(LinearLayout.GONE);
                        } else {
                            int message = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                            // So something here on failed.
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mainProgressBar = (ProgressBar) findViewById(R.id.prgrBar);
        progressBar1 = (ProgressBar) findViewById(R.id.prgrBar1);

        t=findViewById(R.id.txt1data);
        t1=findViewById(R.id.txt2data);
        rm=findViewById(R.id.remove);

        t.setVisibility(LinearLayout.GONE);
        t1.setVisibility(LinearLayout.GONE);
        rm.setVisibility(LinearLayout.GONE);
        progressBar1.setVisibility(LinearLayout.GONE);


        rm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterByStatus (DownloadManager.STATUS_FAILED|DownloadManager.STATUS_PENDING|DownloadManager.STATUS_RUNNING);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor c = dm.query(query);
                while(c.moveToNext()) {
                    dm.remove(c.getLong(c.getColumnIndex(DownloadManager.COLUMN_ID)));
                }
                mainLayout.setVisibility(LinearLayout.VISIBLE);
                t.setVisibility(LinearLayout.GONE);
                t1.setVisibility(LinearLayout.GONE);
                rm.setVisibility(LinearLayout.GONE);
                progressBar1.setVisibility(LinearLayout.GONE);
            }
        });



        registerReceiver(onComplete,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        isStoragePermissionGranted();
        Intent i=getIntent();
        youtubeLink=i.getStringExtra("url");

        if (youtubeLink != null
                && (youtubeLink.contains("://youtu.be/") || youtubeLink.contains("youtube.com/watch?v="))) {
            getYoutubeDownloadUrl(youtubeLink);
        } else {
            Toast.makeText(this,"Not a valid YouTube link!", Toast.LENGTH_LONG).show();
            finish();
        }

    }
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                //Log.v(TAG,"Permission is granted");
                return true;
            } else {

                //Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            //Log.v(TAG,"Permission is granted");
            return true;
        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//           // Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
//
//        }
//    }

    private void getYoutubeDownloadUrl(String youtubeLink) {
       new YouTubeExtractor(this) {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                mainProgressBar.setVisibility(View.GONE);

                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    Toast.makeText(DownloadActivity.this, "Please Enter Valid URL!", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);

                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
                        addButtonToMainLayout(vMeta.getTitle(), ytFile);
                    }
                }
            }
        }.extract(youtubeLink, true, false);
    }

    private void addButtonToMainLayout(final String videoTitle, final YtFile ytfile) {
        // Display some buttons and let the user choose the format
        String btnText = (ytfile.getFormat().getHeight() == -1) ? "Audio " +
                ytfile.getFormat().getAudioBitrate() + " kbit/s" :
                ytfile.getFormat().getHeight() + "p";
        btnText += (ytfile.getFormat().isDashContainer()) ? " dash" : "";
        Button btn = new Button(this);
        btn.setText(btnText);
        btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String filename;
                if (videoTitle.length() > 55) {
                    filename = videoTitle.substring(0, 55) + "." + ytfile.getFormat().getExt();
                } else {
                    filename = videoTitle + "." + ytfile.getFormat().getExt();
                }
                filename = filename.replaceAll("[\\\\><\"|*?%:#/]", "");
                downloadFromUrl(ytfile.getUrl(), videoTitle, filename);
                //finish();
                mainLayout.setVisibility(LinearLayout.GONE);
                t.setVisibility(LinearLayout.VISIBLE);
                t1.setVisibility(LinearLayout.VISIBLE);
                rm.setVisibility(LinearLayout.VISIBLE);
                progressBar1.setVisibility(LinearLayout.VISIBLE);
            }
        });
        mainLayout.addView(btn);
    }

    private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
        Uri uri = Uri.parse(youtubeDlUrl);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(downloadTitle);

        request.setDescription("Downloading");//

        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);///
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        //request.setRequiresCharging(false);//
        request.setAllowedOverMetered(true);//
        request.setAllowedOverRoaming(true);//

        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadID = manager.enqueue(request);//
        //
        startProgressChecker();
    }



    /**
     * Checks download progress.
     */
    private void checkProgress() {
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterByStatus(~(DownloadManager.STATUS_FAILED | DownloadManager.STATUS_SUCCESSFUL));
        Cursor cursor = manager.query(query);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }
        do {
            double reference = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));
            double progress = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            double total = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            // do whatever you need with the progress
            //Toast.makeText(this, reference+"\n"+progress, Toast.LENGTH_SHORT).show();
            //t.setText(reference+"\n"+progress+"\n"+total);
            double tt=progress/total;
            double dd=tt*100;
            int tt1= (int)dd;
            int progressStatus=tt1;
            double ttt=(total/1024)/1024;

            t1.setText(ttt+"MB");
            progressBar1.setProgress(progressStatus);
            //Toast.makeText(this, progressStatus+"\n"+progress+"\n"+total+"\n"+tt, Toast.LENGTH_SHORT).show();
            t.setText(progressStatus + "% of " + progressBar1.getMax());
        } while (cursor.moveToNext());
        cursor.close();
    }

    /**
     * Starts watching download progress.
     *
     * This method is safe to call multiple times. Starting an already running progress checker is a no-op.
     */
    private void startProgressChecker() {
        if (!isProgressCheckerRunning) {
            progressChecker.run();
            isProgressCheckerRunning = true;
        }
    }

    /**
     * Stops watching download progress.
     */
    public void stopProgressChecker() {
        handler.removeCallbacks(progressChecker);
        isProgressCheckerRunning = false;
    }

    /**
     * Checks download progress and updates status, then re-schedules itself.
     */
    private Runnable progressChecker = new Runnable() {
        @Override
        public void run() {
            try {
                checkProgress();
                // manager reference not found. Commenting the code for compilation
                //manager.refresh();
            } finally {
                handler.postDelayed(progressChecker, PROGRESS_DELAY);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

}
