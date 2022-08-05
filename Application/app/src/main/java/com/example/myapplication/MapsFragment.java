package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;
import android.Manifest;
import android.graphics.Color;
import android.location.Location;
import android.location.Criteria;
import android.location.LocationListener;
import android.location.LocationManager;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "MyActivity";
    String Loc[]={"",""};
    String Val[]={""};
    GoogleMap map;
    private LatLng mOrigin;
    private LatLng mDestination;
    private Polyline mPolyline;
    ArrayList<LatLng> mMarkerPoints;
    List<String>guid = new ArrayList<>();
    int i=0;
    TextView gu1,gu2,gu3;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View mfLayout = inflater.inflate(R.layout.fragment_maps, container, false);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference val= database.getReference().child("Responsiveness");
        mMarkerPoints = new ArrayList<>();
        guid.add(getString(R.string.guid));

        val.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Val[0] = dataSnapshot.getValue(String.class);
                if (Val[0].equals("Unresponsive"))
                {
                    guid.add(getString(R.string.guid1));
                }
                DatabaseReference val1= database.getReference().child("Question5");
                val1.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Val[0] = snapshot.getValue(String.class);
                        if (Val[0].equals("No") && i==0)
                            guid.add(getString(R.string.guid2));

                        String[] s= guid.toArray(new String[guid.size()]);

                        ArrayAdapter adapter = new ArrayAdapter<String>(getActivity(),R.layout.lv,s);

                        ListView listView = (ListView) mfLayout.findViewById(R.id.lv);
                        listView.setAdapter(adapter);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to read value.",Toast.LENGTH_LONG);
            }
        });



        mapFragment.getMapAsync(this);
        return mfLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

    }



    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map=googleMap;

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference loc= database.getReference().child("Location");

        loc.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Loc[0] = dataSnapshot.child("Latitude").getValue(String.class);
                Loc[1] = dataSnapshot.child("Longitude").getValue(String.class);

                LatLng latLng = new LatLng(Double.parseDouble(Loc[0]),Double.parseDouble(Loc[1]));
                LatLng latLng1 = new LatLng(Double.parseDouble(Loc[0])+0.1,Double.parseDouble(Loc[1]));
                map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                    @Override
                    public void onMapLoaded() {
                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        map.addMarker(markerOptions);
                        markerOptions.position(latLng1);
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        map.addMarker(markerOptions);
                        mOrigin=latLng;
                        mDestination=latLng1;
                        drawRoute();
                        map.moveCamera(CameraUpdateFactory.newLatLng(mDestination));
                        map.animateCamera(CameraUpdateFactory.zoomTo(10));
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to read value.",Toast.LENGTH_LONG);
            }
        });
    }
    private void drawRoute(){

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(mOrigin, mDestination);

        DownloadTask downloadTask = new DownloadTask();
        Log.d("Url",url);
        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }
    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Key
        String key = "key=" + getString(R.string.google_maps_key);

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception on download", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /** A class to download data from Google Directions URL */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("DownloadTask","DownloadTask : " + data);
                Log.d("Seq1","DT InBackground");
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            Log.d("Seq1","DT onPostExe(before Super)" + result);
            super.onPostExecute(result);
            Log.d("Seq1","DT onPostExe(after Super)" + result);
            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Directions in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("Seq1","PT InBackground" + routes);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            Log.d("Seq1","PT onPostExe" + result.get(0).size());
            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
//                    Log.d("Seq1","Points" + position);
                    points.add(position);
//                    Log.d("Seq1","APoints" + points);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.RED);
                Log.d("Seq1","LineOptions" + lineOptions);
            }
            Log.d("Seq1","poly" + mPolyline);
            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                if(mPolyline != null){
                    mPolyline.remove();
                }
                mPolyline = map.addPolyline(lineOptions);
                Log.d("Seq1","poly" + mPolyline);

            }else
                Toast.makeText(getActivity().getApplicationContext(),"No route is found", Toast.LENGTH_LONG).show();
        }
    }

}
