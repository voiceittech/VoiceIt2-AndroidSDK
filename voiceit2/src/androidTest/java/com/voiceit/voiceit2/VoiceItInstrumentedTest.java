package com.voiceit.voiceit2;

import android.content.Context;
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

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class VoiceItInstrumentedTest {

    Context appContext = InstrumentationRegistry.getTargetContext();

    final private String apiKey = "key_f3a9fb29944a4e4180d4c98e7f03c713";
    final private String apiTok = "tok_be57cbfb92ae4e139f94843468095502";
    final private VoiceItAPI2 myVoiceIt = new VoiceItAPI2(apiKey, apiTok);

    private String phrase = "Never forget tomorrow is a new day";

    private ArrayList<String> userIds = new ArrayList<>();
    private String groupId = "";
    private ArrayList<String> enrollmentIds = new ArrayList<>();
    private ArrayList<String> faceEnrollmentIds = new ArrayList<>();

    private String dir = appContext.getExternalFilesDir(null).getAbsolutePath();

    private CountDownLatch signal;

    JsonHttpResponseHandler ResponseHandler(final int expectedStatusCode, final String expectedResponseCode, final CountDownLatch signal){

        return new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                System.out.println("onSuccess Response:" + response.toString());
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
                    System.out.println("onFailure Result : " + errorResponse.toString());
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

    private void before(int count) {
        signal = new CountDownLatch(count);
    }

    private void after(String method) {
        try {
            if(!signal.await(60, TimeUnit.SECONDS)){
                assertEquals(method, false);
            }
        }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    void downloadFile(String source, String file) {
        try {
            FileUtils.copyURLToFile( new URL(source), new File(dir, file));
        } catch (MalformedURLException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    void deleteFile(String path) {
        File file = new File(dir, path);
        if(file.delete()) {
            System.out.println("File " + path + " deleted successfully");
        } else {
            System.err.println("Failed to delete file at: " + path);
        }
    }

    @Test
    public void TestBasics () {

        before(1);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        after("createUser");

        before(2);
        myVoiceIt.getAllUsers(ResponseHandler(200, "SUCC", signal));

        myVoiceIt.checkUserExists(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        after("checkUserExists");

        before(1);
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201,
                "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.getGroupsForUser(userIds.get(0), ResponseHandler(200,
                "SUCC", signal));

        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200,
                "SUCC", signal));
        after("addUserToGroup");

        before(3);
        myVoiceIt.getGroupsForUser(userIds.get(0), ResponseHandler(200,
                "SUCC", signal));

        myVoiceIt.getGroup(groupId, ResponseHandler(200, "SUCC", signal));

        myVoiceIt.groupExists(groupId, ResponseHandler(200, "SUCC", signal));
        after("groupExists");

        before(1);
        myVoiceIt.removeUserFromGroup(groupId, userIds.get(0), ResponseHandler(200,
                "SUCC", signal));
        after("removeUserFromGroup");

        before(2);
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));

        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        after("deleteUser");
        userIds.clear();
    }


    @Test
    public void TestVideo() {

        before(3);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201,
                "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("addUserToGroup");

        // Create Video Enrollments

        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan1.mov", "videoEnrollmentArmaan1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan2.mov", "videoEnrollmentArmaan2.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan3.mov", "videoEnrollmentArmaan3.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoVerificationArmaan1.mov", "videoVerificationArmaan1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov", "videoEnrollmentStephen1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov", "videoEnrollmentStephen2.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov", "videoEnrollmentStephen3.mov");

        before(1);
        myVoiceIt.createVideoEnrollment(userIds.get(0), "en-US", phrase, dir+"/videoEnrollmentArmaan1.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollment");

        before(1);
        myVoiceIt.createVideoEnrollment(userIds.get(0), "en-US", phrase, dir+"/videoEnrollmentArmaan2.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollment");

        before(1);
        myVoiceIt.createVideoEnrollment(userIds.get(0), "en-US", phrase, dir+"/videoEnrollmentArmaan3.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollment");

        before(1);
        myVoiceIt.getAllVideoEnrollments(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        after("getAllVideoEnrollments");

        before(1);
        myVoiceIt.createVideoEnrollment(userIds.get(1), "en-US", phrase, dir+"/videoEnrollmentStephen1.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollment");

        before(1);
        myVoiceIt.createVideoEnrollment(userIds.get(1), "en-US", phrase, dir+"/videoEnrollmentStephen2.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollment");

        before(1);
        myVoiceIt.createVideoEnrollment(userIds.get(1), "en-US", phrase, dir+"/videoEnrollmentStephen3.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollment");

        before(1);
        myVoiceIt.videoVerification(userIds.get(0), "en-US", phrase, dir+"/videoVerificationArmaan1.mov",
                ResponseHandler(200, "SUCC", signal));
        after("videoVerification");

        before(1);
        myVoiceIt.videoIdentification(groupId, "en-US", phrase, dir+"/videoVerificationArmaan1.mov",
                ResponseHandler(200, "SUCC", signal));
        after("videoIdentification");
        assertEquals(userIds.get(0), userIds.get(2));

        before(4);
        myVoiceIt.deleteVideoEnrollment(userIds.get(0), enrollmentIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteVideoEnrollment(userIds.get(0), enrollmentIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteVideoEnrollment(userIds.get(0), enrollmentIds.get(2), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("deleteAllVideoEnrollments");

        // Cleanup
        before(3);
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));
        after("deleteGroup");
        userIds.clear();
        enrollmentIds.clear();

        // Video Enrollments By URL

        before(3);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201, "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("addUserToGroup");

        before(1);
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan1.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before(1);
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan2.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before(1);
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentArmaan3.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before(1);
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before(1);
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before(1);
        myVoiceIt.createVideoEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createVideoEnrollmentByUrl");

        before(1);
        myVoiceIt.videoVerificationByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoVerificationArmaan1.mov",
                ResponseHandler(200, "SUCC", signal));
        after("videoVerificationByUrl");

        before(1);
        myVoiceIt.videoIdentificationByUrl(groupId, "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoVerificationArmaan1.mov",
                ResponseHandler(200, "SUCC", signal));
        after("videoIdentificationByUrl");
        assertEquals(userIds.get(0), userIds.get(2));

        before(2);
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteAllVideoEnrollments(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("deleteAllVideoEnrollments");

        before(3);
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));
        after("deleteGroup");
        userIds.clear();
        enrollmentIds.clear();

        deleteFile(dir+"/videoEnrollmentArmaan1.mov");
        deleteFile(dir+"/videoEnrollmentArmaan2.mov");
        deleteFile(dir+"/videoEnrollmentArmaan3.mov");
        deleteFile(dir+"/videoVerificationArmaan1.mov");
        deleteFile(dir+"/videoEnrollmentStephen1.mov");
        deleteFile(dir+"/videoEnrollmentStephen2.mov");
        deleteFile(dir+"/videoEnrollmentStephen3.mov");

    }

    @Test
    public void TestVoice() {

        before(3);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201,
                "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("addUserToGroup");

        // Create Voice Enrollments
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan1.wav", "enrollmentArmaan1.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan2.wav", "enrollmentArmaan2.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan3.wav", "enrollmentArmaan3.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/verificationArmaan1.wav", "verificationArmaan1.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen1.wav", "enrollmentStephen1.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen2.wav", "enrollmentStephen2.wav");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen3.wav", "enrollmentStephen3.wav");

        before(1);
        myVoiceIt.createVoiceEnrollment(userIds.get(0), "en-US", phrase, dir+"/enrollmentArmaan1.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollment");

        before(1);
        myVoiceIt.createVoiceEnrollment(userIds.get(0), "en-US", phrase, dir+"/enrollmentArmaan2.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollment");

        before(1);
        myVoiceIt.createVoiceEnrollment(userIds.get(0), "en-US", phrase, dir+"/enrollmentArmaan3.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollment");

        before(1);
        myVoiceIt.getAllVoiceEnrollments(userIds.get(0),ResponseHandler(200, "SUCC", signal));
        after("getAllVoiceEnrollments");

        before(1);
        myVoiceIt.createVoiceEnrollment(userIds.get(1), "en-US", phrase, new File(dir+"/enrollmentStephen1.wav"),
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollment");

        before(1);
        myVoiceIt.createVoiceEnrollment(userIds.get(1), "en-US", phrase, new File(dir+"/enrollmentStephen2.wav"),
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollment");

        before(1);
        myVoiceIt.createVoiceEnrollment(userIds.get(1), "en-US", phrase, new File(dir+"/enrollmentStephen3.wav"),
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollment");

        before(1);
        myVoiceIt.voiceVerification(userIds.get(0), "en-US", phrase, dir+"/verificationArmaan1.wav",
                ResponseHandler(200, "SUCC", signal));
        after("voiceVerification");


        before(1);
        myVoiceIt.voiceIdentification(groupId, "en-US", phrase, dir+"/verificationArmaan1.wav",
                ResponseHandler(200, "SUCC", signal));
        after("voiceIdentification");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before(2);
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("deleteAllVoiceEnrollments");

        before(3);
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));
        after("deleteGroup");
        userIds.clear();
        enrollmentIds.clear();

        // By URL

        before(3);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201,
                "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("addUserToGroup");

        before(1);
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan1.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before(1);
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan2.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before(1);
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentArmaan3.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before(1);
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen1.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before(1);
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen2.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before(1);
        myVoiceIt.createVoiceEnrollmentByUrl(userIds.get(1), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/enrollmentStephen3.wav",
                ResponseHandler(201, "SUCC", signal));
        after("createVoiceEnrollmentByUrl");

        before(1);
        myVoiceIt.voiceVerificationByUrl(userIds.get(0), "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/verificationArmaan1.wav",
                ResponseHandler(200, "SUCC", signal));
        after("voiceVerificationByUrl");

        before(1);
        myVoiceIt.voiceIdentificationByUrl(groupId, "en-US", phrase, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/verificationArmaan1.wav",
                ResponseHandler(200, "SUCC", signal));
        after("voiceIdentificationByUrl");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before(2);
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteAllVoiceEnrollments(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("deleteAllVoiceEnrollments");

        before(3);
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));
        after("deleteGroup");
        userIds.clear();
        enrollmentIds.clear();

        deleteFile(dir+"/enrollmentArmaan1.wav");
        deleteFile(dir+"/enrollmentArmaan2.wav");
        deleteFile(dir+"/enrollmentArmaan3.wav");
        deleteFile(dir+"/verificationArmaan1.wav");
        deleteFile(dir+"/enrollmentStephen1.wav");
        deleteFile(dir+"/enrollmentStephen2.wav");
        deleteFile(dir+"/enrollmentStephen3.wav");
    }

    @Test
    public void TestFace() {

        before(3);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201,
                "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("addUserToGroup");

        // Create Face Enrollments
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan1.mp4", "faceEnrollmentArmaan1.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan2.mp4", "faceEnrollmentArmaan2.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan3.mp4", "faceEnrollmentArmaan3.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceVerificationArmaan1.mp4", "faceVerificationArmaan1.mp4");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov", "faceEnrollmentStephen1.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov", "faceEnrollmentStephen2.mov");
        downloadFile("https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov", "faceEnrollmentStephen3.mov");

        before(1);
        myVoiceIt.createFaceEnrollment(userIds.get(0), dir+"/faceEnrollmentArmaan1.mp4",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollment");

        before(1);
        myVoiceIt.createFaceEnrollment(userIds.get(0), dir+"/faceEnrollmentArmaan2.mp4",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollment");

        before(1);
        myVoiceIt.createFaceEnrollment(userIds.get(0), dir+"/faceEnrollmentArmaan3.mp4",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollment");

        before(1);
        myVoiceIt.getAllFaceEnrollments(userIds.get(0),
                ResponseHandler(200, "SUCC", signal));
        after("getAllFaceEnrollments");

        before(1);
        myVoiceIt.createFaceEnrollment(userIds.get(1), dir+"/faceEnrollmentStephen1.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollment");

        before(1);
        myVoiceIt.createFaceEnrollment(userIds.get(1), dir+"/faceEnrollmentStephen2.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollment");

        before(1);
        myVoiceIt.createFaceEnrollment(userIds.get(1), dir+"/faceEnrollmentStephen3.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollment");

        before(1);
        myVoiceIt.faceVerification(userIds.get(0), dir+"/faceVerificationArmaan1.mp4",
                ResponseHandler(200, "SUCC", signal));
        after("faceVerification");

        before(1);
        myVoiceIt.faceIdentification(groupId, dir+"/faceVerificationArmaan1.mp4",
                ResponseHandler(200, "SUCC", signal));
        after("faceIdentification");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before(4);
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(2), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteAllFaceEnrollments(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("deleteAllFaceEnrollments");

        before(3);
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));
        after("deleteGroup");
        userIds.clear();
        faceEnrollmentIds.clear();

        // By Url

        before(3);
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createUser(ResponseHandler(201, "SUCC", signal));
        myVoiceIt.createGroup("Sample Group Description", ResponseHandler(201,
                "SUCC", signal));
        after("createGroup");

        before(2);
        myVoiceIt.addUserToGroup(groupId, userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.addUserToGroup(groupId, userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("addUserToGroup");

        before(1);
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan1.mp4",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before(1);
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan2.mp4",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before(1);
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceEnrollmentArmaan3.mp4",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before(1);
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(1), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen1.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before(1);
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(1), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen2.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");

        before(1);
        myVoiceIt.createFaceEnrollmentByUrl(userIds.get(1), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/videoEnrollmentStephen3.mov",
                ResponseHandler(201, "SUCC", signal));
        after("createFaceEnrollmentByUrl");


        before(1);
        myVoiceIt.faceVerificationByUrl(userIds.get(0), "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceVerificationArmaan1.mp4",
                ResponseHandler(200, "SUCC", signal));
        after("faceVerificationByUrl");

        before(1);
        myVoiceIt.faceIdentificationByUrl(groupId, "https://s3.amazonaws.com/voiceit-api2-testing-files/test-data/faceVerificationArmaan1.mp4",
                ResponseHandler(200, "SUCC", signal));
        after("faceIdentificationByUrl");
        assertEquals(userIds.get(0), userIds.get(2));

        // Cleanup
        before(4);
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteFaceEnrollment(userIds.get(0), faceEnrollmentIds.get(2), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteAllFaceEnrollments(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        after("deleteAllFaceEnrollments");

        before(3);
        myVoiceIt.deleteUser(userIds.get(0), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteUser(userIds.get(1), ResponseHandler(200, "SUCC", signal));
        myVoiceIt.deleteGroup(groupId, ResponseHandler(200, "SUCC", signal));
        after("deleteGroup");
        userIds.clear();
        faceEnrollmentIds.clear();

        deleteFile("faceEnrollmentArmaan1.mp4");
        deleteFile("faceEnrollmentArmaan2.mp4");
        deleteFile("faceEnrollmentArmaan3.mp4");
        deleteFile("faceVerificationArmaan1.mp4");
        deleteFile("faceEnrollmentStephen1.mp4");
        deleteFile("faceEnrollmentStephen2.mp4");
        deleteFile("faceEnrollmentStephen3.mp4");

    }
}
