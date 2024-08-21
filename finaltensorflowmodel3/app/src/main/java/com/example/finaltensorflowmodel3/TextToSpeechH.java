package com.example.finaltensorflowmodel3;
import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class TextToSpeechH {

    private TextToSpeech textToSpeech;

    public TextToSpeechH(Context context) {
        textToSpeech = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                textToSpeech.setLanguage(Locale.US);
                float speechRate = 2.0f; // Adjust this value as needed (1.0 is the default)
                textToSpeech.setSpeechRate(speechRate);
            }
        });
    }

    public void speak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void shutdown() {
        textToSpeech.stop();
        textToSpeech.shutdown();
    }


}
