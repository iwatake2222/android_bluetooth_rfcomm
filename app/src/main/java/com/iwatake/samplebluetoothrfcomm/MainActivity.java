package com.iwatake.samplebluetoothrfcomm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  implements SensorEventListener {
    private final String TAG = "MyApp:MainActivity";
    private final int REQUEST_CODE_FOR_PERMISSIONS = 1234;;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.BLUETOOTH"};
    private final BtRfcommHelper btRfcommHelper = new BtRfcommHelper();

    private ScrollView scrollViewRxList;
    private ScrollView scrollViewTxList;
    private TextView textViewRxList;
    private TextView textViewTxList;
    private EditText editTextTx;
    private Button buttonSend;
    private CheckBox checkBoxCr;
    private CheckBox checkBoxLf;
    private Button buttonClearTx;
    private Button buttonClearAll;
    private Spinner spinnerDeviceList;
    private Button buttonConnect;

    private SensorManager m_sensorManager;
    private float[] m_acc = null;
    private String m_previousTxString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getViews();
        resetViews();
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
        Log.d(TAG, "[startApp] in");
        getBtDeviceList();
        m_acc = new float[3];
        m_sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),  SensorManager.SENSOR_DELAY_UI);
        Log.d(TAG, "[startApp] out");
    }

    private void getBtDeviceList() {
        Log.d(TAG, "[getBtDeviceList] in");
        ArrayList<String> nameList = btRfcommHelper.getNameList();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(nameList);
        spinnerDeviceList.setAdapter(adapter);
        Log.d(TAG, "[getBtDeviceList] out");
    }

    private void stopApp() {
        Log.d(TAG, "[stopApp] in");
        if (buttonConnect.getText().equals("DISCONNECT")) {
            if (!btRfcommHelper.disconnect()) {
                Log.e(TAG, "[stopApp] failed to disconnect");
                return;
            }
        }
        Log.d(TAG, "[stopApp] out");
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
                    textViewTxList.setText("");
                    textViewRxList.setText("");
                } else {
                    Log.i(TAG, "[buttonConnect] failed");
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                btRfcommHelper.disconnect();
                buttonConnect.setText("CONNECT");
                buttonSend.setEnabled(false);
            }
        });

        buttonSend.setOnClickListener((View v) -> {
            String txString = editTextTx.getText().toString();
            if (checkBoxCr.isChecked()) {
                txString += "\r";
            }
            if (checkBoxLf.isChecked()) {
                txString += "\n";
            }
            btRfcommHelper.send(txString);
            textViewTxList.setText(textViewTxList.getText() + "\n" + txString);
            scrollViewTxList.fullScroll((ScrollView.FOCUS_DOWN));
        });

        buttonClearTx.setOnClickListener((View v) -> {
            editTextTx.setText("");
        });

        buttonClearAll.setOnClickListener((View v) -> {
            editTextTx.setText("");
            textViewRxList.setText("");
            textViewTxList.setText("");
        });

        btRfcommHelper.setRxCallback((String text) -> {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewRxList.setText(textViewRxList.getText() + text);
                    scrollViewRxList.fullScroll((ScrollView.FOCUS_DOWN));
                }
            });
        });
    }

    private void getViews() {
        scrollViewRxList = findViewById(R.id.scrollViewRxList);
        scrollViewTxList = findViewById(R.id.scrollViewTxList);
        textViewRxList = findViewById(R.id.textViewRxList);
        textViewTxList = findViewById(R.id.textViewTxList);
        editTextTx = findViewById(R.id.editTextTx);
        buttonSend = findViewById(R.id.buttonSend);
        checkBoxCr = findViewById(R.id.checkBoxCr);
        checkBoxLf = findViewById(R.id.checkBoxLf);
        buttonClearTx = findViewById(R.id.buttonClearTx);
        buttonClearAll = findViewById(R.id.buttonClearAll);
        spinnerDeviceList = findViewById(R.id.spinnerDeviceList);
        buttonConnect = findViewById(R.id.buttonConnect);
    }

    private void resetViews() {
        buttonConnect.setText("CONNECT");
        buttonSend.setEnabled(false);
        checkBoxCr.setChecked(false);
        checkBoxLf.setChecked(false);
        textViewTxList.setText("");
        textViewRxList.setText("");
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
                Log.e(TAG, "[onRequestPermissionsResult] Failed to get permissions");
                this.finish();
            }
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float[] accRaw = event.values;
            lowPassFilter(accRaw, m_acc);
            Log.i(TAG, "[onSensorChanged] " + accRaw[0] + ", " + accRaw[1] + ", " + accRaw[2]);

            String txString = analyzeGesture(m_acc[0] / 9.8f, m_acc[1] / 9.8f, m_acc[2] / 9.8f);
            if (txString != m_previousTxString) {
//                btRfcommHelper.send("d");
                btRfcommHelper.send(txString);
                textViewTxList.setText(textViewTxList.getText() + "\n" + txString);
                scrollViewTxList.fullScroll((ScrollView.FOCUS_DOWN));
            }
            m_previousTxString = txString;
        }
    }

    private void lowPassFilter(float[] accRaw, float[] accResult) {
        final float RATIO = 0.5f;
        for (int i = 0; i < accRaw.length; i++) {
            accResult[i] = RATIO * accRaw[i] + (1 - RATIO) * accResult[i];
        }
    }

    private String analyzeGesture(float x, float y, float z) {
        String txString = "";
        final float TH_MOVE = 0.4f;
        final float TH_BEHAVIOR = 0.9f;
        if (TH_MOVE < y && y < TH_BEHAVIOR) {
            txString = "kbk";
        } else if (TH_MOVE < -y && -y < TH_BEHAVIOR) {
            txString = "kcrF";
        } else if (TH_MOVE < x && x < TH_BEHAVIOR) {
            txString = "kwkL";
        } else if (TH_MOVE < -x && -x < TH_BEHAVIOR) {
            txString = "kwkR";
        } else if (TH_BEHAVIOR < y) {
            txString = "khi";
        } else if (TH_BEHAVIOR < -y) {
            txString = "ksit";
        } else if (TH_BEHAVIOR < x) {
            txString = "kstr";
        } else if (TH_BEHAVIOR < -x) {
            txString = "kpee";
        } else {
            txString = "kbalance";
        }
        return txString;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
