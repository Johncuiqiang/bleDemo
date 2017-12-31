package cuiqiang.ling.ai.blelibdemo.device;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import cuiqiang.ling.ai.blelibdemo.R;
import cuiqiang.ling.ai.blelibdemo.device2.BleDeviceClientManager;
import cuiqiang.ling.ai.blelibdemo.device2.BleDeviceSearchActivity;

public class DeviceSearchActivity extends Activity  {

	private static final String TAG = "ble";
	private Context mContext;
	private EditText mEtText;
	private View mBtnSend;
	private BleDeviceManager mBleDeviceManager;
	private TextView mTvShow;

	private Handler LinkDetectedHandler = new Handler() {

		@Override
		public void handleMessage(android.os.Message msg) {
			mTvShow.setText((String)msg.obj);
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.act_device);
		initView();
		initData();
	}

	private void initView() {
		mEtText = findViewById(R.id.et_text);
		mBtnSend = findViewById(R.id.btn_send);
		mTvShow = findViewById(R.id.tv_show);
	}

	private void initData() {
		mContext = this;
		mBleDeviceManager = new BleDeviceManager(mContext);
		mBleDeviceManager.setMsgCallback(new BleDeviceManager.IReceiveMsgCallback() {
			@Override
			public void onReceive(String message) {
				Log.d(TAG,"message "+message);
				Message msg = new Message();
				msg.obj = message;
				msg.what = 0;
				LinkDetectedHandler.sendMessage(msg);
			}
		});
		mBtnSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String message = mEtText.getText().toString().trim();
				mBleDeviceManager.sendMessage(message);
			}
		});
	}


	@Override
	protected void onResume() {
		mBleDeviceManager.connect();
		super.onResume();
	}

	/**
	 * 界面销毁前，退出蓝牙功能
	 */
	@Override
	protected void onDestroy() {
		Log.e(TAG,"onDestroy()");
		mBleDeviceManager.release();
		mBleDeviceManager = null;
		super.onDestroy();
	}
}
