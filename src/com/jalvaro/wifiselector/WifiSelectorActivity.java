package com.jalvaro.wifiselector;

import java.util.ArrayList;
import java.util.List;

import com.jalvaro.wifiselector.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class WifiSelectorActivity extends Activity {

	private WifiManager wifi;
	private ListView lv;
	private ToggleButton buttonScan;
	private Button refreshButton;
	private ArrayAdapter<ScanResult> adapter;
	private BroadcastReceiver wifiReceiver;
	private ScanResult result;
	private AlertDialog alertDialog;
	private TextView availableNetTextView;
	
	private static String TAG = WifiSelectorActivity.class.getName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		lv = (ListView) findViewById(R.id.lvNetworks);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				result = adapter.getItem(arg2);

				LayoutInflater factory = LayoutInflater.from(WifiSelectorActivity.this);
				View dialogView = factory.inflate(R.layout.dialog, null);
				TextView textView = (TextView) dialogView.findViewById(R.id.dialog_text);
				textView.setText(R.string.type_password);
				alertDialog = new AlertDialog.Builder(WifiSelectorActivity.this).setTitle(R.string.confirmation)
						.setView(dialogView).setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								String password = ((EditText) alertDialog.findViewById(R.id.dialog_password)).getText()
										.toString();
								int netId = addNewAccessPoint(result, password);
								if (connectToAccessPoint(netId)) {
									Toast.makeText(WifiSelectorActivity.this,
											String.format(getString(R.string.connected), result.SSID),
											Toast.LENGTH_LONG).show();
								} else {
									Toast.makeText(WifiSelectorActivity.this,
											String.format(getString(R.string.not_connected), result.SSID),
											Toast.LENGTH_LONG).show();
								}
							}
						}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
							}
						}).create();
				alertDialog.show();
				if (result.capabilities.equals("")) {
					(alertDialog.findViewById(R.id.dialog_linearLayout)).setVisibility(View.GONE);
				}
			}
		});

		availableNetTextView = (TextView) findViewById(R.id.availableNetworks);

		refreshButton = (Button) findViewById(R.id.refreshButton);
		refreshButton.setText(R.string.refresh);
		refreshButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				wifi.startScan();
			}
		});

		buttonScan = (ToggleButton) findViewById(R.id.buttonConnect);
		buttonScan.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					// Toast.makeText(MyAppActivity.this, R.string.wifi_enabled,
					// Toast.LENGTH_LONG).show();
					wifi.setWifiEnabled(true);
					refreshButton.setEnabled(true);
				} else {
					// Toast.makeText(MyAppActivity.this,
					// R.string.wifi_disabled, Toast.LENGTH_LONG).show();
					wifi.setWifiEnabled(false);
					refreshButton.setEnabled(false);
					showNetworks(new ArrayList<ScanResult>());
				}
			}
		});

		if (wifi.isWifiEnabled()) {
			buttonScan.setChecked(true);
			refreshButton.setEnabled(true);
		} else {
			buttonScan.setChecked(false);
			refreshButton.setEnabled(false);
		}

		wifiReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent intent) {
				List<ScanResult> scanList = wifi.getScanResults();

				showNetworks(scanList);
			}
		};

		registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	}

	private void showNetworks(List<ScanResult> scanList) {
		if (scanList != null) {
			availableNetTextView.setText(Integer.toString(scanList.size()));
			Log.d(TAG, String.format(getString(R.string.scanning), scanList.size()));

			adapter = new ResultAdapter(this, R.layout.item_text, scanList);

			lv.setAdapter(adapter);
		}
	}

	private class ResultAdapter extends ArrayAdapter<ScanResult> {
		private int textViewResourceId;

		public ResultAdapter(Context context, int textViewResourceId, List<ScanResult> objects) {
			super(context, textViewResourceId, objects);
			this.textViewResourceId = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView textView;
			if (convertView == null) {
				LayoutInflater li = ((Activity) getContext()).getLayoutInflater();

				convertView = (LinearLayout) li.inflate(textViewResourceId, null);
				textView = (TextView) convertView.findViewById(R.id.item_textView);
			} else {
				textView = (TextView) convertView.findViewById(R.id.item_textView);
			}

			textView.setText(getItem(position).SSID);
			return convertView;
		}
	}

	private int addNewAccessPoint(ScanResult scanResult, String password) {

		WifiConfiguration wc = new WifiConfiguration();
		wc.SSID = '\"' + scanResult.SSID + '\"';
		wc.preSharedKey = "\"" + password + "\"";
		wc.hiddenSSID = true;
		wc.status = WifiConfiguration.Status.ENABLED;
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
		wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
		wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
		wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
		int netId = wifi.addNetwork(wc);
		Log.d(TAG, "add Network returned " + netId);

		return netId;
	}

	private boolean connectToAccessPoint(int netId) {
		boolean connected = wifi.enableNetwork(netId, true);
		Log.d(TAG, "enableNetwork returned " + connected);
		boolean c = wifi.reconnect();
		Log.d(TAG, "reconnect returned " + c);
		return connected;
	}

	@Override
	public void onStop() {
		super.onStop();
		try {
			unregisterReceiver(wifiReceiver);
		} catch (Exception e) {

		}
	}
}