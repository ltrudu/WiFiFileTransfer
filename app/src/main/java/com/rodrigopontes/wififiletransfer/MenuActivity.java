package com.rodrigopontes.wififiletransfer;

// Original code made by:
// https://github.com/RodrigoDLPontes/WiFiFileTransfer

// Updated fork:
// https://github.com/ltrudu/WiFiFileTransfer

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MenuActivity extends AppCompatActivity {

	private static final int DEMO_PERMISSION = 1;
	private static final String[] DEMO_PERMISSIONS_LIST = new String[]{
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.INTERNET,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.ACCESS_WIFI_STATE
	};


	TextView instructionsTextView;
	TextView ipAddressTextView;
	ImageButton switchButton;
	ImageView wifiLed;
	ImageView hddLed;
	ImageButton settingsButton;
	AdView adView;

	static HttpFileServer httpFileServer;
	static String formattedIpAddress;
	static ConnectivityManager cm;
	static boolean hasConnection;
	static short port = 8080;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);

		checkPermissions();

		instructionsTextView = (TextView)findViewById(R.id.instructions_text_view);
		ipAddressTextView = (TextView)findViewById(R.id.ip_address_text_view);
		switchButton = (ImageButton)findViewById(R.id.switch_button);
		wifiLed = (ImageView)findViewById(R.id.wifi_led);
		hddLed = (ImageView)findViewById(R.id.hdd_led);
		adView = (AdView)findViewById(R.id.adView);

	}

	private void checkPermissions()
	{
		boolean shouldNotRequestPermissions = true;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
			for(String permission : DEMO_PERMISSIONS_LIST)
			{
				shouldNotRequestPermissions &= (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
			}
		}

		if (shouldNotRequestPermissions) {
			setupServer();
		}
		else
		{
			ActivityCompat.requestPermissions(this,DEMO_PERMISSIONS_LIST, DEMO_PERMISSION);
		}
	}

	private void setupServer()
	{
		new AsyncTask<Void, Void, AdRequest>() {
			@Override
			protected AdRequest doInBackground(Void... params) {
				return new AdRequest.Builder()
						.addTestDevice("F8D7EE5FF969EB12BE4735D286D3D767")
						.build();
			}

			@Override
			protected void onPostExecute(AdRequest request) {
				adView.loadAd(request);
			}
		}.execute();

		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		httpFileServer = new HttpFileServer(port, getApplicationContext(), this);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(new BroadcastReceiver() {
							 @Override
							 public void onReceive(Context context, Intent intent) {
								 hasConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
								 if(hasConnection) {
									 WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
									 final int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
									 formattedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
											 (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
									 switchButton.setImageResource(R.drawable.switch_button_off);
									 instructionsTextView.setText(R.string.serverOffline);
									 ipAddressTextView.setText("");
								 } else {
									 if(httpFileServer != null) httpFileServer.terminate();
									 instructionsTextView.setText(R.string.noActiveConnection);
									 ipAddressTextView.setText(R.string.pleaseConnectToWiFiFirst);
									 switchButton.setImageResource(R.drawable.switch_button_off);
								 }
							 }
						 }

				, intentFilter);
	}

	@Override
	protected void onResume() {
		super.onResume();
		hasConnection = cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI;
		if(hasConnection) {
			WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
			final int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
			formattedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
					(ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
			if(httpFileServer.isAlive()) {
				switchButton.setImageResource(R.drawable.switch_button_on);
				instructionsTextView.setText(R.string.typeInBrowser);
				ipAddressTextView.setText(formattedIpAddress + ":" + port);
			} else {
				switchButton.setImageResource(R.drawable.switch_button_off);
				instructionsTextView.setText(R.string.serverOffline);
				ipAddressTextView.setText("");
			}
		} else {
			instructionsTextView.setText(R.string.noActiveConnection);
			ipAddressTextView.setText(R.string.pleaseConnectToWiFiFirst);
			switchButton.setImageResource(R.drawable.switch_button_off);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(httpFileServer != null) httpFileServer.terminate();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
		switch (requestCode) {
			case DEMO_PERMISSION:
				boolean allPermissionGranted = true;
				for(int grantResult : grantResults)
				{
					allPermissionGranted &= (grantResult == PackageManager.PERMISSION_GRANTED);
				}
				if (allPermissionGranted) {
					setupServer();
				} else {
					ShowAlertDialog(MenuActivity.this, "Error", "Please grant the necessary permission to launch the application.");
				}
				return;
		}
	}

	private void ShowAlertDialog(Context context, String title, String message)
	{
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				context);

		// set title
		alertDialogBuilder.setTitle(title);

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton("OK",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						// if this button is clicked, close
						// current activity
						checkPermissions();
					}
				});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	public void switchButtonPressed(View view) {
		if(hasConnection) {
			if(httpFileServer.isAlive()) {
				switchButton.setImageResource(R.drawable.switch_button_off);
				instructionsTextView.setText(R.string.serverOffline);
				ipAddressTextView.setText("");
				httpFileServer.terminate();
			} else {
				switchButton.setImageResource(R.drawable.switch_button_on);
				instructionsTextView.setText(R.string.typeInBrowser);
				ipAddressTextView.setText(formattedIpAddress + ":" + port);
				httpFileServer.create();
			}
		}
	}

	public void settingsButtonPressed(View view) {
		Intent intent = new Intent(this, SettingsActivity.class);
		intent.putExtra("Port", port);
		startActivityForResult(intent, 0);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(data != null) {
			final boolean serverWasRunning = httpFileServer.isAlive();
			final int newPort = data.getIntExtra("Port", 8080);
			if(newPort != port) {
				port = (short)newPort;
				httpFileServer.terminate();
				httpFileServer = new HttpFileServer(port, getApplicationContext(), this);
				if(serverWasRunning) {
					httpFileServer.create();
				}
			}
		}
	}

	protected void activateWiFiLED() {
		new AsyncTask<Void, Boolean, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for(byte i = 0 ; i < 2 ; i++) {
					publishProgress(true);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					publishProgress(false);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Boolean... state) {
				super.onProgressUpdate(state);
				if(state[0]) {
					wifiLed.setImageResource(R.drawable.led_on);
				} else {
					wifiLed.setImageResource(R.drawable.led_off);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	protected void activateHDDLED() {
		new AsyncTask<Void, Boolean, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				for(byte i = 0 ; i < 2 ; i++) {
					publishProgress(true);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
					publishProgress(false);
					try {
						Thread.sleep(50);
					} catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(Boolean... state) {
				super.onProgressUpdate(state);
				if(state[0]) {
					hddLed.setImageResource(R.drawable.led_on);
				} else {
					hddLed.setImageResource(R.drawable.led_off);
				}
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void alertAlert(String msg) {
		new AlertDialog.Builder(MenuActivity.this)
				.setTitle("Permission Request")
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// do somthing here
					}
				})
				.show();
	}
}
