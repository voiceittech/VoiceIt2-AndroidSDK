package com.voiceit.voiceit2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.loopj.android.http.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;

public class VoiceItAPI2 {
    private static final String BASE_URL = "https://api.voiceit.io";
    private AsyncHttpClient client;
    private String apiKey;
    private String apiToken;

    public VoiceItAPI2(String apiKey, String apiToken){
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        client = new AsyncHttpClient();
        this.client.removeAllHeaders();
        this.client.setTimeout(15 * 1000);
        this.client.setBasicAuth(apiKey, apiToken);
    }

    public void getAllUsers(AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl("/users"), responseHandler);
    }

    public void createUser(AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl("/users"), responseHandler);
    }

    public void getUser(String userId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.get(getAbsoluteUrl("/users/" + userId), responseHandler);
    }

    public void deleteUser(String userId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.delete(getAbsoluteUrl("/users/" + userId), responseHandler);
    }

    public void getGroupsForUser(String userId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.get(getAbsoluteUrl("/users/" + userId + "/groups"), responseHandler);
    }

    public void getAllEnrollmentsForUser(String userId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.get(getAbsoluteUrl("/enrollments/" + userId), responseHandler);
    }

    public void deleteAllEnrollmentsForUser(String userId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.delete(getAbsoluteUrl("/enrollments/" + userId + "/all"), responseHandler);
    }

    public void getFaceFaceEnrollmentsForUser(String userId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.get(getAbsoluteUrl("/enrollments/face/" + userId), responseHandler);
    }

    public void createVoiceEnrollment(String userId, String contentLanguage, File recording, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("recording", recording);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments"), params, responseHandler);
    }

    public void createVoiceEnrollment(final String userId, final String contentLanguage, final AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        try{
            final File recordingFile =  File.createTempFile("tempEnrollmentFile", ".wav");
            final MediaRecorder myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            myRecorder.setAudioSamplingRate(44100);
            myRecorder.setAudioChannels(1);
            myRecorder.setAudioEncodingBitRate(16000);
            myRecorder.setOutputFile(recordingFile.getAbsolutePath());
            myRecorder.prepare();
            myRecorder.start();
            CountDownTimer countDowntimer = new CountDownTimer(4800, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    try{
                        myRecorder.stop();
                        myRecorder.reset();
                        myRecorder.release();
                        createVoiceEnrollment(userId, contentLanguage, recordingFile, responseHandler);
                    } catch(Exception ex){
                        System.out.println("Exception Error:"+ex.getMessage());
                    }
                }
            };
            countDowntimer.start();
        }
        catch(Exception ex)
        {
            System.out.println("Recording Error:" + ex.getMessage());
        }
    }

    public void createVoiceEnrollmentByUrl(String userId, String contentLanguage, String fileUrl, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/enrollments/byUrl"), params, responseHandler);
    }

    public void createFaceEnrollment(String userId, File video, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments/face"), params, responseHandler);
    }

    public void createFaceEnrollment(String userId, File video, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("doBlinkDetection", doBlinkDetection);
        try {
            params.put("videoFilePath", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments/face"), params, responseHandler);
    }

    public void createVideoEnrollment(String userId, String contentLanguage, File audio, File photo, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("audio", audio);
            params.put("photo", photo);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments/video"), params, responseHandler);
    }

    public void createVideoEnrollment(String userId, String contentLanguage, File audio, File photo, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("doBlinkDetection", doBlinkDetection);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("audio", audio);
            params.put("photo", photo);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments/video"), params, responseHandler);
    }

    public void createVideoEnrollment(String userId, String contentLanguage, File video, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments/video"), params, responseHandler);
    }

    public void createVideoEnrollment(String userId, String contentLanguage, File video, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("doBlinkDetection", doBlinkDetection);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/enrollments/video"), params, responseHandler);
    }

    public void createVideoEnrollmentByUrl(String userId, String contentLanguage, String fileUrl, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/enrollments/video/byUrl"), params, responseHandler);
    }

    public void createVideoEnrollmentByUrl(String userId, String contentLanguage, String fileUrl, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("doBlinkDetection", doBlinkDetection);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/enrollments/video/byUrl"), params, responseHandler);
    }

    public void deleteFaceEnrollment(String userId, String faceEnrollmentId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.delete(getAbsoluteUrl("/enrollments/face/" + userId + "/" + faceEnrollmentId), responseHandler);
    }

    public void deleteEnrollmentForUser(String userId, String enrollmentId, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.delete(getAbsoluteUrl("/enrollments/" + userId + "/" + enrollmentId), responseHandler);
    }

    public void getAllGroups(AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl("/groups"), responseHandler);
    }

    public void getGroup(String groupId, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.get(getAbsoluteUrl("/groups/" + groupId), responseHandler);
    }

    public void groupExists(String groupId, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.get(getAbsoluteUrl("/groups/" + groupId + "/exists"), responseHandler);
    }

    public void createGroup(String description, AsyncHttpResponseHandler responseHandler) {
        RequestParams params = new RequestParams();
        params.put("description", description);
        client.post(getAbsoluteUrl("/groups"), params, responseHandler);
    }

    public void addUserToGroup(String groupId, String userId, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId) || !userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("userId", userId);
        client.put(getAbsoluteUrl("/groups/addUser"), params, responseHandler);
    }

    public void removeUserFromGroup(String groupId, String userId, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId) || !userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("userId", userId);
        client.put(getAbsoluteUrl("/groups/removeUser"), params, responseHandler);
    }

    public void deleteGroup(String groupId, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        client.delete(getAbsoluteUrl("/groups/" + groupId), responseHandler);
    }

    public void voiceVerification(String userId, String contentLanguage, File recording, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("recording", recording);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }
        client.post(getAbsoluteUrl("/verification"), params, responseHandler);
    }

    public void voiceVerification(final String userId, final String contentLanguage, final AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        try{
            final File recordingFile =  File.createTempFile("tempEnrollmentFile", ".wav");
            final MediaRecorder myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            myRecorder.setAudioSamplingRate(44100);
            myRecorder.setAudioChannels(1);
            myRecorder.setAudioEncodingBitRate(16000);
            myRecorder.setOutputFile(recordingFile.getAbsolutePath());
            myRecorder.prepare();
            myRecorder.start();
            CountDownTimer countDowntimer = new CountDownTimer(4800, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    try{
                        myRecorder.stop();
                        myRecorder.reset();
                        myRecorder.release();
                        voiceVerification(userId, contentLanguage, recordingFile, responseHandler);
                    } catch(Exception ex){
                        System.out.println("Exception Error:"+ex.getMessage());
                    }
                }
            };
            countDowntimer.start();
        }
        catch(Exception ex)
        {
            System.out.println("Recording Error:" + ex.getMessage());
        }
    }

    public void voiceVerificationByUrl(String userId, String contentLanguage, String fileUrl, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/verification/byUrl"), params, responseHandler);
    }

    public void faceVerification(String userId, File video, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("doBlinkDetection", doBlinkDetection);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/verification/face"), params, responseHandler);
    }

    public void faceVerification(String userId, File video,  AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/verification/face"), params, responseHandler);
    }

    public void videoVerification(String userId, String contentLanguage, File video, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("doBlinkDetection", doBlinkDetection);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/verification/video"), params, responseHandler);
    }

    public void videoVerification(String userId, String contentLanguage, File video, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/verification/video"), params, responseHandler);
    }

    public void videoVerification(String userId, File audio, File photo, String contentLanguage, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("doBlinkDetection", doBlinkDetection);
        try {
            params.put("audio", audio);
            params.put("photo", photo);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/verification/video"), params, responseHandler);
    }

    public void videoVerification(String userId, File audio, File photo, String contentLanguage, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("audio", audio);
            params.put("photo", photo);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/verification/video"), params, responseHandler);
    }

    public void videoVerificationByUrl(String userId, String contentLanguage, String fileUrl, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("doBlinkDetection", doBlinkDetection);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/verification/video/byUrl"), params, responseHandler);
    }

    public void videoVerificationByUrl(String userId, String contentLanguage, String fileUrl, AsyncHttpResponseHandler responseHandler) {
        if(!userIdCheck(userId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("userId", userId);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/verification/video/byUrl"), params, responseHandler);
    }

    public void voiceIdentification(String groupId, File recording, String contentLanguage, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("recording", recording);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/identification"), params, responseHandler);
    }

    public void voiceIdentification(final String groupId, final String contentLanguage, final AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        try{
            final File recordingFile =  File.createTempFile("tempEnrollmentFile", ".wav");
            final MediaRecorder myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            myRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            myRecorder.setAudioSamplingRate(44100);
            myRecorder.setAudioChannels(1);
            myRecorder.setAudioEncodingBitRate(16000);
            myRecorder.setOutputFile(recordingFile.getAbsolutePath());
            myRecorder.prepare();
            myRecorder.start();
            CountDownTimer countDowntimer = new CountDownTimer(4800, 1000) {
                public void onTick(long millisUntilFinished) {}
                public void onFinish() {
                    try{
                        myRecorder.stop();
                        myRecorder.reset();
                        myRecorder.release();
                        voiceIdentification(groupId, recordingFile, contentLanguage, responseHandler);
                    } catch(Exception ex){
                        System.out.println("Exception Error:"+ex.getMessage());
                    }
                }
            };
            countDowntimer.start();
        }
        catch(Exception ex)
        {
            System.out.println("Recording Error:" + ex.getMessage());
        }
    }

    public void voiceIdentificationByUrl(String groupId, String fileUrl, String contentLanguage, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/identification/byUrl"), params, responseHandler);
    }

    public void videoIdentification(String groupId, File video, String contentLanguage, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("contentLanguage", contentLanguage);
        params.put("doBlinkDetection", doBlinkDetection);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/identification/video"), params, responseHandler);
    }

    public void videoIdentification(String groupId, File video, String contentLanguage, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("contentLanguage", contentLanguage);
        try {
            params.put("video", video);
        } catch (FileNotFoundException e) {
            Log.d("error: ", e.getMessage());
        }

        client.post(getAbsoluteUrl("/identification/video"), params, responseHandler);
    }

    public void videoIdentificationByUrl(String groupId, String fileUrl, String contentLanguage, boolean doBlinkDetection, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("contentLanguage", contentLanguage);
        params.put("doBlinkDetection", doBlinkDetection);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/identification/video/byUrl"), params, responseHandler);
    }

    public void videoIdentificationByUrl(String groupId, String fileUrl, String contentLanguage, AsyncHttpResponseHandler responseHandler) {
        if(!groupIdCheck(groupId)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }
        RequestParams params = new RequestParams();
        params.put("groupId", groupId);
        params.put("contentLanguage", contentLanguage);
        params.put("fileUrl", fileUrl);

        client.post(getAbsoluteUrl("/identification/video/byUrl"), params, responseHandler);
    }

    public void encapsulatedVideoEnrollment(Activity activity, String userID, String contentLanguage, String phrase, final JsonHttpResponseHandler responseHandler) {
        if(!userIdCheck(userID)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }

        Intent intent = new Intent(activity, VideoEnrollmentView.class);
        Bundle bundle = new Bundle();
        bundle.putString("apiKey", this.apiKey);
        bundle.putString("apiToken", this.apiToken);
        bundle.putString("userID", userID);
        bundle.putString("contentLanguage", contentLanguage);
        bundle.putString("phrase", phrase);
        intent.putExtras(bundle);
        activity.startActivity(intent);

        // Our handler for received Intents. This will be called whenever an Intent
        // with an action named "verification-event" is broad-casted.
        BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            boolean broadcastTriggerd = false;
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!broadcastTriggerd) {
                    broadcastTriggerd = true;
                    // Get extra data included in the Intent
                    String Response = intent.getStringExtra("Response");

                    if (intent.getAction().equals("enrollment-success")) {
                        responseHandler.sendSuccessMessage(200, null, Response.getBytes());
                    }
                    if (intent.getAction().equals("enrollment-failure")) {
                        responseHandler.sendFailureMessage(200, null, Response.getBytes(), new Throwable());
                    }
                }
            }
        };

        // Register observers (mMessageReceiver) to receive Intents with named actions
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("enrollment-success");
        intentFilter.addAction("enrollment-failure");
        LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver, intentFilter);
    }

    public void encapsulatedVideoVerification(Activity activity, String userID, String contentLanguage, String phrase, final JsonHttpResponseHandler responseHandler) {
        if (!userIdCheck(userID)) {
            JSONObject json = new JSONObject();
            try {
                json.put("message", "Incorrectly formatted argument");
            } catch(JSONException e) {
                System.out.println("JSON Exception : " + e.getMessage());
            }
            responseHandler.sendFailureMessage(200, null, json.toString().getBytes(), new Throwable());
            return;
        }

        Intent intent = new Intent(activity, VideoVerificationView.class);
        Bundle bundle = new Bundle();
        bundle.putString("apiKey", this.apiKey);
        bundle.putString("apiToken", this.apiToken);
        bundle.putString("userID", userID);
        bundle.putString("contentLanguage", contentLanguage);
        bundle.putString("phrase", phrase);
        intent.putExtras(bundle);
        activity.startActivity(intent);

        // Our handler for received Intents. This will be called whenever an Intent
        // with an action named "verification-event" is broad-casted.
        BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            boolean broadcastTriggerd = false;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (!broadcastTriggerd) {
                    broadcastTriggerd = true;
                    // Get extra data included in the Intent
                    String Response = intent.getStringExtra("Response");

                    if (intent.getAction().equals("verification-success")) {
                        responseHandler.sendSuccessMessage(200, null, Response.getBytes());
                    }
                    if (intent.getAction().equals("verification-failure")) {
                        responseHandler.sendFailureMessage(200, null, Response.getBytes(), new Throwable());
                    }
                }
            }
        };

        // Register observers (mMessageReceiver) to receive Intents with named actions
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("verification-success");
        intentFilter.addAction("verification-failure");
        LocalBroadcastManager.getInstance(activity).registerReceiver(mMessageReceiver, intentFilter);
    }

    private boolean userIdCheck(String arg) {
        String id = arg.substring(arg.lastIndexOf('_') + 1);
        if (!id.matches("[A-Za-z0-9]+")
                || !arg.substring(0, 3).equals("usr")
                || id.length() != 32) {
            System.out.println("UserId is invalid!");
            return false;
        }
        return true;
    }

    private boolean groupIdCheck(String arg) {
        String id = arg.substring(arg.lastIndexOf('_') + 1);
        if (!id.matches("[A-Za-z0-9]+")
                || !arg.substring(0, 3).equals("usr")
                || id.length() != 32) {
            System.out.println("GroupId is invalid!");
            return false;
        }
        return true;
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
