package com.example.freshmilk.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.freshmilk.R;
import com.example.freshmilk.databinding.ActivityMapPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    // Default location: India center
    private static final LatLng DEFAULT_LOCATION = new LatLng(20.5937, 78.9629);

    private ActivityMapPickerBinding binding;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LatLng selectedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if an initial location was passed (for editing)
        double initLat = getIntent().getDoubleExtra("latitude", 0);
        double initLng = getIntent().getDoubleExtra("longitude", 0);
        if (initLat != 0 && initLng != 0) {
            selectedLocation = new LatLng(initLat, initLng);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", selectedLocation.latitude);
                resultIntent.putExtra("longitude", selectedLocation.longitude);

                // Try to get address from coordinates
                String address = getAddressFromLocation(selectedLocation.latitude, selectedLocation.longitude);
                if (address != null) {
                    resultIntent.putExtra("address", address);
                }

                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, R.string.location_not_set, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Set initial position
        if (selectedLocation != null) {
            // Editing mode — go to existing location
            moveToLocation(selectedLocation, 15f);
        } else {
            // Try to get current location
            requestLocationAndMove();
        }

        // Let user tap to select location
        googleMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.select_location))
                    .draggable(true));
        });

        // Support marker dragging
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull com.google.android.gms.maps.model.Marker marker) {
            }

            @Override
            public void onMarkerDrag(@NonNull com.google.android.gms.maps.model.Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(@NonNull com.google.android.gms.maps.model.Marker marker) {
                selectedLocation = marker.getPosition();
            }
        });
    }

    private void requestLocationAndMove() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        googleMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        moveToLocation(currentLatLng, 15f);
                    } else {
                        // Fallback to default location
                        moveToLocation(DEFAULT_LOCATION, 5f);
                    }
                })
                .addOnFailureListener(e -> moveToLocation(DEFAULT_LOCATION, 5f));
    }

    private void moveToLocation(LatLng latLng, float zoom) {
        googleMap.clear();
        selectedLocation = latLng;
        googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(getString(R.string.select_location))
                .draggable(true));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(address.getAddressLine(i));
                }
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndMove();
            } else {
                Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
                moveToLocation(DEFAULT_LOCATION, 5f);
            }
        }
    }
}
