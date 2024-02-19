package org.tensorflow.lite.examples.soundclassifier;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecyclerOverviewListAdapter extends RecyclerView.Adapter<RecyclerOverviewListAdapter.ScriptViewHolder> {

    private Context context;
    private final List<BirdObservation> birdObservations;

    public RecyclerOverviewListAdapter(Context context, List<BirdObservation> birdObservations) {
        this.context = context;
        this.birdObservations = birdObservations;
    }

    @Override
    public ScriptViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bird_observation, parent, false);
        return new ScriptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ScriptViewHolder holder, int position) {

        holder.name.setText(birdObservations.get(position).getName());
        holder.probability.setText((int) Math.round(birdObservations.get(position).getProbability()*100.0)+ " %");

        if (birdObservations.get(position).getProbability() < 0.5 )  holder.holder.setBackgroundResource(R.drawable.oval_holo_red_dark_thin);
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
        String datetimeString = sdf.format(date);
        holder.date.setText(datetimeString);

    }

    @Override
    public int getItemCount() {
        return birdObservations.size();
    }

    public int getSpeciesID(int position) {
        return birdObservations.get(position).getSpeciesId();
    }

    public static class ScriptViewHolder extends RecyclerView.ViewHolder {

        private TextView name;
        private TextView probability;
        private CardView holder;
        private TextView date;

        public ScriptViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.probability = (TextView) itemView.findViewById(R.id.probability);
            this.holder = (CardView) itemView.findViewById(R.id.holder);
            this.date = (TextView) itemView.findViewById(R.id.date);

        }

    }
}