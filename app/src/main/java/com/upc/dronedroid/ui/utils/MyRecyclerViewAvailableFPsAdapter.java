package com.upc.dronedroid.ui.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import androidx.recyclerview.widget.RecyclerView;

import com.upc.dronedroid.R;
import com.upc.dronedroid.models.FlightPlan;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MyRecyclerViewAvailableFPsAdapter extends RecyclerView.Adapter<MyRecyclerViewAvailableFPsAdapter.ViewHolder> {

    private static ClickListener clickListener;
    List<FlightPlan> availableFlightPlans;
    Context context;
    private int selectedPosition = -1;

    public MyRecyclerViewAvailableFPsAdapter(Context context, List<FlightPlan> availableFlightPlans) {
        this.availableFlightPlans = availableFlightPlans;
        this.context = context;
    }

    @Override
    public MyRecyclerViewAvailableFPsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.availablefpsrecyclerview_row, null);

        MyRecyclerViewAvailableFPsAdapter.ViewHolder viewHolder = new MyRecyclerViewAvailableFPsAdapter.ViewHolder(itemLayoutView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MyRecyclerViewAvailableFPsAdapter.ViewHolder viewHolder, int position) {
        //TODO: Allow to unselect radiobutton
        //Bind data from the flight plan to the layout
        viewHolder.fpName.setText(availableFlightPlans.get(position).getName());

        // Checked selected radio button
        viewHolder.fpName.setChecked(position == selectedPosition);

        // set listener on radio button
        viewHolder.fpName.setOnCheckedChangeListener((compoundButton, bChecked) -> {
            if (bChecked) {
                // update selected position
                selectedPosition = viewHolder.getAdapterPosition();
                // Call listener
                clickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return availableFlightPlans.size();
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        MyRecyclerViewAvailableFPsAdapter.clickListener = clickListener;
    }

    public void clearSelection(){
        selectedPosition = -1;
        clickListener.onItemClick(selectedPosition);
    }

    @Override
    public void onAttachedToRecyclerView(@NotNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public FlightPlan getAvailableFlightPlan(int position) {
        return availableFlightPlans.get(position);
    }

    public interface ClickListener {
        void onItemClick(int position);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public RadioButton fpName;

        public ViewHolder(View availableFP) {
            super(availableFP);
            fpName = availableFP.findViewById(R.id.radioButton);
            availableFP.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition());
        }
    }
}
