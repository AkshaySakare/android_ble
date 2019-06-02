package myapplication.aps.com.myapplication;

import android.app.Activity;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class CommunicateActivity extends AppCompatActivity {

    private TextView mConnectionText, mMessagesView;
    private EditText mTextBox;
    GPSTracker gpsTracker;
    private String mobileNumber;
    private Button mSendButton, mConnectButton;
    String stringLatitude = "", stringLongitude = "", nameOfLocation="";
    private CommunicateViewModel viewModel;

    public static final String PHONE = "000";
    private static final String PREFS_NAME = "preferenceName";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup our activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communicate);

        // Enable the back button in the action bar if possible
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        gpsTracker=new GPSTracker(CommunicateActivity.this);


        // Setup our ViewModel
        viewModel = ViewModelProviders.of(this).get(CommunicateViewModel.class);

        // This method return false if there is an error, so if it does, we should close.
        if (!viewModel.setupViewModel(getIntent().getStringExtra("device_name"), getIntent().getStringExtra("device_mac"))) {
            finish();
            return;
        }

        // Setup our Views
        mConnectionText = findViewById(R.id.communicate_connection_text);
        mMessagesView = findViewById(R.id.communicate_messages);
        mTextBox = findViewById(R.id.communicate_message);
        mSendButton = findViewById(R.id.communicate_send);
        mConnectButton = findViewById(R.id.communicate_connect);
        mTextBox.setText(getPreference(CommunicateActivity.this,PHONE));
if (mTextBox.getText().toString().equals("000")){
    mTextBox.setText("");
}
        // Start observing the data sent to us by the ViewModel
        viewModel.getConnectionStatus().observe(this, this::onConnectionStatus);
        viewModel.getDeviceName().observe(this, name -> setTitle(getString(R.string.device_name_format, name)));
        viewModel.getMessages().observe(this, message -> {
            if (TextUtils.isEmpty(message)) {
                message = getString(R.string.no_messages);
            }
            mMessagesView.setText("\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);
        });
        viewModel.getMessage().observe(this, message -> {
            // Only update the message if the ViewModel is trying to reset it

        });
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        stringLatitude= intent.getStringExtra(LocationMonitoringService.EXTRA_LATITUDE);
                        stringLongitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LONGITUDE);
                        if (stringLatitude != null && stringLongitude != null) {
                            mMessagesView.setText("\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);

                        }
                    }
                }, new IntentFilter(LocationMonitoringService.ACTION_LOCATION_BROADCAST)
        );
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mobileNumber= mTextBox.getText().toString();
                if (mobileNumber.length()<10){
                    Toast.makeText(getBaseContext(), "Check mobile number ...", Toast.LENGTH_SHORT).show();

                }else if(mobileNumber.length()==10)
            {
                Toast.makeText(getBaseContext(), "Number save ...", Toast.LENGTH_SHORT).show();

                setPreference(CommunicateActivity.this,PHONE,mobileNumber);}
            }
        });
        // Setup the send button click action
      //  mSendButton.setOnClickListener(v -> viewModel.sendMessage(mTextBox.getText().toString()));
    }

    // Called when the ViewModel updates us of our connectivity status
    private void onConnectionStatus(CommunicateViewModel.ConnectionStatus connectionStatus) {
        switch (connectionStatus) {
            case CONNECTED:
                mConnectionText.setText(R.string.status_connected);
                mTextBox.setEnabled(false);
                mSendButton.setEnabled(false);
                mConnectButton.setEnabled(true);
                sendSMS(getPreference(CommunicateActivity.this,PHONE),"Bluetooth is CONNECTED & location is"+"\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);
                mConnectButton.setText(R.string.disconnect);
                mConnectButton.setOnClickListener(v -> viewModel.disconnect());
                break;
            case ERROR:
                mConnectionText.setText(R.string.status_disconnected);
                mTextBox.setEnabled(false);
                mSendButton.setEnabled(false);
                mConnectButton.setEnabled(true);
                sendSMS(getPreference(CommunicateActivity.this,PHONE),"Bluetooth is DISCONNECTED & location is"+"\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);
                mConnectButton.setText(R.string.disconnect);
                        mConnectButton.setText(R.string.connect);
                mConnectButton.setOnClickListener(v -> viewModel.connect());
                break;
            case CONNECTING:
                mConnectionText.setText(R.string.status_connecting);
                mTextBox.setEnabled(false);
                mSendButton.setEnabled(false);
                mConnectButton.setEnabled(false);
                mConnectButton.setText(R.string.connect);
                break;

            case DISCONNECTED:
                mConnectionText.setText(R.string.status_disconnected);
                mTextBox.setEnabled(true);
                mSendButton.setEnabled(true);
                mConnectButton.setEnabled(true);

                mConnectButton.setText(R.string.connect);
                mConnectButton.setOnClickListener(v -> viewModel.connect());
                break;
        }
    }

    // Called when a button in the action bar is pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case android.R.id.home:
                // If the back button was pressed, handle it the normal way
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Called when the user presses the back button
    @Override
    public void onBackPressed() {
        // Close the activity
        finish();
    }

    public void sendSMS(String phoneNumber, String message) {
        ArrayList<PendingIntent> sentPendingIntents = new ArrayList<PendingIntent>();
        ArrayList<PendingIntent> deliveredPendingIntents = new ArrayList<PendingIntent>();
        PendingIntent sentPI = PendingIntent.getBroadcast(CommunicateActivity.this, 0,
                new Intent(CommunicateActivity.this, SmsSentReceiver.class), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(CommunicateActivity.this, 0,
                new Intent(CommunicateActivity.this, SmsDeliveredReceiver.class), 0);
        try {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> mSMSMessage = sms.divideMessage(message);
            for (int i = 0; i < mSMSMessage.size(); i++) {
                sentPendingIntents.add(i, sentPI);
                deliveredPendingIntents.add(i, deliveredPI);
            }
            sms.sendMultipartTextMessage(phoneNumber, null, mSMSMessage,
                    sentPendingIntents, deliveredPendingIntents);
            Toast.makeText(getBaseContext(), "SMS sending ...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "SMS sending failed...", Toast.LENGTH_SHORT).show();
        }

    }
    public class SmsDeliveredReceiver extends BroadcastReceiver {

        private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // ... //Device found
                }
                else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    //... //Device is now connected
                    Log.e("connected","-------------------->");
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //... //Done searching
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                    //... //Device is about to disconnect
                    Log.e("about to disconnect","-------------------->");
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    //... //Device has disconnected
                    Log.e("DISCONNECTED","-------------------->");

                }
            }
        };
        @Override
        public void onReceive(Context context, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "SMS delivered", Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(context, "SMS not delivered", Toast.LENGTH_SHORT).show();
                    break;
            }
        }}
// sms send
    public class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "SMS Sent", Toast.LENGTH_SHORT).show();

                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(context, "SMS generic failure", Toast.LENGTH_SHORT)
                            .show();

                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(context, "SMS no service", Toast.LENGTH_SHORT)
                            .show();

                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(context, "SMS null PDU", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(context, "SMS radio off", Toast.LENGTH_SHORT).show();
                    break;
            }
        }}
    public static boolean setPreference(Context context, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    public static String getPreference(Context context, String key) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return settings.getString(key, "000");
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.e("keycode","-------------->"+keyCode);
        Log.e("keyevent","-------------->"+event);
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyDown(int code, KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_POWER) {
        sendSMS(getPreference(CommunicateActivity.this,PHONE),"Urgently help me & location is"+"\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);

            // Your Logic Is Here
            return true;
        }
        return super.onKeyDown(code, keyEvent);
    }
    public void onCloseSystemDialogs(String reason) {
        if ("globalactions".equals(reason)) {
            Log.i("Key", "Long press on power button");
                        sendSMS(getPreference(CommunicateActivity.this,PHONE),"Urgently help me & location is"+"\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);


        } else if ("homekey".equals(reason)) {
            //home key pressed
        } else if ("recentapss".equals(reason)) {
            // recent apps button clicked
        }
    }
    /* use for the downkey send sms*/
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                || event.getKeyCode() == KeyEvent.KEYCODE_CAMERA
                || event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
            Log.i("Key", "keycode " + event.getKeyCode());
            if (event.getKeyCode()== KeyEvent.KEYCODE_VOLUME_DOWN){
                sendSMS(getPreference(CommunicateActivity.this,PHONE),"Urgently help me & location is"+"\n Latitude : " + stringLatitude + "\n Longitude: " + stringLongitude);

            }
        }
        return super.dispatchKeyEvent(event);
    }
}
