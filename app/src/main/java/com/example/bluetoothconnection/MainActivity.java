package com.example.bluetoothconnection;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //CONSTANTS
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBtAdapter;
    ListView listView;
    ArrayAdapter<String> arrayAdapter;
    ArrayList<String> arrayDeviceNamesList;
    ArrayList<String> arrayDeviceMAList;

    //Bluetooth connection service
    BlueToothConnectionService btConnectionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "getting default adapter");
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            //No bluetooth in the deivce
            Toast.makeText(this, "Sorry the device doesn't support bluetooth", Toast.LENGTH_LONG).show();
            finish();

        }

        Log.d(TAG, "InitViwes");
        //init views
        initViews();

        //initlistview
        initListVIew();


    }



    @Override
    protected void onResume() {
        super.onResume();


        //If bluetooth not enabled
        if (!mBtAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        } else {
            initActivity();
        }

    }

    private void initActivity() {
        Log.d(TAG, "setupService()");

        if (btConnectionService == null) {
            Log.d(TAG, "inti btConnnection service");
            btConnectionService = new BlueToothConnectionService(this);


        }

        //If connection state is NONE start method
        if (btConnectionService.getState() == BlueToothConnectionService.STATE_NONE) {
            Log.d(TAG, "starting btConnectionService");
            btConnectionService.start();
            updateView();
        }

    }

    private void updateView() {

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        arrayDeviceNamesList.clear();
        arrayDeviceMAList.clear();

        for (BluetoothDevice bd : pairedDevices) {
            arrayDeviceNamesList.add(bd.getName());
            arrayDeviceMAList.add(bd.getAddress());
        }

        Log.d(TAG, "updating adapter");
        //init adapter and set arraylist in adapter
        arrayAdapter.notifyDataSetChanged();

    }

    private void setupService() {
        Log.d(TAG, "setupService()");

        if (btConnectionService == null) {
            Log.d(TAG, "inti btConnnection service");
            btConnectionService = new BlueToothConnectionService(this);

        }

        //If connection state is NONE start method
        if (btConnectionService.getState() == BlueToothConnectionService.STATE_NONE) {
            Log.d(TAG, "starting btConnectionService");
            btConnectionService.start();
        }
        else if(btConnectionService.getState() == BlueToothConnectionService.STATE_CONNECTING)
        {
            Log.d(TAG,"Connecting");
        }

    }

    private void initListVIew() {

        Log.d(TAG, "initLIST VIEW");
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        arrayDeviceNamesList = new ArrayList<>();
        arrayDeviceMAList = new ArrayList<>();

        for (BluetoothDevice bd : pairedDevices) {
            arrayDeviceNamesList.add(bd.getName());
            arrayDeviceMAList.add(bd.getAddress());
        }

        Log.d(TAG, "Init adapter and arraylist and listview");
        //init adapter and set arraylist in adapter
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayDeviceNamesList);
        listView.setAdapter(arrayAdapter);


        listView.setOnItemClickListener(mDeviceClickListener);
    }

    private void initViews() {
        listView = findViewById(R.id.listView);

    }


    //results from enabling bluetooth intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            initActivity();
        } else {
            Toast.makeText(this, "can't start without bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void connectDevice(String address) {

        // Get the BluetoothDevice object
        BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        btConnectionService.connect(device);
    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
            connectDevice(arrayDeviceMAList.get(arg2));

        }
    };

}
