package cuiqiang.ling.ai.blelibdemo.device;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by cuiqiang on 17-12-21.
 *
 * @author cuiqiang
 */

public class BleDeviceAgent {

    private static final String TAG = "ble";
    // 代表服务器名称的常量(随便设置)
    private static final String PROTOCOL_SCHEME_RFCOMM = "ble_X";
    // 是否已开启本机蓝牙可见
    private final int RESULT_DISCOVERABLE = 0x12;
    // 本机蓝牙可被发现时间
    private int mDiscoverableTime = 300;
    // 是否被设置为可见
    private boolean isShowDevice = false;
    //是否保持连接
    private boolean isConnect = false;
    /**
     * 两边的UUID必须是一样的，这是一个服务的唯一标识，而且这个UUID的值必须是00001101-0000-1000-8000-00805F9B34FB
     * 因为这个是android的API上面说明的用于普通蓝牙适配器和android手机蓝牙模块连接的
     */
    private UUID mUuid;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mServerSocket;//客户端socket
    private BluetoothSocket mSeverSocketTwo;//客户端二的socket
    private BluetoothServerSocket mBluetoothServer;//服务端socket
    private ReadThread mReadThread;//接收服务端消息的线程
    private ReadThread mReadThreadTwo;//接收服务端二的线程
    private ServerThread mServerThread;//接收客户端的线程
    private BleDeviceManager.IReceiveMsgCallback mIReceiveMsgCallback;

    public BleDeviceAgent(Context context){
        mContext = context;
        mUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        openBle();
        connect();
    }

    /**
     *  打开蓝牙
     */
    public void openBle(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null){
            Log.d(TAG,"设备蓝牙无法使用");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()){
            //若没打开则打开蓝牙
            mBluetoothAdapter.enable();
        }
        discoverBluetooth();
    }

    /**
     * 断开连接
     */
    public void disconnect(){
        shutdownServer();
        isShowDevice = false;
        isConnect = false;
    }

    /**
     * 重新连接
     */
    public void connect(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (isConnect){
            return;
        }
        // 服务端连接
        mServerThread = new ServerThread();
        mServerThread.start();
        isConnect = true;
    }

    /**
     * 设置回调接口
     * @param iReceiveMsgCallback 回调接口
     */
    public void setMsgCallback(BleDeviceManager.IReceiveMsgCallback iReceiveMsgCallback){
        mIReceiveMsgCallback = iReceiveMsgCallback;
    }

    /**
     * 释放
     */
    public void release(){
        shutdownServer();
        if (null != mBluetoothAdapter) {
            isConnect = false;
            isShowDevice = false;
        }
    }

    /**
     * 使蓝牙可发现
     */
    public void discoverBluetooth(){
        // 若未被设为可见
        Log.e(TAG,"isShowDevice"+isShowDevice);
        if (!isShowDevice) {
            Log.e(TAG, "discoverableTime "+mDiscoverableTime);
            Intent intentDiscov = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            // 让当前蓝牙设备可见，默认值为120秒，超过300秒的请求将被限制
            intentDiscov.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, mDiscoverableTime);
            ((Activity)mContext).startActivityForResult(intentDiscov, RESULT_DISCOVERABLE);
            isShowDevice = true;
            timeOut();
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(String message){
        if (mServerSocket == null && mSeverSocketTwo == null){
            return;
        }
        OutputStream outputStream ;
        OutputStream outputStreamTwo;
        try {
            // 判断socket是服务器获得的还是客户端获得的
            if (null != mServerSocket) {
                outputStream = mServerSocket.getOutputStream();
                outputStream.write(message.getBytes());
            }
            if (null != mSeverSocketTwo){
                outputStreamTwo = mSeverSocketTwo.getOutputStream();
                outputStreamTwo.write(message.getBytes());
            }
            Log.d(TAG,"message "+message);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 本机蓝牙被发现到超时关闭
     */
    private void timeOut() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isShowDevice = false;
            }
        }, mDiscoverableTime * 1000);
    }

    /**
     * 停止服务器
     */
    private void shutdownServer() {
        Log.e(TAG, "shutdownServer()");
        DestroyThread destroyThread = new DestroyThread();
        destroyThread.start();
    }

    private class ServerThread extends Thread {

        @Override
        public void run() {
            Log.e(TAG, "ServerThread...running");
            try {
                mBluetoothServer = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM, mUuid);
                if (mServerSocket == null) {
                    mServerSocket = mBluetoothServer.accept();
                    // 启动接受数据
                    mReadThread = new ReadThread(mServerSocket);
                    mReadThread.start();
                }
                if (mSeverSocketTwo == null) {
                    mSeverSocketTwo = mBluetoothServer.accept();
                    // 启动接受数据
                    mReadThreadTwo = new ReadThread(mSeverSocketTwo);
                    mReadThreadTwo.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.run();
        }
    }

    //读取数据
    private class ReadThread extends Thread {

        private BluetoothSocket mSocket;

        public ReadThread(BluetoothSocket socket) {
            this.mSocket = socket;
        }

        @Override
        public void run() {

            Log.e(TAG, "ReadThread...running");

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream = null;
            try {
                inputStream = mSocket.getInputStream();
                while (true) {
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        byte[] bufData = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            bufData[i] = buffer[i];
                        }
                        String receiveMsg = new String(bufData);
                        if (null != mIReceiveMsgCallback) {
                            mIReceiveMsgCallback.onReceive(receiveMsg);
                        }
                    }
                }
            } catch (Exception e) {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            super.run();
        }
    }

    private class DestroyThread extends Thread{
        @Override
        public void run() {
            if (mServerThread != null) {
                mServerThread.interrupt();
                mServerThread = null;
            }
            if (mReadThread != null) {
                mReadThread.interrupt();
                mReadThread = null;
            }
            if (mReadThreadTwo != null){
                mReadThreadTwo.interrupt();
                mReadThreadTwo = null;
            }
            if (mBluetoothServer != null) {
                try {
                    mBluetoothServer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mBluetoothServer = null;
            }
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mServerSocket = null;
            }
            if (mSeverSocketTwo != null) {
                try {
                    mSeverSocketTwo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mSeverSocketTwo = null;
            }
            super.run();
        }
    }



}
