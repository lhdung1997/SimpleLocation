package com.example.hoangdung.simplelocation.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.example.hoangdung.simplelocation.R;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.ArrayList;

/**
 * Created by hoangdung on 11/6/17.
 */

public class PlaceAutoCompleteAdapter extends RecyclerView.Adapter<PlaceAutoCompleteAdapter.PlaceViewHolder> implements Filterable{

    private Context mContext;
    private GeoDataClient mGeoDataClient;
    private ArrayList<AutocompletePrediction> mPredictPlaces;
    private OnPlaceClickListener mPlaceClickCallback;
    public String mCountryCode;

    public interface OnPlaceClickListener {
        void onPlaceClick(ArrayList<AutocompletePrediction> predictPlaces,int position);
    }

    public void setOnPlaceClickCallback(OnPlaceClickListener callback){
        mPlaceClickCallback = callback;
    }
    public PlaceAutoCompleteAdapter(Context context, GeoDataClient geoDataClient){
        mContext = context;
        mGeoDataClient = geoDataClient;
        if(mPredictPlaces == null)
            mPredictPlaces = new ArrayList<>(0);
    }
    @Override
    public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.search_item,parent,false);
        PlaceViewHolder placeViewHolder = new PlaceViewHolder(view);
        return placeViewHolder;
    }

    @Override
    public void onBindViewHolder(PlaceViewHolder holder, final int position) {
        holder.primaryText.setText(mPredictPlaces.get(position).getPrimaryText(null));
        holder.secondaryText.setText(mPredictPlaces.get(position).getSecondaryText(null));
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaceClickCallback.onPlaceClick(mPredictPlaces,position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPredictPlaces.size();
    }
    public void clearData(){
        mPredictPlaces.clear();
    }

    //Main Operation For Querying Suggestion and Storing Suggestion
    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();
                if(!constraint.toString().isEmpty()) {

                    //Query Autocomplete Predictions Using, filter downto user current location's country
                    com.google.android.gms.tasks.Task<AutocompletePredictionBufferResponse> task = mGeoDataClient
                            .getAutocompletePredictions(constraint.toString(),
                                    new LatLngBounds(new LatLng(-0, -0), new LatLng(0, 0)),
                                    new AutocompleteFilter.Builder().setCountry(mCountryCode).build());
                    task.addOnCompleteListener(new OnCompleteListener<AutocompletePredictionBufferResponse>() {
                        @Override
                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<AutocompletePredictionBufferResponse> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                //If there are some results, store the results and update UI
                                if(task.getResult().getCount() != 0){
                                    for (AutocompletePrediction prediction : task.getResult()) {
                                        mPredictPlaces.add(prediction.freeze());
                                    }
                                    task.getResult().release();
                                    filterResults.values = mPredictPlaces;
                                    filterResults.count = mPredictPlaces.size();
                                    notifyDataSetChanged();
                                }
                                //If no result is return, retry with no country filter
                                else{
                                    Log.d("MapsActivity","Results not found");
                                    final com.google.android.gms.tasks.Task<AutocompletePredictionBufferResponse> newTask = mGeoDataClient
                                            .getAutocompletePredictions(constraint.toString(),
                                                    new LatLngBounds(new LatLng(-0, -0), new LatLng(0, 0)),
                                                    new AutocompleteFilter.Builder().build());
                                    newTask.addOnCompleteListener(new OnCompleteListener<AutocompletePredictionBufferResponse>() {
                                        @Override
                                        public void onComplete(@NonNull com.google.android.gms.tasks.Task<AutocompletePredictionBufferResponse> task) {
                                            if (newTask.isSuccessful() && newTask.getResult() != null) {

                                                for (AutocompletePrediction prediction : newTask.getResult()) {
                                                    mPredictPlaces.add(prediction.freeze());
                                                }
                                                newTask.getResult().release();
                                                filterResults.values = mPredictPlaces;
                                                filterResults.count = mPredictPlaces.size();
                                                notifyDataSetChanged();
                                            }
                                        }
                                    });
                                }
                            }
                            else{

                            }
                        }
                    });
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                Log.d("MapsActivity","publishResults");
                notifyDataSetChanged();
            }
        };
        return filter;
    }

    public class PlaceViewHolder extends RecyclerView.ViewHolder{
        public TextView primaryText;
        public TextView secondaryText;
        public View parentLayout;
        public PlaceViewHolder(View item){
            super(item);
            primaryText = item.findViewById(R.id.search_text_primary);
            secondaryText = item.findViewById(R.id.search_text_secondary);
            parentLayout = item;
        }
    }
}
