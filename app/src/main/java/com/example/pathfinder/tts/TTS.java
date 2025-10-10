package com.example.pathfinder.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

public class TTS implements TTSInterface, TextToSpeech.OnInitListener {
    public static final String TAG = "TTS";

    public static final int SUCCESS = 0;
    public static final int NOT_INITIALIZED = -1;
    public static final int SPEAK_FAILED = -2;
    public static final int TEXT_TOO_LONG = 1;
    public static final int TEXT_EMPTY = 2;

    private final TextToSpeech tts;
    private boolean isInitialized = false;

    public TTS(Context context) {
        tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to Brazilian Portuguese
            int result = tts.setLanguage(Locale.forLanguageTag("pt-BR"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language not supported
                Log.e(TAG, "Language not supported: pt-BR");
            } else {
                Log.i(TAG, "Initialization successful");
                isInitialized = true;
            }
        } else {
            Log.e(TAG, "Initialization failed");
        }
    }

    @Override
    public int speak(String text) {
        Log.d("TTS", String.format("speak called with text: '%s'", text));
        if (text == null || text.isEmpty())
            return TEXT_EMPTY;
        if (text.length() > TextToSpeech.getMaxSpeechInputLength())
            return TEXT_TOO_LONG;
        if (!isInitialized)
            return NOT_INITIALIZED;

        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        if (result == TextToSpeech.ERROR)
            return SPEAK_FAILED;
        return SUCCESS;
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
