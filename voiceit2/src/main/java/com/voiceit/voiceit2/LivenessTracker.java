package com.voiceit.voiceit2;

import android.app.Activity;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.media.MediaPlayer;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;


import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Face tracker for each detected individual.
 */
class LivenessTracker extends Tracker<Face> {

    private final Activity mActivity;
    private final RadiusOverlayView mOverlay;
    private final viewCallBacks mCallbacks;

    private final String mTAG = "LivenessTracker";

    private final int [] mLivenessChallengeOrder;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private final boolean mDoLivenessCheck;
    private final boolean mDoLivenessAudioCheck;
    private final int mLivenessChallengeFailsAllowed;
    private final int mLivenessChallengesNeeded;
    private static int challengeIndex = 0;

    static int livenessChallengesPassed = 0;
    static int livenessChallengeFails = 0;
    private static final double faceTurnedValue = 18.0;
    private boolean mDisplayingChallenge = false;
    private boolean mDisplayingChallengeOutcome = false;
    private boolean mTimingLiveness = false;
    private boolean mLivenessSuccess = false;
    private List<String> mLcoStrings;
    private List<String> mLco;
    private String mLcoId;
    private String mUiLivenessInstruction;
    private float mChallengeTime;
    private String mCountryCode;
    private String mScreenType;
    private String mPhrase;
    private CameraSource mCameraSource;
    private MediaRecorder recorder;
    static boolean continueDetecting = true;
    static boolean lookingAway = false;
    static boolean isLivenessTested = false;
    static final Handler livenessTimer = new Handler();

    LivenessTracker(RadiusOverlayView overlay, Activity activity, viewCallBacks callbacks, int[] livenessChallengeOrder,
                    boolean doLivenessCheck, boolean doLivenessAudioCheck, String phrase, int livenessChallengeFailsAllowed,
                    int livenessChallengesNeeded, String uiLivenessInstruction, List<String> lcoStrings,
                    List<String> lco, float challengeTime, boolean livenessSuccess, String lcoId, String countryCode,
                    String screenType, CameraSource cameraSource) {
        mOverlay = overlay;
        mActivity = activity;
        mCallbacks = callbacks;
        mLivenessChallengeOrder = livenessChallengeOrder;
        mDoLivenessCheck = doLivenessCheck;
        mDoLivenessAudioCheck = doLivenessAudioCheck;
        mLivenessChallengeFailsAllowed = livenessChallengeFailsAllowed;
        mLivenessChallengesNeeded = livenessChallengesNeeded;
        mLivenessSuccess = livenessSuccess;
        mChallengeTime = challengeTime;
        mLco = lco;
        mLcoStrings = lcoStrings;
        mLcoId = lcoId;
        mUiLivenessInstruction = uiLivenessInstruction;
        mCountryCode = countryCode;
        mScreenType = screenType;
        mPhrase = phrase;
        mCameraSource = cameraSource;
    }

    private void setProgressCircleColor(final Integer color) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOverlay.setProgressCircleColor(mActivity.getResources().getColor(color));
            }
        });
    }

    private void setProgressCircleAngle(final Double startAngle, final Double endAngle) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOverlay.setProgressCircleAngle(startAngle, endAngle);
            }
        });
    }

    private void updateDisplayText(final String text, final boolean lock) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(lock) {
                    mOverlay.updateDisplayTextAndLock(text);
                } else {
                    mOverlay.updateDisplayText(text);
                }
            }
        });
    }
    private void playLivenessPrompt(final String livenessPrompt) {
        if(mDoLivenessAudioCheck) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
                    if (mCountryCode == "es-ES") {
                        int resId = mActivity.getApplicationContext().getResources().getIdentifier(
                                livenessPrompt + "_es",
                                "raw",
                                mActivity.getApplicationContext().getPackageName()
                        );
                        mediaPlayer = MediaPlayer.create(mActivity.getApplicationContext(), resId);
                        mediaPlayer.start();
                    } else {
                        int resId = mActivity.getApplicationContext().getResources().getIdentifier(
                                livenessPrompt,
                                "raw",
                                mActivity.getApplicationContext().getPackageName()
                        );
                        mediaPlayer = MediaPlayer.create(mActivity.getApplicationContext(), resId);
                        mediaPlayer.start();
                    }
                }
            });
        }
    }

    /**
     * Update the position/characteristics of the face.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, final Face face) {
        if(mDoLivenessCheck) {
            if (!isLivenessTested) {
                isLivenessTested = true;
                updateDisplayText(mUiLivenessInstruction,false);
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                performLivenessTest();
                            }
                        }, 5000);
                    }
                });
            }
        } else {
            if (LivenessTracker.continueDetecting) {
                final int numFaces = detectionResults.getDetectedItems().size();

                if (numFaces == 1 && mOverlay.insidePortraitCircle(mActivity, face)) {
                    LivenessTracker.lookingAway = false;

                    // Success or skip liveness detection
                    if (LivenessTracker.livenessChallengesPassed == mLivenessChallengesNeeded || !mDoLivenessCheck) {
                        LivenessTracker.continueDetecting = false;
                        LivenessTracker.livenessChallengeFails = 0;

                        updateDisplayText(mActivity.getString(R.string.WAIT), false);
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Quick pause, .75 seconds, after all challenges are done
                                        setProgressCircleAngle(270.0, 0.0);
                                        setProgressCircleColor(R.color.progressCircle);
                                        // Display picture of user at the end of process
                                        mOverlay.displayPicture = true;

                                        if (!mDoLivenessCheck) {
                                            // Take picture of user in preview to show
                                            CameraSource.captureNextPreviewFrame = true;

                                            // Since picture was not taken during any liveness checks,
                                            // take one now then auth
                                            mCallbacks.takePictureCallBack();
                                        } else {
                                            // verify or enroll callback
                                            mCallbacks.authMethodToCallBack();
                                        }
                                    }
                                }, 750);
                            }
                        });

                    } else if (LivenessTracker.livenessChallengesPassed < mLivenessChallengesNeeded) {

                        // Display a liveness challenge to the user

                        // Start a timer for current liveness challenge
                        if (!mTimingLiveness && LivenessTracker.continueDetecting) {
                            mTimingLiveness = true;
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Fail after 3 seconds
                                    livenessTimer.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Fail if user didn't pass a liveness check in time
                                        }
                                    }, 3000);
                                }
                            });
                        }
                    }

                } else if (numFaces > 1) {
                    Log.d(mTAG, "Too many faces present");
                    updateDisplayText(mActivity.getString(R.string.TOO_MANY_FACES), false);
                    setProgressCircleAngle(270.0, 0.0);
                }
            }
        }
    }

    private void beginChallenge() {
        updateDisplayText(mLcoStrings.get(0), false);
        illuminateCircles(mLco.get(0).toLowerCase());
        playLivenessPrompt(mLco.get(0).toLowerCase());
        long time = (long) mChallengeTime / mLco.size();
        for (int i = 1; i < mLco.size(); i++) {
            final String challenge = mLcoStrings.get(i);
            final int finalI = i;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    playLivenessPrompt(mLco.get(finalI).toLowerCase());
                    updateDisplayText(challenge, false);
                    illuminateCircles(mLco.get(finalI).toLowerCase());
                }
            }, time * 1000);
        }
    }

    public void startRecording(){
        recorder = new MediaRecorder();
        recorder.setCamera(mCameraSource.getCameraInstance());
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        recorder.setOutputFile(mActivity.getFilesDir() + "/" + File.separator + "video.mp4");
        try{
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void performLivenessTest() {
        if(mScreenType.equals("face_verification")) {
            // start writing to video file
            // start recording
            beginChallenge();
            // stop recording
            // send video file and handle response
        }
        if(mScreenType.equals("video_verification")) {

            startRecording();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateDisplayText(mActivity.getString(R.string.SAY_PASSPHRASE, mPhrase), true);
                    mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.progressCircle));
                    mOverlay.startDrawingProgressCircle();
                }
            }, (long)mChallengeTime * 1000);

            beginChallenge();

            float time = (mChallengeTime*1000) + 1000;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    recorder.stop();
                }
            }, (long)time);
        }
    }

    private void illuminateCircles(String direction){
        setProgressCircleColor(R.color.pendingLivenessSuccess);
        switch (direction){
            case "face_left":
                setProgressCircleAngle(135.0, 90.0);
                break;
            case "face_right":
                setProgressCircleAngle(315.0, 90.0);
                break;
            case "face_up":
                setProgressCircleAngle(-60.0, -65.0);
                break;
            case "face_down":
                setProgressCircleAngle(60.0, 65.0);
                break;
            case "face_tilt_left":
                setProgressCircleAngle(270.0, -60.0);
                break;
            case "face_tilt_right":
                setProgressCircleAngle(-30.0, -60.0);
                break;
        }
    }

    /**
     * Called when the face is assumed to be gone for good.
     */
    @Override
    public void onDone() {
        LivenessTracker.lookingAway = true;
        if(LivenessTracker.continueDetecting) {
            Log.d(mTAG, "No face present");

            setProgressCircleAngle(270.0, 0.0);
            updateDisplayText(mActivity.getString(R.string.LOOK_INTO_CAM), false);
        }
    }

    interface viewCallBacks { // interface with callback methods for the views
        void authMethodToCallBack();
        void takePictureCallBack();
    }
}
