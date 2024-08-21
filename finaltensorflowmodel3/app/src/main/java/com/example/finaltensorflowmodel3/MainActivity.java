package com.example.finaltensorflowmodel3;
///main
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Range;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.finaltensorflowmodel3.ml.SsdMobilenetV11Metadata1;

import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    List<String> labels_txt;
    private boolean isDetectionPaused = false;



    RealHeightClass realHeightClass=new RealHeightClass();
    TextView textView;
    Paint paint=new Paint();
    boolean isDetectionAllowed = true;
    Timer DetectionTimer = new Timer();
    Boolean objectsDetected=false;
    ImageProcessor iP;
    Bitmap bitmap;
    ImageView imageView;
    CameraDevice cD;
    TextToSpeechH textToSpeechHelper;
    Handler handler_main;
    CameraManager cM;
    TextureView textureView;
    SsdMobilenetV11Metadata1 model;
    float focalLengthInPixels;
    float x1,x2,y1,y2;
    HandlerThread bT;
    TextToSpeech textToSpeech_iB;
    TextToSpeech tts;
    private int objectsDetectedCount = 0;
    private boolean isPrintingPaused = false;
    private Handler handler = new Handler();




    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPermission();
        textView = findViewById(R.id.text_view);

        try {
            model = SsdMobilenetV11Metadata1.newInstance(this);
            labels_txt = FileUtil.loadLabels(this, "labels.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        textToSpeechHelper = new TextToSpeechH(this);


        iP = new ImageProcessor.Builder().add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build();

        HandlerThread hT = new HandlerThread("videoThread");
        hT.start();

        // In your onCreate() method, initialize the background thread

        bT = new HandlerThread("BackgroundThread");
        bT.start();
        handler_main = new Handler(bT.getLooper());
        //Uncomment this line for paint//
        //imageView=findViewById(R.id.imageView);


        textToSpeech_iB = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int tar = textToSpeech_iB.setLanguage(Locale.US);
                if (tar == TextToSpeech.LANG_MISSING_DATA ||
                        tar == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TextToSpeech not supported in your device.", Toast.LENGTH_SHORT).show();
                } else {
                    // Speak "text mode on"
                    textToSpeech_iB.speak("Object mode on", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            } else {
                Toast.makeText(this, "TextToSpeech initialization failed.", Toast.LENGTH_SHORT).show();
            }
        });
        textureView = findViewById(R.id.textureView);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                // Move the image processing and model inference tasks to the background thread



                handler_main.post(new Runnable() {

                    @Override
                    public void run() {
                        if (isDetectionPaused) {
                            // Detection is paused, do nothing
                            return;
                        }

                        StringBuilder detectedObjectsBuilder = new StringBuilder();
                        bitmap = textureView.getBitmap();
                        TensorImage T_image = TensorImage.fromBitmap(bitmap);


                        SsdMobilenetV11Metadata1.Outputs outputs = model.process(T_image);
                        float[] location_model = outputs.getLocationsAsTensorBuffer().getFloatArray();
                        float[] class_model = outputs.getClassesAsTensorBuffer().getFloatArray();
                        float[] score_model = outputs.getScoresAsTensorBuffer().getFloatArray();
                        float[] nod_model = outputs.getNumberOfDetectionsAsTensorBuffer().getFloatArray();

                        Bitmap mutable_XY = bitmap.copy(Bitmap.Config.ARGB_8888, true);

                        //float focalLength = ...; // Camera focal length (in pixels) - You need to find or calibrate this value.
                        //float realObjectHeight = ...; // Height of the real-world object (in meters) you want to detect.



                        int x;

                        for (int index = 0; index < score_model.length; index++) {
                            x = index * 4;

                            if (score_model[index] > 0.50) {
                                objectsDetected=true;


                                if (labels_txt.get((int) class_model[index]).equals("bottle")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealBottleHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) * mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();



                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    detectedObjectsBuilder.append("Bottle ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("person")){

                                    float averageRealHeight = realHeightClass.calculateAverageRealPersonHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Person ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }



                                else if (labels_txt.get((int) class_model[index]).equals("motorcycle")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealMotorcycleHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Motorcycle ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("car")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealCarHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Car detected ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }



                                else if (labels_txt.get((int) class_model[index]).equals("truck")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealTruckHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Truck ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }



                                else if (labels_txt.get((int) class_model[index]).equals("laptop")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealLaptopHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Laptop ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("dog")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealDogHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Dog ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }
                                else if (labels_txt.get((int) class_model[index]).equals("cat")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealCatHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Cat ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }




                                else if (labels_txt.get((int) class_model[index]).equals("traffic light")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealTrafficLightsHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Traffic Light ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }



                                else if (labels_txt.get((int) class_model[index]).equals("toilet")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealToiletHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Toilet ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("couch")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealCouchHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Couch ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("chair")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealChairHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Chair ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("bed")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealBedHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Bed ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("dining table")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealDiningTableHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Dining Table ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("keyboard")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealKeyboardHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Keyboard ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }

                                else if (labels_txt.get((int) class_model[index]).equals("cell phone")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealCellphoneHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Cell phone ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }

                                else if (labels_txt.get((int) class_model[index]).equals("sink")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealSinkHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Sink ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }

                                else if (labels_txt.get((int) class_model[index]).equals("book")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealBookHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Book ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }


                                else if (labels_txt.get((int) class_model[index]).equals("cup")){


                                    float averageRealHeight = realHeightClass.calculateAverageRealCupHeight();

                                    float objectHeightInImage = (location_model[x + 2] - location_model[x]) *mutable_XY.getHeight();
                                    float objectWidthInImage = (location_model[x + 3] - location_model[x + 1]) * mutable_XY.getWidth();

                                    double distance =  (((averageRealHeight*focalLengthInPixels)/objectHeightInImage)/100);
                                    // float distance =  (averageRealBottleHeight*focalLengthInPixels)/objectHeightInImage;
                                    detectedObjectsBuilder.append("Cup ").append(DistanceClass(distance)).append(" meters away detected").append("\n");
                                }
                                else {


                                    detectedObjectsBuilder.append(labels_txt.get((int) class_model[index])).append(" detected").append("\n");
                                }


                            }
                        }
                        if (!objectsDetected) {
                            // No objects with confidence score greater than 0.55 were found
                            detectedObjectsBuilder.append("No objects detected.");
                        }

                        // Update the TextView on the UI thread
                        StringBuilder finalDetectedObjectsBuilder = detectedObjectsBuilder;
                        //textToSpeech_iB.speak(finalDetectedObjectsBuilder.toString(),TextToSpeech.QUEUE_FLUSH,null,null);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                textView.setText(finalDetectedObjectsBuilder.toString());
                                textView.setMaxLines(5); // Set maximum number of lines to 1\
                                textToSpeechHelper.speak(finalDetectedObjectsBuilder.toString());

                                //Uncomment this line for paint//
                                //imageView.setImageBitmap(mutable_XY);

                            }
                        });



                        isDetectionPaused = true;
                        handler_main.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                isDetectionPaused = false; // Resume detection after 5 seconds
                            }
                        }, 5000); // 5000 milliseconds (5 seconds)



                    }
                });
            }

        });

        cM = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    }

    public boolean onTouchEvent(MotionEvent tE){
        switch (tE.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1=tE.getX();
                y1=tE.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2=tE.getX();
                y2=tE.getY();
                if(x1>x2){
                    Intent intent1=new Intent(MainActivity.this, TextDetector.class);
                    startActivity(intent1);
                    overridePendingTransition(R.anim.sliderighti, R.anim.slidelefto);
                }else if(x1<x2){
                    Intent intent2=new Intent(MainActivity.this, TextDetector.class);
                    startActivity(intent2);
                    overridePendingTransition(R.anim.slidelefti, R.anim.sliderighto);
                }
                break;




        }
        return false;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.close();
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Stop the text-to-speech functionality
        textToSpeechHelper.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-initialize or restart the text-to-speech functionality here if needed
        textToSpeechHelper = new TextToSpeechH(this);

    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        try {
            cM.openCamera(cM.getCameraIdList()[0], new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cD = camera;

                    SurfaceTexture sTV = textureView.getSurfaceTexture();
                    Surface surface = new Surface(sTV);

                    try {


                        // Create a CaptureRequest.Builder object
                        CaptureRequest.Builder cRB = cD.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        // Find the camera cMCameraCharacteristics for the selected camera device
                        CameraCharacteristics cMCameraCharacteristics = cM.getCameraCharacteristics(cM.getCameraIdList()[0]);

                        float[] focalLengths = cMCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        float focalLengthInMillimeters = focalLengths[0]; // Choose the first focal length if multiple are available

                        float sensorSizeInMillimeters = cMCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE).getHeight();

                        Rect sensorResolution = cMCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                        int sensorHeightInPixels = sensorResolution.height();

                        focalLengthInPixels = focalLengthInMillimeters * sensorHeightInPixels / sensorSizeInMillimeters;


                        // Get the available frame rate ranges for the camera
                        Range<Integer>[] fpsRanges = cMCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

                        // Choose a desired frame rate range (e.g., 60 frames per second if available)
                        Range<Integer> desiredFpsRange = null;
                        for (Range<Integer> fpsRange : fpsRanges) {
                            if (fpsRange.getUpper() >= 60) {
                                desiredFpsRange = fpsRange;
                                break;
                            }
                        }
                        if (desiredFpsRange == null) {
                            // If 60 fps is not available, choose the highest available range
                            desiredFpsRange = fpsRanges[fpsRanges.length - 1];
                        }

                        // Set the frame rate range in the capture request builder
                        cRB.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, desiredFpsRange);

                        cRB.addTarget(surface);

                        cD.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                try {
                                    // Set the configured CaptureRequest to the session
                                    session.setRepeatingRequest(cRB.build(), null, null);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {

                            }
                        }, handler_main);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {

                }

                @Override
                public void onError(CameraDevice camera, int error) {

                }
            }, handler_main);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String DistanceClass(double distance){
        String distanceinstring="";
        if (distance<0.5){
            distanceinstring="less than 0.5";
        }
        else if(distance>=0.5 && distance<1){
            distanceinstring="Between 0.5 and 1";
        }
        else if(distance==1){
            distanceinstring="1";
        }

        else if(distance>1 && distance<2){
            distanceinstring="Between 1 and 2";
        }
        else if(distance==2){
            distanceinstring="2";
        }
        else if(distance>2 && distance<3){
            distanceinstring="Between 2 and 3";
        }
        else if(distance==3){
            distanceinstring="3";
        }
        else if(distance>3 && distance<4){
            distanceinstring="Between 3 and 4";
        }
        else if(distance==4){
            distanceinstring="4";
        }
        else if(distance>4 && distance<5){
            distanceinstring="Between 4 and 5";
        }
        else{
            distanceinstring="More than 5";
        }
        return distanceinstring;
    }


    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int code, String[] permission_request, int[] gR) {
        super.onRequestPermissionsResult(code, permission_request, gR);
        if (gR[0] != PackageManager.PERMISSION_GRANTED) {
            getPermission();
        }
    }
}