package cuiqiang.ling.ai.blelibdemo.clientTest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import cuiqiang.ling.ai.blelibdemo.R;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.MESSAGE;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECTED;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECTING;
import static cuiqiang.ling.ai.blelibdemo.clientTest.BleClientAgent.SERVER_CONNECT_ERROR;

public class ClientBleActivity extends Activity {

    private static final String TAG = "ble";

    private Context mContext;
    private ListView mLvDevice;
    private EditText mEdit;
    private Button mBtnSend;//发送消息按键
    private Button mBtnRetry;//重试按键
    private ArrayList<String> mMsgList;
    private ArrayAdapter<String> mAdapter;
    private Handler mHandler = new Handler();
    private BleClientAgent mBleClientAgent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_bluetooth2);

        // 初始化控件
        initView();
        // 初始化数据
        initData();
        // 适配
        initAdapter();
        // 监听
        initListener();
        // 配置蓝牙
        setBluetooth();
    }

    private void initView() {
        Log.e(TAG, "initView()");

        mLvDevice = findViewById(R.id.lv_device);
        mEdit = findViewById(R.id.et_edit);
        mBtnSend = findViewById(R.id.btn_right);
        mBtnRetry = findViewById(R.id.btn_left);
    }

    private void initData() {
        Log.e(TAG, "initData()");
        mContext = this;
        mMsgList = new ArrayList<>();
        ReceiveHandler receiveHandler = new ReceiveHandler(mHandler.getLooper());
        mBleClientAgent =  new BleClientAgent(mContext);
        mBleClientAgent.setReceiveHandler(receiveHandler);
    }

    private void initAdapter() {
        Log.e(TAG, "initAdapter()");

        mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_list_item_1, mMsgList);
        mLvDevice.setAdapter(mAdapter);
        mLvDevice.setFastScrollEnabled(true);
    }

    private void initListener() {
        Log.e(TAG, "initListener()");

        // 发送信息
        mBtnSend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "mBtnSend()");

                String msgText = mEdit.getText().toString();
                if (msgText.length() > 0) {
                    // 发送信息，清除输入框
                    sendMessageHandle(msgText);
                    mEdit.setText("");
                    //收起软件盘
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
                } else {
                    Toast.makeText(getApplicationContext(), "发送内容不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 断开连接
        mBtnRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "mBtnRetry()");
                mBleClientAgent.release();
                //onBackPressed();
                setBluetooth();
            }
        });
    }

    private void setBluetooth() {
        Log.e(TAG, "setBluetooth()");
        mBleClientAgent.setBleEnable();
    }

    /*
     * 每次开启屏幕时重新检测连接
     */
    @Override
    protected void onResume() {
        Log.e(TAG, "onResume()");
        setBluetooth();
        super.onResume();
    }

    private void sendMessageHandle(String msg) {
        Log.e(TAG, "sendMessageHandle()");
        mBleClientAgent.sendMessage(msg);
        mMsgList.add(mBleClientAgent.getDeviceName() + "（本机）：" + msg);
        mAdapter.notifyDataSetChanged();
        mLvDevice.setSelection(mMsgList.size() - 1);
    }

    public class ReceiveHandler extends Handler {

        ReceiveHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int what = msg.what;
            String message = (String) msg.obj;
            switch (what) {
                //连接中
                case SERVER_CONNECTING:
                    mMsgList.add(message);
                    break;
                //已经连接
                case SERVER_CONNECTED:
                    mMsgList.add(message);
                    break;
                //连接错误
                case SERVER_CONNECT_ERROR:
                    mMsgList.add(message);
                    break;
                // 处理收到的消息
                case MESSAGE:
                    mMsgList.add(mBleClientAgent.getRmoteDeviceName() + "：" + message);
                    break;
                default:
                    break;
            }
            mAdapter.notifyDataSetChanged();
            mLvDevice.setSelection(mMsgList.size());
        }
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy()");
        super.onDestroy();
        mBleClientAgent.release();
    }

}
