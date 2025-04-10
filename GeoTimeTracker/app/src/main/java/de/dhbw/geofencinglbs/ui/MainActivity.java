package de.dhbw.geofencinglbs.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import de.dhbw.geofencinglbs.R;
import de.dhbw.geofencinglbs.databinding.ActivityMainBinding;
import de.dhbw.geofencinglbs.location.LocationService;
import de.dhbw.geofencinglbs.model.GeofenceModel;
import de.dhbw.geofencinglbs.util.NotificationHelper;

public class MainActivity extends AppCompatActivity implements AddGeofenceDialogFragment.GeofenceDialogListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 1002;

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private GeofencesAdapter geofencesAdapter;
    private EventsAdapter eventsAdapter;

    // Service-Verbindung
    private LocationService locationService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            serviceBound = true;

            // Standort-Listener setzen
            locationService.setLocationListener(new LocationService.LocationListener() {
                @Override
                public void onLocationChanged(Location location, float batteryLevel, boolean isCharging, String networkType) {
                    viewModel.setCurrentLocation(location);
                    viewModel.updateDeviceStatus(batteryLevel, isCharging, networkType);
                    updateLocationUI(location);
                }
            });

            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // ViewModel initialisieren
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Benachrichtigungskanal erstellen
        NotificationHelper.createNotificationChannel(this);

        // UI initialisieren
        setupUI();

        // Berechtigungen prüfen und anfordern
        checkAndRequestPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // LocationService starten und binden
        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Service unbinden, aber weiterlaufen lassen
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    /**
     * UI-Elemente initialisieren und einrichten.
     */
    private void setupUI() {
        // RecyclerView für Geofences
        geofencesAdapter = new GeofencesAdapter(new GeofencesAdapter.GeofenceListener() {
            @Override
            public void onGeofenceClick(GeofenceModel geofence) {
                showGeofenceDetails(geofence);
            }

            @Override
            public void onGeofenceToggle(GeofenceModel geofence, boolean isActive) {
                viewModel.toggleGeofenceActive(geofence, isActive);
            }
        });

        binding.contentMain.recyclerViewGeofences.setLayoutManager(new LinearLayoutManager(this));
        binding.contentMain.recyclerViewGeofences.setAdapter(geofencesAdapter);

        // RecyclerView für Events
        eventsAdapter = new EventsAdapter();
        binding.contentMain.recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.contentMain.recyclerViewEvents.setAdapter(eventsAdapter);

        // FAB für neue Geofences
        binding.fab.setOnClickListener(view -> {
            if (viewModel.getCurrentLocation().getValue() != null) {
                showAddGeofenceDialog(viewModel.getCurrentLocation().getValue());
            } else {
                Snackbar.make(view, "Warte auf Standortbestimmung...", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // LiveData für Geofences beobachten
        viewModel.getAllGeofences().observe(this, geofences -> {
            geofencesAdapter.submitList(geofences);
            binding.contentMain.textViewNoGeofences.setVisibility(
                    geofences != null && !geofences.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // LiveData für Events beobachten
        viewModel.getRecentEvents().observe(this, events -> {
            eventsAdapter.submitList(events);
            binding.contentMain.textViewNoEvents.setVisibility(
                    events != null && !events.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Status-Meldungen beobachten
        viewModel.getStatusMessage().observe(this, message -> {
            binding.contentMain.textViewStatus.setText(message);
        });

        // Lade-Status beobachten
        viewModel.isLoading().observe(this, isLoading -> {
            binding.contentMain.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Gerätestatus beobachten
        viewModel.getDeviceStatus().observe(this, status -> {
            String batteryStatus = String.format("Akku: %.1f%% %s",
                    status.getBatteryLevel(),
                    status.isCharging() ? "(lädt)" : "");
            binding.contentMain.textViewBatteryStatus.setText(batteryStatus);
            binding.contentMain.textViewNetworkStatus.setText("Netzwerk: " + status.getNetworkType());
        });
    }

    /**
     * Aktualisiert die Standortanzeige in der UI.
     */
    private void updateLocationUI(Location location) {
        if (location != null) {
            String locationText = String.format("%.6f, %.6f (±%.1fm)",
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAccuracy());
            binding.contentMain.textViewLocation.setText(locationText);
            binding.contentMain.textViewProvider.setText("Provider: " + location.getProvider());
        }
    }

    /**
     * Zeigt den Dialog zum Hinzufügen eines neuen Geofence.
     */
    private void showAddGeofenceDialog(Location location) {
        AddGeofenceDialogFragment dialog = AddGeofenceDialogFragment.newInstance(
                location.getLatitude(), location.getLongitude());
        dialog.show(getSupportFragmentManager(), "AddGeofenceDialog");
    }

    /**
     * Zeigt Details zu einem Geofence an.
     */
    private void showGeofenceDetails(GeofenceModel geofence) {
        // TODO: Implement detailed view or dialog for geofence
        Toast.makeText(this, "Geofence: " + geofence.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGeofenceCreated(String name, double latitude, double longitude, float radius) {
        viewModel.addGeofence(name, latitude, longitude, radius);
    }

    /**
     * Prüft und fordert bei Bedarf Standortberechtigungen an.
     */
    private void checkAndRequestPermissions() {
        // Vordergrund-Standortberechtigung prüfen
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // Hintergrund-Standortberechtigung prüfen (nur für Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Zeige einen Dialog, der die Notwendigkeit der Hintergrundberechtigung erklärt
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Hintergrund-Standortberechtigung benötigt")
                            .setMessage("Diese App benötigt Zugriff auf deinen Standort im Hintergrund, " +
                                    "um Geofence-Ereignisse zu erkennen, auch wenn die App nicht geöffnet ist.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                ActivityCompat.requestPermissions(this,
                                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                        REQUEST_BACKGROUND_LOCATION_PERMISSION);
                            })
                            .setNegativeButton("Abbrechen", null)
                            .show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            // Ergebnis der Vordergrund-Standortberechtigung
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Berechtigung erteilt, nun nach Hintergrund-Berechtigung fragen
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    checkAndRequestPermissions(); // Wird nun die Hintergrundberechtigung prüfen
                }
            } else {
                // Berechtigung verweigert
                Snackbar.make(binding.getRoot(),
                        "Standortberechtigung verweigert. Geofencing funktioniert nicht ohne Standortzugriff.",
                        Snackbar.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            // Ergebnis der Hintergrund-Standortberechtigung
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Hintergrund-Berechtigung erteilt
                Snackbar.make(binding.getRoot(),
                        "Hintergrund-Standortberechtigung erteilt. Geofencing ist vollständig aktiviert.",
                        Snackbar.LENGTH_SHORT).show();
            } else {
                // Hintergrund-Berechtigung verweigert
                Snackbar.make(binding.getRoot(),
                        "Hintergrund-Standortberechtigung verweigert. Geofencing funktioniert nur bei geöffneter App.",
                        Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // TODO: Öffne Settings-Aktivität
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}