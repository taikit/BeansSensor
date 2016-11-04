package com.example.taiki.beanssensor;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements SensorEventListener {

    private final static int FILE_EXPORT_TIME = 1000 * 60 * 60 * 60;
    private final static int SENS_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private final static int SENS_HEARTRATE = Sensor.TYPE_HEART_RATE;

    SensorManager mSensorManager;

    private Sensor mHeartrateSensor;
    private ScheduledExecutorService mScheduler;
    public StringBuilder accelerometer_buf = new StringBuilder();
    public StringBuilder heart_rate_buf = new StringBuilder();
    public Date lastExportDate;


    @Override
    public void onCreate() {
        super.onCreate();

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Sensor Dashboard");
        builder.setContentText("Collecting sensor data..");
        startForeground(1, builder.build());

        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        exportData();
        stopMeasurement();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(SENS_ACCELEROMETER);
        mHeartrateSensor = mSensorManager.getDefaultSensor(SENS_HEARTRATE);

        // Register the listener
        if (mSensorManager != null) {
            if (accelerometerSensor != null) {
                mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w("BeansSensor", "No Accelerometer found");
            }

            if (mHeartrateSensor != null) {
                final int measurementDuration = 10;   // Seconds
                final int measurementBreak = 5;    // Seconds

                mScheduler = Executors.newScheduledThreadPool(1);
                mScheduler.scheduleAtFixedRate(
                        new Runnable() {
                            @Override
                            public void run() {
                                Log.d("BeansSensor", "register Heartrate Sensor");
                                mSensorManager.registerListener(SensorService.this, mHeartrateSensor, SensorManager.SENSOR_DELAY_NORMAL);

                                try {
                                    Thread.sleep(measurementDuration * 1000);
                                } catch (InterruptedException e) {
                                    Log.e("BeansSensor", "Interrupted while waitting to unregister Heartrate Sensor");
                                }

                                Log.d("BeansSensor", "unregister Heartrate Sensor");
                                mSensorManager.unregisterListener(SensorService.this, mHeartrateSensor);
                            }
                        }, 3, measurementDuration + measurementBreak, TimeUnit.SECONDS);

            } else {
                Log.e("BeansSensor", "No Heartrate Sensor found");
            }
        }
    }

    private void stopMeasurement() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (mScheduler != null && !mScheduler.isTerminated()) {
            mScheduler.shutdown();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Date now = new Date();
        String date = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(now);
        String sensorType = event.sensor.getStringType();
        if (lastExportDate == null) {
            lastExportDate = now;
        }
        if (sensorType == "android.sensor.accelerometer") {
            accelerometer_buf.append(date);
            accelerometer_buf.append(",");
            for (float val : event.values) {
                accelerometer_buf.append(val);
                accelerometer_buf.append(",");
            }
            accelerometer_buf.append(System.getProperty("line.separator"));
        } else {
            heart_rate_buf.append(date);
            heart_rate_buf.append(",");
            heart_rate_buf.append(event.values[0]);
            heart_rate_buf.append(System.getProperty("line.separator"));
        }

        // エキスポート
        if (passed(lastExportDate)) {
            exportData();
        }
    }

    private void exportData(){
        Date now = new Date();
        String date = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(now);
        String filename = String.format("%s_%s.csv", "accelerometer", date);
        exportFile(filename, accelerometer_buf.toString());
        accelerometer_buf = new StringBuilder();

        filename = String.format("%s_%s.csv", "heart_rate", date);
        exportFile(filename, heart_rate_buf.toString());
        heart_rate_buf = new StringBuilder();
        lastExportDate = now;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void exportFile(String filename, String content) {
        final String directory = String.valueOf(Environment.getExternalStorageDirectory()) + "/SensorData";
        final File logfile = new File(directory, filename);
        final File logPath = logfile.getParentFile();
        if (!logPath.isDirectory() && !logPath.mkdirs()) {
            Log.e("BeansSensor", "Could not create directory for log files");
        }
        try {
            FileWriter filewriter = new FileWriter(logfile);
            BufferedWriter bw = new BufferedWriter(filewriter);
            bw.write(content);
            bw.flush();
            bw.close();
            Log.i("BeansSensor", "Export finished!");
        } catch (IOException ioe) {
            Log.e("BeansSensor", ioe.toString());
            Log.e("BeansSensor", "IOException while writing Logfile");
        }
    }

    private boolean passed(Date date) {
        Date now = new Date();
        long nowTime = now.getTime();
        long dateTime = date.getTime();
        long diff = nowTime - dateTime;
        return diff > FILE_EXPORT_TIME;
    }
}
