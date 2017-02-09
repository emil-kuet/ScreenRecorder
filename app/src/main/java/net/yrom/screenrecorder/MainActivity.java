/*
 * Copyright (c) 2014 Yrom Wang <http://www.yrom.net>

 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.yrom.screenrecorder;


import net.yrom.screenrecorder.R;
import android.app.Activity;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;


public class MainActivity extends Activity implements View.OnClickListener {
    private static final int REQUEST_CODE= 1;
    private static final int REQUEST_CODE_SEND = 0;
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecorder mRecorder;
    private Button mButton;
    private Button sendVideoButton;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);
        sendVideoButton = (Button) findViewById(R.id.button1);

        //noinspection ResourceType
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_CODE){
            MediaProjection mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection == null) {
                Log.e("@@", "media projection is null");
                return;
            }
            // video size
            final int width = 1280;
            final int height = 720;
            file = new File(Environment.getExternalStorageDirectory(),
                    "record-" + width + "x" + height + "-" + System.currentTimeMillis() + ".mp4");
            final int bitrate = 6000000;
            mRecorder = new ScreenRecorder(width, height, bitrate, 1, mediaProjection, file.getAbsolutePath());
            mRecorder.start();
            mButton.setText("Stop Recorder");
            Toast.makeText(this, "Screen recorder is running...", Toast.LENGTH_SHORT).show();
            moveTaskToBack(true);
        }else if(requestCode==REQUEST_CODE_SEND){
            /*String realPath;
            // SDK < API11
            if (Build.VERSION.SDK_INT < 11)
                realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(this, data.getData());

                // SDK >= 11 && SDK < 19
            else if (Build.VERSION.SDK_INT < 19)
                realPath = RealPathUtil.getRealPathFromURI_API11to18(this, data.getData());

                // SDK > 19 (Android 4.4)
            else
                realPath = RealPathUtil.getRealPathFromURI_API19(this, data.getData());*/

            Uri selectedImageURI = data.getData();


            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("video/*");

            String filePath = getPath( getApplicationContext(), selectedImageURI);

            file = new File(filePath);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file) );

            Log.i("myApp", "ok " + file.toString());
//...
            startActivity(intent);
        }



    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }



    public void sendVid(View V)
    {
        /*Intent intent = new Intent(this, WiFiDirectActivity.class);
        startActivity(intent);*/
        Log.i("Wait ", " getting video ");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // 2. pick image only
        Log.i("Wait ", " getting video 2");
        intent.setType("video/*");
        // 3. start activity
        Log.i("Wait ", " getting video 3");

        startActivityForResult(intent, REQUEST_CODE_SEND);
        Log.i("Wait ", " getting video 4");

        /*Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/*");
        String path  =  file.toString();
        Log.i("Second ", " ok2 " + path);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file) );
        */

        startActivity(intent);
//...

    }

    /*public String getPath(Uri uri) {
       String[] projection = {MediaStore.Video.Media.DATA};
       Cursor cursor = managedQuery(uri, projection, null, null, null);
       if (cursor != null) {
           // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
           // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
           int column_index = cursor
                   .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
           cursor.moveToFirst();
           return cursor.getString(column_index);
       } else
           return null;
   }
   private String getRealPathFromURI(Uri contentURI) {
       String result;
       String[] projection = { MediaStore.Video.Media.DATA };
       Cursor cursor = getContentResolver().query(contentURI, projection, null, null, null);
       if (cursor == null) { // Source is Dropbox or other similar local file path
           result = contentURI.getPath();
       } else {
           cursor.moveToFirst();
           int idx = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DATA);
           result = cursor.getString(idx);
           cursor.close();
       }
       return result;
   }
   */
    @Override
    public void onClick(View v) {
        if (mRecorder != null) {
            mRecorder.quit();
            mRecorder = null;
            mButton.setText("Restart recorder");

        } else {
            Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE);
        }
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mRecorder != null){
            mRecorder.quit();
            mRecorder = null;
        }
    }
}
