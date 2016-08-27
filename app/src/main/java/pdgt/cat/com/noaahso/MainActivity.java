package pdgt.cat.com.noaahso;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ActionMenuView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.*;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
//import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import pdgt.cat.com.noaahso.BusinessObjects.HaulEvent;

import com.google.maps.android.PolyUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener,
        OnMapReadyCallback {


    Context context = this;

    Button dbButton;

    //weather variables
    //forecast views for horizontalscrollview
    TextView[] hourText;
    TextView[] tempText;
    ImageView[] iconView;
    //current
    TextView currTemp;
    ImageView currIcon;
    LinearLayout[] childLayout;
    Button button;
    Button alertButton;
    LinearLayout parentLayout;

    LinearLayout layForSeek;

    public SeekBar weatherSeek;
    //public SeekBar dumpRadSeek;
    public static int viewCount = 16; //ideal views for a 12 hour shift Ex. 8am - 8pm
    public static int shiftStart = 8;
    Activity activity;

    WeatherTask WunWeather;

    //haul route variables

    public HaulType type = HaulType.DeadTravel;
    TextView currLoc;
    TextView tol;
    TextView cycle;

    public enum HaulType {
        TravelEmpty,
        TravelFull,
        DeadTravel;
    }

    public int load_num = 0;
    public int dump_num = 0;
    public int dumpZone_num = 0;
    public int index;
    Button loadBut;
    Button dumpBut;
    Button pause;
    Button drawRoute;
    public List<OnNewLocationRaisedListener> locationListeners;
    public final int startDrawing = 1; //cycle # before you start drawing. route assumed stable after 1 cycle

    GoogleMap mMap = null;

    public Polyline fullLine;
    public Polyline empLine;

    LocationManager mLocationManager;

    GoogleApiClient m_GoogleApiClient;

    public SeekBar toleranceBar; //tolerance bar for debug, sets interval for virtual bound grid

    public ArrayList<LatLng> averagedFullRoutes;
    public ArrayList<LatLng> averagedEmptyRoutes;

    ArrayList<Polyline> badFullLines = new ArrayList<>();
    ArrayList<Polyline> badEmptyLines = new ArrayList<>();


    Location m_CurrentLocation = null;

    Location mLastLocation;
    Location prevLocation;

    LocationRequest m_LocationRequest;
    boolean isGPSEnabled = false;

    public boolean mapDisabled = false;

    public Double lat = 0.0;
    public Double lon = 0.0;

    public boolean record = false;
    public int tolerance = 5; //meters

    public ArrayList<LatLng> drawnRoute = new ArrayList<>();
    public PolylineOptions drawnRouteOpt = new PolylineOptions();

    public RelativeLayout dumpRelLay;

    public ArrayList<Circle> dumpZones = new ArrayList<>();
    public ArrayList<Marker> dumpZoneMarks = new ArrayList<>();
    public ArrayList<SeekBar> dumpRadii = new ArrayList<>();
    public int dumpCount = 0;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        activity = this;

        setContentView(R.layout.activity_main);

        parentLayout = (LinearLayout) findViewById(R.id.parentLayout);
        LinearLayout topLayout = (LinearLayout) findViewById(R.id.topLayout);
        HorizontalScrollView hsv = (HorizontalScrollView) findViewById(R.id.hsvView);
        LinearLayout linLay = (LinearLayout) findViewById(R.id.linLay);

        dumpRelLay = (RelativeLayout) findViewById(R.id.dumpRelLay);
        //weatherseek bar shows how far you are in shift
        weatherSeek = (SeekBar) findViewById(R.id.seekBar);
        weatherSeek.setMax((viewCount - 1) * 12);
        weatherSeek.setProgress(0);
        //weatherSeek.incrementProgressBy(1);

        //for haul route virtual bounds interval
        toleranceBar = (SeekBar) findViewById(R.id.seekBar2);
        toleranceBar.setMax(20);
        toleranceBar.setProgress(10); //meters

        layForSeek = (LinearLayout) findViewById(R.id.linLay3);

        /*dumpRadSeek = (SeekBar) findViewById(R.id.dumpZoneSeek);
        dumpRadSeek.setMax(50); //max radius
        dumpRadSeek.setProgress(10); //default 10 meters
*/
        currTemp = (TextView) findViewById(R.id.currTemp);
        currIcon = (ImageView) findViewById(R.id.currTemp_icon);
        cycle = (TextView) findViewById(R.id.textView8);

        hsv.requestLayout();


        tol = (TextView) findViewById(R.id.textView7);
        currLoc = (TextView) findViewById(R.id.textView6);

        childLayout = new LinearLayout[viewCount];
        hourText = new TextView[viewCount];
        tempText = new TextView[viewCount];
        iconView = new ImageView[viewCount];

        Integer childPadding = (int) getResources().getDimension(R.dimen.child_margin);

        for (int i = 0; i < viewCount; i++) {

            childLayout[i] = new LinearLayout(this);
            childLayout[i].setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            params.gravity = Gravity.CENTER_VERTICAL;
            params.setMargins(childPadding, childPadding, childPadding, childPadding);
            childLayout[i].setLayoutParams(params);

            hourText[i] = new TextView(this);
            tempText[i] = new TextView(this);
            iconView[i] = new ImageView(this);

            childLayout[i].addView(hourText[i]);
            childLayout[i].addView(tempText[i]);
            childLayout[i].addView(iconView[i]);

            childLayout[i].requestLayout();
            linLay.addView(childLayout[i]);

            linLay.requestLayout();

        }
        hsv.requestLayout();
        parentLayout.requestLayout();

        button = (Button) findViewById(R.id.button);//weather button
        button.setText("Get Weather");
        button.requestLayout();

        WeatherDBAccess._context = this;

        dbButton = (Button) findViewById(R.id.button2); //save data to db

        hsv.requestLayout();

        //set to invisible unless there's a severe weather alert
        alertButton = new Button(context);
        alertButton.setTextColor(Color.RED);
        alertButton.setText("Severe Weather Alert. Click For Details");
        topLayout.addView(alertButton);
        alertButton.setVisibility(View.GONE);

        //haul route buttons
        loadBut = (Button) findViewById(R.id.button3);

        dumpBut = (Button) findViewById(R.id.button4);

        pause = (Button) findViewById(R.id.button5);

        drawRoute = (Button) findViewById(R.id.button6);

        turnOnGps();

        buildGoogleApiClient();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFrag);


        mapFragment.getMapAsync(this);

        locationListeners = new ArrayList<OnNewLocationRaisedListener>();
        locationListeners.add(WeatherDBAccess.Instance());


        //weather button
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //WeatherEvent wEvent
                GPSTracker gpsTrack = new GPSTracker(getApplicationContext());
                lat = gpsTrack.getLatitude();
                lon = gpsTrack.getLongitude();
                //gpsTrack.stopUsingGPS();
                Log.d("location", lat + ", " + lon);
                //may want to listen for good network connection response first
                if (lat != 0.0 || lon != 0.0) {
                    ConnectivityManager check = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    Network[] networks = check.getAllNetworks();
                    //System.out.println(networks.length); //check number of networks bein pickedup

                    if (networks != null && networks.length > 0) {
                        boolean connectionAvailable = false;
                        for (int i = 0; i < networks.length; i++) {
                            //System.out.println(networks[i]);
                            NetworkInfo netinfo = check.getNetworkInfo(networks[i]);
                            String netType = netinfo.getTypeName();
                            if (netType.equals("WIFI") || netType.equals("MOBILE")) {
                                if (connectionAvailable) {
                                    break;
                                } else {
                                    connectionAvailable = true;
                                    break;
                                }
                            } else {
                                connectionAvailable = false;
                            }
                        }

                        if (connectionAvailable) {
                            updateWeather();
                            weatherUpdateTime.start();
                            button.setEnabled(false);

                        } else {
                            Toast toast = Toast.makeText(context, "No internet connection...", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                } else {
                    Toast toast = Toast.makeText(context, "No gps available...", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });


        //todo: remove for production
        dbButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportDB();

            }
        });

        //button runs async to update map and do all haul route functionality
        loadBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Load")
                        .build());
                record = true;
                type = HaulType.TravelFull;
                load_num++;
                cycle.setText("Cycle #" + load_num);
                loadBut.setEnabled(false);
                dumpBut.setEnabled(true);
                mMap.moveCamera((CameraUpdateFactory.newLatLngZoom(new LatLng(m_CurrentLocation.getLatitude(), m_CurrentLocation.getLongitude()), 16)));
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(m_CurrentLocation.getLatitude(), m_CurrentLocation.getLongitude()))
                        .title("Load " + load_num))
                        .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                WeatherDBAccess.Instance().EnsureDBIsOpen();
                if (dump_num > startDrawing) {
                    AverageRoutes routes = new AverageRoutes(averagedEmptyRoutes, WeatherDBAccess.Instance().getBaseRoute(dump_num - 1, HaulType.TravelEmpty), HaulType.TravelEmpty, false);
                    routes.execute();
                } else if (dump_num == startDrawing) {
                    averagedEmptyRoutes = WeatherDBAccess.Instance().getBaseRoute(startDrawing, HaulType.TravelEmpty);
                }
            }
        });

        dumpBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("Dump")
                        .build());
                record = true;
                type = HaulType.TravelEmpty;
                dump_num++;
                dumpBut.setEnabled(false);
                loadBut.setEnabled(true);
                mMap.moveCamera((CameraUpdateFactory.newLatLngZoom(new LatLng(m_CurrentLocation.getLatitude(), m_CurrentLocation.getLongitude()), 16)));
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(m_CurrentLocation.getLatitude(), m_CurrentLocation.getLongitude()))
                        .title("Dump " + dump_num))
                        .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));

                WeatherDBAccess.Instance().EnsureDBIsOpen();

                if (load_num > startDrawing) {
                    AverageRoutes routes = new AverageRoutes(averagedFullRoutes, WeatherDBAccess.Instance().getBaseRoute(load_num - 1, HaulType.TravelFull), HaulType.TravelFull, false);
                    routes.execute();
                } else if (load_num == startDrawing) {
                    averagedFullRoutes = WeatherDBAccess.Instance().getBaseRoute(startDrawing, HaulType.TravelFull);
                }

            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record = false;
                type = HaulType.DeadTravel;
                loadBut.setEnabled(true);
                dumpBut.setEnabled(true);
            }
        });

        drawRoute.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mapDisabled) {
                    mapDisabled = false;
                    drawRoute.setText("Map Gestures Enabled. Click to Disable");
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Button")
                            .setAction("Draw Route")
                            .setLabel("Map Gestures Enabled")
                            .build());
                } else {
                    mapDisabled = true;
                    drawRoute.setText("Map Gestures Disabled. Click to Enable.");
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Button")
                            .setAction("Draw Route")
                            .setLabel("Map Gestures Disabled")
                            .build());
                }
            }
        });

        //

        FrameLayout fram_map = (FrameLayout) findViewById(R.id.fram_map);

        fram_map.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, @NonNull MotionEvent event) {

                float x = event.getX();
                float y = event.getY();
                LatLng pt = mMap.getProjection().fromScreenLocation(new Point((int) x, (int) y));

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mapDisabled) {
                            drawnRoute.add(pt);
                            mMap.addPolyline(drawnRouteOpt.add(pt));
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //check for difference in prev once
                        if (!pt.equals(drawnRoute.get(drawnRoute.size() - 1))) {
                            drawnRoute.add(pt);
                            mMap.addPolyline(drawnRouteOpt.add(pt));
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        drawnRoute.add(pt);
                        mMap.addPolyline(drawnRouteOpt.add(pt));
                        break;
                }

                if (mapDisabled) {
                    return true;
                } else return false;
            }
        });

        /*dumpRadSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                dum
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });*/




    }

    @Override
    protected void onStart() {
        super.onStart();
        m_GoogleApiClient.connect();

    }

    @Override
    protected void onResume() {
        super.onResume();

        //Log.i(TAG, "Setting screen name: Main");
        mTracker.setScreenName("Image~ Main");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

    }

    protected synchronized void buildGoogleApiClient() {
        //check if Google Play Services is available
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            Log.d("Google Play Status", "Available");
        } else {
            String errorString = GoogleApiAvailability.getInstance().getErrorString(status);
            int errorDialogCode = 4;
            Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, status, errorDialogCode);
            errorDialog.show();
            Log.d("Google Play Status", "Not Available");
            //Log.d("Google Play Error Status", errorString);
        }
        //isGooglePlayServicesAvailable(this);
        m_GoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        m_LocationRequest = new LocationRequest();
        m_LocationRequest.setInterval(1000); // 2.5 seconds
        m_LocationRequest.setFastestInterval(1000); //1 second
        m_LocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //todo: remove for production
    private void exportDB() {
        try {
            File sd = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!sd.exists())
                sd.mkdirs();
            File data = Environment.getDataDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "//data//" + getPackageName() + "//databases//HSO_033016.sqlite";
                String backupDBPath = "//HSO DATABASE//HSO_033016.sqlite";
                File currentDB = new File(data, currentDBPath);
                File backupDB = new File(sd, backupDBPath);
                backupDB.getParentFile().mkdirs();

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();

                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();

                    Toast.makeText(getApplicationContext(), "Database Copied To Public Documents", Toast.LENGTH_LONG).show();
                } else {
                    Log.d("MainActivity", "Current DB doesn't exist " + currentDB.getPath());
                }
            } else {
                Log.d("MainActivity", "Cannot write to SD path " + sd.getPath());
            }
        } catch (Exception e) {
            Log.d("MainActivity", "Failed to export DB " + e.getMessage());
        }
    }

    protected void turnOnGps() {
        try {
            mLocationManager = (LocationManager)
                    this.getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = mLocationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            /* isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);*/

            //if (!isGPSEnabled && !isNetworkEnabled)
            if (!isGPSEnabled) {

            } else {
                //do nothing
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {

            WeatherDBAccess.Instance().finalize();
        } catch (Throwable throwable) {
            Log.d("DataService", "Unable to finalize DataAccess Instance: " + throwable.getMessage());
            throwable.printStackTrace();
        }
        stopLocationUpdates();
        //drawMap.cancel();

    }
    /*try {
        WeatherDBAccess.Instance().finalize();
    }catch(Throwable throwable) {
        Log.d("DataService", "Unable to finalize DataAccess Instance: " + throwable.getMessage());
        throwable.printStackTrace();
    }*/

    long ticker = 5 * 60000; //5 minutes
    int hour;
    int min;

    public CountDownTimer weatherUpdateTime = new CountDownTimer(ticker, ticker / 2) {

        @Override
        public void onTick(long millisUntilFinished) {
            //Toast.makeText(getApplicationContext(), " " + millisUntilFinished, Toast.LENGTH_LONG).show();
            //System.out.println("Ticked");
        }

        @Override
        public void onFinish() {
            updateWeather();
            weatherUpdateTime.start();
        }

    };

    public void updateWeather() {
        WunWeather = new WeatherTask(context, activity, childLayout,
                hourText, tempText, iconView, button, lat, lon, currTemp, currIcon, alertButton);
        WunWeather.execute();
        hour = (DateTime.now().getHourOfDay() - shiftStart) * 12;
        min = (DateTime.now().getMinuteOfHour()) / 5;
        weatherSeek.setProgress(hour + min);
    }

    //haul route stuff

    @Override
    public void onLocationChanged(Location location) {

        WeatherDBAccess._context = this;
        m_CurrentLocation = location;
        HaulEvent event = new HaulEvent();


        currLoc.setText("(" + location.getLatitude() + ", " + location.getLongitude() + ")");

        event.Latitude = location.getLatitude();
        event.Longitude = location.getLongitude();
        event.type = type;
        event.Time = WeatherDBAccess.Instance().GetCurrentTime();
        if (type == HaulType.TravelFull) {
            event.l_to_d_num = load_num;
        } else if (type == HaulType.TravelEmpty) {
            event.d_to_l_num = dump_num;
        }

        if (record) {
            LatLng currLatLng = new LatLng(m_CurrentLocation.getLatitude(), m_CurrentLocation.getLongitude());
            for (OnNewLocationRaisedListener listener : locationListeners) {
                listener.OnNewLocationRaised(event, currLatLng);
            }
        }

    }

    protected void startLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //DO OP WITH LOCATION SERVICE
            }
        }
        LocationServices.FusedLocationApi.
                requestLocationUpdates(m_GoogleApiClient, m_LocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.
                removeLocationUpdates(m_GoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //DO OP WITH LOCATION SERVICE
            }
        }
        mLastLocation = LocationServices.FusedLocationApi.
                getLastLocation(m_GoogleApiClient);
        if (mLastLocation != null) {

            lat = mLastLocation.getLatitude();
            lon = mLastLocation.getLongitude();
            Toast.makeText(this, "Last Location: " + Double.toString(lat) + ", " +
                    Double.toString(lon), Toast.LENGTH_LONG).show();
            prevLocation = mLastLocation;
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            boolean dumpZone = false;

            @Override
            public void onMapLongClick(final LatLng point) {

                AlertDialog.Builder fmPop = new AlertDialog.Builder(context);
                //fmPop.onCreateDialog().getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG);
                fmPop.setMessage("Create Dump Zone Here?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                dumpZone_num++;
                                dumpZone = true;
                                Marker dumpMark = mMap.addMarker(new MarkerOptions()
                                        .position(point)
                                        .title("Dump Zone " + dumpZone_num));
                                dumpZoneMarks.add(dumpMark);

                                final Circle circle = mMap.addCircle(new CircleOptions()
                                        .center(point)
                                        .radius(10)); //default 10 meter dump zone radius

                                dumpZones.add(circle);
                                System.out.println("dump zones size: " + dumpZones.size());

                                SeekBar dumpSeek = new SeekBar(getApplicationContext());
                                dumpSeek.setMax(200);
                                dumpSeek.setProgress(10);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(200,50);
                                dumpSeek.setLayoutParams(lp);
                                layForSeek.addView(dumpSeek);

                                dumpRadii.add(dumpSeek);
                                dumpSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    @Override
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        circle.setRadius(progress);
                                    }

                                    @Override
                                    public void onStartTrackingTouch(SeekBar seekBar) {

                                    }

                                    @Override
                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                        Toast toast = Toast.makeText(context, "Click on marker to finalize dump zone.", Toast.LENGTH_SHORT);
                                        toast.show();

                                    }
                                });

                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dumpZone = false;

                            }
                        }).create().show();


            }
        });


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {


            @Override
            public boolean onMarkerClick(final Marker marker) {
                boolean seeSeek = false;
                System.out.println("onMarkerClick");
                for (int i = 0; i < dumpZoneMarks.size(); i++) {
                    System.out.println("got in forloop");
                    System.out.println("Marker position: " + marker.getPosition() + " vs. dumpZone position: " + dumpZoneMarks.get(i).getPosition());
                    if(marker.equals(dumpZoneMarks.get(i))){

                        index = i;
                        System.out.println("index1: " + index);
                        if (dumpRadii.get(i).getVisibility() == View.GONE) {
                            seeSeek = false;
                        } else {
                            seeSeek = true; //can see seekbar associated with this dump
                        }
                        //dumpRadii.get(i).setVisibility(View.GONE);
                        break;
                    }
                }

                AlertDialog.Builder finalDump = new AlertDialog.Builder(context);
                finalDump.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }

                });
                String title = "";
                if (dumpRadii.get(index).getVisibility() == View.VISIBLE) {
                    finalDump.setMessage("Finalize Dump Bounds?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dumpRadii.get(index).setVisibility(View.GONE);
                                    System.out.println("index2: " + index);

                                }
                            });

                } else if(dumpRadii.get(index).getVisibility() == View.GONE) {
                    finalDump.setMessage("Dump Zone Options")
                            .setPositiveButton("Edit Dump Bounds", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dumpRadii.get(index).setVisibility(View.VISIBLE);
                                    System.out.println("index3: " + index);

                                    dumpRadii.get(index).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                        @Override
                                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                            for(int i = 0; i < dumpRadii.size(); i++){
                                                if(dumpRadii.get(i).equals(seekBar)){
                                                    index = i;
                                                    break;
                                                }
                                            }
                                            dumpZones.get(index).setRadius(progress);
                                        }

                                        @Override
                                        public void onStartTrackingTouch(SeekBar seekBar) {

                                        }

                                        @Override
                                        public void onStopTrackingTouch(SeekBar seekBar) {
                                            Toast toast = Toast.makeText(context, "Click on marker to finalize dump zone.", Toast.LENGTH_SHORT);
                                            toast.show();

                                        }
                                    });
                                }
                            });
                }
                finalDump.create().show();


                return true;
            }
        });

    }

    public interface OnNewLocationRaisedListener {
        void OnNewLocationRaised(HaulEvent event, LatLng latlon);
    }

    /*Sanat's connected site idea: Have foreman draw in desired haul route and notify foreman/
                operator when there's a deviation. For this case m_aBaseRoute would be the route drawn by the
                foreman
                 */
    class AverageRoutes extends AsyncTask<Void, Void, Void> {
//asynctask this
        //todo: modify to include foreman set route

        LinkedHashMap<LatLngBounds, ArrayList<LatLng>> mapOne = new LinkedHashMap<>();
        LinkedHashMap<LatLngBounds, ArrayList<LatLng>> mapTwo = new LinkedHashMap<>();

        ArrayList<LatLngBounds> subBounds = new ArrayList<>();

        public ArrayList<LatLng> one;
        public ArrayList<LatLng> two;

        public LatLngBounds.Builder builder = new LatLngBounds.Builder();
        public LatLngBounds bounds;

        public ArrayList<LatLng> avgRoute = new ArrayList<>();
        public ArrayList<ArrayList<LatLng>> badSpots = new ArrayList<>();

        double div = 11;//number to change if there are too many false positives
        HaulType phase;

        boolean foremanBase = false;


        public AverageRoutes(ArrayList<LatLng> one, ArrayList<LatLng> two, HaulType phase, boolean foremanBase) {
            //boolean foremanBase should be used to tell when a baseRoute "one" should not be averaged, will only output deviations then
            this.one = one;
            this.two = two;
            this.phase = phase;
            this.foremanBase = foremanBase;

            for (LatLng event : one) {
                builder.include(event);
                //System.out.println("locs: " + event.toString());

            }
            for (LatLng event : two) {
                builder.include(event);

            }

            bounds = builder.build();
        }

        @Override
        protected Void doInBackground(Void... params) {
            gridItUp();

            return null;
        }

        public void gridItUp() {
            System.out.println("gridItUp()");


            double latInterval = (bounds.northeast.latitude - bounds.southwest.latitude) / div;
            double lonInterval = (bounds.northeast.longitude - bounds.southwest.longitude) / div;

            for (double x = bounds.southwest.latitude; x <= bounds.northeast.latitude; x += latInterval) {
                for (double y = bounds.southwest.longitude; y <= bounds.northeast.longitude; y += lonInterval) {
                    subBounds.add(new LatLngBounds(new LatLng(x, y), new LatLng(x + latInterval, y + lonInterval)));
                }
            }
            for (LatLng point : one) {

                for (LatLngBounds testBound : subBounds) {

                    //creating two arrays that tell us bounds for each route so we know the order of the route
                    //gets messed up if there's a glitch in polyline points recorded
                    if (testBound.contains(point)) {
                        if (mapOne.containsKey(testBound)) {
                            mapOne.get(testBound).add(point);
                        } else {
                            ArrayList<LatLng> tempPtsOne = new ArrayList<>();
                            tempPtsOne.add(point);
                            mapOne.put(testBound, tempPtsOne);
                        }
                        break;
                    }

                }
                //!!! make sure the temp bounds and list of latlngs is actually being purged

            }
            for (LatLng point : two) {

                for (LatLngBounds testBound : subBounds) {

                    //creating two arrays that tell us bounds for each route so we know the order of the route
                    //gets messed up if there's a glitch in polyline points recorded or if theres an overlap in l to d and d to l roads
                    if (testBound.contains(point)) {
                        if (mapTwo.containsKey(testBound)) {
                            mapTwo.get(testBound).add(point);
                        } else {
                            ArrayList<LatLng> tempPtsOne = new ArrayList<>();
                            tempPtsOne.add(point);
                            mapTwo.put(testBound, tempPtsOne);
                        }
                        break;
                    }
                }
            }
            //now replace all the points in each bound with the average of the points and create new latlng of averages
            for (Map.Entry<LatLngBounds, ArrayList<LatLng>> entry : mapOne.entrySet()) {
                double latSum = 0.0;
                double lonSum = 0.0;
                //double size = entry.getValue().size();

                for (LatLng point : entry.getValue()) {

                    latSum += point.latitude;
                    lonSum += point.longitude;

                }
                LatLng avgPt = new LatLng(latSum / entry.getValue().size(), lonSum / entry.getValue().size());
                entry.getValue().clear();
                entry.getValue().add(avgPt);
                //System.out.println("avg pt for mapOne: " + avgPt.toString());

            }
            for (Map.Entry<LatLngBounds, ArrayList<LatLng>> entry : mapTwo.entrySet()) {
                double latSum = 0.0;
                double lonSum = 0.0;
                //double size = entry.getValue().size();

                for (LatLng point : entry.getValue()) {

                    latSum += point.latitude;
                    lonSum += point.longitude;

                }
                LatLng avgPt = new LatLng(latSum / entry.getValue().size(), lonSum / entry.getValue().size());
                entry.getValue().clear();
                entry.getValue().add(avgPt);
                //System.out.println("avg pt for mapTwo: " + avgPt.toString());
            }

            //now sort through
            int badArrayNum = 0;
            boolean canFinish = false;
            Object[] keys = mapTwo.keySet().toArray(); //latlngbounds
            //Object[] values = mapTwo.values().toArray(); //arraylists of latlng associated with each latlng
            for (int i = 0; i < mapTwo.size(); i++) {
                //for (int j = 0; j < mapOne.size(); j++) {
                if (mapOne.containsKey(keys[i])) {//if base route has i latlngbound in route 2

                    Double newLat = (mapOne.get(keys[i]).get(0).latitude + mapTwo.get(keys[i]).get(0).latitude) / 2.0; //should only be two points to average
                    Double newLon = (mapOne.get(keys[i]).get(0).longitude + mapTwo.get(keys[i]).get(0).longitude) / 2.0;
                    mapOne.get(keys[i]).clear();
                    mapOne.get(keys[i]).add(new LatLng(newLat, newLon));
                    //close out previous bad spot polyline

                    if (!badSpots.isEmpty() && canFinish) {

                        badSpots.get(badArrayNum).add(new LatLng(newLat, newLon));
                    }
                    canFinish = false;
                    //mapOne.get(keys[i])
                } else {//i latlngbound not found in base route

                    if (!PolyUtil.isLocationOnPath(mapTwo.get(keys[i]).get(0), one, true, 7)) {//seeing if point in bound isnt within 7 meter tolerance to base route, may be a foreman defined parameter
                        //bad polylines
                        if (!badSpots.isEmpty()) {//first time a bad latlng is found it will go to else, otherwise the following array will always be initialized already
                            if (badSpots.get(badArrayNum).get(badSpots.get(badArrayNum).size() - 1).equals(mapTwo.get(keys[i - 1]).get(0))) {
                                badSpots.get(badArrayNum).add(mapTwo.get(keys[i]).get(0));
                                //if the already started bad array contains the previous loc, then add the current loc

                            } else {//else start new bad spot array, initialize array at new index and add prev loc and curr loc/
                                badArrayNum++;
                                ArrayList<LatLng> newBadLocsList = new ArrayList<>();
                                newBadLocsList.add(mapTwo.get(keys[i - 1]).get(0));
                                newBadLocsList.add(mapTwo.get(keys[i]).get(0));
                                badSpots.add(newBadLocsList);

                            }
                        } else {//if badspots is empty
                            ArrayList<LatLng> firstBadLocList = new ArrayList<>();
                            if (i > 0) {//there will be a prev loc to add from prev latlngbound even if that latlngbound had a match to the base route
                                firstBadLocList.add(mapTwo.get(keys[i - 1]).get(0));
                            }
                            firstBadLocList.add(mapTwo.get(keys[i]).get(0));
                            badSpots.add(firstBadLocList);
                        }
                        canFinish = true;
                    }
                }
                // }
            }
            //loop through new mapOne and produce final arraylist of latlngs which is the averaged route
            //and now you can also create polylines showing bad spots

            for (Map.Entry<LatLngBounds, ArrayList<LatLng>> entry : mapOne.entrySet()) {
                for (LatLng point : entry.getValue()) {
                    avgRoute.add(point);
                }
            }
        }

        @Override
        protected void onPostExecute(Void v) {
            //clear out old averaged lines

            if (phase == HaulType.TravelFull) {

                if (load_num > startDrawing + 1) {
                    fullLine.remove();//removes previous line
                    for (Polyline bad : badFullLines) {
                        bad.remove();
                    }
                }
                if (!foremanBase) {
                    averagedFullRoutes = avgRoute;
                    fullLine = mMap.addPolyline(new PolylineOptions().addAll(avgRoute)
                            .geodesic(true)
                            .color(Color.DKGRAY)
                            .clickable(true).zIndex(3));
                }
                for (ArrayList<LatLng> maloSpot : badSpots) {
                    System.out.println("maloSpot: " + maloSpot.toString());

                    badFullLines.add(mMap.addPolyline(new PolylineOptions().addAll(maloSpot)
                            .color(Color.MAGENTA)
                            .geodesic(true).clickable(true).zIndex(4)));
                }
            } else if (phase == HaulType.TravelEmpty) {

                if (dump_num > startDrawing + 1) {
                    empLine.remove(); //removes previous line
                    for (Polyline bad : badEmptyLines) {
                        bad.remove();
                    }
                }
                if (!foremanBase) {
                    averagedEmptyRoutes = avgRoute;
                    empLine = mMap.addPolyline(new PolylineOptions().addAll(avgRoute)
                            .geodesic(true)
                            .color(Color.LTGRAY)
                            .clickable(true).zIndex(3));
                }
                for (ArrayList<LatLng> maloSpot : badSpots) {
                    System.out.println("maloSpot: " + maloSpot.toString());

                    badEmptyLines.add(mMap.addPolyline(new PolylineOptions().addAll(maloSpot)
                            .color(Color.RED)
                            .geodesic(true).clickable(true).zIndex(4)));
                }
            }
        }
    }

}



