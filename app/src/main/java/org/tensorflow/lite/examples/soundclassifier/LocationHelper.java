package org.tensorflow.lite.examples.soundclassifier;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class LocationHelper {
    private static Location oldLocation;
    private static LocationListener locationListenerGPS;

    static void stopLocation(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationListenerGPS!=null) locationManager.removeUpdates(locationListenerGPS);
        locationListenerGPS=null;
    }

    static void requestLocation(Context context, SoundClassifier soundClassifier) {
        oldLocation = null;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkLocationProvider(context)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationListenerGPS==null) locationListenerGPS = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Location roundLoc = new Location(location);
                    roundLoc.setLatitude(Math.round(location.getLatitude() * 100.0) / 100.0);
                    roundLoc.setLongitude(Math.round(location.getLongitude() * 100.0) / 100.0);
                    if (oldLocation == null ||
                            (roundLoc.getLatitude() != oldLocation.getLatitude()) ||
                            (roundLoc.getLongitude() != oldLocation.getLongitude())){

                        oldLocation = roundLoc;
                        soundClassifier.runMetaInterpreter(roundLoc);
                    }
                }

                @Deprecated
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListenerGPS);
        }
    }

    public static boolean checkLocationProvider(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(context, "Error no GPS", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

}