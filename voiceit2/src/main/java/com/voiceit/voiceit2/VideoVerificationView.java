package com.voiceit.voiceit2;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.loopj.android.http.JsonHttpResponseHandler;
import cz.msebera.android.httpclient.Header;

import org.json.JSONException;
import org.json.JSONObject;

public class VideoVerificationView extends AppCompatActivity {

    final int PERMISSIONS_REQUEST_RECORD_AUDIO = 0;
    final int PERMISSIONS_REQUEST_CAMERA = 1;
    final int ASK_MULTIPLE_PERMISSION_REQUEST_CODE = 2;

    private static final int RC_HANDLE_GMS = 9001;

    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private MediaRecorder mMediaRecorder;

    private static final String TAG = "VideoVerificationView";
    private Context mContext;

    private RadiusOverlayView overlay;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_AUDIO = 2;

    private VoiceItAPI2 myVoiceIt2;
    private String userID = "";
    private String contentLanguage = "";
    private String phrase = "";
    private boolean doLivenessCheck;

    private int enrollmentCount = 0;
    private final int neededEnrollments = 3;
    private int failedAttempts = 0;
    private final int maxFailedAttempts = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);

        // Grab data from parent activity
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            myVoiceIt2 = new VoiceItAPI2(bundle.getString("apiKey"), bundle.getString("apiToken"));
            userID = bundle.getString("userID");
            contentLanguage = bundle.getString("contentLanguage");
            phrase = bundle.getString("phrase");
            doLivenessCheck = bundle.getBoolean("doLivenessCheck");
        }

        // Hide action bar
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
            System.out.println("Cannot hide action bar");
        }

        // Set context
        mContext = this;
        // Set content view
        setContentView(R.layout.activity_video_verification_view);
        mPreview = findViewById(R.id.camera_preview);

        // Orient screen
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Text output on overlay
        overlay = findViewById(R.id.overlay);
        overlay.updateDisplayText(getString(R.string.LOOK_INTO_CAM));

        // Request media device permissions
        requestHardwarePermissions();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new FaceTrackerFactory(this))
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, "Face detector dependencies cannot be downloaded due to low device storage", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Face detector dependencies cannot be downloaded due to low device storage");
            }
        }

        // Build camera source and attach detector
        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class FaceTrackerFactory implements MultiProcessor.Factory<Face> {

        private Activity mActivity;
        final int livenessChallengeTypesCount = 3;
        int [] livenessChallengeOrder = {1, 2, 3};

        private FaceTrackerFactory(VideoVerificationView activity) {
            mActivity = activity;

            FaceTracker.continueDetecting = true;
            FaceTracker.livenessChallengesPassed = 0;

            // Randomize liveness check test order
            final Random rand = new Random();
            for(int i = 0; i < livenessChallengeTypesCount; i++) {
                int j = rand.nextInt(livenessChallengeTypesCount -1);
                int temp = livenessChallengeOrder[i];
                livenessChallengeOrder[i] = livenessChallengeOrder[j];
                livenessChallengeOrder[j] = temp;
            }
        }

        @Override
        public Tracker<Face> create(Face face) {
            return new FaceTracker(overlay, mActivity, new FaceTrackerCallBackImpl(), livenessChallengeOrder, doLivenessCheck);
        }
    }

    // Verify after taking picture
    private CameraSource.PictureCallback mPicture = new CameraSource.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data) {

            // Create file
            final File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }
            // Write picture to file
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            // Wait for ~1 seconds
            new CountDownTimer(1000, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    overlay.updateDisplayText(getString(R.string.SAY_PASSPHRASE));
                    // Wait for ~0.1 seconds for user to see phrase
                    new CountDownTimer(100, 100) {
                        public void onTick(long millisUntilFinished) {}
                        public void onFinish() {

                            // Capture Audio
                            final MediaRecorder mMediaRecorder = new MediaRecorder();
                            try {
                                // Create file for audio
                                final File audioFile = getOutputMediaFile(MEDIA_TYPE_AUDIO);

                                // Setup audio device
                                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                                mMediaRecorder.setAudioSamplingRate(44000);
                                mMediaRecorder.setAudioChannels(1);
                                mMediaRecorder.setAudioEncodingBitRate(8000);
                                mMediaRecorder.setOutputFile(audioFile.getAbsolutePath());
                                mMediaRecorder.prepare();
                                mMediaRecorder.start();

                                overlay.setProgressCircleColor(getResources().getColor(R.color.yellow));
                                overlay.startDrawingProgressCircle();
                                // Record for ~5 seconds, then send data
                                // 4800 to make sure recording is not over 5 seconds
                                new CountDownTimer(4800, 4800) {
                                    public void onTick(long millisUntilFinished) {}
                                    public void onFinish() {
                                        try {
                                            mMediaRecorder.stop();
                                            mMediaRecorder.reset();
                                            mMediaRecorder.release();

                                            overlay.updateDisplayText(getString(R.string.WAIT));
                                            myVoiceIt2.videoVerification(userID,  audioFile, pictureFile, contentLanguage, new JsonHttpResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                                                    System.out.println("JSONResult : " + response.toString());

                                                    try {
                                                        // Wrong phrase
                                                        if (!response.getString("text").toLowerCase().equals(phrase.toLowerCase())) {
                                                            overlay.setProgressCircleColor(getResources().getColor(R.color.red));
                                                            overlay.updateDisplayText(getString(R.string.VERIFY_FAIL));

                                                            // Wait for ~1.5 seconds
                                                            new CountDownTimer(1500, 1500) {
                                                                public void onTick(long millisUntilFinished) {}
                                                                public void onFinish() {

                                                                    overlay.updateDisplayText(getString(R.string.INCORRECT_PASSPHRASE));

                                                                    // Wait for ~4.5 seconds
                                                                    new CountDownTimer(4500, 4500) {
                                                                        public void onTick(long millisUntilFinished) {
                                                                        }

                                                                        public void onFinish() {
                                                                            audioFile.deleteOnExit();
                                                                            pictureFile.deleteOnExit();
                                                                            failedAttempts++;

                                                                            // User failed too many times
                                                                            if (failedAttempts >= maxFailedAttempts) {
                                                                                overlay.updateDisplayText(getString(R.string.TOO_MANY_ATTEMPTS));
                                                                                // Wait for ~2 seconds
                                                                                new CountDownTimer(2000, 2000) {
                                                                                    public void onTick(long millisUntilFinished) {
                                                                                    }

                                                                                    public void onFinish() {
                                                                                        exitViewWithMessage("Too many attempts");
                                                                                    }
                                                                                }.start();
                                                                            }
                                                                        }
                                                                    }.start();
                                                                }
                                                            }.start();
                                                        // Success
                                                        } else if (response.getString("responseCode").contains("SUCC")) {
                                                            overlay.setProgressCircleColor(getResources().getColor(R.color.green));
                                                            overlay.updateDisplayText(getString(R.string.VERIFY_SUCCESS));

                                                            // Wait for ~2 seconds
                                                            new CountDownTimer(2000, 2000) {
                                                                public void onTick(long millisUntilFinished) {}
                                                                public void onFinish() {
                                                                    audioFile.deleteOnExit();
                                                                    pictureFile.deleteOnExit();

                                                                    exitViewWithJSON(response);
                                                                }
                                                            }.start();
                                                        // Fail
                                                        } else {
                                                            overlay.setProgressCircleColor(getResources().getColor(R.color.red));
                                                            overlay.updateDisplayText(getString(R.string.VERIFY_FAIL));

                                                            // Wait for ~1.5 seconds
                                                            new CountDownTimer(1500, 1500) {
                                                                public void onTick(long millisUntilFinished) {}
                                                                public void onFinish() {
                                                                    try {
                                                                        // Report error to user
                                                                        overlay.updateDisplayText(getString((getResources().getIdentifier(response.
                                                                                getString("responseCode"), "string", getPackageName()))));
                                                                    } catch (JSONException e) {
                                                                        System.out.println("JSON exception : " + e.toString());
                                                                    }
                                                                    // Wait for ~4.5 seconds
                                                                    new CountDownTimer(4500, 4500) {
                                                                        public void onTick(long millisUntilFinished) {}
                                                                        public void onFinish() {
                                                                            audioFile.deleteOnExit();
                                                                            pictureFile.deleteOnExit();
                                                                            failedAttempts++;

                                                                            // User failed too many times
                                                                            if(failedAttempts > maxFailedAttempts) {
                                                                                overlay.updateDisplayText(getString(R.string.TOO_MANY_ATTEMPTS));
                                                                                // Wait for ~2 seconds
                                                                                new CountDownTimer(2000, 2000) {
                                                                                    public void onTick(long millisUntilFinished) {}
                                                                                    public void onFinish() {
                                                                                        exitViewWithJSON(response);
                                                                                    }
                                                                                }.start();
                                                                            }
                                                                        }
                                                                    }.start();
                                                                }
                                                            }.start();
                                                        }
                                                    } catch (JSONException e) {
                                                        System.out.println("JSON Error: " + e.getMessage());
                                                    }


                                                }

                                                @Override
                                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, final JSONObject errorResponse) {
                                                    if (errorResponse != null) {
                                                        System.out.println("JSONResult : " + errorResponse.toString());

                                                        overlay.setProgressCircleColor(getResources().getColor(R.color.red));
                                                        overlay.updateDisplayText(getString(R.string.VERIFY_FAIL));

                                                        // Wait for ~1.5 seconds
                                                        new CountDownTimer(1500, 1500) {
                                                            public void onTick(long millisUntilFinished) {}
                                                            public void onFinish() {
                                                                try {
                                                                    // Report error to user
                                                                    overlay.updateDisplayText(getString((getResources().getIdentifier(errorResponse.
                                                                            getString("responseCode"), "string", getPackageName()))));
                                                                } catch (JSONException e) {
                                                                    System.out.println("JSON exception : " + e.toString());
                                                                }
                                                                // Wait for ~4.5 seconds
                                                                new CountDownTimer(4500, 4500) {
                                                                    public void onTick(long millisUntilFinished) {}
                                                                    public void onFinish() {
                                                                        audioFile.deleteOnExit();
                                                                        pictureFile.deleteOnExit();
                                                                        failedAttempts++;

                                                                        // User failed too many times
                                                                        if(failedAttempts > maxFailedAttempts) {
                                                                            overlay.updateDisplayText(getString(R.string.TOO_MANY_ATTEMPTS));
                                                                            // Wait for ~2 seconds
                                                                            new CountDownTimer(2000, 2000) {
                                                                                public void onTick(long millisUntilFinished) {}
                                                                                public void onFinish() {
                                                                                    exitViewWithJSON(errorResponse);
                                                                                }
                                                                            }.start();
                                                                        }
                                                                    }
                                                                }.start();
                                                            }
                                                        }.start();
                                                    }
                                                }
                                            });
                                        } catch (Exception ex) {
                                            System.out.println("Verification Exception Error: " + ex.getMessage());
                                            exitViewWithMessage("Verification Exception Error");
                                        }
                                    }
                                }.start();
                            } catch (Exception ex) {
                                System.out.println("Recording Error: " + ex.getMessage());
                                exitViewWithMessage("Recording Error");
                            }
                        }
                    }.start();
                }
            }.start();
        }
    };

    /** Create a File for saving an image or audio file */
    private static File getOutputMediaFile(int type){
        try {
            if (type == MEDIA_TYPE_IMAGE) {
                return File.createTempFile("prefix-", "-suffix");
            } else if (type == MEDIA_TYPE_AUDIO) {
                return File.createTempFile("tempfile", ".wav");
            } else {
                return null;
            }
        } catch (IOException e) {
            System.out.println("Creating file failed with exception : " + e.getMessage());
            return null;
        }
    }

    private void requestHardwarePermissions() {
        // MY_PERMISSIONS_REQUEST_* is an app-defined int constant. The callback method gets the
        // result of the request.
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                requestPermissions(new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.CAMERA},
                        ASK_MULTIPLE_PERMISSION_REQUEST_CODE);
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSIONS_REQUEST_RECORD_AUDIO);
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                            PERMISSIONS_REQUEST_CAMERA);
                }
            }
        } else {
            // Try to setup camera source
            createCameraSource();
            // Try to start camera
            startCameraSource();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            System.out.println("Hardware Permissions not granted");
            exitViewWithMessage("Hardware Permissions not granted");

        }
    }

    private void exitViewWithMessage(String message) {
        Intent intent = new Intent("voiceit-failure");
        JSONObject json = new JSONObject();
        try {
            json.put("message", message);
        } catch(JSONException e) {
            System.out.println("JSON Exception : " + e.getMessage());
        }
        intent.putExtra("Response", json.toString());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        finish();
    }

    private void exitViewWithJSON(JSONObject json) {
        Intent intent = new Intent("voiceit-failure");
        intent.putExtra("Response", json.toString());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        exitViewWithMessage("User Canceled");
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestHardwarePermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
        }
    }

    public void verifyUser() {
        // Check enrollments then verify
        myVoiceIt2.getAllEnrollmentsForUser(userID, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, final JSONObject response) {
                System.out.println("getAllEnrollmentsForUser JSONResult : " + response.toString());

                try {
                    enrollmentCount = response.getInt("count");
                    // Check If enough enrollments, otherwise return to previous activity
                    if(enrollmentCount < neededEnrollments) {
                        overlay.updateDisplayText(getString(R.string.NOT_ENOUGH_ENROLLMENTS));
                        // Wait for ~2.5 seconds
                        new CountDownTimer(2500, 2500) {
                            public void onTick(long millisUntilFinished) {}
                            public void onFinish() {
                                exitViewWithJSON(response);
                            }
                        }.start();
                    } else {
                        try {
                            // Wait for ~.1 seconds for camera to stabilize
                            new CountDownTimer(100, 100) {
                                public void onTick(long millisUntilFinished) {}
                                public void onFinish() {
                                    Camera mCamera = CameraSourcePreview.getCamera(mCameraSource);
                                    if(mCamera != null) {
                                        Camera.Parameters params = mCamera.getParameters();
                                        List<Integer> formats = params.getSupportedPictureFormats();
                                        if (formats.contains(ImageFormat.JPEG)) {
                                            params.setPictureFormat(ImageFormat.JPEG);
                                            params.setJpegQuality(50);
                                        }
                                        mCamera.setParameters(params);
                                        mCameraSource.takePicture(null, mPicture);
                                    }
                                }
                            }.start();
                        } catch (Exception e) {
                            System.out.println("Camera exception : " + e.getMessage());
                            exitViewWithMessage("Camera exception");
                        }
                    }
                } catch (JSONException e) {
                    System.out.println("JSON userId error: " + e.getMessage());
                    exitViewWithMessage("JSON userId error");
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                if (errorResponse != null) {
                    exitViewWithJSON(errorResponse);
                }
            }
        });
    }

    class FaceTrackerCallBackImpl implements FaceTracker.authCallBack { //class that implements the method to callback defined in the interface
        public void authMethodToCallBack() {
            verifyUser();
        }
    }


}