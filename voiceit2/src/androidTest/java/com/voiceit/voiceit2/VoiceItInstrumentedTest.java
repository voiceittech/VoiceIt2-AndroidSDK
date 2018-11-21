package com.voiceit.voiceit2;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VoiceItInstrumentedTest {

    final private String apiKey = InstrumentationRegistry.getArguments().getString("key");
    final private String apiTok = InstrumentationRegistry.getArguments().getString("tok");

    final private VoiceItAPI2 myVoiceIt = new VoiceItAPI2(apiKey, apiTok);

    final private String phrase = "Never forget tomorrow is a new day";

    private ArrayList<String> userIds = new ArrayList<>();
    private String groupId = "";
    private ArrayList<String> enrollmentIds = new ArrayList<>();
    private ArrayList<String> faceEnrollmentIds = new ArrayList<>();

    final private String dir = InstrumentationRegistry.getTargetContext().getFilesDir().getAbsolutePath();

    private CountDownLatch signal;

    private JsonHttpResponseHandler ResponseHandler(final String method, final int expectedStatusCode, final String expectedResponseCode, final CountDownLatch signal){

        return new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                System.out.println(method + " onSuccess Response:" + response.toString());
                assertEquals(expectedStatusCode, statusCode);
                try {
                    assertEquals(expectedResponseCode, response.getString("responseCode"));
                    if(response.has("userId")) {
                        userIds.add(response.getString("userId"));
                    }
                    if(response.has("groupId")) {
                        groupId = response.getString("groupId");
                    }
                    if(response.has("faceEnrollmentId")) {
                        faceEnrollmentIds.add(response.getString("faceEnrollmentId"));
                    }
                    if(response.has("id")) {
                        enrollmentIds.add(response.getString("id"));
                    }
                } catch (JSONException e) {
                    System.out.println("ResponseHandler JSONException" + e.getMessage());
                }
                signal.countDown();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                if (errorResponse != null) {
                    System.out.println(method + " onFailure Result : " + errorResponse.toString());
                    assertEquals(expectedStatusCode, statusCode);
                    signal.countDown();
                } else {
                    assertEquals(true, false);
                }
            }

            @Override
            public boolean getUseSynchronousMode() {
                return false;
            }

        };
    }

    private void before() {
        signal = new CountDownLatch(1);
    }

    private void after(String method) {
        try {
            if(!signal.await(30, TimeUnit.SECONDS)){
                assertEquals(method, "Timeout");
            }
        } catch (InterruptedException e) { e.printStackTrace(); }
    }

    private void downloadFile(String source, String file) {
        try {
            FileUtils.copyURLToFile( new URL(source), new File(dir, file));
        } catch (MalformedURLException e) {
            System.err.println("downloadFile MalformedURLException: " + e);
        } catch (IOException e) {
            System.err.println("downloadFile IOException: " + e);
        }
    }

    private void deleteFile(String path) {
        File file = new File(dir, path);
        if(file.delete()) {
            System.out.println("File " + path + " deleted successfully");
        } else {
            System.err.println("Failed to delete file at: " + path);
        }
    }

    @Test
    public void TestBasics () {

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.getAllUsers(ResponseHandler("getAllUsers",200, "SUCC", signal));
        after("getAllUsers");

        before();
        myVoiceIt.checkUserExists(userIds.get(0), ResponseHandler("checkUserExists",200, "SUCC", signal));
        after("checkUserExists");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup",201,
                "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.getGroupsForUser(userIds.get(0), ResponseHandler("getGroupsForUser",200,
                "SUCC", signal));
        after("getGroupsForUser");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200,
                "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.getGroupsForUser(userIds.get(0), ResponseHandler("getGroupsForUser",200,
                "SUCC", signal));
        after("getGroupsForUser");

        before();
        myVoiceIt.getGroup(groupId, ResponseHandler("getGroup",200, "SUCC", signal));
        after("getGroup");

        before();
        myVoiceIt.groupExists(groupId, ResponseHandler("groupExists",200, "SUCC", signal));
        after("groupExists");

        before();
        myVoiceIt.removeUserFromGroup(groupId, userIds.get(0), ResponseHandler("removeUserFromGroup",200,
                "SUCC", signal));
        after("removeUserFromGroup");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");
        userIds.clear();
    }

    @Test
    public void TestVideo() {

        // Download Video Enrollments
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan1.mov", "videoEnrollmentArmaan1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan2.mov", "videoEnrollmentArmaan2.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan3.mov", "videoEnrollmentArmaan3.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoVerificationArmaan1.mov", "videoVerificationArmaan1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov", "videoEnrollmentStephen1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov", "videoEnrollmentStephen2.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov", "videoEnrollmentStephen3.mov");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser", 201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser", 201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup", 201,
                "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.createVideoEnrollment(userIds.get(0), "en-US", phrase, dir+"/videoEnrollmentArmaan1.mov",
                ResponseHandler("createVideoEnrollment",201, "SUCC", signal));
        after("createVideoEnrollment");

        before();
        myVoiceIt.createVideoEnrollment(userIds.get(0), "en-US", phrase, dir+"/videoEnrollmentArmaan2.mov",
                ResponseHandler("createVideoEnrollment",201, "SUCC", signal));
        after("createVideoEnrollment");

        before();
        myVoiceIt.createVideoEnrollment(userIds.get(0), "en-US", phrase, dir+"/videoEnrollmentArmaan3.mov",
                ResponseHandler("createVideoEnrollment", 201, "SUCC", signal));
        after("createVideoEnrollment");

        before();
        myVoiceIt.getAllVideoEnrollments(userIds.get(0), ResponseHandler("getAllVideoEnrollments",200, "SUCC", signal));
        after("getAllVideoEnrollments");

        before();
        myVoiceIt.createVideoEnrollment(userIds.get(1), "en-US", phrase, dir+"/videoEnrollmentStephen1.mov",
                ResponseHandler("createVideoEnrollment",201, "SUCC", signal));
        after("createVideoEnrollment");

        before();
        myVoiceIt.createVideoEnrollment(userIds.get(1), "en-US", phrase, dir+"/videoEnrollmentStephen2.mov",
                ResponseHandler("createVideoEnrollment",201, "SUCC", signal));
        after("createVideoEnrollment");

        before();
        myVoiceIt.createVideoEnrollment(userIds.get(1), "en-US", phrase, dir+"/videoEnrollmentStephen3.mov",
                ResponseHandler("createVideoEnrollment",201, "SUCC", signal));
        after("createVideoEnrollment");

        before();
        myVoiceIt.videoVerification(userIds.get(0), "en-US", phrase, dir+"/videoVerificationArmaan1.mov",
                ResponseHandler("videoVerification",200, "SUCC", signal));
        after("videoVerification");

        before();
        myVoiceIt.videoIdentification(groupId, "en-US", phrase, dir+"/videoVerificationArmaan1.mov",
                ResponseHandler("videoIdentification",200, "SUCC", signal));
        after("videoIdentification");
        assertEquals(userIds.get(0), userIds.get(2));

        before();
        myVoiceIt.deleteVideoEnrollment(userIds.get(0), enrollmentIds.get(0), ResponseHandler("deleteVideoEnrollment",200, "SUCC", signal));
        after("deleteVideoEnrollment");

        before();
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(0), ResponseHandler("deleteAllVideoEnrollments",200, "SUCC", signal));
        after("deleteAllVideoEnrollments");

        before();
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(1), ResponseHandler("deleteAllVideoEnrollments",200, "SUCC", signal));
        after("deleteAllVideoEnrollments");

        // Cleanup
        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        userIds.clear();
        enrollmentIds.clear();

        deleteFile("videoEnrollmentArmaan1.mov");
        deleteFile("videoEnrollmentArmaan2.mov");
        deleteFile("videoEnrollmentArmaan3.mov");
        deleteFile("videoVerificationArmaan1.mov");
        deleteFile("videoEnrollmentStephen1.mov");
        deleteFile("videoEnrollmentStephen2.mov");
        deleteFile("videoEnrollmentStephen3.mov");

        // Video Enrollments By URL

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup",201, "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan1.mov",
                ResponseHandler("createVideoEnrollmentByUrl",201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before();
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan2.mov",
                ResponseHandler("createVideoEnrollmentByUrl",201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before();
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan3.mov",
                ResponseHandler("createVideoEnrollmentByUrl",201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before();
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov",
                ResponseHandler("createVideoEnrollmentByUrl",201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before();
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov",
                ResponseHandler("createVideoEnrollmentByUrl",201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before();
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov",
                ResponseHandler("createVideoEnrollmentByUrl",201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before();
        myVoiceIt.videoVerificationByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoVerificationArmaan1.mov",
                ResponseHandler("videoVerificationByUrl",200, "SUCC", signal));
        after("videoVerificationByUrl");

        before();
        myVoiceIt.videoIdentificationByUrl(groupId, "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoVerificationArmaan1.mov",
                ResponseHandler("videoIdentificationByUrl",200, "SUCC", signal));
        after("videoIdentificationByUrl");
        assertEquals(userIds.get(0), userIds.get(2));

        before();
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(0), ResponseHandler("deleteAllVideoEnrollments",200, "SUCC", signal));
        after("deleteAllVideoEnrollments");

        before();
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(1), ResponseHandler("deleteAllVideoEnrollments",200, "SUCC", signal));
        after("deleteAllVideoEnrollments");

        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        userIds.clear();
        enrollmentIds.clear();

    }

    @Test
    public void TestVoice() {

        // Download Voice Enrollments
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan1.wav", "enrollmentArmaan1.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan2.wav", "enrollmentArmaan2.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan3.wav", "enrollmentArmaan3.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/verificationArmaan1.wav", "verificationArmaan1.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen1.wav", "enrollmentStephen1.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen2.wav", "enrollmentStephen2.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen3.wav", "enrollmentStephen3.wav");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup",201,
                "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.createVoiceEnrollment(userIds.get(0), "en-US", phrase, dir+"/enrollmentArmaan1.wav",
                ResponseHandler("createVoiceEnrollment",201, "SUCC", signal));
        after("createVoiceEnrollment");

        before();
        myVoiceIt.createVoiceEnrollment(userIds.get(0), "en-US", phrase, dir+"/enrollmentArmaan2.wav",
                ResponseHandler("createVoiceEnrollment",201, "SUCC", signal));
        after("createVoiceEnrollment");

        before();
        myVoiceIt.createVoiceEnrollment(userIds.get(0), "en-US", phrase, dir+"/enrollmentArmaan3.wav",
                ResponseHandler("createVoiceEnrollment",201, "SUCC", signal));
        after("createVoiceEnrollment");

        before();
        myVoiceIt.getAllVoiceEnrollments(userIds.get(0),ResponseHandler("getAllVoiceEnrollments",200, "SUCC", signal));
        after("getAllVoiceEnrollments");

        before();
        myVoiceIt.createVoiceEnrollment(userIds.get(1), "en-US", phrase, new File(dir+"/enrollmentStephen1.wav"),
                ResponseHandler("createVoiceEnrollment",201, "SUCC", signal));
        after("createVoiceEnrollment");

        before();
        myVoiceIt.createVoiceEnrollment(userIds.get(1), "en-US", phrase, new File(dir+"/enrollmentStephen2.wav"),
                ResponseHandler("createVoiceEnrollment",201, "SUCC", signal));
        after("createVoiceEnrollment");

        before();
        myVoiceIt.createVoiceEnrollment(userIds.get(1), "en-US", phrase, new File(dir+"/enrollmentStephen3.wav"),
                ResponseHandler("createVoiceEnrollment",201, "SUCC", signal));
        after("createVoiceEnrollment");

        before();
        myVoiceIt.voiceVerification(userIds.get(0), "en-US", phrase, dir+"/verificationArmaan1.wav",
                ResponseHandler("voiceVerification",200, "SUCC", signal));
        after("voiceVerification");


        before();
        myVoiceIt.voiceIdentification(groupId, "en-US", phrase, dir+"/verificationArmaan1.wav",
                ResponseHandler("voiceIdentification",200, "SUCC", signal));
        after("voiceIdentification");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup

        before();
        myVoiceIt.deleteVoiceEnrollment(userIds.get(0), enrollmentIds.get(0), ResponseHandler("deleteVoiceEnrollment",200, "SUCC", signal));
        after("deleteVideoEnrollment");

        before();
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(0), ResponseHandler("deleteAllVoiceEnrollments",200, "SUCC", signal));
        after("deleteAllVoiceEnrollments");

        before();
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(1), ResponseHandler("deleteAllVoiceEnrollments",200, "SUCC", signal));
        after("deleteAllVoiceEnrollments");

        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        userIds.clear();
        enrollmentIds.clear();

        deleteFile("enrollmentArmaan1.wav");
        deleteFile("enrollmentArmaan2.wav");
        deleteFile("enrollmentArmaan3.wav");
        deleteFile("verificationArmaan1.wav");
        deleteFile("enrollmentStephen1.wav");
        deleteFile("enrollmentStephen2.wav");
        deleteFile("enrollmentStephen3.wav");

        // By URL

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup",201,
                "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan1.wav",
                ResponseHandler("createVoiceEnrollmentByUrl",201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before();
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan2.wav",
                ResponseHandler("createVoiceEnrollmentByUrl",201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before();
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan3.wav",
                ResponseHandler("createVoiceEnrollmentByUrl",201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before();
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen1.wav",
                ResponseHandler("createVoiceEnrollmentByUrl",201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before();
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen2.wav",
                ResponseHandler("createVoiceEnrollmentByUrl",201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before();
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen3.wav",
                ResponseHandler("createVoiceEnrollmentByUrl",201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before();
        myVoiceIt.voiceVerificationByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/verificationArmaan1.wav",
                ResponseHandler("voiceVerificationByUrl",200, "SUCC", signal));
        after("voiceVerificationByUrl");

        before();
        myVoiceIt.voiceIdentificationByUrl(groupId, "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/verificationArmaan1.wav",
                ResponseHandler("voiceIdentificationByUrl",200, "SUCC", signal));
        after("voiceIdentificationByUrl");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before();
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(0), ResponseHandler("deleteAllVoiceEnrollments",200, "SUCC", signal));
        after("deleteAllVoiceEnrollments");

        before();
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(1), ResponseHandler("deleteAllVoiceEnrollments",200, "SUCC", signal));
        after("deleteAllVoiceEnrollments");

        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        userIds.clear();
        enrollmentIds.clear();

    }

    @Test
    public void TestFace() {

        // Download Face Enrollments
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan1.mp4", "faceEnrollmentArmaan1.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan2.mp4", "faceEnrollmentArmaan2.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan3.mp4", "faceEnrollmentArmaan3.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceVerificationArmaan1.mp4", "faceVerificationArmaan1.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov", "faceEnrollmentStephen1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov", "faceEnrollmentStephen2.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov", "faceEnrollmentStephen3.mov");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup",201,
                "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.createFaceEnrollment(userIds.get(0), dir+"/faceEnrollmentArmaan1.mp4",
                ResponseHandler("createFaceEnrollment",201, "SUCC", signal));
        after("createFaceEnrollment");

        before();
        myVoiceIt.createFaceEnrollment(userIds.get(0), dir+"/faceEnrollmentArmaan2.mp4",
                ResponseHandler("createFaceEnrollment",201, "SUCC", signal));
        after("createFaceEnrollment");

        before();
        myVoiceIt.createFaceEnrollment(userIds.get(0), dir+"/faceEnrollmentArmaan3.mp4",
                ResponseHandler("createFaceEnrollment",201, "SUCC", signal));
        after("createFaceEnrollment");

        before();
        myVoiceIt.getAllFaceEnrollments(userIds.get(0),
                ResponseHandler("getAllFaceEnrollments",200, "SUCC", signal));
        after("getAllFaceEnrollments");

        before();
        myVoiceIt.createFaceEnrollment(userIds.get(1), dir+"/faceEnrollmentStephen1.mov",
                ResponseHandler("createFaceEnrollment",201, "SUCC", signal));
        after("createFaceEnrollment");

        before();
        myVoiceIt.createFaceEnrollment(userIds.get(1), dir+"/faceEnrollmentStephen2.mov",
                ResponseHandler("createFaceEnrollment",201, "SUCC", signal));
        after("createFaceEnrollment");

        before();
        myVoiceIt.createFaceEnrollment(userIds.get(1), dir+"/faceEnrollmentStephen3.mov",
                ResponseHandler("createFaceEnrollment",201, "SUCC", signal));
        after("createFaceEnrollment");

        before();
        myVoiceIt.faceVerification(userIds.get(0), dir+"/faceVerificationArmaan1.mp4",
                ResponseHandler("faceVerification",200, "SUCC", signal));
        after("faceVerification");

        before();
        myVoiceIt.faceIdentification(groupId, dir+"/faceVerificationArmaan1.mp4",
                ResponseHandler("faceIdentification",200, "SUCC", signal));
        after("faceIdentification");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before();
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(0), ResponseHandler("deleteFaceEnrollment",200, "SUCC", signal));
        after("deleteFaceEnrollment");

        before();
        myVoiceIt.deleteAllFaceEnrollments(userIds.get(0), ResponseHandler("deleteAllFaceEnrollments",200, "SUCC", signal));
        after("deleteAllFaceEnrollments");

        before();
        myVoiceIt.deleteAllFaceEnrollments(userIds.get(1), ResponseHandler("deleteAllFaceEnrollments",200, "SUCC", signal));
        after("deleteAllFaceEnrollments");

        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        userIds.clear();
        faceEnrollmentIds.clear();

        deleteFile("faceEnrollmentArmaan1.mp4");
        deleteFile("faceEnrollmentArmaan2.mp4");
        deleteFile("faceEnrollmentArmaan3.mp4");
        deleteFile("faceVerificationArmaan1.mp4");
        deleteFile("faceEnrollmentStephen1.mov");
        deleteFile("faceEnrollmentStephen2.mov");
        deleteFile("faceEnrollmentStephen3.mov");

        // By Url

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createUser(ResponseHandler("createUser",201, "SUCC", signal));
        after("createUser");

        before();
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler("createGroup",201,
                "SUCC", signal));
        after("createGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler("addUserToGroup",200, "SUCC", signal));
        after("addUserToGroup");

        before();
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan1.mp4",
                ResponseHandler("createFaceEnrollmentByUrl",201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before();
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan2.mp4",
                ResponseHandler("createFaceEnrollmentByUrl",201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before();
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan3.mp4",
                ResponseHandler("createFaceEnrollmentByUrl",201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before();
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(1), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov",
                ResponseHandler("createFaceEnrollmentByUrl",201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before();
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(1), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov",
                ResponseHandler("createFaceEnrollmentByUrl",201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before();
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(1), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov",
                ResponseHandler("createFaceEnrollmentByUrl",201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");


        before();
        myVoiceIt.faceVerificationByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceVerificationArmaan1.mp4",
                ResponseHandler("faceVerificationByUrl",200, "SUCC", signal));
        after("faceVerificationByUrl");

        before();
        myVoiceIt.faceIdentificationByUrl(groupId, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceVerificationArmaan1.mp4",
                ResponseHandler("faceIdentificationByUrl",200, "SUCC", signal));
        after("faceIdentificationByUrl");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before();
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(0), ResponseHandler("deleteFaceEnrollment",200, "SUCC", signal));
        after("deleteFaceEnrollment");

        before();
        myVoiceIt.deleteAllFaceEnrollments(userIds.get(1), ResponseHandler("deleteAllFaceEnrollments",200, "SUCC", signal));
        after("deleteAllFaceEnrollments");

        before();
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler("deleteUser",200, "SUCC", signal));
        after("deleteUser");

        before();
        myVoiceIt.deleteGroup(groupId, ResponseHandler("deleteGroup",200, "SUCC", signal));
        after("deleteGroup");

        userIds.clear();
        faceEnrollmentIds.clear();
    }
}
