package cuiqiang.ling.ai.blelibdemo;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import cuiqiang.ling.ai.blelibdemo.blemusic.BleAvrcpAct;
import cuiqiang.ling.ai.blelibdemo.client.BleClientSearchActivity;
import cuiqiang.ling.ai.blelibdemo.clientTest.ClientSearchActivity;
import cuiqiang.ling.ai.blelibdemo.device.DeviceSearchActivity;
import cuiqiang.ling.ai.blelibdemo.device2.BleDeviceSearchActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "ble";
    // 是否已开启本机蓝牙可见
    private final int RESULT_DISCOVERABLE = 0x12;

    private Context mContext;
    private View mBtnBleConn;//连接
    private View mBtnBleDevice;//设备
    private View mBtnBleSend;//发送
    private View mBtnBleClient;//客户端
    private View mBtnBleClose;//关闭蓝牙
    private View mBtnBleSearch;//搜索
    private EditText mEtText;//消息

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initView() {
        mContext = this;
        mBtnBleDevice = findViewById(R.id.btn_ble_device);
        mBtnBleConn = findViewById(R.id.btn_ble_avrcp);
        mBtnBleSend = findViewById(R.id.btn_ble_send);
        mBtnBleClient = findViewById(R.id.btn_ble_client);
        mEtText = findViewById(R.id.et_ble_msg);
        mBtnBleClose = findViewById(R.id.btn_ble_close);
        mBtnBleSearch = findViewById(R.id.btn_ble_search);
    }

    private void initData() {
        mBtnBleClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, ClientSearchActivity.class);
                startActivity(intent);
            }
        });
        mBtnBleDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, DeviceSearchActivity.class);
                startActivity(intent);
            }
        });
        mBtnBleConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //客户端只完成配对连接
                Intent intent = new Intent();
                intent.setClass(mContext, BleAvrcpAct.class);
                startActivity(intent);
            }
        });

        mBtnBleSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, BleClientSearchActivity.class);
                startActivity(intent);
            }
        });
        mBtnBleClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, BleDeviceSearchActivity.class);
                startActivity(intent);
            }
        });
        mBtnBleSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //BleClientManager.getInstance().release();
    }

}
