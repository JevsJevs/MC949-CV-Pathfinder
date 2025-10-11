package com.example.pathfinder.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.pathfinder.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ExecutorService cameraExecutor;
    private PreviewView viewFinder;

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Permissao da camera concedida", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permissao da camera recusada", Toast.LENGTH_SHORT).show();
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

        viewFinder = findViewById(R.id.viewFinder);
        setupButtons();

        // Check for camera permission and request it if it's not granted
        if (allPermissionsGranted()) {
            // You can use the API that requires the permission.
            startCamera();
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void setupButtons() {
        // Find the buttons from the layout
        ImageButton onOffButton = findViewById(R.id.onOffButton);
        ImageButton soundButton = findViewById(R.id.soundButton);
        ImageButton thirdButton = findViewById(R.id.thirdButton);

        // Set click listeners
        onOffButton.setOnClickListener(v -> {
            Toast.makeText(this, "On/Off button clicked", Toast.LENGTH_SHORT).show();
            // Action for this button will go here later
        });

        soundButton.setOnClickListener(v -> {
            Toast.makeText(this, "Sound button clicked", Toast.LENGTH_SHORT).show();
            // Action for this button will go here later
        });

        thirdButton.setOnClickListener(v -> {
            Toast.makeText(this, "Third button clicked", Toast.LENGTH_SHORT).show();
            // Action for this button will go here later
        });
    }

    private void startCamera() {
        // Get a future instance of the camera provider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // Add a listener to the future
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Select the back camera
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind everything before rebinding
                cameraProvider.unbindAll();

                // Bind the use cases to the camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

            } catch (ExecutionException | InterruptedException e) {
                // Handle any errors (like the process being killed)
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this)); // Run on the main thread
    }


    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor to free up resources
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}