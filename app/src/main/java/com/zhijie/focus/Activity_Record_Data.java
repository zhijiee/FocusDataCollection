/**
 * Example of using libmuse library on android.
 * Interaxon, Inc. 2016
 */

package com.zhijie.focus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseFileFactory;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/*
    TODO: Create Spinner while connecting
 */
public class Activity_Record_Data extends Activity {

    private final String TAG = "TestLibMuseAndroid";
    private final Handler handler = new Handler();

    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();
    private final double[] eegBuffer = new double[6];
    private final double[] alphaBuffer = new double[6];
    private final double[] accelBuffer = new double[3];
    /**
     * We don't want to block the UI thread while we write to a file, so the file
     * writing is moved to a separate thread.
     */
    private final Thread fileThread = new Thread() {
        @Override
        public void run() {
            Looper.prepare();

            SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US);
            Date now = new Date();
            String fileName = formatter.format(now) + ".muse";

            fileHandler.set(new Handler());
            final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(dir, fileName);
            // MuseFileWriter will append to an existing file.
            // In this case, we want to start fresh so the file
            // if it exists.
            if (file.exists()) {
                file.delete();
            }
            Log.i(TAG, "Writing data to: " + file.getAbsolutePath());
            fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
            Looper.loop();
        }
    };
    AlertDialog dialog;
    private Muse muse;
    private MuseManagerAndroid manager;
    private DataListener dataListener; // Receive packets from connected band
    private ConnectionListener connectionListener; //Headband connection Status
    private boolean eegStale;
    private boolean alphaStale;
    private boolean accelStale;
    private Context context;
    private boolean recording = false;
    private Button start_record_btn;
    private TextView instr_textview;
    private Runnable recording_events = new Runnable() {
        @Override
        public void run() {
            // todo: Create the sequence of events for the users to perform to record the data
            instr_textview.setText("Instructions to tell users what to do! TODO");


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        // Load the Muse Library
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        // Setup Callback
        WeakReference<Activity_Record_Data> weakActivity =
                new WeakReference<Activity_Record_Data>(this);
        connectionListener = new ConnectionListener(weakActivity); //Status of Muse Headband
        dataListener = new DataListener(weakActivity); //Get data from EEG

//        fileThread.start(); // Start File Thread


        // Connect Muse Activity
        Intent i = new Intent(this, Activity_Connect_Muse.class);
        startActivityForResult(i, R.integer.SELECT_MUSE_REQUEST);

        initUI(); // Init UI Elements

    }

//    private final Runnable tickUi = new Runnable() {
//        @Override
//        public void run() {
//            if (eegStale) {
//                TextView tp9 = findViewById(R.id.eeg_tp9);
//                TextView fp1 = findViewById(R.id.eeg_af7);
//                TextView fp2 = findViewById(R.id.eeg_af8);
//                TextView tp10 = findViewById(R.id.eeg_tp10);
//                tp9.setText(String.format("%6.2f", eegBuffer[0]));
//                fp1.setText(String.format("%6.2f", eegBuffer[1]));
//                fp2.setText(String.format("%6.2f", eegBuffer[2]));
//                tp10.setText(String.format("%6.2f", eegBuffer[3]));
//
//            }
//            handler.postDelayed(tickUi, 1000 / 10); // update 10 times per second
//        }
//    };

    private void initFileWriter() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = formatter.format(now) + ".muse";

        fileHandler.set(new Handler());
        final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        final File file = new File(dir, fileName);
        // MuseFileWriter will append to an existing file.
        // In this case, we want to start fresh so the file
        // if it exists.
        if (file.exists()) {
            file.delete();
        }
        Log.i(TAG, "Writing data to: " + file.getAbsolutePath());
        fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
    }

    /*
     *  -------------- Return from startActivityForResult ------------------
     */

    private void initUI() {
        setContentView(R.layout.arithmetic_task);

//        instr_textview = findViewById(R.id.instr_tv);

//        start_record_btn = findViewById(R.id.start_recording);
//        start_record_btn.setEnabled(false);
//        start_record_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (recording) {
//                    // Stop Recording and save file
//                    saveFile();
//                    recording = false;
//                    start_record_btn.setText(R.string.start_rec);
//
//                } else {
//                    // Start Recording Data
//                    initFileWriter();
//                    fileWriter.get().addAnnotationString(0, "Recording Started");
//                    recording = true;
//                    start_record_btn.setText(R.string.stop_rec);
//                    handler.post(recording_events);
//                }
//            }
//        });

    }

    private void start_arithmetic_test_dialog() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.arith_test_instruction)
                .setTitle("Instructions");


        builder.setPositiveButton("Begin Test", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });

        // 3. Get the AlertDialog from create()
        dialog = builder.create();

        dialog.show();

        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                .setEnabled(false);


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check which request we're responding to
        if (requestCode == R.integer.SELECT_MUSE_REQUEST) {
            if (resultCode == RESULT_OK) {

                int position = data.getIntExtra("pos", 0);

                List<Muse> availableMuse = manager.getMuses();
                muse = availableMuse.get(position);
                connect_to_muse();

                start_arithmetic_test_dialog();
                // Thread to update UI
//                handler.post(tickUi);
            }
        }
    }
    /*
     * -------------------- Begin File I/O --------------------------
     */

    private final void connect_to_muse() {
        muse.unregisterAllListeners();
        muse.registerConnectionListener(connectionListener);
        muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
        muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
        muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
        muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
        muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
        muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

        // Initiate a connection to the headband and stream the data asynchronously.
        muse.runAsynchronously();

    }

    /**
     * Writes the provided MuseDataPacket to the file.  MuseFileWriter knows
     * how to write all packet types generated from LibMuse.
     *
     * @param p The data packet to write.
     */
    private void writeDataPacketToFile(final MuseDataPacket p) {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    fileWriter.get().addDataPacket(0, p);
                }
            });
        }
    }


    private void saveFile() {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    MuseFileWriter w = fileWriter.get();
                    // Annotation strings can be added to the file to
                    // give context as to what is happening at that point in
                    // time.  An annotation can be an arbitrary string or
                    // may include additional AnnotationData.
//                    w.addConfiguration(0, muse.getMuseConfiguration());
                    w.addAnnotationString(0, "Disconnected");
                    w.flush();
                    w.close();
                    Toast.makeText(context, "File Saved", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /*
     * ----------------------- Begin Callback methods ---------------
     */

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     *
     * @param p    A packet containing the current and prior connection states
     * @param muse The headband whose state changed.
     */
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = current.toString();
        Log.i(TAG, "Muse Connection Status: " + status);

        handler.post(new Runnable() {
            @Override
            public void run() {
                // Update the UI with the change in connection state.
                final TextView statusText = (TextView) findViewById(R.id.con_status);
                statusText.setText(status);

            }
        });

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());

            // TODO discard file
            // Save the data file once streaming has stopped.
//            saveFile();


            // We have disconnected from the headband, so set our cached copy to null.
//            this.muse = null;
            android.widget.Toast.makeText
                    (this, "Muse Disconnected! Reconnecting!", Toast.LENGTH_SHORT).show();

            if (muse != null) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (muse != null)
                            connect_to_muse();
                    }
                }, 1000);
            }

            //            start_record_btn.setEnabled(false);
//            start_record_btn.setText(R.string.start_rec);


        } else if (current == ConnectionState.CONNECTED) {
//            start_record_btn.setEnabled(true);
            ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    .setEnabled(true);

        }
    }


    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     *
     * @param p    The data packet containing the data from the headband (eg. EEG data)
     * @param muse The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {

        // Write to file when recording started
        if (recording) {
            writeDataPacketToFile(p);
        }
        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert (eegBuffer.length >= n);
                getEegChannelValues(eegBuffer, p);
                eegStale = true;
                break;
            case ACCELEROMETER:
                assert (accelBuffer.length >= n);
                getAccelValues(p);
                accelStale = true;
                break;
            case ALPHA_RELATIVE:
                assert (alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer, p);
                alphaStale = true;
                break;
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }

    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    private void getAccelValues(MuseDataPacket p) {
        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    }


    @Override
    public void onBackPressed() {

        // TODO: Prompt dialog to ask if really want to exit
        if (recording) {
            Toast.makeText(context, "Recording in Progress", Toast.LENGTH_SHORT).show();
        } else {
            // Disconnect Muse when returning to previous activity.
            if (muse != null)
                muse.disconnect();
            muse = null;
            finish();
        }

    }


    /*
     *  ------------- Begin Setup classes for callback ---------------
     */
    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<Activity_Record_Data> activityRef;

        ConnectionListener(final WeakReference<Activity_Record_Data> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<Activity_Record_Data> activityRef;

        DataListener(final WeakReference<Activity_Record_Data> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            activityRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            // Not going to use Muse default Artifact removal
            //activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }


}
