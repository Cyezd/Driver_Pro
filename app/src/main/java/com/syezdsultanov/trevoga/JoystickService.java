package com.syezdsultanov.trevoga;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by syezdsultanov on 11/21/17.
 */

public class JoystickService extends Service {

    private WindowManager mWindowManager;
    private ImageView mJoystickImageView;
    private long lastPressTime, pressTime;
    private String sms, phoneNumber;

    private static boolean isAvailable(Context ctx, Intent intent) {

        final PackageManager mgr = ctx.getPackageManager();

        List<ResolveInfo> list =
                mgr.queryIntentActivities(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);

        return list.size() > 0;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mJoystickImageView = new ImageView(this);
        mJoystickImageView.setImageResource(R.drawable.logo_sos_start);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        mWindowManager.addView(mJoystickImageView, params);

        SharedPreferences pref = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        phoneNumber = pref.getString("number", "");
        sms = pref.getString("text", "");
        if (sms.equals("")) {
            sms = "Driver Pro";
        }

        mJoystickImageView.setOnTouchListener(new View.OnTouchListener() {
            private final WindowManager.LayoutParams paramsF = params;
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        pressTime = System.currentTimeMillis();

                        if (pressTime - lastPressTime <= 300) {
                            JoystickService.this.stopSelf();
                        }
                        lastPressTime = pressTime;
                        initialX = paramsF.x;
                        initialY = paramsF.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        break;


                    case MotionEvent.ACTION_UP:
                        if ((System.currentTimeMillis() - pressTime) > 1400) {

                            if (isAvailable(getApplicationContext(), new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION))) {
                                Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } else if (isAvailable(getApplicationContext(), new Intent(MediaStore.ACTION_VIDEO_CAPTURE))) {
                                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                            sendMessage();
                            mJoystickImageView.setImageResource(R.drawable.logo_sos);
                        }
                        break;


                    case MotionEvent.ACTION_MOVE:

                        paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                        paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mJoystickImageView, paramsF);
                        break;


                }
                return false;

            }

        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    private void sendMessage() {
        StringBuilder smsBody = new StringBuilder(sms);
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.
                    checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            smsBody.append("  ");
            smsBody.append("http://maps.google.com?q=");
            smsBody.append(latitude);
            smsBody.append(",");
            smsBody.append(longitude);
        } catch (Exception e) {
            Toast.makeText(this, "Location not detected.", Toast.LENGTH_SHORT).show();
        }
        Log.d("TAG", "" + smsBody);
        try {
            String numbers[] = phoneNumber.split(",");

            for (int x = 0; x < numbers.length; x++) {
                SmsManager.getDefault().sendTextMessage(numbers[x].trim(), null,
                        smsBody.toString(), null, null);
                Log.d("TAG", "" + numbers[x]);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Message sent to invalid destination.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mJoystickImageView != null) {
            mWindowManager.removeView(mJoystickImageView);
            mJoystickImageView = null;
        }
    }
}

