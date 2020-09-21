package com.example.mysecondapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mysecondapp.ui.home.HomeFragment;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import travel.ithaka.android.horizontalpickerlib.PickerLayoutManager;

public class MainActivity extends AppCompatActivity {
    public State state;

    public User user;

    public LocationManager locationManager;
    public Location lastKnownLocation;

    private ArrayList<Station> stations = new ArrayList<Station>();;
    public ArrayList<String> stationNames = new ArrayList<>();

    private Socket clientSocket;
    private BufferedWriter out;
    private DataInputStream in;
    boolean ping = false;

    boolean openingQR = false;

    public HashMap<String, Station> stationMap = new HashMap<String, Station>();

    public ViewPager2 viewPager;
    private RecyclerView rvNavigationPicker;
    private PickerAdapter navigationAdapter;
    private List<String> fragmentTitles;
    private NoBounceLinearLayoutManager pickerLayoutManager;
    private int currentFrag = 1;

    public ArrayList<Station> interchangeables = new ArrayList<>();
    public Station assigned;

    SharedPreferences mPrefs;
    @Override
    protected void onDestroy() {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(user);
        prefsEditor.putString("user", json);
        json = gson.toJson(state);
        prefsEditor.putString("state", json);
        prefsEditor.apply();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        mPrefs = getPreferences(MODE_PRIVATE);
        Gson gson = new Gson();
        String json = mPrefs.getString("user", "");
        user = gson.fromJson(json, User.class);
        if (user == null){
            user = new User("","", "" ,"helenzuo123");
        }

//        json = mPrefs.getString("state", "");
//        state = gson.fromJson(json, State.class);
//        if (state == null){
//            state = new State();
//        }

        state = new State();
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        rvNavigationPicker = (RecyclerView) findViewById(R.id.rvNavigationPicker);
        viewPager = findViewById(R.id.view_pager);
        pickerLayoutManager = new NoBounceLinearLayoutManager(this, PickerLayoutManager.HORIZONTAL, false);
        pickerLayoutManager.setScaleDownBy(0.25f);
        pickerLayoutManager.setScaleDownDistance(0.7f);
        SnapHelper snapHelper = new LinearSnapHelper();
        fragmentTitles = new ArrayList<String>();
        fragmentTitles.add("Profile");
        fragmentTitles.add("Reservation");
        fragmentTitles.add("Stations List");
        navigationAdapter = new PickerAdapter(this, fragmentTitles, rvNavigationPicker, snapHelper, pickerLayoutManager);
        snapHelper.attachToRecyclerView(rvNavigationPicker);
        rvNavigationPicker.setLayoutManager(pickerLayoutManager);
        rvNavigationPicker.setAdapter(navigationAdapter);
        rvNavigationPicker.scrollToPosition(1);
        rvNavigationPicker.smoothScrollBy(-1, 0);
        new Connect(this).execute();
        new GetMsg(this).execute("initialise");
    }

    private void setUpScreen(){
        viewPager.setOffscreenPageLimit(1);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle()));

        pickerLayoutManager.setOnScrollStopListener(new PickerLayoutManager.onScrollStopListener() {
            @Override
            public void selectedView(View view) {
                currentFrag = fragmentTitles.indexOf(((TextView) view).getText().toString());
                viewPager.setCurrentItem(currentFrag, true);
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (currentFrag != position) {
                    rvNavigationPicker.smoothScrollToPosition(position);
                }
                currentFrag = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });

        viewPager.setCurrentItem(1, false);

        Intent intent = getIntent();
        if (intent.getIntExtra("Place Number", 0) == 0) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                updateUserLocation();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                updateUserLocation();
            }
        }
    }


    public void startQRScanner() {
        openingQR = true;
        Intent myIntent = new Intent(MainActivity.this, QRScanner.class);
        MainActivity.this.startActivityForResult(myIntent, 1);
    }

    private Location getLastKnownLocation() {
        locationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void updateUserLocation() {
        lastKnownLocation = getLastKnownLocation();
        for (Station station : getStations()){
            station.updateDistanceFrom(lastKnownLocation);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                queryServerStation(new BookingMessageToServer("QRScanned", data.getExtras().getString("QRCode").replaceAll("\\D+",""), -1, -1));
                ((HomeFragment)((ViewPagerAdapter)viewPager.getAdapter()).getFragment(1)).QRCodeScannedAnimation();
            }
            else if (resultCode == RESULT_CANCELED) {
                //Write your code if no result
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!openingQR)
            new CloseSocket(this).execute();
        openingQR = false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        new Connect(this).execute();
    }

    public ArrayList<Station> getStations(){
        return stations;
    }

    public void queryServerStation(BookingMessageToServer msg){
        new SendMessage(this).execute(msg.getKey(), new Gson().toJson(msg));
    }

    private static class Connect extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        Connect(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (!activity.ping) {
                try {
                    activity.clientSocket = new Socket("192.168.20.11", 8080);
                    activity.out = new BufferedWriter(new OutputStreamWriter(activity.clientSocket.getOutputStream()));
                    activity.in = new DataInputStream(activity.clientSocket.getInputStream());
                    activity.out.write(new Gson().toJson(activity.user));
                    activity.out.flush();
                    activity.ping = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

    }

    // Called to perform work in a worker thread.
    private static class SendMessage extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        SendMessage(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected void onPostExecute(String s) {
            new GetMsg(activityReference.get()).execute(s);
        }

        protected String doInBackground(String... strings) {
            MainActivity activity = activityReference.get();
            if (activity.ping) {
                try {
                    if (!activity.clientSocket.isClosed()) {
                        activity.clientSocket.close();
                    }
                    activity.clientSocket = new Socket("192.168.20.11", 8080);
                    activity.out = new BufferedWriter(new OutputStreamWriter(activity.clientSocket.getOutputStream()));
                    activity.in = new DataInputStream(activity.clientSocket.getInputStream());
                    activity.out.write(strings[1]);
                    activity.out.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return strings[0];
        }
    }

    // Called to perform work in a worker thread.
    private static class CloseSocket extends AsyncTask<Void, Void, Void> {
        private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        CloseSocket(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }
        protected Void doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (activity.ping) {
                try {
                    if (!activity.clientSocket.isClosed()) activity.clientSocket.close();
                    activity.clientSocket = new Socket("192.168.20.11", 8080);
                    activity.out = new BufferedWriter(new OutputStreamWriter(activity.clientSocket.getOutputStream()));
                    activity.in = new DataInputStream(activity.clientSocket.getInputStream());
                    activity.out.write(new Gson().toJson(new BookingMessageToServer("quit", "", -1, -1)));
                    activity.out.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            MainActivity activity = activityReference.get();
            activity.ping = false;
            try {
                activity.clientSocket.close();
                activity.in.close();
                activity.out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private static class GetMsg extends AsyncTask<String, Void, String> {
        private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        GetMsg(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... strings) {
            MainActivity activity = activityReference.get();
            return strings[0] + "##" +activity.readUTF8();
        }

        @Override
        protected void onPostExecute(String s) {
            MainActivity activity = activityReference.get();
            String[] strings = s.split("##");
            if (strings[0].equals("initialise")){
                activity.initialiseStationInfo(strings[1]);
                activity.setUpScreen();
            } else if (strings[0].equals("queryDepart")){
                activity.departQueryResults(strings[1]);
            } else if (strings[0].equals("QRScanned")){
                activity.QRScanned(strings[1]);
            } else if (strings[0].equals("queryArrival")){
                activity.arrivalQueryResults(strings[1]);
            }
        }
    }

    private void initialiseStationInfo(String s){
        try {
            JSONArray jsonArr  = new JSONArray(s);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Station station = new Station(this, jsonObj.getDouble("lat"), jsonObj.getDouble("long"), jsonObj.getInt("cap"), jsonObj.getInt("occ"), jsonObj.getString("id"));
                stations.add(station);
                stationMap.put(station.getId(), station);
                stationNames.add(station.getName());
            }
        }catch (JSONException | IOException e){
            System.out.println(e);
        }
        Collections.sort(stations);
    }

    private void departQueryResults(String s){
        try {
            JSONArray jsonArr  = new JSONArray(s);
            interchangeables = new ArrayList<>();
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Station station = stationMap.get(jsonObj.getString("id"));
                station.setOccupancy(jsonObj.getInt("occ"));
                station.setPredictedOcc(jsonObj.getInt("predictedOcc"));
                interchangeables.add(stationMap.get(jsonObj.getString("id")));
                if (jsonObj.getInt("assigned") == 1) {
                    assigned = station;
                }
            }
        } catch (JSONException e){
            System.out.println(e);
        }
    }

    private void QRScanned(String s){
        if (s.equals("success")){
            state.bookingStateTransition(true);
        } else if (s.equals("empty")){
            ((HomeFragment)((ViewPagerAdapter) viewPager.getAdapter()).getFragment(1))
                    .addQRScreenMessage(new Message("out",
                            String.format("%s.", assigned.getName())));
            ((HomeFragment)((ViewPagerAdapter) viewPager.getAdapter()).getFragment(1))
                    .addQRScreenMessage(new Message("in",
                            "This is an empty dock. Please try scanning another one."));
        } else {
            ((HomeFragment)((ViewPagerAdapter) viewPager.getAdapter()).getFragment(1))
                .addQRScreenMessage(new Message("out",
                        String.format("%s.", stationMap.get(s).getName())));
            ((HomeFragment)((ViewPagerAdapter) viewPager.getAdapter()).getFragment(1))
                    .addQRScreenMessage(new Message("in",
                            String.format("QR Code scanned belongs to %s! You booked a departure from %s.", stationMap.get(s).getName(), state.getDepartingStation().getName())));
            ((HomeFragment)((ViewPagerAdapter) viewPager.getAdapter()).getFragment(1))
                    .addQRScreenMessage(new Message("in", "If you want to depart from here, please rebook by cancelling this reservation!"));
        }
    }

    private void arrivalQueryResults(String s){
        try {
            System.out.println(s);
            JSONArray jsonArr  = new JSONArray(s);
            interchangeables = new ArrayList<>();
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Station station = stationMap.get(jsonObj.getString("id"));
                station.setOccupancy(jsonObj.getInt("occ"));
                station.setPredictedOcc(station.getCapacity() - jsonObj.getInt("predictedDocks"));
                station.setEstArr(jsonObj.getInt("estArr"));
                interchangeables.add(stationMap.get(jsonObj.getString("id")));
                if (jsonObj.getInt("assigned") == 1) {
                    assigned = station;
                }
            }
        } catch (JSONException e){
            System.out.println(e);
        }
    }

    private String readUTF8(){
        int n;
        char[] buffer = new char[1024 * 4];
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(in, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        StringWriter writer = new StringWriter();
        while (ping){
            try {
                n = reader.read(buffer);
                if (n == -1){
                    return writer.toString();
                }
                writer.write(buffer, 0, n);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

}