package com.example.myapplication;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoActivity extends AppCompatActivity implements SurfaceHolder.Callback, Handler.Callback {


    private final static int CAMERA_PERMISSION_CODE = 0;
    private final static String CAMERA_ID = "0";
    private final static int MSG_SURFACE_CREATED = 0;
    private final static int MSG_CAMERA_OPENED = 1;


    private Handler mHandler = new Handler(this);
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Surface mCameraSurface;
    private boolean mIsCameraSurfaceCreated;

    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraStateCallBack;
    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback;
    private CameraCaptureSession mCameraCaptureSession;

    private Button mButton;
    private boolean mIsRecordingVideo;

    private MediaRecorder mMediaRecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mSurfaceView = findViewById(R.id.surface_view_video);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mButton = findViewById(R.id.button4);
        mButton.setOnClickListener(new View.OnClickListener() { // step 1: define and display recording button and manage UI
            @Override
            public void onClick(View v) {
                if (!mIsRecordingVideo) {
                    startVideoRecording();
                } else {
                    stopVideoRecording();
                }
            }
        });
    }

    private void startVideoRecording() {
        Log.d("*****************************","handleVideoRecording");

        if (mCameraDevice != null) {

            closeCameraSession();

            List<Surface> surfaceList = new ArrayList<Surface>();
            try {
                setupMediaRecorder();
            } catch (IOException e) {
                e.printStackTrace();
            }

            final CaptureRequest.Builder recordingBuilder;
            try {
                recordingBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

                surfaceList.add(mCameraSurface);
                recordingBuilder.addTarget(mCameraSurface);

                surfaceList.add(mMediaRecorder.getSurface());
                recordingBuilder.addTarget(mMediaRecorder.getSurface());

                Log.d("*****************************","surfaces added");
                mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        Log.d("*****************************","recording configured");
                        mCameraCaptureSession = session;

                        try {
                            mCameraCaptureSession.setRepeatingRequest(recordingBuilder.build(), null, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("*****************************","user entered recording UI");
                                mMediaRecorder.start();
                                mButton.setText("Recording...");
                                mIsRecordingVideo = true;
                            }
                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.d("*****************************","Recording onConfigureFailed");
                    }
                }, mHandler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopVideoRecording() {
        Log.d("*****************************","stopVideoRecording");
        closeCameraSession();
        configureCamera();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mButton.setText("Record Video");
                mIsRecordingVideo = false;
            }
        });
    }



    private void setupMediaRecorder() throws IOException {


        Log.d("*****************************","setupMediaRecorder");
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        mMediaRecorder.setOutputFile(getOutputFile().getAbsolutePath());


        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);


        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);


        mMediaRecorder.setOrientationHint(90);

        mMediaRecorder.prepare();
    }

    private File getOutputFile() {
        File dir = new File(Environment.getExternalStorageDirectory().toString(), "MyVideos");
        if (!dir.exists()) {
            dir.mkdir();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File imageFile = new File (dir.getPath() + File.separator + "VID_"+timeStamp+".mp4");

        Log.d("*********************************","imagefilename="+imageFile.getAbsolutePath());

        return imageFile;
    }


    @Override
    public void surfaceCreated(SurfaceHolder holdconfigureCameraer) {
        Log.d("******************","1 surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("************************","2 surfaceChanged");
        mCameraSurface = holder.getSurface();
        mHandler.sendEmptyMessage(MSG_SURFACE_CREATED);
        mIsCameraSurfaceCreated = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("************************","surfaceDestroyed");
        mIsCameraSurfaceCreated = false;
    }


    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, CAMERA_PERMISSION_CODE);
    }

    @SuppressLint("MissingPermission")
    private void handleCamera() {

        Log.d ("****************************","3 handle camera");

        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraIds[] = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIds) {
                Log.e ("******************************","cameraId="+cameraId);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }



        mCameraStateCallBack = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d("********************************","4 onOpened -"+camera.getId());
                mCameraDevice = camera;
                mHandler.sendEmptyMessage(MSG_CAMERA_OPENED);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.d("********************************","onDisconnected -"+camera.getId());

            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.d("********************************","onDisconnected -"+camera.getId());
            }
        };

        try {
            mCameraManager.openCamera(CAMERA_ID, mCameraStateCallBack, new Handler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        android.util.Log.e ("****************************","msg.what="+msg.what);
        android.util.Log.e ("****************************","mIsCameraSurfaceCreated="+mIsCameraSurfaceCreated);
        android.util.Log.e ("****************************","mCameraDevice="+mCameraDevice);
        switch (msg.what) {
            case MSG_SURFACE_CREATED:
            case MSG_CAMERA_OPENED:
                if (mIsCameraSurfaceCreated && (mCameraDevice != null)) {
                    mIsCameraSurfaceCreated = false;
                    configureCamera();
                }
        }
        return true;
    }

    private void configureCamera() {
        Log.d ("****************************","4 configureCamera");

        List<Surface> surfaceList = new ArrayList<Surface>();
        surfaceList.add(mCameraSurface); // surface to be viewed

        mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.d ("***************************","onConfigured");
                mCameraCaptureSession = session;

                try {
                    CaptureRequest.Builder previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(mCameraSurface);

                    mCameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
                    Log.d ("****************************","5 setRepeatingRequest");
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.d ("***************************","onConfigureFailed");
            }
        };

        try {
            mCameraDevice.createCaptureSession(surfaceList, mCameraCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleCamera();
            }
        }

    }

    private void closeCameraSession() {
        Log.d("*****************************","closeCameraSession");
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            mCameraCaptureSession.close();
            mCameraCaptureSession = null;

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeCameraSession();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mCameraDevice.close();
        mCameraDevice = null;
    }
}
