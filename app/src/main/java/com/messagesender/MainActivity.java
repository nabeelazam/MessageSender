package com.messagesender;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.messageservice.IConsumeMessage;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MessageResultReceiver.Receiver {

    private static final String TAG = "MainActivity";
    private EditText etMessage;
    private Button btnSend;
    private MessageConnection serviceConnection;
    private IConsumeMessage iConsumeMessage;
    private MessageResultReceiver mReceiver;
    private ProgressBar progressBar;

    final static String KEY_FROM_CLIENT = "fromClient";
    final static int TTS_INIT_ERROR = 0, TTS_LANG_NOT_SUPPORTED = -1, TTS_COMPLETED = 200;
    final static String STR_SERVICE_PACKAGE = "com.messageservice", STR_SERVICE_ACTION = "service.message";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etMessage = (EditText) findViewById(R.id.etMessage);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnSend = (Button) findViewById(R.id.btnSendMessage);
        btnSend.setOnClickListener(this);

        mReceiver = new MessageResultReceiver(new Handler());
        mReceiver.setReceiver(this);

        // Start Speech Service
        startSpeechService();

        // Call init
        initializeServiceConnection();
    }

    /**
     * Function that converts receiver to parsable so that it can be sent to remote service
     *
     * @param actualReceiver
     * @return
     */
    public static ResultReceiver receiverForSending(ResultReceiver actualReceiver) {
        Parcel parcel = Parcel.obtain();
        actualReceiver.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }

    /**
     * Service connection Initialization along with binding
     */
    private void initializeServiceConnection() {
        serviceConnection = new MessageConnection();
        if (iConsumeMessage == null) {
            Intent intent = new Intent();
            intent.setPackage(STR_SERVICE_PACKAGE);
            intent.setAction(STR_SERVICE_ACTION);

//            ResultReceiver tempReceiver = receiverForSending(mReceiver);
//            intent.putExtra(KEY_FROM_CLIENT, tempReceiver);

            // binding to remote service
            bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnSendMessage:

                // Call hide soft keyboard
                hideSoftKeyboard(etMessage, MainActivity.this);

                try {
                    if (iConsumeMessage != null) {
                        String text = etMessage.getText().toString().trim();
                        if (text.length() > 0) {
                            btnSend.setClickable(false);
                            progressBar.setVisibility(View.VISIBLE);
                            ResultReceiver tempReceiver = receiverForSending(mReceiver);
                            iConsumeMessage.speakTextMessage(etMessage.getText().toString(), tempReceiver);
                        } else {
                            Toast.makeText(MainActivity.this, "Please provide some text to Speak", Toast.LENGTH_SHORT).show();
                        }

                    } else {
                        Toast.makeText(MainActivity.this, "Service not available.", Toast.LENGTH_SHORT).show();

                        // Try again if service application is installed later
                        startSpeechService();

                        // Call init
                        initializeServiceConnection();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                break;
            default:
                break;
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {

        Log.e(TAG, "onReceiveResult called");

        btnSend.setClickable(true);

        if (resultCode == TTS_COMPLETED) {
            Toast.makeText(MainActivity.this, "Sound Play completed", Toast.LENGTH_SHORT).show();
        } else if (resultCode == TTS_INIT_ERROR) {
            Toast.makeText(MainActivity.this, "TTS Initialization failed.", Toast.LENGTH_SHORT).show();
        } else if (resultCode == TTS_LANG_NOT_SUPPORTED) {
            Toast.makeText(MainActivity.this, "Mentioned language not supported by TTS.", Toast.LENGTH_SHORT).show();
        }

        // Remove progress indicator
        progressBar.setVisibility(View.GONE);
    }


    /**
     * inner class for Service Connection management
     */
    class MessageConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            iConsumeMessage = IConsumeMessage.Stub.asInterface(service);
            Log.e(TAG, "onServiceConnected : Connected");
            Toast.makeText(MainActivity.this, "Service Connected", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            iConsumeMessage = null;
            Log.e(TAG, "onServiceDisconnected : Disconnected");
            Toast.makeText(MainActivity.this, "Service Disconnected", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        stopSpeechService();
    }


    /**
     * Starting service
     */
    private void startSpeechService() {
        Log.e(TAG, "startSpeechService : Service Started");
        Intent intent = new Intent();
        intent.setPackage(STR_SERVICE_PACKAGE);
        intent.setAction(STR_SERVICE_ACTION);
        startService(intent);
    }

    /**
     * Stopping service
     */
    private void stopSpeechService() {
        Log.e(TAG, "startSpeechService : Service Stopped");
        Intent intent = new Intent();
        intent.setPackage(STR_SERVICE_PACKAGE);
        intent.setAction(STR_SERVICE_ACTION);
        stopService(intent);
    }


    /**
     * Hiding soft keyboard
     *
     * @param editText
     * @param context
     */
    private void hideSoftKeyboard(EditText editText, Context context) {
        InputMethodManager mgr = (InputMethodManager) context
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
