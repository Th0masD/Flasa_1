package com.example.flasa;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;


import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.example.flasa.Constants.BATTERY_LEVEL_SERVICE_UUID;
import static com.example.flasa.Constants.BATTERY_LEVEL_CHARACTERISTIC_UUID;

import static com.example.flasa.Constants.HEART_RATE_SERVICE_UUID;
import static com.example.flasa.Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID;
import static com.example.flasa.Constants.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID;

import static java.lang.Math.abs;


/**
 *
 *
 This activity represents the Peripheral/Server role.
 Bluetooth communication flow:
 1. advertise [peripheral]
 2. scan [central]
 3. connect [central]
 4. notify [peripheral]
 5. receive [central]
 */
public class PeripheralRoleActivity extends BluetoothActivity implements GPSManager7.GPSListener,View.OnClickListener, SensorEventListener {

    private BluetoothGattService mHeartRateService;
    public BluetoothGattCharacteristic mBodySensorCharacteristic;
    public BluetoothGattCharacteristic mHeartRateMeasureCharacteristic;

    private BluetoothGattService mBaterkaLevelService;
    public BluetoothGattCharacteristic mBaterkaLevelCharacteristic;



    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private HashSet<BluetoothDevice> mBluetoothDevices;

    private Button mNotifyButton;
    private Switch mEnableAdvertisementSwitch;
    //tomas
    public TextView mMobilCislo1;
    private TextView xValue;
    private TextView xValueZaciatocna;
    private TextView yValue;
    private TextView yValueZaciatocna;
    private TextView zValue;
    private TextView zValueZaciatocna;
    private TextView mBaterka;

    private Button mZavolaCislo1;
    public String mobilcislo;
    public String mPrikaz;
    public String mStav;
    public boolean ZapVyp = false;
    public boolean TestNaklonu = false;
    public boolean PrvyKrat = true;

    private TextView txtViewLatGPS;
    private TextView txtViewLongGPS;
    private LocationManager mLocationManagerGPS;
    private LocationListener mLocationListenerGPS;

    private MediaPlayer mediaPlayer;

    double lat;
    double lon;
    float accuracy;
    float speed;

    float zmenaY;
    float zmenaYzaciatocna;

    float zmenaX;
    float zmenaXzaciatocna;

    float zmenaZ;
    float zmenaZzaciatocna;

    int pocitadlo_nakolnu = 5;  //Pocita kolko krat sa zavola gyroskop
    private SensorManager sensorManager;
    Sensor acclerometer;
    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
    private boolean sensorEnabled = false; //Zapinanie vypinavie senzoru


    //tomas
    Timer timer;
    TimerTask timerTask;

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();

    private GPSManager7 gpsManager;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mMobilCislo1 = (TextView) findViewById(R.id.mobil_cislo_1);
        xValue = (TextView) findViewById(R.id.xValue);
        xValueZaciatocna = (TextView) findViewById(R.id.xValueZaciatocna);

        mBaterka = (TextView) findViewById(R.id.txtBaterka);

//        mEnableAdvertisementSwitch.setOnClickListener(this);
        //tomas
//        mZavolaCislo1 = (Button) findViewById(R.id.zavola_cislo1);
//        mZavolaCislo1.setOnClickListener(this);


        txtViewLatGPS = findViewById(R.id.txtViewLatGPS);
        txtViewLongGPS = findViewById(R.id.txtViewLonGPS);


        setGattServer();
        setBluetoothService();

        //tomas

        gpsManager = new GPSManager7(this, this);
     //   gpsManager.start();

        //Senzory inicializacia
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        acclerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(PeripheralRoleActivity.this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);


        startAdvertising();


    }

    //tomas
    @Override
    protected void onResume() {

        super.onResume();
        if (sensorManager != null && acclerometer != null) {
            sensorManager.registerListener(this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //tomas
        //onResume we start our timer so it can start when the app comes from the background
        //
        //  startTimer();
        //  startAdvertising();
    }

    @Override
    protected void onPause() {
        super.onPause();

        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        //stoptimertask();

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        Toast.makeText(PeripheralRoleActivity.this, "Restart", Toast.LENGTH_LONG).show();
        //sensorManager.unregisterListener(PeripheralRoleActivity.this);
    }


    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        gpsManager.stop();
    }



    @Override
    public void onGPSStatusChanged(String status) {
        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_LONG).show();

        if (status == "FIRST_FIX")
        {
            mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.gps);
            mediaPlayer.start();

            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
            }
        }

    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d("GPS", "Lat: " + location.getLatitude() +
                " Lon: " + location.getLongitude());
        txtViewLatGPS.setText(Double.toString(location.getLatitude()));
        txtViewLongGPS.setText(Double.toString(location.getLongitude()));
        lat       = location.getLatitude();
        lon       = location.getLongitude();
        accuracy  = location.getAccuracy();
        speed     = location.getSpeed();

/*
            mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.sms);
            mediaPlayer.start();

            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
            }

        */
    }




    @Override
    protected int getLayoutId() {
        return R.layout.activity_peripheral_role;
    }


    @Override
    public void onClick(View view) {


        int format = -1;

        int id = view.getId();


/*
        if (id == R.id.zavola_cislo1) {

            airplaneOff(view);

            new Handler().postDelayed(() -> {

                if (isMobileNetworkAvailable()) {
                    zavolajCislo();
                } else {
                    Toast.makeText(PeripheralRoleActivity.this,
                            "Neni siet",
                            Toast.LENGTH_LONG).show();
                }

            }, 10000);
        }
*/
    }




    private int getBatteryVoltage() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);

        if (bm == null) {
            return -1;
        }

        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }





    //tomas
    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 1000, 3000); //
    }

    public void initializeTimerTask() {
        final Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {


                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));

                        } else {
                            //deprecated in API 26
                            v.vibrate(100);

                        }
                    }
                });
            }
        };
    }


    //tomas

    public void airplaneOn(View v) {

        try {

            Process process = Runtime.getRuntime().exec("su");

            DataOutputStream os =
                    new DataOutputStream(process.getOutputStream());

            os.writeBytes(
                    "settings put global airplane_mode_on 1\n");

            os.writeBytes(
                    "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true\n");

            os.writeBytes("exit\n");
            os.flush();

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void airplaneOff(View v) {

        try {

            Process process = Runtime.getRuntime().exec("su");

            DataOutputStream os =
                    new DataOutputStream(process.getOutputStream());

            os.writeBytes(
                    "settings put global airplane_mode_on 0\n");

            os.writeBytes(
                    "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false\n");

            os.writeBytes("exit\n");
            os.flush();

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void startSensor() {
        if (!sensorEnabled && sensorManager != null && acclerometer != null) {
            sensorManager.registerListener(PeripheralRoleActivity.this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorEnabled = true;
        }
    }

    private void stopSensor() {
        if (sensorEnabled && sensorManager != null) {
            sensorManager.unregisterListener(PeripheralRoleActivity.this);
            sensorEnabled = false;
        }
    }


    // kontrola mobilnej siete
    private boolean isMobileNetworkAvailable() {

        TelephonyManager tm =
                (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        return tm != null
                && tm.getNetworkOperator() != null
                && !tm.getNetworkOperator().isEmpty();
    }



    private void zavolajCislo() {

        MyPhoneListener phoneListener = new MyPhoneListener(this);

        TelephonyManager telephonyManager =
                (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        // ✅ kontrola permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    1);
            return;
        }

        // ✅ už máš permission → volať
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + mobilcislo));
        startActivity(callIntent);
    }




    private class MyPhoneListener extends PhoneStateListener {

        private boolean outgoingCall = false;
        private boolean callAnswered = false;
        private Context context;

        public MyPhoneListener(Context ctx) {
            this.context = ctx;
        }

        public void setOutgoingCall(boolean outgoing) {
            this.outgoingCall = outgoing;
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {

                case TelephonyManager.CALL_STATE_RINGING:
                    Toast.makeText(context, "RINGING", Toast.LENGTH_SHORT).show();
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:

                    Toast.makeText(context, "OFFHOOK", Toast.LENGTH_SHORT).show();

                    // hovor bol zdvihnutý alebo pripojený
                    callAnswered = true;

                /*
                new Handler(Looper.getMainLooper()).postDelayed(() -> {

                    AudioManager audioManager =
                            (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                    audioManager.setSpeakerphoneOn(true);

                }, 500);
                */

                    break;

                case TelephonyManager.CALL_STATE_IDLE:

                    Toast.makeText(context, "IDLE", Toast.LENGTH_SHORT).show();

                    if (outgoingCall) {

                        if (callAnswered) {
                            Toast.makeText(context,
                                    "Hovor ukončený (bol zdvihnutý)",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context,
                                    "Nebol zdvihnutý",
                                    Toast.LENGTH_LONG).show();
                        }
                    }

                    // reset premenných
                    outgoingCall = false;
                    callAnswered = false;

                    break;
            }
        }
    }





    //tomas
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        xValue.setText("Value: " + String.format("%.1f  %.1f  %.1f", sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
        zmenaX = sensorEvent.values[0];
        zmenaY = sensorEvent.values[1];
        zmenaZ = sensorEvent.values[2];


        if (PrvyKrat == true)
        {
            zmenaXzaciatocna = sensorEvent.values[0];
            zmenaYzaciatocna = sensorEvent.values[1];
            zmenaZzaciatocna = sensorEvent.values[2];
            xValueZaciatocna.setText("Zacia: " + String.format("%.1f  %.1f  %.1f", sensorEvent.values[0],sensorEvent.values[1],sensorEvent.values[2]));
            PrvyKrat = false;
        }


        if (((abs(zmenaXzaciatocna-zmenaX)>1) || (abs(zmenaYzaciatocna-zmenaY)>1) || (abs(zmenaZzaciatocna-zmenaZ)>1)) && TestNaklonu)
        {
            //      ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            //toneG.startTone(ToneGenerator.TONE_CDMA_DIAL_TONE_LITE, 100);
            if(pocitadlo_nakolnu==0) {
                notifyCharacteristicChanged();
                pocitadlo_nakolnu=3;
            }
            pocitadlo_nakolnu--;
            //  sensorManager.unregisterListener(PeripheralRoleActivity.this);
        }

        if (((abs(zmenaXzaciatocna-zmenaX)>1) || (abs(zmenaYzaciatocna-zmenaY)>1) || (abs(zmenaZzaciatocna-zmenaZ)>1)) && ZapVyp)
        {
            //      ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            zavolajCislo();
            //  SendSMS();
            //  sensorManager.unregisterListener(PeripheralRoleActivity.this);
        }



    }

    //  @Override
    public void SendSMS() {

        //  double lat       = location.getLatitude();
        //  double lon       = location.getLongitude();
        //  float  accuracy  = location.getAccuracy();
        //  float  speed     = location.getSpeed();
        //  String direction = getDirection(location);

        SmsManager sms   = SmsManager.getDefault();

        ArrayList<String> messages = new ArrayList<String>();

        messages.add(
                truncateSmsMessage(
                        sms,
                        String.format(Locale.US, "Current Location:\n  Lat: %1$s\n  Lon: %2$s\n  Accuracy (meters): %3$s\n  Speed (meters/sec): %4$s", lat, lon, accuracy, speed)
                )
        );
        messages.add(
                truncateSmsMessage(sms, String.format(Locale.US, "Google Maps:\nhttps://maps.google.com/?q=%1$s,%2$s", lat, lon)
                )
        );

        //    Log.i(TAG, "GPS location sent.\nto: " + mobilcislo + "\n" + messages.get(0) + "\n" + messages.get(1));

        sms.sendMultipartTextMessage(mobilcislo, null, messages, null, null);

    }

    private static String truncateSmsMessage(SmsManager sms, String message) {
        ArrayList<String> parts = sms.divideMessage(message);
        return parts.get(0);
    }



    @Override
    protected int getTitleString() {
        return R.string.peripheral_screen;
    }


    /**
     * Starts BLE Advertising by starting {@code PeripheralAdvertiseService}.
     */
    private void startAdvertising() {
        // TODO bluetooth - maybe bindService? what happens when closing app?
        startService(getServiceIntent(this));
    }


    /**
     * Stops BLE Advertising by stopping {@code PeripheralAdvertiseService}.
     */
    private void stopAdvertising() {
        stopService(getServiceIntent(this));
        mEnableAdvertisementSwitch.setChecked(false);
    }

    private void setGattServer() {

        mBluetoothDevices = new HashSet<>();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (mBluetoothManager != null) {
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        } else {
            showMsgText(R.string.error_unknown);
        }
    }

    private void setBluetoothService() {

        // create the Service
        mHeartRateService = new BluetoothGattService(HEART_RATE_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        mBaterkaLevelService = new BluetoothGattService(BATTERY_LEVEL_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        /*
        create the Characteristic.
        we need to grant to the Client permission to read (for when the user clicks the "Request Characteristic" button).
        no need for notify permission as this is an action the Server initiate.
         */

        int permission = BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ;
        int property = BluetoothGattCharacteristic.PROPERTY_WRITE|BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE;


        //mBodySensorCharacteristic = new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE,  BluetoothGattCharacteristic.PERMISSION_WRITE);

    //    mBodySensorCharacteristic = new BluetoothGattCharacteristic(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID, property,  permission);
        mHeartRateMeasureCharacteristic = new BluetoothGattCharacteristic(HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID, property,  permission);
        mBaterkaLevelCharacteristic = new BluetoothGattCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID, property,  permission);

        mBodySensorCharacteristic =
                new BluetoothGattCharacteristic(
                        BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID,
                        property,
                        permission
                );


        // add the Characteristic to the Service
        mHeartRateService.addCharacteristic(mBodySensorCharacteristic);
        mHeartRateService.addCharacteristic(mHeartRateMeasureCharacteristic);
        mBaterkaLevelService.addCharacteristic(mBaterkaLevelCharacteristic);



        // add the Service to the Server/Peripheral
        //Oneskorenie pri pridavani servisu

        mGattServer.clearServices();

        mGattServer.addService(mHeartRateService);

        new Handler().postDelayed(() ->
                        mGattServer.addService(mBaterkaLevelService),
                100);



        //Zisti napatie baterie v percentach 0-100.
        int batteryPercent = getBatteryVoltage();
        //Zapise do charkteristiky
        mBaterkaLevelCharacteristic.setValue(batteryPercent, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        //Nacita z charkteristiky
        batteryPercent = mBaterkaLevelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        Log.v(MainActivity.TAG, "Battery = " + batteryPercent + "%");
        //mBaterka.setText(String.format("%d", batteryPercent));
        mBaterka.setText(String.valueOf(batteryPercent));

        mBodySensorCharacteristic.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mHeartRateMeasureCharacteristic.setValue(10, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
    }




    /*
    update the value of Characteristic.
    the client will receive the Characteristic value when:
        1. the Client user clicks the "Request Characteristic" button
        2. teh Server user clicks the "Notify Client" button

    value - can be between 0-255 according to:
    https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_sensor_location.xml
     */
    private void setCharacteristic(int checkedId) {
        /*
        done each time the user changes a value of a Characteristic
         */
    }

    private byte[] getValue(int value) {
        return new byte[]{(byte) value};
    }



    private void notifyCharacteristicChanged() {

    //Indicate potvrdenie prijatia
        //    boolean indicate = (mBodySensorCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;

        byte[] hrData = {
                0x00,  // Flags
                75     // Heart Rate Value (75 BPM)
        };

        mHeartRateMeasureCharacteristic.setValue(hrData);

        for (BluetoothDevice device : mBluetoothDevices) {
            if (mGattServer != null) {
                mGattServer.notifyCharacteristicChanged(device, mHeartRateMeasureCharacteristic, false);
            }
        }
    }




    /**
     * Returns Intent addressed to the {@code PeripheralAdvertiseService} class.
     */
    private Intent getServiceIntent(Context context) {
        return new Intent(context, PeripheralAdvertiseService.class);
    }


    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {

            super.onConnectionStateChange(device, status, newState);

            String msg;

            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (newState == BluetoothGatt.STATE_CONNECTED) {

                    mBluetoothDevices.add(device);


                    msg = "Connected to device: " + device.getAddress();
                    Log.v(MainActivity.TAG, msg);
                    showMsgText(msg);

                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {

                    mBluetoothDevices.remove(device);
                    msg = "Disconnected from device";
                    Log.v(MainActivity.TAG, msg);
                    showMsgText(msg);
                    //tomas
                    startAdvertising();


                }

            } else {
                mBluetoothDevices.remove(device);

                msg = getString(R.string.status_error_when_connecting) + ": " + status;
                Log.e(MainActivity.TAG, msg);
                showMsgText(msg);

            }
        }


        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            super.onNotificationSent(device, status);
            Log.v(MainActivity.TAG, "Notification sent. Status: " + status);
        }




        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if (mGattServer == null) {
                return;
            }


            Log.d(MainActivity.TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(MainActivity.TAG, "Value: " + Arrays.toString(characteristic.getValue()));


            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
            //   mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, result);
        }



        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);


            String msg = "Characteristic Write request: " + Arrays.toString(value);
            Log.d(MainActivity.TAG, msg);
            //      showMsgText(msg);

            mBodySensorCharacteristic.setValue(value);

            if (responseNeeded) {
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
            }

            //tomas
            mPrikaz = mBodySensorCharacteristic.getStringValue(0);
            // mobilcislo = mBodySensorCharacteristic.getStringValue(0);
            //mMobilCislo1.setText(mobilcislo);


            String[] separated = mPrikaz.split(":");
            mobilcislo = separated [1]; // this will contain "Fruit"
            mStav = separated [0]; // this will contain " they taste good"



            switch (mStav) {
                case "ON":
                    ZapVyp = true;
                    TestNaklonu = false;

                    //     mMobilCislo1.setText("ZAP " + mobilcislo);
                    //       toneG.startTone(ToneGenerator.TONE_DTMF_0, 200);
                    //        mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.strazim);
                    //        mediaPlayer.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMobilCislo1.setTextColor(Color.RED);
                            mMobilCislo1.setText("ARMED");
                        }
                    });




                    pocitadlo_nakolnu=5;
                    startSensor();
                    //tomas



                    break;

                case "OF":
                    ZapVyp = false;
                    //    mMobilCislo1.setText("VYP ");
                    //      toneG.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 500);
                    //      toneG.startTone(ToneGenerator.TONE_CDMA_ABBR_REORDER, 4000);
                    //     mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.nestrazim);
                    //    mediaPlayer.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMobilCislo1.setTextColor(Color.GREEN);
                            mMobilCislo1.setText("DISARMED");
                        }
                    });



                    TestNaklonu = false;
                    stopSensor();

                    //    String msg2 = mobilcislo;
                    //  sensorManager.unregisterListener(PeripheralRoleActivity.this);

                    //    Log.v(MainActivity.TAG, msg2);
                    //    showMsgText(msg2);

                    break;

                case "SM":
                    SendSMS();

                    break;

                case "TS":
                    ZapVyp = false;
                    TestNaklonu = true;
                    PrvyKrat = true;

                    //     mMobilCislo1.setText("TEST" + mobilcislo);
                    //     toneG.startTone(ToneGenerator.TONE_DTMF_9, 200);
                    //        mediaPlayer = MediaPlayer.create(PeripheralRoleActivity.this, R.raw.testujem);
                    //        mediaPlayer.start();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMobilCislo1.setText("TESTING");
                        }
                    });


                    startSensor();


                    //tomas
                    sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    acclerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    sensorManager.registerListener(PeripheralRoleActivity.this, acclerometer, SensorManager.SENSOR_DELAY_NORMAL);


                    break;


            }


        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {

            super.onDescriptorReadRequest(device, requestId, offset, descriptor);

            if (mGattServer == null) {
                return;
            }

            Log.d(MainActivity.TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(MainActivity.TAG, "Value: " + Arrays.toString(descriptor.getValue()));

            mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value) {

            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            Log.v(MainActivity.TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));

//            int status = BluetoothGatt.GATT_SUCCESS;
//            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
//                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                boolean supportsNotifications = (characteristic.getProperties() &
//                        BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//                boolean supportsIndications = (characteristic.getProperties() &
//                        BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
//
//                if (!(supportsNotifications || supportsIndications)) {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                } else if (value.length != 2) {
//                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
//                } else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsDisabled(characteristic);
//                    descriptor.setValue(value);
//                } else if (supportsNotifications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
//                    descriptor.setValue(value);
//                } else if (supportsIndications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
//                    descriptor.setValue(value);
//                } else {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//            } else {
//                status = BluetoothGatt.GATT_SUCCESS;
//                descriptor.setValue(value);
//            }
//            if (responseNeeded) {
//                mGattServer.sendResponse(device, requestId, status,
//            /* No need to respond with offset */ 0,
//            /* No need to respond with a value */ null);
//            }

        }
    };



}

