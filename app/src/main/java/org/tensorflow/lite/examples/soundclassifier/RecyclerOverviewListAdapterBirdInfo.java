package org.tensorflow.lite.examples.soundclassifier;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlin.Pair;


public class RecyclerOverviewListAdapterBirdInfo extends RecyclerView.Adapter<RecyclerOverviewListAdapterBirdInfo.ObservationViewHolder> {
    private ArrayList<kotlin.Pair<Integer, String>> birdList;

    public RecyclerOverviewListAdapterBirdInfo(Context context, ArrayList<kotlin.Pair<Integer, String>> birdList) {
        this.birdList = birdList;
    }

    @Override
    public ObservationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bird_info, parent, false);
        return new ObservationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ObservationViewHolder holder, int position) {
        List<String> parts = Arrays.asList(birdList.get(position).getSecond().split("_"));
        holder.name.setText(parts.get(parts.size() - 1));
        holder.latinName.setText(parts.get(0));
        holder.holder.setBackgroundResource(R.drawable.oval_holo_green_dark_thin);
    }

    @Override
    public int getItemCount() {
        return birdList.size();
    }

    public int getSpeciesID(int position) {
        return birdList.get(position).getFirst();
    }

    public void updateBirdList(@NotNull ArrayList<Pair<Integer, String>> allBirdsList) {
        birdList = allBirdsList;
        notifyDataSetChanged();
    }

    public static class ObservationViewHolder extends RecyclerView.ViewHolder {

        private final TextView name;
        private final TextView latinName;

        private final LinearLayout holder;

        public ObservationViewHolder(View itemView) {
            super(itemView);
            this.name = (TextView) itemView.findViewById(R.id.name);
            this.latinName = (TextView) itemView.findViewById(R.id.latinname);
            this.holder = (LinearLayout) itemView.findViewById(R.id.holder);

        }

    }
}
