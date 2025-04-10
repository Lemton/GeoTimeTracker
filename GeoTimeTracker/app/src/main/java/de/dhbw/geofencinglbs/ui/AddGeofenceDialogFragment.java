package de.dhbw.geofencinglbs.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.dhbw.geofencinglbs.databinding.DialogAddGeofenceBinding;

/**
 * Dialog zum Erstellen eines neuen Geofence.
 */
public class AddGeofenceDialogFragment extends DialogFragment {

    private static final String ARG_LATITUDE = "latitude";
    private static final String ARG_LONGITUDE = "longitude";

    private DialogAddGeofenceBinding binding;
    private GeofenceDialogListener listener;

    private double latitude;
    private double longitude;

    /**
     * Factory-Methode für eine neue Instance des Dialogs.
     */
    public static AddGeofenceDialogFragment newInstance(double latitude, double longitude) {
        AddGeofenceDialogFragment fragment = new AddGeofenceDialogFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_LATITUDE, latitude);
        args.putDouble(ARG_LONGITUDE, longitude);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            latitude = getArguments().getDouble(ARG_LATITUDE);
            longitude = getArguments().getDouble(ARG_LONGITUDE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogAddGeofenceBinding.inflate(LayoutInflater.from(getContext()));

        // Aktuelle Koordinaten anzeigen
        binding.textViewCoordinates.setText(
                String.format("Standort: %.6f, %.6f", latitude, longitude));

        // Dialog erstellen
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Geofence hinzufügen")
                .setView(binding.getRoot())
                .setPositiveButton("Hinzufügen", (dialog, which) -> {
                    // Dialog ist noch nicht geschlossen, deshalb können wir noch auf Fehler prüfen
                    // Wird durch onClickListener überschrieben
                })
                .setNegativeButton("Abbrechen", (dialog, which) -> {
                    dismiss();
                })
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Überschreibe den positiven Button, um Validierung durchzuführen
        ((androidx.appcompat.app.AlertDialog) getDialog()).getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (validateInput()) {
                String name = binding.editTextName.getText().toString().trim();
                float radius = Float.parseFloat(binding.editTextRadius.getText().toString().trim());

                if (listener != null) {
                    listener.onGeofenceCreated(name, latitude, longitude, radius);
                }

                dismiss();
            }
        });
    }

    /**
     * Validiert die Eingaben und zeigt Fehlermeldungen an.
     */
    private boolean validateInput() {
        boolean isValid = true;

        // Name prüfen
        String name = binding.editTextName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            binding.editTextName.setError("Name darf nicht leer sein");
            isValid = false;
        } else {
            binding.editTextName.setError(null);
        }

        // Radius prüfen
        String radiusStr = binding.editTextRadius.getText().toString().trim();
        if (TextUtils.isEmpty(radiusStr)) {
            binding.editTextRadius.setError("Radius darf nicht leer sein");
            isValid = false;
        } else {
            try {
                float radius = Float.parseFloat(radiusStr);
                if (radius <= 0) {
                    binding.editTextRadius.setError("Radius muss größer als 0 sein");
                    isValid = false;
                } else if (radius < 20) {
                    binding.editTextRadius.setError("Radius sollte mindestens 20 Meter betragen");
                    isValid = false;
                } else if (radius > 5000) {
                    binding.editTextRadius.setError("Radius darf maximal 5000 Meter betragen");
                    isValid = false;
                } else {
                    binding.editTextRadius.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.editTextRadius.setError("Ungültiger Zahlenwert");
                isValid = false;
            }
        }

        return isValid;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (GeofenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " muss das GeofenceDialogListener-Interface implementieren");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    /**
     * Interface für Callbacks aus dem Dialog.
     */
    public interface GeofenceDialogListener {
        void onGeofenceCreated(String name, double latitude, double longitude, float radius);
    }
}