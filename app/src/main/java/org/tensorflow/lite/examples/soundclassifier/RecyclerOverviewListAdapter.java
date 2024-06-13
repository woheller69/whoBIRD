package org.tensorflow.lite.examples.soundclassifier;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecyclerOverviewListAdapter extends RecyclerView.Adapter<RecyclerOverviewListAdapter.ObservationViewHolder> {

    private Context context;
    private final List<BirdObservation> birdObservations;

    public RecyclerOverviewListAdapter(Context context, List<BirdObservation> birdObservations) {
        this.context = context;
        this.birdObservations = birdObservations;
    }

    @Override
    public ObservationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bird_observation, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ObservationViewHolder holder, int position) {

        holder.name.setText(birdObservations.get(position).getName());
        holder.probability.setText((int) Math.round(birdObservations.get(position).getProbability()*100.0)+ " %");

        if (birdObservations.get(position).getProbability() < 0.3 )  holder.holder.setBackgroundResource(R.drawable.oval_holo_red_dark_thin_dotted);
        else if (birdObservations.get(position).getProbability() < 0.5 )  holder.holder.setBackgroundResource(R.drawable.oval_holo_orange_dark_thin);
        else if (birdObservations.get(position).getProbability() < 0.65 )  holder.holder.setBackgroundResource(R.drawable.oval_holo_orange_dark_thin);
        else if (birdObservations.get(position).getProbability() < 0.8 )  holder.holder.setBackgroundResource(R.drawable.oval_holo_orange_light_thin);
        else holder.holder.setBackgroundResource(R.drawable.oval_holo_green_light_thin);

        SimpleDateFormat sdf;
        Date date = new Date(birdObservations.get(position).getMillis());
        if (android.text.format.DateFormat.is24HourFormat(context)){
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        } else {
            sdf = new SimpleDateFormat("hh:mm aa", Locale.getDefault());
        }
        String timeString = sdf.format(date);
        holder.time.setText(timeString);

        java.text.DateFormat df = java.text.DateFormat.getDateInstance(DateFormat.SHORT);
        String dateString = df.format(birdObservations.get(position).getMillis());
        holder.date.setText(dateString);

        if (position == 0) {
            holder.date.setVisibility(View.VISIBLE);
        } else {
            String previousDateString = df.format(birdObservations.get(position-1).getMillis());
            if (!dateString.equals(previousDateString)) {
                holder.date.setVisibility(View.VISIBLE);
            } else {
                holder.date.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return birdObservations.size();
    }

    public int getSpeciesID(int position) {
        return birdObservations.get(position).getSpeciesId();
    }

    public long getMillis(int position) {
        return birdObservations.get(position).getMillis();
    }

    public String getLocation(int position) { return birdObservations.get(position).getLatitude() + ", " + birdObservations.get(position).getLongitude();}

    public static class ObservationViewHolder extends RecyclerView.ViewHolder {

        private TextView name;
        private TextView probability;
        private LinearLayout holder;
        private TextView time;
        private TextView date;

        public ObservationViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.probability = (TextView) itemView.findViewById(R.id.probability);
            this.holder = (LinearLayout) itemView.findViewById(R.id.holder);
            this.time = (TextView) itemView.findViewById(R.id.time);
            this.date = (TextView) itemView.findViewById(R.id.date);

        }

    }
}