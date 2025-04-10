package de.dhbw.geofencinglbs.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.dhbw.geofencinglbs.databinding.ItemGeofenceBinding;
import de.dhbw.geofencinglbs.model.GeofenceModel;

/**
 * Adapter für die Anzeige von Geofences in einer RecyclerView.
 */
public class GeofencesAdapter extends ListAdapter<GeofenceModel, GeofencesAdapter.GeofenceViewHolder> {

    private final GeofenceListener listener;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    /**
     * Konstruktor mit Listener für Geofence-Interaktionen.
     */
    public GeofencesAdapter(GeofenceListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * DiffUtil-Callback für effiziente Updates der Liste.
     */
    private static final DiffUtil.ItemCallback<GeofenceModel> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GeofenceModel>() {
                @Override
                public boolean areItemsTheSame(@NonNull GeofenceModel oldItem, @NonNull GeofenceModel newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull GeofenceModel oldItem, @NonNull GeofenceModel newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                            oldItem.getLatitude() == newItem.getLatitude() &&
                            oldItem.getLongitude() == newItem.getLongitude() &&
                            oldItem.getRadius() == newItem.getRadius() &&
                            oldItem.isActive() == newItem.isActive() &&
                            oldItem.getLastEntryTime() == newItem.getLastEntryTime() &&
                            oldItem.getLastExitTime() == newItem.getLastExitTime();
                }
            };

    @NonNull
    @Override
    public GeofenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGeofenceBinding binding = ItemGeofenceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GeofenceViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GeofenceViewHolder holder, int position) {
        GeofenceModel geofence = getItem(position);
        holder.bind(geofence, listener);
    }

    /**
     * ViewHolder für Geofence-Elemente.
     */
    static class GeofenceViewHolder extends RecyclerView.ViewHolder {
        private final ItemGeofenceBinding binding;

        public GeofenceViewHolder(@NonNull ItemGeofenceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Bindet einen Geofence an den ViewHolder.
         */
        public void bind(GeofenceModel geofence, GeofenceListener listener) {
            binding.textViewName.setText(geofence.getName());
            binding.textViewCoordinates.setText(
                    String.format(Locale.getDefault(), "%.6f, %.6f",
                            geofence.getLatitude(), geofence.getLongitude()));
            binding.textViewRadius.setText(
                    String.format(Locale.getDefault(), "Radius: %.0f m", geofence.getRadius()));

            // Zeige Aufenthaltszeit an
            binding.textViewDwellTime.setText(
                    "Aufenthaltszeit: " + geofence.getFormattedDwellTime());

            // Letzter Ein-/Austritt
            if (geofence.getLastEntryTime() > 0) {
                String lastEntry = "Letzter Eintritt: " +
                        dateFormat.format(new Date(geofence.getLastEntryTime()));
                binding.textViewLastEntry.setText(lastEntry);
            } else {
                binding.textViewLastEntry.setText("Noch kein Eintritt");
            }

            if (geofence.getLastExitTime() > 0) {
                String lastExit = "Letzter Austritt: " +
                        dateFormat.format(new Date(geofence.getLastExitTime()));
                binding.textViewLastExit.setText(lastExit);
            } else {
                binding.textViewLastExit.setText("Noch kein Austritt");
            }

            // Status (aktiv/inaktiv)
            binding.switchActive.setChecked(geofence.isActive());
            binding.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onGeofenceToggle(geofence, isChecked);
                }
            });

            // Klick-Listener für das gesamte Item
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGeofenceClick(geofence);
                }
            });
        }
    }

    /**
     * Interface für Geofence-Interaktionen.
     */
    public interface GeofenceListener {
        void onGeofenceClick(GeofenceModel geofence);
        void onGeofenceToggle(GeofenceModel geofence, boolean isActive);
    }
}