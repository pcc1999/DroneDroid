package com.upc.dronedroid.ui.utils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.upc.dronedroid.R;
import com.upc.dronedroid.models.Waypoint;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyRecyclerViewWaypointsAdapter extends RecyclerView.Adapter<MyRecyclerViewWaypointsAdapter.ViewHolder> {
    private static MyRecyclerViewWaypointsAdapter.ClickListener clickListener;
    List<Waypoint> waypointsList;
    Context context;
    Boolean isEditable = false;
    Boolean isCreating = false;

    public MyRecyclerViewWaypointsAdapter(Context context, List<Waypoint> waypointsList) {
        this.waypointsList = waypointsList;
        this.context = context;
    }

    @NotNull
    @Override
    public MyRecyclerViewWaypointsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.waypointsrecyclerview_row, null);

        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(MyRecyclerViewWaypointsAdapter.ViewHolder viewHolder, int position) {
        //Bind data from the flight plan to the layout
        viewHolder.latitude.setText(String.valueOf(waypointsList.get(position).getCoords().getLatitude()));
        viewHolder.longitude.setText(String.valueOf(waypointsList.get(position).getCoords().getLongitude()));
        viewHolder.takePicture.setChecked(waypointsList.get(position).getTakePicture());
        viewHolder.takePicture.setEnabled(isEditable || isCreating);
        if (isEditable || isCreating) {
            viewHolder.deleteButton.setVisibility(View.VISIBLE);
        } else {
            viewHolder.deleteButton.setVisibility(View.INVISIBLE);
        }
        viewHolder.takePicture.setOnClickListener(view -> {
            //Based on the object since position might change when deleting waypoints
            /*for (int i = 0; i < waypointsList.size(); i++) {
                if (waypointsList.get(i).equals(new Waypoint(Double.parseDouble(viewHolder.latitude.getText().toString()),
                        Double.parseDouble(viewHolder.longitude.getText().toString()),
                        viewHolder.takePicture.isChecked()))) {
                    waypointsList.set(i, new Waypoint(Double.parseDouble(viewHolder.latitude.getText().toString()),
                            Double.parseDouble(viewHolder.longitude.getText().toString()),
                            !viewHolder.takePicture.isChecked()));
                }
            }*/
            waypointsList.get(position).setTakePicture(!waypointsList.get(position).getTakePicture());
            this.notifyDataSetChanged();
        });
        viewHolder.deleteButton.setOnClickListener(view -> {
            //Based on the object since position might change when adding/deleting waypoints
            /*for (Waypoint waypoint : waypointsList) {
                if (waypoint.equals(new Waypoint(
                        Double.parseDouble(viewHolder.latitude.getText().toString()),
                        Double.parseDouble(viewHolder.longitude.getText().toString()),
                        viewHolder.takePicture.isChecked()))) {
                    waypointsList.remove(waypoint);
                }
            }*/
            waypointsList.remove(position);
            this.notifyDataSetChanged();
            Gson gson = new Gson();
            Intent reCreateWaypoints = new Intent();
            reCreateWaypoints.setAction("com.upc.dronedroid.WAYPOINT_DELETED");
            reCreateWaypoints.putExtra("currentWaypoints", gson.toJson(waypointsList));
            view.getContext().sendBroadcast(reCreateWaypoints);
        });
    }

    @Override
    public int getItemCount() {
        return waypointsList.size();
    }

    public void setOnItemClickListener(MyRecyclerViewWaypointsAdapter.ClickListener clickListener) {
        MyRecyclerViewWaypointsAdapter.clickListener = clickListener;
    }

    @Override
    public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public Waypoint getWaypoint(int position) {
        return waypointsList.get(position);
    }

    public void toggleEditable() {
        this.isEditable = !this.isEditable;
        this.notifyDataSetChanged();
    }

    public void setIsCreating(Boolean isCreating) {
        this.isCreating = isCreating;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView latitude;
        public TextView longitude;
        public SwitchCompat takePicture;
        public ImageButton deleteButton;

        public ViewHolder(View waypoint) {
            super(waypoint);
            latitude = waypoint.findViewById(R.id.WPLatitude);
            longitude = waypoint.findViewById(R.id.WPLongitude);
            takePicture = waypoint.findViewById(R.id.switchPicture);
            deleteButton = waypoint.findViewById(R.id.imageButton6);
            waypoint.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(), v);
        }
    }
}
