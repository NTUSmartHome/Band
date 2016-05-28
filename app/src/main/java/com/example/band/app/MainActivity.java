package com.example.band.app;

import java.lang.ref.WeakReference;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.*;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.os.AsyncTask;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MainActivity extends Activity {

    private BandClient client = null;
    private Button btnStart;
    private TextView txtStatus;

    private float yaw, roll, pitch;
    private float aX, aY, aZ, gX, gY, gZ;
    private float skinTemp;
    private BandContactState conT;
    final private float deg_to_rad = (float) Math.PI/180;
    final private float rad_to_deg = 180/(float) Math.PI;

    private String gyroscope_string;
    private String skinTemp_string;
    private String heart_string;
    private String rrInterval_string;
    private String contact_string;
    private String[] sensorDataStream;
//    private String send_data_string;
    private String send_socket_string = "";

    static private int send_cnt = 0;
    private final double DURATION_SEND = 1000;
    private long previousTime;

    private String address = "140.112.90.184";// 連線的ip
    private int port = 54321;// 連線的port

    private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
                aX = event.getAccelerationX();
                aY = event.getAccelerationY();
                aZ = event.getAccelerationZ();
                sensorDataStream[0] += aX*10 + ",";
                sensorDataStream[1] += aY*10 + ",";
                sensorDataStream[2] += aZ*10 + ",";
                Quaternions(aX, aY, aZ, gX*deg_to_rad, gY*deg_to_rad, gZ*deg_to_rad);
                sensorDataStream[6] += yaw + ",";
                sensorDataStream[7] += roll + ",";
                sensorDataStream[8] += pitch + ",";
                if(heart_string == null) {
                    heart_string = "\n\nYou have to click the 'START' again and press YES to get heart rate.";
                    rrInterval_string = "\n";
                }
                appendToUI(String.format("Ax = %.3f    Ay = %.3f    Az = %.3f", aX*10,aY*10, aZ*10)
                        + gyroscope_string + String.format("\nY = %.3f    R = %.3f    P = %.3f", yaw, roll, pitch)
                        + skinTemp_string + heart_string + rrInterval_string + contact_string);

                if(System.currentTimeMillis() - previousTime > DURATION_SEND){
                    previousTime = System.currentTimeMillis();

                    sensorDataStream[9] += skinTemp + ",";
                    sensorDataStream[13] += conT + ",";

                    for(int i = 0; i < sensorDataStream.length; i++) {
                        send_socket_string += sensorDataStream[i].substring(0, sensorDataStream[i].length() - 1) + ";";
                    }
                    socketClient(send_socket_string);

                    sensorDataStream[0] = "Ax:";
                    sensorDataStream[1] = "\r\nAy:";
                    sensorDataStream[2] = "\r\nAz:";
                    sensorDataStream[3] = "\r\nGx:";
                    sensorDataStream[4] = "\r\nGy:";
                    sensorDataStream[5] = "\r\nGz:";
                    sensorDataStream[6] = "\r\nY:";
                    sensorDataStream[7] = "\r\nR:";
                    sensorDataStream[8] = "\r\nP:";
                    sensorDataStream[9] = "\r\nST:";
                    sensorDataStream[10] = "\r\nHR:";
                    sensorDataStream[11] = "\r\nHRQ:";
                    sensorDataStream[12] = "\r\nRRi:";
                    sensorDataStream[13] = "\r\nCnt:";
                    send_socket_string = "";
                }
//                appendToUI(send_data_string);

//                send_socket_string += send_data_string;
//                send_cnt += 1;
//                if(send_cnt >= 13){
//                    socketClient(send_socket_string);
//                    send_socket_string = "";
//                    send_cnt = 0;
//                }
            }
        }
    };

    private BandGyroscopeEventListener mGyroscopeEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(final BandGyroscopeEvent event) {
            if (event != null) {
                gX = event.getAngularVelocityX();
                gY = event.getAngularVelocityY();
                gZ = event.getAngularVelocityZ();
                sensorDataStream[3] += gX + ",";
                sensorDataStream[4] += gY + ",";
                sensorDataStream[5] += gZ + ",";
                gyroscope_string = String.format("\nGx = %.3f    Gy = %.3f    Gz = %.3f", gX,
                        gY, gZ);
            }
        }
    };

    private BandSkinTemperatureEventListener mSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(final BandSkinTemperatureEvent event) {
            if (event != null) {
                skinTemp = event.getTemperature();
                skinTemp_string = String.format("\nSkinTemperature = %.2f degrees Celsius", skinTemp);
            }
        }
    };

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                sensorDataStream[10] += event.getHeartRate() + ",";
                sensorDataStream[11] += event.getQuality() + ",";
                heart_string = String.format("\nHeartRate = %d beats per minute"
                        + "\nQuality = %s", event.getHeartRate(), event.getQuality());
            }
        }
    };

    private BandRRIntervalEventListener mRRIntervalEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(final BandRRIntervalEvent event) {
            if (event != null) {
                sensorDataStream[12] += event.getInterval() + ",";
                rrInterval_string = String.format("\nRRinterval = %.3f s", event.getInterval());
            }
        }
    };

    private BandContactEventListener mContactEventListener = new BandContactEventListener() {
        @Override
        public void onBandContactChanged(final BandContactEvent event) {
            if (event != null) {
                conT = event.getContactState();
                contact_string = String.format("\nContact = %s", conT);
            }
        }
    };

    private HeartRateConsentListener heartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean consentGiven) {
            if (consentGiven == true) {
                Log.d("connecting", "UserConsent is GRANTED");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorDataStream = new String[14];
        sensorDataStream[0] = "Ax:";
        sensorDataStream[1] = "\r\nAy:";
        sensorDataStream[2] = "\r\nAz:";
        sensorDataStream[3] = "\r\nGx:";
        sensorDataStream[4] = "\r\nGy:";
        sensorDataStream[5] = "\r\nGz:";
        sensorDataStream[6] = "\r\nY:";
        sensorDataStream[7] = "\r\nR:";
        sensorDataStream[8] = "\r\nP:";
        sensorDataStream[9] = "\r\nST:";
        sensorDataStream[10] = "\r\nHR:";
        sensorDataStream[11] = "\r\nHRQ:";
        sensorDataStream[12] = "\r\nRRi:";
        sensorDataStream[13] = "\r\nCnt:";

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        previousTime =  System.currentTimeMillis();

//        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        txtStatus = (TextView) findViewById(R.id.txtStatus);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("");
//                new HeartRateConsentTask().execute(reference);
                new AccelerometerSubscriptionTask().execute();
                //new HeartRateSubscriptionTask().execute();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        //txtStatus.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterAccelerometerEventListener(mAccelerometerEventListener);
                client.getSensorManager().unregisterGyroscopeEventListener(mGyroscopeEventListener);
                client.getSensorManager().unregisterSkinTemperatureEventListener(mSkinTemperatureEventListener);
                client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
                client.getSensorManager().unregisterRRIntervalEventListener(mRRIntervalEventListener);
                client.getSensorManager().unregisterContactEventListener(mContactEventListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage());
            }
        }
    }

    private class AccelerometerSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    appendToUI("Band is connected.\n");
                    client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS32);
                    client.getSensorManager().registerGyroscopeEventListener(mGyroscopeEventListener, SampleRate.MS32);
                    client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
                    client.getSensorManager().registerContactEventListener(mContactEventListener);
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                        client.getSensorManager().registerRRIntervalEventListener(mRRIntervalEventListener);
                    } else {
                        client.getSensorManager().requestHeartRateConsent(MainActivity.this, heartRateConsentListener);
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Start button and press YES.\n");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage = "";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }

//    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
//        @Override
//        protected Void doInBackground(WeakReference<Activity>... params) {
//            try {
//                if (getConnectedBandClient()) {
//                    //check params has been recycled or not
//                    if (params[0].get() != null) {
//                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
//                            @Override
//                            public void userAccepted(boolean consentGiven) {
//                            }
//                        });
//                    }
//                } else {
//                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
//                }
//            } catch (BandException e) {
//                String exceptionMessage="";
//                switch (e.getErrorType()) {
//                    case UNSUPPORTED_SDK_VERSION_ERROR:
//                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
//                        break;
//                    case SERVICE_ERROR:
//                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
//                        break;
//                    default:
//                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
//                        break;
//                }
//                appendToUI(exceptionMessage);
//
//            } catch (Exception e) {
//                appendToUI(e.getMessage());
//            }
//            return null;
//        }
//    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    private void appendToUI(final String string) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(string);
            }
        });
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            return true;
        }

        appendToUI("Band is connecting...\n");
        return ConnectionState.CONNECTED == client.connect().await();
    }

    private void Quaternions(float ax, float ay, float az, float gx, float gy, float gz) {
        final float Kp = 2.0f;                        // proportional gain governs rate of convergence to accelerometer/magnetometer
        final float Ki = 0.005f;                          // integral gain governs rate of convergence of gyroscope biases
        final float halfT = 0.5f;                   // half the sample period

        float q0 = 1, q1 = 0, q2 = 0, q3 = 0;    // quaternion elements representing the estimated orientation
        float exInt = 0, eyInt = 0, ezInt = 0;    // scaled integral error

        float norm;
        float vx, vy, vz;
        float ex, ey, ez;

        if(ax*ay*az != 0) {
            norm = (float) Math.sqrt(ax*ax + ay*ay + az*az);
            ax = ax /norm;
            ay = ay / norm;
            az = az / norm;

            // estimated direction of gravity and flux (v and w)
            vx = 2*(q1*q3 - q0*q2);
            vy = 2*(q0*q1 + q2*q3);
            vz = q0*q0 - q1*q1 - q2*q2 + q3*q3 ;

            // error is sum of cross product between reference direction of fields and direction measured by sensors
            ex = (ay*vz - az*vy) ;
            ey = (az*vx - ax*vz) ;
            ez = (ax*vy - ay*vx) ;

            exInt = exInt + ex * Ki;
            eyInt = eyInt + ey * Ki;
            ezInt = ezInt + ez * Ki;

            // adjusted gyroscope measurements
            gx = gx + Kp*ex + exInt;
            gy = gy + Kp*ey + eyInt;
            gz = gz + Kp*ez + ezInt;
            // integrate quaternion rate and normalise
            q0 = q0 + (-q1*gx - q2*gy - q3*gz)* halfT;
            q1 = q1 + (q0*gx + q2*gz - q3*gy) * halfT;
            q2 = q2 + (q0*gy - q1*gz + q3*gx) * halfT;
            q3 = q3 + (q0*gz + q1*gy - q2*gx) * halfT;

            // normalise quaternion
            norm = (float) Math.sqrt(q0*q0 + q1*q1 + q2*q2 + q3*q3);
            q0 = q0 / norm;
            q1 = q1 / norm;
            q2 = q2 / norm;
            q3 = q3 / norm;

            yaw = (float)(Math.atan2(2 * q1 * q2 + 2 * q0 * q3, (-2) * q2 * q2 + (-2) * q3 * q3 + 1) * rad_to_deg);
            roll = (float)(Math.atan2(2 * q2 * q3 + 2 * q0 * q1, (-2) * q1 * q1 - 2 * q2 * q2 + 1) * rad_to_deg);
            pitch = (float)(Math.asin((-2) * q1 * q3 + 2 * q0 * q2) * rad_to_deg);
        }
    }

    public void socketClient(final String data){
        new Thread(new Runnable(){
            @Override
            public void run() {
                Socket client = new Socket();
                InetSocketAddress isa = new InetSocketAddress(address, port);
                try {
                    client.connect(isa, 10000);
                    BufferedOutputStream out = new BufferedOutputStream(client
                            .getOutputStream());
                    // 送出字串
                    //out.write((currentLabel + "\r\n").getBytes());
                    out.write(data.getBytes());
                    out.flush();
                    out.close();
                    out = null;
                    client.close();
                    client = null;

                } catch (java.io.IOException e) {
                    Log.d("Socket","Socket連線有問題 !");
                    Log.d("Socket","IOException :" + e.toString());
                }
            }
        }).start();
    }

}
