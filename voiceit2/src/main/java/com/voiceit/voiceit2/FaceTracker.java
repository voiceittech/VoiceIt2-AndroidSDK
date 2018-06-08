package com.voiceit.voiceit2;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Face tracker for each detected individual.
 */
class FaceTracker extends Tracker<Face> {

    private final Activity mActivity;
    private final RadiusOverlayView mOverlay;
    private final viewCallBacks mCallbacks;

    private final String mTAG = "FaceTracker";

    private final int [] mLivenessChallengeOrder;

    private final boolean mDoLivenessCheck;

    public static int livenessChallengesPassed = 0;
    private boolean mDisplayingChallenge = false;
    private boolean mDisplayingChallengeOutcome = false;
    private boolean mTimingLiveness = false;
//    private boolean updateBlinkDisplay = true;
//    private boolean blinking = false;
//    private int blinks = 0;
//    private int neededBlinks = 2;

    public static boolean continueDetecting = true;
    public static boolean lookingAway = false;
    private static boolean timingLookingAway = false;

    FaceTracker(RadiusOverlayView overlay, Activity activity, viewCallBacks callbacks, int [] livenessChallengeOrder, boolean doLivenessCheck) {
        mOverlay = overlay;
        mActivity = activity;
        mCallbacks = callbacks;
        mLivenessChallengeOrder = livenessChallengeOrder;
        mDoLivenessCheck = doLivenessCheck;
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

    private void completeLivenessChallenge() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Quick pause, .75 seconds, in-between challenges
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(FaceTracker.continueDetecting) {
                            setProgressCircleAngle(270.0, 0.0);
                            mDisplayingChallenge = false;
                            mDisplayingChallengeOutcome = false;
                            mTimingLiveness = false;
                            FaceTracker.timingLookingAway = false;
                            FaceTracker.livenessChallengesPassed++;
                            // Take picture in the middle of liveness checks
                            if (FaceTracker.livenessChallengesPassed == 1) {
                                FaceTracker.continueDetecting = false;
                                mCallbacks.takePictureCallBack();
                            }
                        }
                    }
                }, 750);
            }
        });
    }

    private void livenessCheck(Face face) {

        switch (mLivenessChallengeOrder[FaceTracker.livenessChallengesPassed]) {

            // Smile
            case 1:
                if (!mDisplayingChallenge) {
                    mDisplayingChallenge = true;
                    updateDisplayText(mActivity.getString(R.string.SMILE), false);

                } else if (face.getIsSmilingProbability() > .5 && !mDisplayingChallengeOutcome) {
                    mDisplayingChallengeOutcome = true;
                    setProgressCircleColor(R.color.green);
                    setProgressCircleAngle(270.0,359.999);
                    completeLivenessChallenge();
                }
                break;

                // Turn face left
            case 2:
                if (!mDisplayingChallenge) {
                    mDisplayingChallenge = true;
                    updateDisplayText(mActivity.getString(R.string.TURN_LEFT), false);
                    setProgressCircleColor(R.color.lightGreen);
                    setProgressCircleAngle(135.0, 90.0);

                } else if (!mDisplayingChallengeOutcome) {
                    if(face.getEulerY() > 18.0) {
                        mDisplayingChallengeOutcome = true;
                        setProgressCircleColor(R.color.green);
                        completeLivenessChallenge();

                    } else if (face.getEulerY() < -22.0) {
                        // Turned wrong direction so fail
                        failLiveness();
                    }
                }
                break;

                // Turn face right
            case 3:
                if (!mDisplayingChallenge) {
                    mDisplayingChallenge = true;
                    updateDisplayText(mActivity.getString(R.string.TURN_RIGHT), false);
                    setProgressCircleColor(R.color.lightGreen);
                    setProgressCircleAngle(315.0, 90.0);

                } else if (!mDisplayingChallengeOutcome) {
                    if(face.getEulerY() < -18.0) {
                        mDisplayingChallengeOutcome = true;
                        setProgressCircleColor(R.color.green);
                        completeLivenessChallenge();

                    } else if (face.getEulerY() > 22.0) {
                        // Turned wrong direction so fail
                        failLiveness();
                    }
                }
                break;

                /* The current Android FaceDetector does not catch blinks fast/well in
                    enough environments for liveness detection  */

                // Blink
//            case 4:
//                if(!mDisplayingChallenge && updateBlinkDisplay) {
//                    mActivity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            updateDisplayText(mActivity.getString(mActivity.getResources()
//                                    .getIdentifier("BLINK_"+ (neededBlinks - blinks) +"_TIME" ,
//                                            "string", mActivity.getPackageName())));
//                            updateBlinkDisplay = false;
//                        }
//                    });
//                }
//
//                double rightProb = face.getIsRightEyeOpenProbability();
//                double leftProb = face.getIsLeftEyeOpenProbability();
//                double eyeOpenThreshold = .45;
//
//                if(!blinking) {
//                    if (rightProb < eyeOpenThreshold
//                            && leftProb < eyeOpenThreshold) {
//                        blinking = true;
//                    }
//                } else {
//                    if (rightProb > eyeOpenThreshold
//                            && leftProb > eyeOpenThreshold) {
//                        blinking = false;
//                        blinks++;
//                        updateBlinkDisplay = true;
//                    }
//                }
//
//                if(blinks == neededBlinks && !mDisplayingChallengeOutcome) {
//                    mDisplayingChallengeOutcome = true;
//                    mActivity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            overlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.green));
//                            overlay.setProgressCircleAngle(359.999);
//                            // Quick pause in-between challenges
//                            new Handler().postDelayed(new Runnable() {
//                                @Override
//                                public void run() {
//                                    completeLivenessChallenge();
//                                    updateBlinkDisplay = true;
//                                }
//                             }, 750);
//                        }
//                    });
//                }
//                break;
            default:
                break;
        }
    }

    private void failLiveness() {
        FaceTracker.continueDetecting = false;

        // Display fail to user
        setProgressCircleAngle(0.0, 0.0);
        setProgressCircleColor(R.color.red);
        setProgressCircleAngle(270.0, 359.0);
        // Lock display so threaded liveness check displays don't interrupt failed message
        updateDisplayText(mActivity.getString(R.string.FAILED_LIVENESS), true);

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Wait for 2.5 seconds
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent("voiceit-failure");
                        JSONObject json = new JSONObject();
                        try {
                            json.put("message", "User Failed Liveness Detection");
                        } catch (JSONException e) {
                            Log.d(mTAG, "JSON Exception : " + e.getMessage());
                        }
                        intent.putExtra("Response", json.toString());
                        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(intent);

                        // Unlock display to show next message then exit
                        mOverlay.unlockDisplay();
                        mActivity.finish();
                    }
                }, 2500);
            }
        });
    }

    /**
     * Update the position/characteristics of the face.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, final Face face) {
        if (FaceTracker.continueDetecting) {
            final int numFaces = detectionResults.getDetectedItems().size();
            final int mLivenessChallengesNeeded = 2;

            if (numFaces == 1) {
                FaceTracker.lookingAway = false;
                FaceTracker.timingLookingAway = false;

                // End liveness detection
                if (FaceTracker.livenessChallengesPassed == mLivenessChallengesNeeded || !mDoLivenessCheck) {
                    FaceTracker.continueDetecting = false;

                    updateDisplayText(mActivity.getString(R.string.WAIT), false);
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // Quick pause, 1 seconds, after all challenges are done
                                    setProgressCircleAngle(270.0, 0.0);
                                    setProgressCircleColor(R.color.yellow);
                                    if (!mDoLivenessCheck) {
                                        // Since picture was not taken during liveness check, take one now then auth
                                        mCallbacks.takePictureCallBack();
                                    } else {
                                        // verify or enroll callback
                                        mCallbacks.authMethodToCallBack();
                                    }
                                }
                            }, 1000);
                        }
                    });

                } else if (FaceTracker.livenessChallengesPassed < mLivenessChallengesNeeded) {
                    // Display a liveness challenge to the user
                    livenessCheck(face);

                    // Start a timer for current liveness challenge
                    if (!mTimingLiveness) {
                        mTimingLiveness = true;
                        // Check if user failed liveness check
                        // Store current liveness checks passed count to compare if progress was made later
                        final int cachedLivenessChallengesPassed = FaceTracker.livenessChallengesPassed;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Fail after 3 seconds
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Fail if on the same liveness check as when the timer started
                                        if (cachedLivenessChallengesPassed == FaceTracker.livenessChallengesPassed) {
                                            failLiveness();
                                        }
                                    }
                                }, 3000);
                            }
                        });
                    }
                }

            } else if (numFaces > 1) {
                Log.d("FaceDetection", "Too many faces present");
                updateDisplayText(mActivity.getString(R.string.TOO_MANY_FACES), false);
                setProgressCircleAngle(270.0, 0.0);
            }
        }
    }

    /**
     * Called when the face is assumed to be gone for good.
     */
    @Override
    public void onDone() {
        FaceTracker.lookingAway = true;
        if(FaceTracker.continueDetecting) {
            Log.d("FaceDetection", "No face present");

            setProgressCircleAngle(270.0, 0.0);
            updateDisplayText(mActivity.getString(R.string.LOOK_INTO_CAM), false);
            FaceTracker.timingLookingAway = true;

            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Fail after 2 seconds
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Fail if the user looked away too long after starting
                            if (FaceTracker.timingLookingAway && FaceTracker.continueDetecting) {
                                FaceTracker.timingLookingAway = false;
                                failLiveness();
                            }
                        }
                    }, 2000);
                }
            });

        }
    }

    public interface viewCallBacks { // interface with callback methods for the views
        void authMethodToCallBack();
        void takePictureCallBack();
    }
}
