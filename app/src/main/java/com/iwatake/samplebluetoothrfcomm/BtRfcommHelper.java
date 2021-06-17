package com.iwatake.samplebluetoothrfcomm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BtRfcommHelper extends Thread {
    private static String TAG = "MyApp:BtRfcommHelper";
    private String SERVER_SERVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB";    // SPP
    private BluetoothDevice m_server;
    private BluetoothAdapter m_adapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket m_socket;
    private OutputStream m_os;
    private InputStream m_is;
    private boolean m_isStop = false;

    public interface RxCallback {
        void onRx(String text);
    }
    RxCallback m_rxCallback = null;
    ArrayList<String> m_txTextList = new ArrayList<String>();
    private final Lock m_lockTx = new ReentrantLock();

    public boolean init() {
        Log.d(TAG, "[init] in");

        m_adapter = BluetoothAdapter.getDefaultAdapter();
        if (m_adapter == null) {
            Log.e(TAG, "[initialize] No bluetooth support.");
            return false;
        }
        if (!m_adapter.isEnabled()) {
            Log.e(TAG, "[initialize] Bluetooth is disabled.");
            return false;
        }

        Log.d(TAG, "[init] out");
        return true;
    }

    public ArrayList<String> getNameList() {
        Set<BluetoothDevice> serverList;
        ArrayList<String> nameList = new ArrayList<String>();
        serverList = m_adapter.getBondedDevices();
        for (Iterator<BluetoothDevice> iterator = serverList.iterator(); iterator.hasNext(); ) {
            nameList.add(iterator.next().getName());
//            Log.i(TAG, "[getNameList] name:  " + iterator.next().getName());
        }
        return nameList;
    }

    public boolean connect(String serverName) {
        Log.d(TAG, "[connect] in");
        Set<BluetoothDevice> serverList;
        ArrayList<String> nameList = new ArrayList<String>();
        serverList = m_adapter.getBondedDevices();
        boolean isFound = false;
        for (Iterator<BluetoothDevice> iterator = serverList.iterator(); iterator.hasNext(); ) {
            BluetoothDevice server = iterator.next();
            if (serverName.equals(server.getName())) {
                isFound = true;
                m_server = server;
                break;
            }
        }

        if (!isFound) {
            Log.e(TAG, "[connect] selected device was not found:  " + serverName);
            return false;
        }

        try {
            m_socket = m_server.createRfcommSocketToServiceRecord(UUID.fromString(SERVER_SERVICE_UUID));
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "[connect] createRfcommSocketToServiceRecord failed.");
            return false;
        }

        try {
            m_socket.connect();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "[connect] Unable to connect.");

            try {
                m_socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.d(TAG, "[connect] Unable to close socket.");
                return false;
            }
            return false;
        }

        try {
            m_os = m_socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "[connect] Unable to create output stream.");
            try {
                m_socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.d(TAG, "[connect] Unable to close socket.");
                return false;
            }
            return false;
        }

        try {
            m_is = m_socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "[connect] Unable to create input stream.");
            try {
                m_socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                Log.d(TAG, "[connect] Unable to close socket.");
                return false;
            }
            return false;
        }

        Log.d(TAG, "[connect] out");
        return true;
    }

    public boolean disconnect() {
        Log.d(TAG, "[disconnect] in");
        m_isStop = true;
        try {
            join();
            m_isStop = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.e(TAG, "[disconnect] Unable to join the thread.");
            return false;
        }

        try {
            m_os.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "[disconnect] Unable to close output stream.");
            return false;
        }

        try {
            m_is.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "[disconnect] Unable to close input stream.");
            return false;
        }

        try {
            m_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "[disconnect] Unable to close socket.");
            return false;
        }

        Log.d(TAG, "[disconnect] out");
        return true;
    }



    public void run() {
        Log.d(TAG, "[run] in");
        while (!m_isStop) {
            m_lockTx.lock();
            for (String txText : m_txTextList) {
                try {
                    m_os.write(txText.getBytes(Charset.forName("ascii")));
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "[run] Unable to write to output stream.");
                }
            }
            m_txTextList.clear();
            m_lockTx.unlock();

            try {
                if (m_is.available() > 0) {
                    byte[] buffer = new byte[256];
                    int bytes = m_is.read(buffer, 0, 256);
                    if (bytes > 0) {
                        String rxText = new String(buffer, 0, bytes);
                        //                Log.i(TAG, "[run] rxText: " + rxText);
                        if (m_rxCallback != null) {
                            m_rxCallback.onRx(rxText);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "[run] Unable to read from input stream.");
            }
        }
        Log.d(TAG, "[run] out");
    }

    public void send(String str) {
        m_lockTx.lock();
        if (isAlive()) {
            m_txTextList.add(str);
        }
        m_lockTx.unlock();
    }

    public void setRxCallback(RxCallback cb) {
        m_rxCallback = cb;
    }

}
