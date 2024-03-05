package ru.lomov.mobileserv;

import static android.Manifest.permission.RECORD_AUDIO;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private String stringUrlEndPoint = "https://api.deepseek.com/v1/chat/completions";
    private String stringApiKey = "sk-6a30b7d345954eb5ba94edb6ac71b7a4";
    private String stringOutput = "";

    private Button buttonAssist;
    private TextView textView;
    private EditText editText;

    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActivityCompat.requestPermissions(this,
                new String[]{RECORD_AUDIO},
                PackageManager.PERMISSION_GRANTED);
        buttonAssist = findViewById(R.id.button);
        textView = findViewById(R.id.text_view);
        editText = findViewById(R.id.editText);

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            final Locale locale = new Locale("RU");
            @Override
            public void onInit(int i) {
                textToSpeech.setSpeechRate((float) 0.8);
            }
        });

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        Intent serviceIntent = new Intent(this, SpeechRecognitionService.class);
        startService(serviceIntent);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                textView.setText("");
                if (matches != null) {
                    string = matches.get(0);
                    editText.setText(string);
                    deepSeekModel(string);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
    }

    public void buttonAssist(View view) {
        if(textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            return;
        }
        stringOutput = "";
        speechRecognizer.startListening(intent);

    }
    private void deepSeekModel(String stringInput) {
        textView.setText("Сию минуту, Сэр!");
        textToSpeech.speak("Сию минуту, Сэр!", TextToSpeech.QUEUE_FLUSH, null,null);

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("model", "deepseek-chat");

            JSONArray jsonArrayMessage = new JSONArray();
            JSONObject jsonObjectMessage = new JSONObject();
            jsonObjectMessage.put("role", "user");
            jsonObjectMessage.put("content", stringInput);
            jsonArrayMessage.put(jsonObjectMessage);

            jsonObject.put("messages", jsonArrayMessage);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                stringUrlEndPoint, jsonObject, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String stringText = null;
                try {
                    stringText = response.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                stringOutput = stringOutput + stringText;
                textView.setText(stringOutput);
                textToSpeech.speak(stringOutput, TextToSpeech.QUEUE_FLUSH, null,null);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> mapHeader = new HashMap<>();
                mapHeader.put("Authorization", "Bearer " + stringApiKey);
                mapHeader.put("Content-Type", "application/json");

                return mapHeader;
            }

            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                return super.parseNetworkResponse(response);
            }
        };

        int intTimeoutPeriod = 60000; // 60 seconds timeout duration defined
        RetryPolicy retryPolicy = new DefaultRetryPolicy(intTimeoutPeriod,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent serviceIntent = new Intent(this, SpeechRecognitionService.class);
        stopService(serviceIntent);
    }
}