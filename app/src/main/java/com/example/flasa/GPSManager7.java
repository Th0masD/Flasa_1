package com.example.flasa;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;


public class GPSManager7 {


    public interface GPSListener {
        void onGPSStatusChanged(String status);
        void onLocationChanged(Location location);
    }


    private final Context context;
    private final GPSListener listener;
    private LocationManager lm;
    private GpsStatus.Listener gpsStatusListener;
    private LocationListener locationListener;


    public GPSManager7(Context ctx, GPSListener lst) {
        this.context = ctx;
        this.listener = lst;
        lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
    }


    public void start() {
        try {
// --- GPS STATUS LISTENER ---
            gpsStatusListener = new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    switch (event) {
                        case GpsStatus.GPS_EVENT_STARTED:
                            listener.onGPSStatusChanged("ON");
                            break;
                        case GpsStatus.GPS_EVENT_STOPPED:
                            listener.onGPSStatusChanged("STOPPED");
                            break;
                        case GpsStatus.GPS_EVENT_FIRST_FIX:
                            listener.onGPSStatusChanged("FIRST_FIX");
                            break;
                        //   case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        //       listener.onGPSStatusChanged("SAT_UPDATE");
                        //       break;
                    }
                }
            };
            lm.addGpsStatusListener(gpsStatusListener);


// --- LOCATION LISTENER ---
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    listener.onLocationChanged(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {}
            };


            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);


        } catch (SecurityException e) {
            Log.e("GPS", "Permission missing: " + e.getMessage());
        }
    }


    public void stop() {
        try {
            if (lm != null) {
                if (gpsStatusListener != null)
                    lm.removeGpsStatusListener(gpsStatusListener);
                if (locationListener != null)
                    lm.removeUpdates(locationListener);
            }
        } catch (Exception ignored) {}
    }
}
