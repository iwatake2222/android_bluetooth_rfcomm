package com.iwatake.samplebluetoothrfcomm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MyApp:MainActivity";
    private final int REQUEST_CODE_FOR_PERMISSIONS = 1234;;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.BLUETOOTH"};
    private final BtRfcommHelper btRfcommHelper = new BtRfcommHelper();

    private EditText editTextTxList;
    private EditText editTextRxList;
    private EditText editTextTx;
    private Button buttonSend;
    private Button buttonClear;
    private Spinner spinnerDeviceList;
    private Button buttonConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getViews();
        setEventListeners();

        if (checkPermissions()) {
            startApp();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopApp();
    }

    private void startApp() {
        Log.i(TAG, "[startApp] in");
        getDeviceList();


//        if (!btRfcommHelper.connect()) {
//            Log.w(TAG, "[startApp] failed to connect");
//            return;
//        }
//        btRfcommHelper.start();
        Log.i(TAG, "[startApp] out");
    }

    private void getDeviceList() {
        Log.i(TAG, "[getDeviceList] in");
        ArrayList<String> nameList = btRfcommHelper.getNameList();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(nameList);
        spinnerDeviceList.setAdapter(adapter);
        Log.i(TAG, "[getDeviceList] out");
    }

    private void stopApp() {
        Log.i(TAG, "[stopApp] in");
        if (!btRfcommHelper.disconnect()) {
            Log.w(TAG, "[stopApp] failed to disconnect");
            return;
        }
        Log.i(TAG, "[stopApp] out");
    }

    private void setEventListeners() {
        buttonConnect.setOnClickListener((View v) -> {
            if (buttonConnect.getText().equals("CONNECT")) {
                String selected = (String) spinnerDeviceList.getSelectedItem();
                Log.i(TAG, "[buttonConnect] " + selected);
                Toast.makeText(this, "Connecting to " + selected, Toast.LENGTH_SHORT).show();
                if (btRfcommHelper.connect(selected)) {
                    Log.i(TAG, "[buttonConnect] succeed");
                    Toast.makeText(this, "Connection successful", Toast.LENGTH_SHORT).show();
                    btRfcommHelper.start();
                    buttonConnect.setText("DISCONNECT");
                    buttonSend.setEnabled(true);
                    buttonClear.setEnabled(true);
                } else {
                    Log.i(TAG, "[buttonConnect] failed");
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                btRfcommHelper.disconnect();
                buttonConnect.setText("CONNECT");
                buttonSend.setEnabled(false);
                buttonClear.setEnabled(false);
            }
        });

    }

    private void getViews() {
        editTextTxList = findViewById(R.id.editTextTxList);
        editTextRxList = findViewById(R.id.editTextRxList);
        editTextTx = findViewById(R.id.editTextTx);
        buttonSend = findViewById(R.id.buttonSend);
        buttonClear = findViewById(R.id.buttonClear);
        spinnerDeviceList = findViewById(R.id.spinnerDeviceList);
        buttonConnect = findViewById(R.id.buttonConnect);

        buttonConnect.setText("CONNECT");
        buttonSend.setEnabled(false);
        buttonClear.setEnabled(false);
    }

    private boolean checkPermissions() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_FOR_PERMISSIONS){
            if(checkPermissions()){
                startApp();
            } else{
                Log.w(TAG, "[onRequestPermissionsResult] Failed to get permissions");
                this.finish();
            }
        }
    }
}
