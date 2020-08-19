package com.example.mysecondapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import travel.ithaka.android.horizontalpickerlib.PickerLayoutManager;

public class MainActivity extends AppCompatActivity {
    public ViewPager2 viewPager;

    private int selectionState;
    public Station reservedDepartureStation;
    public Station selectedDepartureStation;
    public Station selectedArrivalStation;
    public String departureTime = "";
    public String distanceWalking = "";
    public boolean customDistance = false;
    public String reservationTime;

    public String firstName;
    public String surname;
    public String email;
    public String gender;
    public NavController navController;
    public BottomNavigationView navView;
    private ArrayList<Station> stations;
    private ArrayList<Station> favouriteStations;

    private Socket clientSocket;
    private BufferedWriter out;
    private DataInputStream in;
    boolean ping = false;

    public HashMap<String, Station> stationMap = new HashMap<String, Station>();


    private RecyclerView rvNavigationPicker;
    private PickerAdapter navigationAdapter;
    private List<String> fragmentTitles;
    private int currentFrag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        selectionState = STATIC_DEFINITIONS.START_BOOKING_STATE;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        stations = new ArrayList<Station>();
        Station tempStation;
        try {
            tempStation = new Station("Bourke", 144.96803330048928, -37.81393990463194, 22, 15, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Collins", 144.97192836840318, -37.815022142571, 22, 8, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Flagstaff", 144.9606810878229, -37.81493656474232, 22, 20, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Flinders", 144.9699301383418, -37.81673328727038, 22, 20, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Lcollins", 144.97003459169014, -37.81451269649511, 22, 8, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Lygon", 144.96766165512835, -37.81182608904015, 22, 15, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("mc", 144.9647403339459, -37.81155259053488, 22, 16, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Parliament", 144.97304788833318, -37.811337130250315, 22, 16, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Queens", 144.96316335849318, -37.81421830705699, 22, 10, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Rmit", 144.96381831022302, -37.80956830069795, 22, 13, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
            tempStation = new Station("Yarra", 144.97239159357775, -37.81601925202357, 22, 20, this);
            stations.add(tempStation);
            stationMap.put(tempStation.getName(), tempStation);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        favouriteStations = new ArrayList<>();
        appendFavouriteStation(stations.get(1));

//        new Connect(this).execute();

        rvNavigationPicker = (RecyclerView) findViewById(R.id.rvNavigationPicker);

        final NoBounceLinearLayoutManager pickerLayoutManager = new NoBounceLinearLayoutManager(this, PickerLayoutManager.HORIZONTAL, false);
        pickerLayoutManager.setScaleDownBy(0.25f);
        pickerLayoutManager.setScaleDownDistance(0.7f);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle()));
        fragmentTitles = new ArrayList<String>();
        fragmentTitles.add("Profile");
        fragmentTitles.add("Reservation");
        fragmentTitles.add("Stations List");
        SnapHelper snapHelper = new LinearSnapHelper();
        navigationAdapter = new PickerAdapter(this, fragmentTitles, rvNavigationPicker, snapHelper, pickerLayoutManager);
        snapHelper.attachToRecyclerView(rvNavigationPicker);
        rvNavigationPicker.setLayoutManager(pickerLayoutManager);
        rvNavigationPicker.setAdapter(navigationAdapter);


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

        viewPager.setCurrentItem(1);

    }
//
//    public void updateViewpager(){
//        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle()));
//        viewPager.setCurrentItem(1);
//    }

    public void bookingStateTransition(boolean forward){
        if (forward){
            selectionState++;
        } else {
            selectionState--;
        }
    }

    public int getBookingState(){
        return selectionState;
    }

    public void startQRScanner() {
        Intent myIntent = new Intent(MainActivity.this, QRScanner.class);
        MainActivity.this.startActivityForResult(myIntent, 1);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        getSupportActionBar().show();
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
//                String result = data.getExtras().getString("QRCode");
                bookingStateTransition(true);
                navController.popBackStack(R.id.navigation_home, true);
                navController.navigate(R.id.navigation_home);
            }
            else if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
//        new CloseSocket(this).execute();
    }

    @Override
    protected void onResume(){
        super.onResume();
//        new Connect(this).execute();
    }

    public ArrayList<Station> getStations(){
        return stations;
    }

    public void appendFavouriteStation(Station station) {
        favouriteStations.add(station);
    }

    public void removeFavouriteStation(Station station) {
        favouriteStations.remove(station);
    }

    public ArrayList<Station> getFavouriteStations(){
        return favouriteStations;
    }


    @Override public void onBackPressed() {
        navView.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getSupportActionBar()).show();
        super.onBackPressed();
    }

    public void queryDepartureStation(int minutesUntilBorrow, Station departingStation, Station arrivingStation){
        new SendMessage(this).execute(Integer.toString(minutesUntilBorrow), departingStation.getName(), arrivingStation.getName());
    }

    private static class Connect extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        Connect(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            MainActivity activity = activityReference.get();
            if (!activity.ping) {
                try {
                    activity.clientSocket = new Socket("10.0.2.2", 8080);
                    activity.out = new BufferedWriter(new OutputStreamWriter(activity.clientSocket.getOutputStream()));
                    activity.in = new DataInputStream(activity.clientSocket.getInputStream());
                    activity.ping = true;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return activity.ping;
        }
    }
    // Called to perform work in a worker thread.
    private static class SendMessage extends AsyncTask<String, Void, Void> {
        private WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        SendMessage(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }
        protected Void doInBackground(String... strings) {
            MainActivity activity = activityReference.get();
            if (activity.ping) {
                try {
                    String outputMessage = "QUERY START";
                    for (String string : strings) {
                        outputMessage += "#" + string;
                    }
                    outputMessage += "#QUERY END" ;
                    activity.out.write(outputMessage);
                    activity.out.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return null;
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
                    activity.out.write("quit");
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

    public String readUTF8() throws IOException {
        String msg = "";
        if (ping) {
            if (in.available() > 0) {
                int length = in.readInt();
                byte[] encoded = new byte[length];
                in.readFully(encoded);
                msg = new String(encoded, StandardCharsets.UTF_8);
            }
        }
        return msg;
    }

    public boolean getPing(){
        return ping;
    }

}