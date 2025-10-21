package com.example.pathfinder.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pathfinder.R;
import com.example.pathfinder.manager.Manager;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private PreviewView viewFinder;

    private TextView permissionDeniedText;
    private Manager manager;

    private ArFragment arFragment;


    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permissao da camera concedida", Toast.LENGTH_SHORT).show();
                    permissionDeniedText.setVisibility(View.GONE);
                    startArCore();
                } else {
                    Toast.makeText(this, "Permissao da camera recusada", Toast.LENGTH_SHORT).show();
                    permissionDeniedText.setVisibility(View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        permissionDeniedText = findViewById(R.id.permissionDeniedText);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        setupButtons();

        manager = new Manager();

        if (allPermissionsGranted()) {
            startArCore();
        } else {
            permissionDeniedText.setVisibility(View.VISIBLE);
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void setupButtons() {
        // Find the buttons from the layout
        ImageButton onOffButton = findViewById(R.id.onOffButton);
        ImageButton soundButton = findViewById(R.id.soundButton);
        ImageButton repeatButton = findViewById(R.id.repeatButton);

        // Set click listeners
        onOffButton.setOnClickListener(v -> {
            Toast.makeText(this, "On/Off button clicked", Toast.LENGTH_SHORT).show();
            // Action for this button will go here later
        });

        soundButton.setOnClickListener(v -> {
            Toast.makeText(this, "Sound button clicked", Toast.LENGTH_SHORT).show();
            // Action for this button will go here later
        });

        repeatButton.setOnClickListener(v -> {
            Toast.makeText(this, "Repeat button clicked", Toast.LENGTH_SHORT).show();
            // Action for this button will go here later
        });
    }

    private void startArCore() {
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        if (arFragment == null) {
            return;
        }

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::handleFrameUpdate);
    }

    private void handleFrameUpdate(FrameTime frameTime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if (frame == null) return;

        try {
            Pose pose = frame.getCamera().getPose();
            float[] t = pose.getTranslation();
            Log.d("ARPose", "Posição: x=" + t[0] + " y=" + t[1] + " z=" + t[2]);
        } catch (Exception e) {
            Log.e("ARCore", "Erro ao capturar frame: " + e.getMessage());
        }
    }


    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (arFragment != null) {
            try {
                arFragment.getArSceneView().resume();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onPause() {
        if (arFragment != null) {
            arFragment.getArSceneView().pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (arFragment != null) {
            arFragment.getArSceneView().destroy();
        }
        super.onDestroy();
    }
}