package br.ufc.syssuclienttest2;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import br.ufc.great.syssu.base.Pattern;
import br.ufc.great.syssu.base.Tuple;
import br.ufc.great.syssu.base.interfaces.IClientReaction;
import br.ufc.great.syssu.base.interfaces.IFilter;
import br.ufc.great.syssu.base.interfaces.ISysSUService;

public class SysSUClientTestActivity extends Activity {
	private static final String TAG = "AIDLDemo";
	private ISysSUService service;

	private MyServiceConnection connection;

	private Button startButton;
	private Button stopButton;
	private Button showButton;
	private Button configReactionButton;
	private TextView tv;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		startButton = (Button) findViewById(R.id.startButton);
		stopButton = (Button) findViewById(R.id.stopButton);
		showButton = (Button) findViewById(R.id.showButton);
		configReactionButton = (Button) findViewById(R.id.buttonReaction);
		tv = (TextView) findViewById(R.id.tv1);

		initService();

		startButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Tupla de interesse em dados
				Tuple t = (Tuple) new Tuple().addField("AppId",
						"HelloWorldLoCCAM").addField("InterestElement",
						"context.device.gpslocation");
				// .addField("InterestElement",
				// "context.device.gpslocation");
//				leGPS();

				// Publica interesse
				try {
					service.put(t);
					Toast.makeText(SysSUClientTestActivity.this,
							"Interesse publicado", Toast.LENGTH_LONG).show();
				} catch (RemoteException e) {
					Toast.makeText(SysSUClientTestActivity.this,
							"Erro ao publicar interesse", Toast.LENGTH_LONG)
							.show();
					e.printStackTrace();
				}

			}
		});

		stopButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Padr�o de interesse em dados
				Pattern p = (Pattern) new Pattern().addField("AppId",
						"HelloWorldLoCCAM").addField("InterestElement",
						"context.device.gpslocation");
				// .addField("InterestElement",
				// "context.device.gpslocation");
				;

				// Retira interesse
				try {
					service.take(p, null);
					Toast.makeText(SysSUClientTestActivity.this,
							"Interesse retirado", Toast.LENGTH_LONG).show();
				} catch (RemoteException e) {
					Toast.makeText(SysSUClientTestActivity.this,
							"Erro ao retirar interesse", Toast.LENGTH_LONG)
							.show();
					e.printStackTrace();
				}

			}
		});

		showButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Padrão de interesse em dados
				Pattern p = (Pattern) new Pattern()
					.addField("ContextKey", "context.device.gpslocation");
					
				// Lê interesse
				try {
					List<Tuple> tuples = service.read(p, null);
					tv.setText((tuples.size() > 0) ? generateString(tuples.get(0)) : "tuple.size() == 0");
//					Toast.makeText(SysSUClientTestActivity.this, (tuples.size() > 0) ? generateString(tuples.get(0)) : "null" , Toast.LENGTH_LONG).show();
				} catch (RemoteException e) {
					Toast.makeText(SysSUClientTestActivity.this, "Erro ao ler informação", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		});
		

		configReactionButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Toast.makeText(SysSUClientTestActivity.this,
						"Configuring reaction", Toast.LENGTH_LONG).show();

				// Padr�o de interesse em dados
				Pattern p = (Pattern) new Pattern().addField("ContextKey",
						"context.device.gpslocation");

				// L� interesse
				try {
					service.subscribe(new IClientReaction.Stub() {
						public void react(Tuple tuple) throws RemoteException {
							// Toast.makeText(SysSUClientTestActivity.this,
							// "REACT: " + generateString(tuple),
							// Toast.LENGTH_LONG).show();
							System.out.println("REACT: "
									+ generateString(tuple));
						}
					}, "put", p, new IFilter.Stub() {
						public boolean filter(Tuple tuple)
								throws RemoteException {
							for (int i = 0; i < tuple.size(); i++) {
								if (tuple.getField(i).getName()
										.equalsIgnoreCase("Values")) {
									@SuppressWarnings("unchecked")
									List<String> values = (List<String>) tuple
											.getField(i).getValue();

									int v = Integer.parseInt(values.get(0));

									if (v > 10)
										return true;
								}
							}

							return false;
						}
					});

					// service.subscribe(new IClientReaction.Stub() {
					// public void react(Tuple arg0) throws RemoteException {
					// System.out.println("TAKE REACTION - TUPLE: " +
					// arg0.getField(0).getName() + " - " +
					// arg0.getField(0).getValue());
					// }
					// },
					// "put",
					// (Pattern)new Pattern().addField("?", "?"),
					// null);

				} catch (RemoteException e) {
					Toast.makeText(SysSUClientTestActivity.this,
							"Erro na subscri��o", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
			}
		});
	}

	class MyServiceConnection implements ServiceConnection {

		public void onServiceConnected(ComponentName name, IBinder boundService) {
			service = ISysSUService.Stub.asInterface(boundService);
		}

		public void onServiceDisconnected(ComponentName name) {
			service = null;
		}
	}

	@SuppressLint("InlinedApi")
	private void initService() {
		connection = new MyServiceConnection();
		Intent i = new Intent();
		i.setClassName("br.ufc.great.loccam",
				"br.ufc.great.loccam.service.SysSUService");
		boolean ret = bindService(i, connection, Context.BIND_ABOVE_CLIENT);
		Log.d(TAG, "initService() bound with " + ret);
	}

	private void releaseService() {
		unbindService(connection);
		connection = null;
		Log.d(TAG, "releaseService() unbound.");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseService();
	}

	private final IFilter.Stub filter = new IFilter.Stub() {

		public boolean filter(Tuple tuple) throws RemoteException {
			return tuple.getField(0).getValue().toString().indexOf("4") >= 0;
		}
	};

	public static String generateString(List<Tuple> tuples) {
		String r = "";

		if (tuples != null && !tuples.isEmpty()) {
			for (Tuple tuple : tuples) {
				r += generateString(tuple) + "\n\n";
			}

			r = r.substring(0, r.length() - 2);
		} else {
			r = "NULL";
		}

		return r;
	}

	public static String generateString(Tuple tuple) {
		String r = "{\n";
		for (int i = 0; i < tuple.size(); i++) {
			r += "(" + tuple.getField(i).getName() + ","
					+ tuple.getField(i).getValue() + "),\n";
		}
		Log.v("lana", "TupleSize: "+tuple.size());
		r = r.substring(0, r.length()-2) + "\n}";
		return r;
	}

	public void leGPS() {
		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				// Called when a new location is found by the network location
				// provider.
				tv.setText("GPS: " + location.getLatitude()
						+ location.getLongitude() + "\n");
				// makeUseOfNewLocation(location);
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}
}