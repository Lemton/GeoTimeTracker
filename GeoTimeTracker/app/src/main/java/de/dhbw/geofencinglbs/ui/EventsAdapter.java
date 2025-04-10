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

import de.dhbw.geofencinglbs.databinding.ItemEventBinding;
import de.dhbw.geofencinglbs.model.GeofenceEvent;

/**
 * Adapter für die Anzeige von Geofence-Ereignissen in einer RecyclerView.
 */
public class EventsAdapter extends ListAdapter<GeofenceEvent, EventsAdapter.EventViewHolder> {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());

    public EventsAdapter() {
        super(DIFF_CALLBACK);
    }

    /**
     * DiffUtil-Callback für effiziente Updates der Liste.
     */
    private static final DiffUtil.ItemCallback<GeofenceEvent> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<GeofenceEvent>() {
                @Override
                public boolean areItemsTheSame(@NonNull GeofenceEvent oldItem, @NonNull GeofenceEvent newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull GeofenceEvent oldItem, @NonNull GeofenceEvent newItem) {
                    return oldItem.getGeofenceId() == newItem.getGeofenceId() &&
                            oldItem.getEventType() == newItem.getEventType() &&
                            oldItem.getTimestamp() == newItem.getTimestamp();
                }
            };

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventBinding binding = ItemEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        GeofenceEvent event = getItem(position);
        holder.bind(event);
    }

    /**
     * ViewHolder für Ereignis-Elemente.
     */
    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final ItemEventBinding binding;

        public EventViewHolder(@NonNull ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Bindet ein Ereignis an den ViewHolder.
         */
        public void bind(GeofenceEvent event) {
            // Geofence-ID
            binding.textViewGeofenceId.setText("Zone " + event.getGeofenceId());

            // Ereignistyp
            binding.textViewEventType.setText(event.getEventTypeString());

            // Zeitstempel
            binding.textViewTimestamp.setText(dateFormat.format(new Date(event.getTimestamp())));

            // Genauigkeit und Provider
            String accuracy = String.format(Locale.getDefault(), "Genauigkeit: %.1f m", event.getAccuracy());
            binding.textViewAccuracy.setText(accuracy);
            binding.textViewProvider.setText("Provider: " + (event.getProvider() != null ? event.getProvider() : "Unbekannt"));

            // Batteriestatus
            String batteryStatus = String.format(Locale.getDefault(), "Akku: %.1f%% %s",
                    event.getBatteryLevel(),
                    event.isCharging() ? "(lädt)" : "");
            binding.textViewBatteryStatus.setText(batteryStatus);

            // Netzwerkstatus
            binding.textViewNetworkStatus.setText(
                    "Netzwerk: " + (event.getNetworkConnectionType() != null ?
                            event.getNetworkConnectionType() : "Unbekannt"));
        }
    }
}