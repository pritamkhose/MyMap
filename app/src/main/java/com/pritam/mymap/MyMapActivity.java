package com.pritam.mymap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MyMapActivity extends FragmentActivity implements android.location.LocationListener, OnMarkerDragListener, ConnectionCallbacks, OnConnectionFailedListener {

    private GoogleMap googleMap;
    Marker myMarker;

    double lati = 0.0d, longi = 0.0d;
    Location myLocation;
    boolean markerOff = true, FORMACC = true;
    protected static final String TAG = "basic-location-sample";

    android.location.Address address;
    static LatLng point;

    /**
     * Provides the entry point to Google Play services.
     */
    protected GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map123);

        getActionBar().hide();
        // check for Internet status
        boolean isInternetPresent = Utility.isConnected(MyMapActivity.this);
        if (isInternetPresent) {
            //Toast.makeText(getBaseContext(), "ok", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getBaseContext(), "Check your Internet connection and try again", Toast.LENGTH_LONG).show();
        }

        try {
            mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), "mGoogleApiClient Exception --> " + e.toString(), Toast.LENGTH_LONG).show();
        }


        Intent intent = getIntent();

        if (intent.getStringExtra("FRM") != null && intent.getStringExtra("FRM").length() > 0)
            if (intent.getStringExtra("FRM").equals("LeadAddrActivity"))
                FORMACC = false;

        if (intent.getStringExtra("eid") != null && intent.getStringExtra("eid").length() > 0)
            eid = intent.getStringExtra("eid").toString();

        if (intent.getStringExtra("latitude") != null && intent.getStringExtra("latitude").length() > 0)
            lati = Double.parseDouble(intent.getStringExtra("latitude"));

        if (intent.getStringExtra("longitude") != null && intent.getStringExtra("longitude").length() > 0)
            longi = Double.parseDouble(intent.getStringExtra("longitude"));

        if (lati != 0.0 && longi != 0.0)
            markerOff = false;

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        } else { // Google Play Services are available

            // Getting GoogleMap object from the fragment
            //googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            // Enabling MyLocation Layer of Google Map
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            //http://www.programcreek.com/java-api-examples/index.php?class=com.google.android.gms.maps.GoogleMap&method=setMyLocationEnabled
            googleMap.getUiSettings().setAllGesturesEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);

            googleMap.setTrafficEnabled(true);

            googleMap.setOnMapClickListener(new OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point) {
                    //Toast.makeText(getApplicationContext(), "Map clicked",Toast.LENGTH_SHORT).show();
                    markerOff = false;

                    // https://developer.android.com/reference/android/location/Geocoder.html
                    try {
                        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                        MyMapActivity.point = point;

                        List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);

                        // android.location.Address
                        if (addresses != null && addresses.size() > 0) {
                            address = addresses.get(0);

                            if (address != null) {
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                                    sb.append(address.getAddressLine(i) + "\n");
                                }
                                MapAddr = sb.toString().replaceAll("\n", " ").replaceAll("null", " ");
                                ((TextView) findViewById(R.id.tv_location)).setText("Address : " + MapAddr);
                            } else
                                ((TextView) findViewById(R.id.tv_location)).setText("Address : Not Found");
                        } else
                            ((TextView) findViewById(R.id.tv_location)).setText("Address : Not Found");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        drawMarker(point);
                    } catch (Exception e) {
                    }

                }
            });

            LocationManager locationManager;
            String mprovider;

            GPSTracker gps = new GPSTracker(this);
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // check if GPS enabled
            if (gps.canGetLocation()) {

                if (gps.getLatitude() != 0 && gps.getLongitude() != 0) {
                    lati = gps.getLatitude();
                    longi = gps.getLongitude();

                    //((SBCrmApplication) this.getApplication()).setLongitude(longi);
                    //((SBCrmApplication) this.getApplication()).setLatitude(lati);
                }

                // Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
                //getAddress(gps.getLocation());
            } else {
                // can't get location GPS or Network is not enabled
                // Ask user to enable GPS/network in settings
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    //gps.showSettingsAlert();
                    showSettingsAlert("Enable Location Access");
            }

            Criteria criteria = new Criteria();
            mprovider = locationManager.getBestProvider(criteria, false);
            if (mprovider != null && !mprovider.equals("")) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Location location = locationManager.getLastKnownLocation(mprovider);
                locationManager.requestLocationUpdates(mprovider, 1, 1, this);
                if (location != null)
                    onLocationChanged(location);
                {
                    boolean gps_enabled = false;
                    boolean network_enabled = false;
                    try {
                        gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch (Exception ex) {
                    }
                    try {
                        network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    } catch (Exception ex) {
                    }
                    // notify user
//                    if (!gps_enabled || !network_enabled)
//                        showSettingsAlert();

                    if (!network_enabled)
                        showSettingsAlert("Network Location is Disable");
                    else if (!gps_enabled)
                        showSettingsAlert("GPS is Disable");


                }
            }

        }

    }

    private boolean canToggleGPS() {
        PackageManager pacman = getPackageManager();
        PackageInfo pacInfo = null;

        try {
            pacInfo = pacman.getPackageInfo("com.android.settings", PackageManager.GET_RECEIVERS);
        } catch (PackageManager.NameNotFoundException e) {
            return false; //package not found
        }

        if (pacInfo != null) {
            for (ActivityInfo actInfo : pacInfo.receivers) {
                //test if recevier is exported. if so, we can toggle GPS.
                if (actInfo.name.equals("com.android.settings.widget.SettingsAppWidgetProvider") && actInfo.exported) {
                    return true;
                }
            }
        }

        return false; //default
    }

    @Override
    public void onLocationChanged(Location location) {

        LatLng latLng;
        double latitude;
        double longitude;

        if (markerOff) {
            // Getting latitude of the current location
            latitude = location.getLatitude();

            // Getting longitude of the current location
            longitude = location.getLongitude();

            lati = latitude;
            longi = longitude;

            this.myLocation = location;

            // Creating a LatLng object for the current location
            latLng = new LatLng(latitude, longitude);

        } else {
            latitude = lati;
            longitude = longi;
            latLng = new LatLng(latitude, longitude);
        }

        System.out.println("urlstr markerOff -- > " + markerOff + "\t" + String.valueOf(latitude) + "\t" + String.valueOf(longitude) + "\t" + latLng.toString());
        // Showing the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        // Setting latitude and longitude in the TextView tv_location

        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            // android.location.Address
            if (addresses != null && addresses.size() > 0) {
                address = addresses.get(0);

                if (address != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {

                        sb.append(address.getAddressLine(i) + " ");
                    }
                    MapAddr = sb.toString().replaceAll("\n", " ").replaceAll("null", " ");
                    ((TextView) findViewById(R.id.tv_location)).setText("Address : " + MapAddr);
                } else
                    ((TextView) findViewById(R.id.tv_location)).setText("Address : Not Found");
            } else
                ((TextView) findViewById(R.id.tv_location)).setText("Address : Not Found");
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            point = latLng;
            drawMarker(point);
        } catch (Exception e) {
            // Toast.makeText(getApplicationContext(), "No marker current position", Toast.LENGTH_LONG).show();
        }

        googleMap.setOnMarkerDragListener(this);


    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
       // MenuInflater inflater = getMenuInflater();
       // inflater.inflate(R.menu.blankmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
       /* if (id == R.id.action_save) {
            callSave(null);
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMarkerDrag(Marker arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        // TODO Auto-generated method stub
        LatLng markerPos = marker.getPosition();
        // Setting latitude and longitude in the TextView tv_location
        //((TextView) findViewById(R.id.tv_location)).setText("Latitude:" + markerPos.latitude + ", Longitude:" + markerPos.longitude);
        lati = markerPos.latitude;
        longi = markerPos.longitude;
    }

    @Override
    public void onMarkerDragStart(Marker arg0) {
        // TODO Auto-generated method stub

    }

    void drawMarker(LatLng point) {
        try {
            myMarker.remove();
        } catch (Exception e) {
        }

        String s = "";//((SBCrmApplication) this.getApplication()).getName();
        if (!(s.length() > 0 && s != null))
            s = "My Location";

        String t = "";//((SBCrmApplication) this.getApplication()).getFullAddress();
        if (!(t.length() > 0 && t != null))
            t = "";

        myMarker = googleMap.addMarker(new MarkerOptions()
                        .position(point)
                        .title(s)
                        //.snippet(t)
                        .draggable(true)
                        .alpha(0.6f)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                //.icon(BitmapDescriptorFactory.fromResource(R.drawable.jkmarker))
        );

    }


    public void callSave(View view) {

        if (address != null) {
            String addressText1 = "";
            for (int i = 0; i < address.getMaxAddressLineIndex() - 1; i++) {
                if (address.getAddressLine(i).toString().length() > 0 && address.getAddressLine(i) != null) {
                    if (address.getAddressLine(i).toLowerCase().startsWith(addressText1.toLowerCase()))
                        addressText1 = address.getAddressLine(i);
                    else
                        addressText1 = addressText1 + ", " + address.getAddressLine(i);
                }
            }

            String mAddress = "";
            if (addressText1.length() > 2 && addressText1 != null && addressText1.endsWith(", "))
                mAddress = addressText1.substring(0, addressText1.length() - 2);
            else
                mAddress = addressText1;

            HashMap<String, String> hm = new HashMap<String, String>();
            if (mAddress.length() > 0 && mAddress != null)
                hm.put("mAddress", mAddress);
            else
                hm.put("mAddress", "");
            if (address.getSubLocality() != null)
                hm.put("subLocality", address.getSubLocality());
            else
                hm.put("subLocality", "");
            if (address.getSubLocality() != null)
                hm.put("locality", address.getLocality());
            else
                hm.put("locality", "");
            if (address.getSubLocality() != null)
                hm.put("subAdminArea", address.getSubAdminArea());
            else
                hm.put("subAdminArea", "");
            if (address.getSubLocality() != null)
                hm.put("adminArea", address.getAdminArea());
            else
                hm.put("adminArea", "");
            if (address.getSubLocality() != null)
                hm.put("countryName", address.getCountryName());
            else
                hm.put("countryName", "");
            if (address.getSubLocality() != null)
                hm.put("postalCode", address.getPostalCode());
            else
                hm.put("postalCode", "");
            hm.put("alati", String.valueOf(point.latitude));

            hm.put("alongi", String.valueOf(point.longitude));


            if (hm.isEmpty())
                Toast.makeText(getApplicationContext(), "Please Select Location", Toast.LENGTH_LONG).show();
            else {

//                ((SBCrmApplication) this.getApplication()).setLatitude(lati);
//                ((SBCrmApplication) this.getApplication()).setLongitude(longi);
//
//                ((SBCrmApplication) getApplicationContext()).setMapLocation(hm);


                String locationStr = "&lo=" + String.valueOf(longi) + "&la=" + String.valueOf(lati)
                        + "&addr=" + MapAddr.replaceAll("&", "");
                //doSave(locationStr);
            }
        } else
            Toast.makeText(getApplicationContext(), "No Address Found", Toast.LENGTH_LONG).show();

    }

    String eid = "", MapAddr = "";

   /* public void doSave(String locationStr) {

//        if (eid == null || eid.length() == 0)
//            eid = ((SBCrmApplication) this.getApplication()).getACCEID();

        if (!(eid.length() > 0 && eid != null)) {
            Toast.makeText(this, "Save Info page First", Toast.LENGTH_SHORT).show();
            onBackPressed();
        } else {
            String urlstr = "";

            if (FORMACC)
                urlstr += "e=" + ApplConstants.ACCOUNT_LALO + "&a=" + ApplConstants.ACT_EDIT_SAVE + "&i=" + eid + "&ac=" + ((SBCrmApplication) this.getApplication()).getAccDB() + "&ui=" + ((SBCrmApplication) this.getApplication()).getUID();
            else
                urlstr += "e=" + ApplConstants.LEAD_LALO + "&a=" + ApplConstants.ACT_EDIT_SAVE + "&i=" + eid + "&ac=" + ((SBCrmApplication) this.getApplication()).getAccDB() + "&ui=" + ((SBCrmApplication) this.getApplication()).getUID();

            //urlstr += locationStr + "&adr=" + ((SBCrmApplication) getApplicationContext()).getAddress();
            //urlstr = URLEncoder.encode(urlstr, "utf-8");
            //urlstr = ((SBCrmApplication) this.getApplication()).getApplURL() + urlstr;

            System.out.println(".........doSave::::urlstr --> " + urlstr);
            if (Utility.isConnected(this)) {
              //  GetXMLTask1 task = new GetXMLTask1();
               // task.execute(new String[]{urlstr});
            }
        }
    }

      private class GetXMLTask1 extends HttpAsyncTask {
        @Override
        protected void onPostExecute(String output) {
            //Toast.makeText(getApplicationContext(),"GetXMLTask" + output, Toast.LENGTH_LONG).show();
            //System.out.println("----------"+output);
            if (output.equals("Timeout\n")) {
                //Toast.makeText(getApplicationContext(), R.string.chktimeout, Toast.LENGTH_LONG).show();
            } else {
                output = output.replaceAll("\b", " ");
                String[] result = output.trim().split("\\^", -1);
                if (result.length > 2) {
                    //callModulePage(result);
                } else {
                    if (Utility.isConnected(getApplicationContext()))
                       // Toast.makeText(getApplicationContext(), R.string.serErr, Toast.LENGTH_LONG).show();
                    onBackPressed();
                }
            }
        }
    }

    public void callModulePage(String[] data) {
        if (data.length >= 4 && data[2].equals(ApplConstants.ACT_EDIT_SAVE + "") && data[3].equals("1")) {
            Toast.makeText(getApplicationContext(), R.string.datasucess, Toast.LENGTH_LONG).show();
            ((SBCrmApplication) getApplicationContext()).setOpenMap(true);
            onBackPressed();
        } else
            Toast.makeText(getApplicationContext(), "Something went wrong please try again to save", Toast.LENGTH_LONG).show();
    }*/


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(MyMapActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            myLocation = mLastLocation;
            //mLatitudeText.setText(String.valueOf(mLastLocation.getLatitude()));
            //mLastLocation mLongitudeText.setText(String.valueOf(mLastLocation.getLongitude()));
        } else {
            //Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
        }
    }

    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private void turnGPSOn() {
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

        if (!provider.contains("gps")) { //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            sendBroadcast(poke);
        }
    }

    public void turnGPSOnp() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        sendBroadcast(intent);
    }


    public void showSettingsAlert(String s) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        Boolean b = canToggleGPS();

        // Setting Dialog Title
        alertDialog.setTitle(s);//+ String.valueOf(b)

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

}