package com.example.finaltensorflowmodel3;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.media.Image;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.mlkit.vision.text.TextRecognition;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;


import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TextDetector extends AppCompatActivity {



    static final String TAG = "MainActivity";
    static final int REQUEST_CAMERA_PERMISSION = 1001;

    PreviewView previewView;
    ExecutorService cE;
    TextToSpeechH textToSpeechHelper;
    String previousText = "";
    boolean isTextDetectionAllowed = true;
    Timer textDetectionTimer = new Timer();



    TextToSpeech textToSpeech;
    float x1,x2,y1,y2;



    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.textrecognitionactivity);


        previewView = findViewById(R.id.preview_view);

        // Create an ExecutorService to handle camera operations
        cE = Executors.newSingleThreadExecutor();

        textToSpeechHelper = new TextToSpeechH(this);


        // Check camera permission and start the camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int tar = textToSpeech.setLanguage(Locale.US);
                if (tar == TextToSpeech.LANG_MISSING_DATA ||
                        tar == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TextToSpeech not supported in your device.", Toast.LENGTH_SHORT).show();
                } else {
                    // Speak "text mode on"
                    textToSpeech.speak("Text mode on", TextToSpeech.QUEUE_FLUSH, null, null);
                }
            } else {
                Toast.makeText(this, "TextToSpeech initialization failed.", Toast.LENGTH_SHORT).show();
            }
        });
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
                    Intent intent1=new Intent(TextDetector.this,MainActivity.class);
                    startActivity(intent1);
                    overridePendingTransition(R.anim.sliderighti, R.anim.slidelefto);
                }else if(x1<x2){
                    Intent intent2=new Intent(TextDetector.this,MainActivity.class);
                    startActivity(intent2);
                    overridePendingTransition(R.anim.sliderighti, R.anim.slidelefto);
                }
                break;




        }
        return false;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cE.shutdown();

        textToSpeechHelper.shutdown();

    }

    // Method to start the camera and set up image analysis
    private void startCamera() {
        // Get an instance of ProcessCameraProvider
        ListenableFuture<ProcessCameraProvider> cPF = ProcessCameraProvider.getInstance(this);
        cPF.addListener(() -> {
            try {
                // Get the ProcessCameraProvider object
                ProcessCameraProvider cP = cPF.get();

                // Set up the Preview use case
                Preview preview_builder = new Preview.Builder().build();

                preview_builder.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up the ImageAnalysis use case with the TextAnalyzer
                ImageAnalysis iA = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                iA.setAnalyzer(cE, new TextAnalyzer());

                // Set the camera selector to the back camera
                CameraSelector cS = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                // Bind the use cases to the lifecycle of the activity
                cP.bindToLifecycle((LifecycleOwner) this, cS, preview_builder, iA);

                // Draw bounding boxes on the preview_builder view
                preview_builder.setSurfaceProvider(previewView.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Custom class implementing ImageAnalysis.Analyzer to perform text recognition
    public class TextAnalyzer implements ImageAnalysis.Analyzer {
        public TextRecognizer textRecognizer;
        private boolean isTextRecognized = false;

        public TextAnalyzer() {
            // Create a TextRecognizer using default options
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        }

        @Override
        @ExperimentalGetImage
        public void analyze(@NonNull ImageProxy iP) {
            // Get the media Image from the ImageProxy
            if (!isTextDetectionAllowed) {
                iP.close();
                return;
            }

            Image mI = iP.getImage();

            if (mI != null) {
                // Create an InputImage from the media Image
                InputImage iI = InputImage.fromMediaImage(mI, iP.getImageInfo().getRotationDegrees());

                // Process the input image using the TextRecognizer
                textRecognizer.process(iI)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text recognized_text) {
                                processTextRecognitionResult(recognized_text);
                                iP.close();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Text recognition error: " + e.getMessage());
                                iP.close();
                            }
                        });
            }
        }
    }

    public String processTextRecognitionResult(Text recognized_text) {
        StringBuilder output = new StringBuilder();
        isTextDetectionAllowed = false;

        // Iterate over the detected recognized_text blocks and lines
        // Set a limit on the number of words to detect
        int maxWordsToDetect = 10; // Change this value to the desired limit

        // Iterate over the detected recognized_text blocks and lines
        int wordsDetected = 0;
        for (Text.TextBlock block : recognized_text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                String[] words = line.getText().split("\\s+");
                for (String word : words) {
                    output.append(word).append(" ");
                    wordsDetected++;
                    if (wordsDetected >= maxWordsToDetect) {
                        break; // Break the loop if the maximum words limit is reached
                    }
                }
                if (wordsDetected >= maxWordsToDetect) {
                    break; // Break the loop if the maximum words limit is reached
                }
            }
            if (wordsDetected >= maxWordsToDetect) {
                break; // Break the loop if the maximum words limit is reached
            }
        }
        textDetectionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                isTextDetectionAllowed = true;
            }
        }, 5000);

        // Get the new recognized_text and check if it's significantly different from the previous recognized_text
        final String newText = output.toString().trim();
         // Modify the length requirement as needed


            runOnUiThread(() -> {
                TextView textView = findViewById(R.id.text_view);
                if (newText.isEmpty()) {
                    textView.setText(""); // Clearing the TextView if there is no recognized_text
                } else {
                    textView.setText("Text Detected: " + newText);
                    textView.setMaxLines(5); // Set maximum number of lines to 1
                     // Add ellipsis at the end if recognized_text is too long


                    textToSpeechHelper.speak("Text Detected: " + newText);
                }
            });
            return newText;


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



    // Method to handle the camera permission request result
    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] permission_request, @NonNull int[] gR) {
        super.onRequestPermissionsResult(code, permission_request, gR);
        if (code == REQUEST_CAMERA_PERMISSION) {
            if (gR.length > 0 && gR[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
            }
        }
    }


}
