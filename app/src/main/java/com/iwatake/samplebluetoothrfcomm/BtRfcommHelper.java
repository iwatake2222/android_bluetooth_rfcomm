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

public class BtRfcommHelper extends Thread {
    private static String TAG = "MyApp:BtRfcommHelper";


    private String SERVER_SERVICE_UUID = "cb10eaad-fdcf-4c18-a0d7-067293b37cf3";
    private BluetoothDevice m_server;
    private BluetoothAdapter m_adapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket m_socket;
    private OutputStream m_os;
    private InputStream m_is;
    private boolean m_isStop = false;

    public boolean init() {
        Log.w(TAG, "[init] in");

        m_adapter = BluetoothAdapter.getDefaultAdapter();
        if (m_adapter == null) {
            Log.w(TAG, "[initialize] No bluetooth support.");
            return false;
        }
        if (!m_adapter.isEnabled()) {
            Log.w(TAG, "[initialize] Bluetooth is disabled.");
            return false;
        }

        Log.w(TAG, "[init] out");
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

        Log.w(TAG, "[init] out");
        return true;
    }

    public boolean disconnect() {
        Log.w(TAG, "[disconnect] in");
        m_isStop = true;
        try {
            join();
            m_isStop = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG, "[disconnect] Unable to join the thread.");
            return false;
        }

        try {
            m_os.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "[disconnect] Unable to close output stream.");
            return false;
        }

        try {
            m_is.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "[disconnect] Unable to close input stream.");
            return false;
        }

        try {
            m_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "[disconnect] Unable to close socket.");
            return false;
        }

        Log.w(TAG, "[disconnect] out");
        return true;
    }

    public void run(){
        Log.i(TAG, "[run] in");
        while (!m_isStop) {
            try {
                m_os.write("Hello World from Android\n".getBytes(Charset.forName("ascii")));
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "[run] Unable to write to output stream.");
            }
        }
        Log.i(TAG, "[run] out");
    }
}
