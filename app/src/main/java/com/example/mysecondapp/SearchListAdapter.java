package com.example.mysecondapp;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;

import com.example.mysecondapp.ui.stationSearch.DashboardFragment;

public class SearchListAdapter extends MyAdapter {
    private int resourceLayout;
    private Context context;
    private DashboardFragment dashboardFragment;
    private int expanded = -1;


    public SearchListAdapter(Context context, int resource, DashboardFragment dashboardFragment) {
        super(context, resource);
        this.context = context;
        this.resourceLayout = resource;
        this.dashboardFragment = dashboardFragment;
    }

    public static class Holder{
        TextView stationName;
        TextView stationAddress;
        TextView stationDistance;
        ToggleButton favToggle;
        ImageButton moreButton;
        RelativeLayout moreInfoLayout;
        Button mapButton;
        TextView bikeText;
        TextView dockText;
        SeekBar fillLevelSeekBar;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final Holder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(resourceLayout, parent, false);
            holder = new Holder();
            holder.stationName = (TextView) convertView.findViewById(R.id.stationName);
            holder.stationAddress = convertView.findViewById(R.id.stationAddress);
            holder.stationDistance = convertView.findViewById(R.id.stationDistance);
            holder.favToggle = (ToggleButton) convertView.findViewById(R.id.favouritesToggle);
            holder.moreButton = convertView.findViewById(R.id.moreButton);
            holder.moreInfoLayout = convertView.findViewById(R.id.bottomView);
            holder.bikeText = convertView.findViewById(R.id.bikeText);
            holder.dockText = convertView.findViewById(R.id.dockText);
            holder.mapButton = convertView.findViewById(R.id.mapButton);
            holder.fillLevelSeekBar = convertView.findViewById(R.id.fillLevelSeekBar);
            holder.fillLevelSeekBar.setEnabled(false);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        // Set data into textviews
        holder.stationName.setText(((MainActivity)context).getStations().get(position).getName());
        holder.stationAddress.setText(String.format("%s", ((MainActivity)context).getStations().get(position).getAddress()));
        holder.stationDistance.setText(String.format("%s away",((MainActivity)context).getStations().get(position).getDistanceFrom()));
        if (((MainActivity)context).getStations().get(position).getFavourite()){
            holder.favToggle.setChecked(true);
        } else {
            holder.favToggle.setChecked(false);
        }
        holder.favToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).getStations().get(position).toggleFavourite();
            }

        });

        final View finalConvertView = convertView;
        holder.moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (holder.map != null){
//                    dashboardFragment.getChildFragmentManager().beginTransaction().remove(holder.map).commit();
//                    holder.map = null;
//                } else {
//                    holder.map = new MapViewCard(((MainActivity)context).getStations().get(position));
//                    int id = View.generateViewId();
//                    finalConvertView.findViewWithTag("bottom_layout").setId(id);
//                    dashboardFragment.getChildFragmentManager().beginTransaction().add(id, holder.map,"map").commit();
//                }
                final Station station = ((MainActivity)context).getStations().get(position);
                if (holder.moreInfoLayout.getVisibility() == (View.VISIBLE)) {
                    holder.moreInfoLayout.setVisibility(View.GONE);
                    holder.mapButton.setOnClickListener(null);
                } else {
                    holder.moreInfoLayout.setVisibility(View.VISIBLE);
                    float fl = station.getFillLevel();
                    holder.bikeText.setText(String.format("%d free bikes", (int) (fl*station.getCapacity())));
                    holder.dockText.setText(String.format("%d free docks", (int) ((1-fl)*station.getCapacity())));
                    holder.fillLevelSeekBar.setProgress((int)(fl*100));
                    holder.mapButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dashboardFragment.stationSelectedFromList(station);
                        }
                    });
                }

            }
        });
//        if (holder.map != null){
//            dashboardFragment.getChildFragmentManager().beginTransaction().remove(holder.map).commit();
//            holder.map = null;
//        }
        if (holder.moreInfoLayout.getVisibility() == (View.VISIBLE)) {
            holder.moreInfoLayout.setVisibility(View.GONE);
            holder.mapButton.setOnClickListener(null);
        }

        return convertView;
    }


}