package com.voiceit.voiceit2;

import android.app.Activity;
import android.content.Intent;
import android.os.CountDownTimer;
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
public class FaceTracker extends Tracker<Face> {

    private Activity mActivity;
    private RadiusOverlayView mOverlay;
    private authCallBack mAuthCallback;

    private int [] mLivenessChallengeOrder;

    private boolean mDoLivenessCheck;

    public static int livenessChallengesPassed = 0;
    private boolean displayingChallenge = false;
    private boolean displayingChallengeOutcome = false;
    private boolean timingLiveness = false;
//    private boolean updateBlinkDisplay = true;
//    private boolean blinking = false;
//    private int blinks = 0;
//    private int neededBlinks = 2;

    public static boolean continueDetecting = true;

    FaceTracker(RadiusOverlayView overlay, Activity activity, authCallBack authCallback, int [] livenessChallengeOrder, boolean doLivenessCheck) {
        mOverlay = overlay;
        mActivity = activity;
        mAuthCallback = authCallback;
        mLivenessChallengeOrder = livenessChallengeOrder;
        mDoLivenessCheck = doLivenessCheck;
    }

    private void completeLivenessChallenge() {
        // Quick pause in-between challenges
        new CountDownTimer(750, 750) {
            public void onTick(long millisUntilFinished) {}
            public void onFinish() {
                mOverlay.setProgressCircleAngle(0);
                mOverlay.updateDisplayText("");
                displayingChallenge = false;
                displayingChallengeOutcome = false;
                timingLiveness = false;
                livenessChallengesPassed++;
            }
        }.start();
    }

    private void livenessCheck(Face face) {

        switch (mLivenessChallengeOrder[livenessChallengesPassed]) {

            case 1:
                if (!displayingChallenge) {
                    displayingChallenge = true;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.updateDisplayText(mActivity.getString(R.string.SMILE));
                        }
                    });
                } else if (face.getIsSmilingProbability() > .5 && !displayingChallengeOutcome) {
                    displayingChallengeOutcome = true;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.green));
                            mOverlay.setProgressCircleAngle(359.999);
                            completeLivenessChallenge();
                        }
                    });
                }
                break;

            case 2:
                if (!displayingChallenge) {
                    displayingChallenge = true;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.updateDisplayText(mActivity.getString(R.string.TURN_LEFT));
                            mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.lightGreen));
                            mOverlay.setProgressCircleAngle(135, 90);
                        }
                    });
                } else if (face.getEulerY() > 18.0 && !displayingChallengeOutcome) {
                    displayingChallengeOutcome = true;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.green));
                            completeLivenessChallenge();
                        }
                    });
                }
                break;

            case 3:
                if (!displayingChallenge) {
                    displayingChallenge = true;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.updateDisplayText(mActivity.getString(R.string.TURN_RIGHT));
                            mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.lightGreen));
                            mOverlay.setProgressCircleAngle(315, 90);
                        }
                    });
                } else if (face.getEulerY() < -18.0 && !displayingChallengeOutcome) {
                    displayingChallengeOutcome = true;
                    mActivity.runOnUiThread( new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.green));
                            completeLivenessChallenge();
                        }
                    });
                }
                break;

                /* The current Android FaceDetector does not catch blinks fast/well in
                    enough environments for liveness detection  */
//            case 4:
//                if(!displayingChallenge && updateBlinkDisplay) {
//                    mActivity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mOverlay.updateDisplayText(mActivity.getString(mActivity.getResources()
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
//                if(blinks == neededBlinks && !displayingChallengeOutcome) {
//                    displayingChallengeOutcome = true;
//                    mActivity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            overlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.green));
//                            overlay.setProgressCircleAngle(359.999);
//                            // Quick pause in-between challenges
//                            new CountDownTimer(1000, 1000) {
//                                public void onTick(long millisUntilFinished) {}
//                                public void onFinish() {
//                                    completeLivenessChallenge();
//                                    updateBlinkDisplay = true;
//                                }
//                            }.start();
//                        }
//                    });
//                }
//                break;
            default:
                break;
        }
    }

    /**
     * Update the position/characteristics of the face.
     */
    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, final Face face) {
        if(continueDetecting) {
            final int livesnessChallengesNeeded = 2;
            final int numFaces = detectionResults.getDetectedItems().size();
            if (numFaces == 1) {
                if (livenessChallengesPassed == livesnessChallengesNeeded || !mDoLivenessCheck) {
                    continueDetecting = false;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOverlay.updateDisplayText(mActivity.getString(R.string.LOOK_INTO_CAM));
                            // Quick pause after challenges
                            new CountDownTimer(1000, 1000) {
                                public void onTick(long millisUntilFinished) {}
                                public void onFinish() {
                                    mOverlay.setProgressCircleAngle(0);
                                    mOverlay.setProgressCircleColor(mActivity.getResources().getColor(R.color.yellow));
                                    // verify or enroll callback
                                    mAuthCallback.authMethodToCallBack();
                                }
                            }.start();
                        }
                    });
                } else if (livenessChallengesPassed < livesnessChallengesNeeded) {
                    // Display a liveness challenge to the user
                    livenessCheck(face);

                    // Start a timer for current liveness challenge
                    if (!timingLiveness) {
                        timingLiveness = true;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                // Store current liveness check to compare if progress was made later
                                final int currentLivenessChallengesPassed = livenessChallengesPassed;
                                new CountDownTimer(3000, 3000) {
                                    public void onTick(long millisUntilFinished) {}
                                    public void onFinish() {
                                        // Fail if on the same liveness check as when the timer started
                                        if (currentLivenessChallengesPassed == livenessChallengesPassed) {
                                            continueDetecting = false;

                                            // Display fail to user
                                            mOverlay.setProgressCircleAngle(0);
                                            // Lock display so threaded liveness check displays don't interrupt failed message
                                            mOverlay.updateDisplayTextAndLock(mActivity.getString(R.string.FAILED_LIVENESS));

                                            new CountDownTimer(2500, 2500) {
                                                public void onTick(long millisUntilFinished) {}
                                                public void onFinish() {
                                                    Intent intent = new Intent("voiceit-failure");
                                                    JSONObject json = new JSONObject();
                                                    try {
                                                        json.put("message", "User Failed Liveness Detection");
                                                    } catch (JSONException e) {
                                                        System.out.println("JSON Exception : " + e.getMessage());
                                                    }
                                                    intent.putExtra("Response", json.toString());
                                                    LocalBroadcastManager.getInstance(mActivity).sendBroadcast(intent);

                                                    // Unlock display to show next steps then continue
                                                    mOverlay.unlockDisplay();
                                                    mActivity.finish();
                                                }
                                            }.start();
                                        }
                                    }
                                }.start();
                            }
                        });
                    }
                }
            } else if (numFaces > 1) {
                Log.d("FaceDetection", "Too many faces present");
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mOverlay.updateDisplayText(mActivity.getString(R.string.TOO_MANY_FACES));
                        mOverlay.setProgressCircleAngle(0);
                    }
                });
            }
        }
    }

    /**
     * Called when the face is assumed to be gone for good.
     */
    @Override
    public void onDone() {
        if(continueDetecting) {
            Log.d("FaceDetection", "No face present");
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mOverlay.setProgressCircleAngle(0);
                    mOverlay.updateDisplayText(mActivity.getString(R.string.LOOK_INTO_CAM));
                }
            });

        }
    }

    public interface authCallBack { //declare an interface with callback method
        void authMethodToCallBack();
    }
}
