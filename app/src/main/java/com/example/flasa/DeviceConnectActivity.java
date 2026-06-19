package com.example.flasa;

import static android.media.ToneGenerator.TONE_PROP_BEEP2;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import static com.example.flasa.Constants.SERVER_MSG_FIRST_STATE;
import static com.example.flasa.Constants.SERVER_MSG_SECOND_STATE;

import static com.example.flasa.Constants.BATTERY_LEVEL_SERVICE_UUID;
import static com.example.flasa.Constants.BATTERY_LEVEL_CHARACTERISTIC_UUID;

import static com.example.flasa.Constants.HEART_RATE_SERVICE_UUID;
import static com.example.flasa.Constants.BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID;
import static com.example.flasa.Constants.HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID;


import android.widget.Toast;


public class DeviceConnectActivity extends BluetoothActivity implements View.OnClickListener {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String LIST_NAME = "NAME";
    private static final String LIST_UUID = "UUID";


    private CentralService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mDeviceServices;
    private BluetoothGattCharacteristic mBodySensorCharacteristic;
    private BluetoothGattCharacteristic mHeartRateMeasureCharacteristic;
    private BluetoothGattCharacteristic mBaterkaLevelCharacteristic;


    private String mDeviceName;
    private String mDeviceAddress;

    private TextView mConnectionStatus;
    private TextView mConnectedDeviceName;
    private TextView mReadInfo;
    private TextView mTelCisloZobraz;

    private EditText mInputText;
    //tomas
    public String mTelCislo;

    public int mPrikaz1;
    private TextView mMobilCislo;
    private Button mVypniOchranu;
    private Button mZapniOchranu;
    private Button mZapisCharacteristiku;
    private Button mNacitajInfo;
    private Button mPosliSMS;
    private String mTelCisloSettings;

    public boolean PrenosStav = false;

    private MediaPlayer mediaPlayer;
    private String poslednyPrikaz = "";

    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_connect);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // dôležité


        mDeviceServices = new ArrayList<>();
        mBodySensorCharacteristic = null;


        Intent intent = getIntent();
        if (intent != null) {
            mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        }


        mConnectionStatus = (TextView) findViewById(R.id.connection_status);
        mConnectedDeviceName = (TextView) findViewById(R.id.connected_device_name);
        mTelCisloZobraz = (TextView) findViewById(R.id.tel_cislo_zobraz);

        //tomas
        mZapisCharacteristiku = (Button) findViewById(R.id.test_naklonu);
        mZapisCharacteristiku.setOnClickListener(this);

        mVypniOchranu = (Button) findViewById(R.id.vypni_ochranu);
        mVypniOchranu.setOnClickListener(this);
        mZapniOchranu = (Button) findViewById(R.id.zapni_ochranu);
        mZapniOchranu.setOnClickListener(this);
        mNacitajInfo = (Button) findViewById(R.id.read_info);
        mNacitajInfo.setOnClickListener(this);
        mPosliSMS = (Button) findViewById(R.id.posli_sms);
        mPosliSMS.setOnClickListener(this);

        mReadInfo = (TextView) findViewById(R.id.read_info_text);


        //     mZavolaCislo.setOnClickListener(this);

        if (TextUtils.isEmpty(mDeviceName)) {
            mConnectedDeviceName.setText("");
        } else {
            mConnectedDeviceName.setText(mDeviceName);
        }


        Intent gattServiceIntent = new Intent(this, CentralService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        /*
        updateConnectionState(R.string.connected);
        mRequestReadCharacteristic.setEnabled(true);
        updateInputFromServer(SERVER_MSG_SECOND_STATE);
        */

        SharedPreferences prefs = getSharedPreferences("MojeNastavenia", MODE_PRIVATE);
        mTelCisloSettings = prefs.getString("cislo1","");
        mTelCisloZobraz.setText(mTelCisloSettings);

     //   Log.e("BluetoothLE", "ACTIVITY ONCREATE");


      //  Log.w(MainActivity.TAG, "Oncreate");
      //  Toast.makeText(this, "ONCREATE", Toast.LENGTH_LONG).show();

      //  android.util.Log.e("BLETEST", "ONCREATE TEST 123");
    }




    //tomas

    @Override

    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                    mGattUpdateReceiver,
                    makeGattUpdateIntentFilter(),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }

        Log.e(MainActivity.TAG, "Receiver registered");

        SharedPreferences prefs =
                getSharedPreferences("MojeNastavenia", MODE_PRIVATE);

        mTelCisloSettings = prefs.getString("cislo1", "");
        mTelCisloZobraz.setText(mTelCisloSettings);
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

    }

    //Menu
    @Override
    protected int getLayoutId() {
        return R.layout.activity_device_connect;
    }

    @Override
    protected int getTitleString() { return R.string.central_connection_screen;
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {

            Toast.makeText(this, "Klikol si na Nastavenia", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, Settings.class);
            startActivity(intent);

            return true;

        } else if (id == R.id.action_about) {

            Toast.makeText(this, "O aplikácii", Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 8:

                if (resultCode == RESULT_OK) {

                } else {

                    // User declined to enable Bluetooth, exit the app.
                    Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }




    @Override
    public void onClick(View view) {

        //  mInputText.getText().toString();
        //   mTelCislo = mInputText.getText().toString();

        mTelCislo = mTelCisloSettings;

        int id = view.getId();

        if (id == R.id.test_naklonu) {

            poslednyPrikaz = "TS";
            zapisCharacteristiku("TS:" + mTelCislo);

        } else if (id == R.id.vypni_ochranu) {

            poslednyPrikaz = "OF";
            zapisCharacteristiku("OF:" + mTelCislo);

        } else if (id == R.id.zapni_ochranu) {

            poslednyPrikaz = "ON";
            zapisCharacteristiku("ON:" + mTelCislo);

        } else if (id == R.id.posli_sms) {

            poslednyPrikaz = "SM";
            zapisCharacteristiku("SM:" + mTelCislo);

        } else if (id == R.id.read_info) {

            citajCharacteristiku();
        }
    }



    /*
    request from the Server the value of the Characteristic.
    this request is asynchronous.
     */

    public void citajCharacteristiku() {

        Log.e(MainActivity.TAG, "Read INFO");

        if (mBluetoothLeService != null && mBaterkaLevelCharacteristic != null) {
            mBluetoothLeService.readCharacteristic(mBaterkaLevelCharacteristic);
        } else {
            showMsgText(R.string.error_unknown);
        }


    }



    public void zapisCharacteristiku(String prikaz) {


        if (mBluetoothLeService != null && mBodySensorCharacteristic != null) {
            //   mBluetoothLeService.readCharacteristic(mBodySensorCharacteristic);
            //tomas
            mBluetoothLeService.writeCharacteristic1(mBodySensorCharacteristic, prikaz);
           // mBluetoothLeService.writeCharacteristic1(HEART_RATE_SERVICE_UUID, BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID, prikaz);

        } else {
            showMsgText(R.string.error_unknown);
        }
    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBluetoothLeService = ((CentralService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(MainActivity.TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    /*
     Handles various events fired by the Service.
     ACTION_GATT_CONNECTED: connected to a GATT server.
     ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            String msg1;



            Log.e(MainActivity.TAG, "BroadcastReceiver-DeviceConnect");


            if (action == null) {
                return;
            }

            switch (intent.getAction()) {

                case CentralService.ACTION_GATT_CONNECTED:
                    updateConnectionState(R.string.connected);
                    //mRequestReadCharacteristic.setEnabled(true);
                    Log.e(MainActivity.TAG, "ACTION_GATT_CONNECTED-DeviceConnect");
                    break;

                case CentralService.ACTION_GATT_DISCONNECTED:
                    updateConnectionState(R.string.disconnected);
                    //  mRequestReadCharacteristic.setEnabled(false);
                    break;


                case CentralService.ACTION_GATT_SERVICES_DISCOVERED:
                    // set all the supported services and characteristics on the user interface.
                    setGattServices(mBluetoothLeService.getSupportedGattServices());
                    Log.e(MainActivity.TAG, "ACTION_GATT_SERVICES_DISCOVERED-DeviceConnect");
                    registerCharacteristic();
                    break;

                case CentralService.ACTION_DATA_AVAILABLE:
                    int msg = intent.getIntExtra(CentralService.EXTRA_DATA, -1);

                    Log.e(MainActivity.TAG, "ACTION_DATA_AVAILABLE-DeviceConnect");


                    Log.e(MainActivity.TAG, "MSG = " + msg);


                    if(msg==75) {
                        toneG.startTone(TONE_PROP_BEEP2, 50);
                    }
                    else {
                        mReadInfo.setText(Integer.toString(msg));
                    }

                    break;

                case CentralService.ACTION_WRITE_RESULT:

                    int status = intent.getIntExtra("status", -1);

                    if (status == BluetoothGatt.GATT_SUCCESS) {

                        Log.v(MainActivity.TAG, "Server prijal zapis");

                        switch (poslednyPrikaz) {

                            case "TS":
                                mediaPlayer = MediaPlayer.create(context, R.raw.testujem);
                                break;

                            case "OF":
                                mediaPlayer = MediaPlayer.create(context, R.raw.nestrazim);
                                break;

                            case "ON":
                                mediaPlayer = MediaPlayer.create(context, R.raw.strazim);
                                break;
                        }

                        if (mediaPlayer != null) {
                            mediaPlayer.setOnCompletionListener(mp -> {
                                mp.release();
                                mediaPlayer = null;
                            });

                            mediaPlayer.start();
                        }

                    } else {
                        Log.v(MainActivity.TAG, "Server neprijal zapis " + status);
                        Toast.makeText(context, "SERVER NEPRIJAL ZAPIS", Toast.LENGTH_SHORT).show();
                    }

                    break;

            }
        }
    };


    private void registerCharacteristic() {

        mBodySensorCharacteristic = null;
        mHeartRateMeasureCharacteristic = null;
        mBaterkaLevelCharacteristic = null;

        Log.e(MainActivity.TAG, "RegisterCharcteristic-DeviceConect");



        if (mDeviceServices == null) return;

        for (ArrayList<BluetoothGattCharacteristic> service : mDeviceServices) {

            for (BluetoothGattCharacteristic ch : service) {

                UUID serviceUuid = ch.getService().getUuid();
                UUID charUuid = ch.getUuid();

                // HEART RATE SERVICE
                if (HEART_RATE_SERVICE_UUID.equals(serviceUuid)) {

                    if (BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID.equals(charUuid)) {
                        mBodySensorCharacteristic = ch;
                    }

                    if (HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID.equals(charUuid)) {
                        mHeartRateMeasureCharacteristic = ch;
                    }
                }

                // BATTERY SERVICE
                else if (BATTERY_LEVEL_SERVICE_UUID.equals(serviceUuid)) {

                    if (BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(charUuid)) {
                        mBaterkaLevelCharacteristic = ch;
                    }
                }

            }


        }
        /*
        Log.e("BLE", "BODY = " + mBodySensorCharacteristic);
        Log.e("BLE", "HR = " + mHeartRateMeasureCharacteristic);
        Log.e("BLE", "BAT = " + mBaterkaLevelCharacteristic);

        Log.e("BLE", "HR SERVICE UUID = " + HEART_RATE_SERVICE_UUID);
        Log.e("BLE", "HR CHAR UUID = " + HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID);
        Log.e("BLE", "BAT CHAR UUID = " + BATTERY_LEVEL_CHARACTERISTIC_UUID);
        */

        setupNotifications();
    }

    private void setupNotifications() {

        if (mHeartRateMeasureCharacteristic != null) {
            mBluetoothLeService.setCharacteristicNotification(
                    mHeartRateMeasureCharacteristic, true);
        }

    }



    /*
    Demonstrates how to iterate through the supported GATT Services/Characteristics.
    */
    private void setGattServices(List<BluetoothGattService> gattServices) {

        if (gattServices == null) {
            return;
        }

        mDeviceServices = new ArrayList<>();

        // Loops through available GATT Services from the connected device
        for (BluetoothGattService gattService : gattServices) {
            ArrayList<BluetoothGattCharacteristic> characteristic = new ArrayList<>();
            characteristic.addAll(gattService.getCharacteristics()); // each GATT Service can have multiple characteristic
            mDeviceServices.add(characteristic);
        }

    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatus.setText(resourceId);
            }
        });
    }


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CentralService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(CentralService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(CentralService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(CentralService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(CentralService.ACTION_WRITE_RESULT);
        return intentFilter;
    }




}

