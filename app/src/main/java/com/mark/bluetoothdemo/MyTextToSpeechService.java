package com.mark.bluetoothdemo;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created on 2017/4/27
 *
 * @author Mark Hsu
 */

class MyTextToSpeechService {
    private final static String TAG = "MyTextToSpeechService";
    private TextToSpeech mTts;

    MyTextToSpeechService(Context context) {
        TextToSpeech.OnInitListener mInitListener = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
                if (status == TextToSpeech.SUCCESS) {
                    // Set preferred language to US english.
                    // Note that a language may not be available, and the result will indicate this.
                    int result = mTts.setLanguage(Locale.CHINESE);
                    // Try this someday for some interesting results.
                    // int result mTts.setLanguage(Locale.FRANCE);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Language data is missing or the language is not supported.
                        Log.e(TAG, "Language is not available.");
                    } else {
                        // Check the documentation for other possible result codes.
                        // For example, the language may be available for the locale,
                        // but not for the specified country and variant.

                        // The TTS engine has been successfully initialized.
                        Log.d(TAG, "The TTS engine has been successfully initialized.");
                    }
                } else {
                    // Initialization failed.
                    Log.e(TAG, "Could not initialize TextToSpeech.");
                }
            }
        };
        mTts = new TextToSpeech(context, mInitListener);
    }

    void speak(String content) {
        // min API Level 15 只能使用此 method >>> speak(String text, int queueMode, HashMap<String, String> params)
        mTts.speak(content, TextToSpeech.QUEUE_ADD,  // Drop all pending entries in the playback queue.
                null);
    }

    void destroy() {
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
    }
}
