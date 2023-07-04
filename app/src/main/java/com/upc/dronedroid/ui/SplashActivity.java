package com.upc.dronedroid.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.upc.dronedroid.BuildConfig;
import com.upc.dronedroid.models.User;
import com.upc.dronedroid.utils.ConnectionUtil;
import com.upc.dronedroid.utils.DeviceUtil;
import com.upc.dronedroid.utils.MqttClientUtil;
import com.upc.dronedroid.utils.PermissionUtil;
import com.upc.dronedroid.R;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;

import java.util.List;
import java.util.Objects;

import static android.os.Process.killProcess;
import static android.os.Process.myPid;

public class SplashActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    /* TODO: Set hardcoded texts as text resources in order to grant i18N.
        The hardcoded texts are used almost always on toasts/snackbars
    */

    private final String[] permissionsLocation = {
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final int requestCodeLocation = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        //Due to Android SDK <= 24, check is needed
        if (Build.VERSION.SDK_INT <= 24) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        handleMap();
    }

    public void handleMap() {
        if (DeviceUtil.isTablet(getApplicationContext())) {
            //Device is tablet -- display map
            Log.d("INFO", "Is Tablet");
            requestLocationPermissions();
            //Navigate to Tablet Activity (Technical)
            Intent intent = new Intent(SplashActivity.this, TabletActivity.class);
            handleUserLogon(intent);
        } else {
            //Device is not tablet -- do not display map
            Log.d("INFO", "Is Phone");
            //Navigate to Mobile Activity
            Intent intent = new Intent(SplashActivity.this, MobileActivity.class);
            handleUserLogon(intent);
        }

    }

    private void requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Provide an additional rationale to the user if the permission was not granted
                // and the user would benefit from additional context for the use of the permission.
                Log.i("permissions",
                        "Displaying location permission rationale to provide additional context.");

                // Display a SnackBar with an explanation and a button to trigger the request.
                Snackbar.make(findViewById(R.id.main_layout), "Please provide permissions for location in order to show properly the maps",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("OK", view -> ActivityCompat
                                .requestPermissions(SplashActivity.this, permissionsLocation,
                                        requestCodeLocation)).show();
            } else {
                // Location permissions have not been granted yet. Request them directly.
                //this.requestPermissions(permissionsLocation, requestCodeLocation);
                requestPermissions(permissionsLocation, requestCodeLocation);
            }
        } else {
            Log.d("permissions", "Permissions granted automatically");
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode == requestCodeLocation) {
            Log.i("INFO", "Received response for location permissions request.");

            // We have requested multiple permissions for location, so all of them need to be
            // checked.
            if (PermissionUtil.verifyLocalPermissions(grantResults)) {
                Log.i("INFO", "Location permissions were granted.");
                //findViewById(R.id.OSMView).setVisibility(View.VISIBLE);
            } else {
                Log.i("INFO", "Location permissions were NOT granted.");
                /*Snackbar.make(findViewById(R.id.main_layout), "Location permissions not granted. Map won't be displayed.",
                        Snackbar.LENGTH_SHORT)
                        .show();*/
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void handleUserLogon(Intent intent) {
        // Connect to any user first (display list of available users in a Fragment)
        List<User> userList = PermissionUtil.getAvailableUsers(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Login");
        View dialogView = getLayoutInflater().inflate(R.layout.spinner_users_dialog, null);
        Spinner spinner = dialogView.findViewById(R.id.spinner);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.availableUsers));
        spinner.setAdapter(arrayAdapter);
        Spinner spinnerBroker = dialogView.findViewById(R.id.spinner2);
        ArrayAdapter<String> arrayAdapterBroker = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, ConnectionUtil.availableBrokers);
        spinnerBroker.setAdapter(arrayAdapterBroker);
        builder.setPositiveButton("Ok", (dialog, which) -> {
            if (!spinner.getSelectedItem().toString().equalsIgnoreCase("Please select an user to log in")) {
                User selectedUser = Objects.requireNonNull(userList).get(spinner.getSelectedItemPosition() - 1);
                if (selectedUser.needsPassword) {
                    EditText passwordTextView = dialogView.findViewById(R.id.editTextTextPassword);
                    if (PermissionUtil.checkPassword(selectedUser, passwordTextView.getText().toString())) {
                        Toast.makeText(this, spinner.getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
                        intent.putExtra("brokerSelected", spinnerBroker.getSelectedItem().toString());
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                        handleUserLogon(intent);
                    }
                } else {
                    Toast.makeText(this, spinner.getSelectedItem().toString(), Toast.LENGTH_SHORT).show();
                    intent.putExtra("brokerSelected", spinnerBroker.getSelectedItem().toString());
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> killProcess(myPid()));
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}