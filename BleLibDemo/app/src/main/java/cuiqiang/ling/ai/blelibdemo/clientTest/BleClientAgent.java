package cuiqiang.ling.ai.blelibdemo.clientTest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by cuiqiang on 17-12-26.
 *
 * @author cuiqiang
 */

public class BleClientAgent {

    private int mLastMsgResultWhat = 0x00;
    //正在连接服务端
    public static final int SERVER_CONNECTING = 0x01;
    //连接服务端成功
    public static final int SERVER_CONNECTED = 0x02;
    //连接服务端失败
    public static final int SERVER_CONNECT_ERROR = 0x03;
    //收发消息
    public static final int MESSAGE = 0x04;

    private static final String TAG = "ble";

    private Context mContext;
    private UUID mUuid;
    private ClientThread mClientThread;//客户端发消息线程
    private ReadThread mReadThread; //客户端读消息线程
    private BluetoothSocket mClientSocket; //蓝牙socket连接
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;//蓝牙远程设备对象
    private Handler mReceiveHandler;
    private IBleConnectState mIBleConnectState;

    public BleClientAgent(Context context) {
        mContext = context;
        mUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    }

    /**
     * 获取本机设备名
     *
     * @return 本机设备名
     */
    public String getDeviceName() {
        return mBluetoothAdapter != null ? mBluetoothAdapter.getName() : "";
    }

    /**
     * 获取远端设备名
     *
     * @return 远端设备名
     */
    public String getRmoteDeviceName() {
        return mDevice != null ? mDevice.getName() : "";
    }

    /**
     * 设置蓝牙可用
     */
    public void setBleEnable() {
        Log.e(TAG, "setBluetooth()");
        if (mIBleConnectState != null){
            mIBleConnectState.onConnecting();
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG,"BluetoothMsg.isOpen "+BluetoothMsg.isOpen);
        // 校验是否已连接
        if (BluetoothMsg.isOpen) {
            Toast.makeText(mContext, "设备已连接", Toast.LENGTH_SHORT).show();
            return;
        }
        String address = BluetoothMsg.BlueToothAddress;
        Log.e(TAG, "address:" + address);
        if (!address.equals("null")) {
            mDevice = mBluetoothAdapter.getRemoteDevice(address);
            mClientThread = new ClientThread();
            mClientThread.start();
            BluetoothMsg.isOpen = true;
        } else {
            Toast.makeText(mContext, "连接已断开，请重新获取连接", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(mContext, ClientSearchActivity.class);
            mContext.startActivity(intent);
        }
    }

    /**
     * 发送消息
     */
    public void sendMessage(String msg) {
        if (mClientSocket == null) {
            Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
            return;
        }
        OutputStream os = null;
        try {
            if (null != mClientSocket) {
                os = mClientSocket.getOutputStream();
            }
            os.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setReceiveHandler(Handler receiveHandler) {
        this.mReceiveHandler = receiveHandler;
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        shutdownClient();
        BluetoothMsg.isOpen = false;
        Log.d(TAG, "BluetoothMsg.isOpen " + BluetoothMsg.isOpen);
    }

    /**
     * 释放
     */
    public void release() {
        shutdownClient();
        BluetoothMsg.isOpen = false;
    }

    /**
     * 客户端启动线程
     *
     * @author ZhaBaotan
     */
    private class ClientThread extends Thread {

        @Override
        public void run() {
            Log.e(TAG, "ClientThread...running");
            try {
                Log.e(TAG, "mDevice=" + mDevice);
                mClientSocket = mDevice.createRfcommSocketToServiceRecord(mUuid);
                String msg = BluetoothMsg.BlueToothName + BluetoothMsg.BlueToothAddress + "\n" + "客户端已启动，正在连接服务器...";
                sendHandlerMsg(SERVER_CONNECTING, msg);
                mClientSocket.connect();
                String success = "已连接上服务器，可发送消息";
                sendHandlerMsg(SERVER_CONNECTED, success);
                if (null != mIBleConnectState) {
                    mIBleConnectState.onComplete();
                }
                // 启动接收数据
                mReadThread = new ReadThread(mClientSocket);
                mReadThread.start();

            } catch (IOException e) {
                e.printStackTrace();
                if (null != mIBleConnectState) {
                    mIBleConnectState.onError();
                }
                String msg = "服务端未开启...请在另一台设备上先启动服务端...然后点击重试";
                sendHandlerMsg(SERVER_CONNECT_ERROR, msg);
            }
            super.run();
        }
    }


    /**
     * 读取数据
     */
    private class ReadThread extends Thread {

        private BluetoothSocket socket;

        public ReadThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Log.e(TAG, "ReadThread...running");

            byte[] buffer = new byte[1024];
            int bytes;
            InputStream inputStream = null;

            try {
                inputStream = socket.getInputStream();
                while (true) {
                    if ((bytes = inputStream.read(buffer)) > 0) {
                        byte[] bufData = new byte[bytes];
                        for (int i = 0; i < bytes; i++) {
                            bufData[i] = buffer[i];
                        }
                        String msg = new String(bufData);
                        sendHandlerMsg(MESSAGE, msg);
                    }
                }
            } catch (Exception e) {
                try {
                    inputStream.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            super.run();
        }
    }

    /**
     * 关闭客户端线程和流
     */
    private void shutdownClient() {
        Log.e(TAG, "shutdownClient()");

        if (mClientThread != null) {
            mClientThread.interrupt();
            mClientThread = null;
        }
        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread = null;
        }
        if (mClientSocket != null) {
            try {
                mClientSocket.close();
                mClientSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送消息到handler
     *
     * @param type 不同类别
     * @param data 消息
     */
    private void sendHandlerMsg(int type, Object data) {
        if (null != mReceiveHandler) {
            if (mReceiveHandler.hasMessages(mLastMsgResultWhat)) {
                mReceiveHandler.removeMessages(mLastMsgResultWhat);
            }
            Message msg = mReceiveHandler.obtainMessage();
            msg.what = type;
            msg.obj = data;
            mReceiveHandler.sendMessage(msg);
            //缓存当前result 对象
            this.mLastMsgResultWhat = type;
        }
    }

    public void setIBleConnectState(IBleConnectState iBleConnectState) {
        this.mIBleConnectState = iBleConnectState;
    }

    public interface IBleConnectState {

        void onConnecting();

        void onComplete();

        void onError();
    }
}
