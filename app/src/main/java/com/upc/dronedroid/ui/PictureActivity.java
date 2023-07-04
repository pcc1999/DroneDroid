package com.upc.dronedroid.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.upc.dronedroid.R;
import com.upc.dronedroid.utils.MqttClientUtil;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.imgproc.Imgproc;

public class PictureActivity extends AppCompatActivity {

    private static String origin;
    private static BroadcastReceiver br;
    private static Bitmap originalBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        origin = getIntent().getStringExtra("origin");
        switch (origin) {
            case "picture":
                //In case of picture the image itself will be obtained BEFORE launching the intent
                byte[] bytes = getIntent().getByteArrayExtra("bytes");
                if (!OpenCVLoader.initDebug()) {
                    Log.d(this.getClass().getName(), "OpenCV not initialized");
                } else {
                    originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ImageView imageView = (ImageView) findViewById(R.id.imageView2);
                    imageView.setImageBitmap(originalBitmap);
                }
                break;
            case "video":
                //In case of videos the videoStream will be obtained AFTER launching the intent
                //Activity might show a loading image frame to allow loading the buffer
                //Also a broadcastreceiver must be created
                br = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        byte[] bytes = intent.getByteArrayExtra("bytes");
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        ImageView img = findViewById(R.id.imageView2);
                        img.setImageBitmap(bmp);
                        //TODO: Try to use videoBuffer

                    }
                };
                IntentFilter filter = new IntentFilter("com.upc.dronedroid.VIDEO_STREAM");
                int receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED;
                ContextCompat.registerReceiver(this.getApplicationContext(), br, filter, receiverFlags);
                break;
        }
    }

    public void closeActivity(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        if (origin.equals("video")) {
            //If activity was showing video stop it
            MqttClientUtil.publishMessage(getApplicationContext().getPackageName() + "/cameraService/stopVideoStream", "", this);
            //And destroy the receiver!
            try {
                getApplicationContext().unregisterReceiver(br);
            } catch (Exception ex) {
                //Surround with catch for any exception thrown
                ex.printStackTrace();
            }
        } else {
            //If activity was showing a picture release the semaphore
            setResult(200, this.getIntent());
        }
        finish();
    }

    public void onRedPressed(View v) {
        if (!OpenCVLoader.initDebug()) {
            Log.d(this.getClass().getName(), "OpenCV not initialized");
        } else {
            switch (origin) {
                case "picture":
                    ImageView imageView = (ImageView) findViewById(R.id.imageView2);
                    Mat src = new Mat(originalBitmap.getHeight(), originalBitmap.getWidth(), CvType.CV_8UC3);
                    Utils.bitmapToMat(originalBitmap, src);
                    //Android uses BGR but openCV uses RGB to apply colors
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
                    Imgproc.applyColorMap(src, src, Imgproc.COLORMAP_HOT);
                    Bitmap processedBmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                    //Get back to BGR in order to display picture (if not, blue and red channels are flipped)
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
                    Utils.matToBitmap(src, processedBmp);
                    imageView.setImageBitmap(processedBmp);
                    break;
                case "video":
                    getApplicationContext().unregisterReceiver(br);
                    br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            byte[] bytes = intent.getByteArrayExtra("bytes");
                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            ImageView img = (ImageView) findViewById(R.id.imageView2);
                            Mat src = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
                            Utils.bitmapToMat(bmp, src);
                            //Android uses BGR but openCV uses RGB to apply colors
                            Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
                            Imgproc.applyColorMap(src, src, Imgproc.COLORMAP_HOT);
                            Bitmap processedBmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                            //Get back to BGR in order to display picture (if not, blue and red channels are flipped)
                            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
                            Utils.matToBitmap(src, processedBmp);
                            img.setImageBitmap(processedBmp);
                        }
                    };
                    IntentFilter filter = new IntentFilter("com.upc.dronedroid.VIDEO_STREAM");
                    int receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED;
                    ContextCompat.registerReceiver(this.getApplicationContext(), br, filter, receiverFlags);
                    break;
            }
        }
    }

    public void onBluePressed(View v) {
        if (!OpenCVLoader.initDebug()) {
            Log.d(this.getClass().getName(), "OpenCV not initialized");
        } else {
            switch (origin) {
                case "picture":
                    ImageView imageView = (ImageView) findViewById(R.id.imageView2);
                    Mat src = new Mat(originalBitmap.getHeight(), originalBitmap.getWidth(), CvType.CV_8UC3);
                    Utils.bitmapToMat(originalBitmap, src);
                    //Android uses BGR but openCV uses RGB to apply colors
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
                    Imgproc.applyColorMap(src, src, Imgproc.COLORMAP_OCEAN);
                    Bitmap processedBmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                    //Get back to BGR in order to display picture (if not, blue and red channels are flipped)
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
                    Utils.matToBitmap(src, processedBmp);
                    imageView.setImageBitmap(processedBmp);
                    break;
                case "video":

                    getApplicationContext().unregisterReceiver(br);
                    br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            byte[] bytes = intent.getByteArrayExtra("bytes");
                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            ImageView img = (ImageView) findViewById(R.id.imageView2);
                            Mat src = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
                            Utils.bitmapToMat(bmp, src);
                            //Android uses BGR but openCV uses RGB to apply colors
                            Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
                            Imgproc.applyColorMap(src, src, Imgproc.COLORMAP_OCEAN);
                            Bitmap processedBmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                            //Get back to BGR in order to display picture (if not, blue and red channels are flipped)
                            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
                            Utils.matToBitmap(src, processedBmp);
                            img.setImageBitmap(processedBmp);
                        }
                    };
                    IntentFilter filter = new IntentFilter("com.upc.dronedroid.VIDEO_STREAM");
                    int receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED;
                    ContextCompat.registerReceiver(this.getApplicationContext(), br, filter, receiverFlags);
                    break;
            }
        }
    }

    public void onGreenPressed(View v) {
        if (!OpenCVLoader.initDebug()) {
            Log.d(this.getClass().getName(), "OpenCV not initialized");
        } else {
            switch (origin) {
                case "picture":
                    ImageView imageView = (ImageView) findViewById(R.id.imageView2);
                    Mat src = new Mat(originalBitmap.getHeight(), originalBitmap.getWidth(), CvType.CV_8UC3);
                    Utils.bitmapToMat(originalBitmap, src);
                    //Android uses BGR but openCV uses RGB to apply colors
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
                    Imgproc.applyColorMap(src, src, Imgproc.COLORMAP_SUMMER);
                    Bitmap processedBmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                    //Get back to BGR in order to display picture (if not, blue and red channels are flipped)
                    Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
                    Utils.matToBitmap(src, processedBmp);
                    imageView.setImageBitmap(processedBmp);
                    break;
                case "video":
                    getApplicationContext().unregisterReceiver(br);
                    br = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            byte[] bytes = intent.getByteArrayExtra("bytes");
                            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            ImageView img = (ImageView) findViewById(R.id.imageView2);
                            Mat src = new Mat(bmp.getHeight(), bmp.getWidth(), CvType.CV_8UC3);
                            Utils.bitmapToMat(bmp, src);
                            //Android uses BGR but openCV uses RGB to apply colors
                            Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);
                            Imgproc.applyColorMap(src, src, Imgproc.COLORMAP_SUMMER);
                            Bitmap processedBmp = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
                            //Get back to BGR in order to display picture (if not, blue and red channels are flipped)
                            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
                            Utils.matToBitmap(src, processedBmp);
                            img.setImageBitmap(processedBmp);
                        }
                    };
                    IntentFilter filter = new IntentFilter("com.upc.dronedroid.VIDEO_STREAM");
                    int receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED;
                    ContextCompat.registerReceiver(this.getApplicationContext(), br, filter, receiverFlags);
                    break;
            }
        }
    }

    public void onNonePressed(View v){
        switch (origin) {
            case "picture":
                //In case of picture the image itself will be obtained BEFORE launching the intent
                byte[] bytes = getIntent().getByteArrayExtra("bytes");
                if (!OpenCVLoader.initDebug()) {
                    Log.d(this.getClass().getName(), "OpenCV not initialized");
                } else {
                    originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    ImageView imageView = (ImageView) findViewById(R.id.imageView2);
                    imageView.setImageBitmap(originalBitmap);
                }
                break;
            case "video":
                //In case of videos the videoStream will be obtained AFTER launching the intent
                //Activity might show a loading image frame to allow loading the buffer
                //Also a BroadcastReceiver must be created
                br = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        byte[] bytes = intent.getByteArrayExtra("bytes");
                        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        ImageView img = (ImageView) findViewById(R.id.imageView2);
                        img.setImageBitmap(bmp);
                        //TODO: Try to use videoBuffer
                    }
                };
                IntentFilter filter = new IntentFilter("com.upc.dronedroid.VIDEO_STREAM");
                int receiverFlags = ContextCompat.RECEIVER_NOT_EXPORTED;
                ContextCompat.registerReceiver(this.getApplicationContext(), br, filter, receiverFlags);
                break;
        }
    }
}