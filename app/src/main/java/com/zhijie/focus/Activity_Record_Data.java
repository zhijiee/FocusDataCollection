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
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
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
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import bsh.EvalError;
import bsh.Interpreter;

/*
    TODO: Create Spinner while connecting
    TODO: Better connection animation
 */
public class Activity_Record_Data extends Activity implements View.OnClickListener {

    private final String TAG = "TestLibMuseAndroid";
    private final Handler handler = new Handler();

    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();
    private final double[] eegBuffer = new double[6];
    private final double[] alphaBuffer = new double[6];
    private final double[] accelBuffer = new double[3];

    AlertDialog dialog;
    private Muse muse;
    private MuseManagerAndroid manager;
    private DataListener dataListener; // Receive packets from connected band
    private ConnectionListener connectionListener; //Headband connection Status

    private boolean recording = false;

    private Context context;

    private TextView tv_current_activity_instr;
    private TextView tv_arith_question;
    private ProgressBar pb_timer;

    private int answer;

    private final int MAX_BOUND = 99;



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

        //TODO UNCHANGE THIS BACK BEFORE COMMIT
        // Connect Muse Activity
//        Intent i = new Intent(this, Activity_Connect_Muse.class);
//        startActivityForResult(i, R.integer.SELECT_MUSE_REQUEST);

        start_arithmetic_test_dialog(); //TODO REMOVE, FOR TESTING PURPOSES

        initUI(); // Init UI Elements

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ans0:
                Log.d(TAG, "Ans0");

//                int time = 120 * 1000;
//                pb_timer.setMax(time/1000);
//                new CountDownTimer(time,1000) {
//                    @Override
//                    public void onTick(long millisUntilFinished) {
//                        int progress = (int) (millisUntilFinished/1000);
//                        tv_arith_question.setText("Time:" + progress);
//                        pb_timer.setProgress(progress);
//                    }
//
//                    @Override
//                    public void onFinish() {
//                        pb_timer.setProgress(0);
//                    }
//                }.start();

                break;
            case R.id.ans1:
//                Log.d(TAG, "Ans1");
                generate_questions();
                break;
            case R.id.ans2:
                Log.d(TAG, "Ans2");
                break;
            case R.id.ans3:
                Log.d(TAG, "Ans3");
                break;
            case R.id.ans4:
                Log.d(TAG, "Ans4");
                break;
            case R.id.ans5:
                Log.d(TAG, "Ans5");
                break;
            case R.id.ans6:
                Log.d(TAG, "Ans6");
                break;
            case R.id.ans7:
                Log.d(TAG, "Ans7");
                break;
            case R.id.ans8:
                Log.d(TAG, "Ans8");
                break;
            case R.id.ans9:
                Log.d(TAG, "Ans9");
                break;

            default:
                Toast.makeText(this, "MISSING BREAK STATEMENT/ NO SUCH BUTTON", Toast.LENGTH_LONG).show();

        }

    }

    /* Test Sequences */
    private void start_arithmetic_test_dialog() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setMessage(R.string.arith_test_instruction)
                .setTitle("Instructions");


        builder.setPositiveButton("Begin Test", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Start Recording Data
                initFileWriter();
                fileWriter.get().addAnnotationString(0, "Recording Started");
                recording = true;
                handler.post(arith_test_sequence);
                Log.d(TAG, "Start EEG Recording to file!");
            }
        });

        // 3. Get the AlertDialog from create()
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        //TODO UNCOMMENT THIS
//        (dialog).getButton(AlertDialog.BUTTON_POSITIVE)
//                .setEnabled(false);

    }

    private Runnable arith_test_sequence = new Runnable() {
        @Override
        public void run() {
            handler.post(arith_test_ui_update); //Begin UI Updates
            tv_current_activity_instr.setText("Activity: Practice! 2 Min");

            tv_arith_question.setText("question la"); // todo generate and set question

            int time = 120 * 1000;
            pb_timer.setMax(time / 1000);
            new CountDownTimer(time, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int progress = (int) (millisUntilFinished / 1000);
                    tv_arith_question.setText("Time:" + progress);
                    pb_timer.setProgress(progress);
                }

                @Override
                public void onFinish() {
                    pb_timer.setProgress(0);
                }
            }.start();
        }
    };

    private void generate_questions() {
        Random r = new Random();

        answer = r.nextInt(9);
        String eqn;
        int curr;

        String opt[] = {"+", "-"};
        int o1 = r.nextInt(2);
        int a = gen_next_num(answer, opt[o1]);
        curr = eval(answer + opt[o1] + a);

        int o2 = r.nextInt(2);
        int b = gen_next_num(curr, opt[o2]);


        int c = eval(curr + opt[o2] + b);
        eqn = c + opt[1 - o1] + a + opt[1 - o2] + b;


        Log.d(TAG, eqn + "\n" + "Ans:" + answer);
    }

    private int gen_next_num(int curr, String opt) {
        Random r = new Random();
        Log.d(TAG, "Curr:" + curr);
        int a = -1;
        if (opt.equals("+")) {
            a = r.nextInt(MAX_BOUND - curr) + curr;
        } else if (opt.equals("-")) {
            a = r.nextInt(curr);
        }

        return a;
    }

    private int eval(String eqn) {
        int ans = -1;
        try {
            Interpreter itpr = new Interpreter();
            ans = (int) itpr.eval(eqn);
        } catch (EvalError e) {
            Log.d(TAG, e.getErrorText());
        }
        return ans;
    }

    // TODO UPDATE PROGRESS BAR
    // TODO UPDATE TIMER COUNTDOWN
    private Runnable arith_test_ui_update = new Runnable() {
        @Override
        public void run() {

            handler.postDelayed(arith_test_ui_update, 1000 / 60);
        }
    };


    private void initUI() {
        setContentView(R.layout.arithmetic_task);
        tv_current_activity_instr = findViewById(R.id.current_activity_instr);
        tv_arith_question = findViewById(R.id.arith_question);
        pb_timer = findViewById(R.id.timer_progressbar);

        //Buttons
        Button a0 = findViewById(R.id.ans0);
        Button a1 = findViewById(R.id.ans1);
        Button a2 = findViewById(R.id.ans2);
        Button a3 = findViewById(R.id.ans3);
        Button a4 = findViewById(R.id.ans4);
        Button a5 = findViewById(R.id.ans5);
        Button a6 = findViewById(R.id.ans6);
        Button a7 = findViewById(R.id.ans7);
        Button a8 = findViewById(R.id.ans8);
        Button a9 = findViewById(R.id.ans9);

        a0.setOnClickListener(this);
        a1.setOnClickListener(this);
        a2.setOnClickListener(this);
        a3.setOnClickListener(this);
        a4.setOnClickListener(this);
        a5.setOnClickListener(this);
        a6.setOnClickListener(this);
        a7.setOnClickListener(this);
        a8.setOnClickListener(this);
        a9.setOnClickListener(this);


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
//                }
//             });

    }

    /*
     *  -------------- Return from startActivityForResult ------------------
     */


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

    /*
     * -------------------- Begin File I/O --------------------------
     */
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

//    /**
//     * We don't want to block the UI thread while we write to a file, so the file
//     * writing is moved to a separate thread.
//     */
//    private final Thread fileThread = new Thread() {
//        @Override
//        public void run() {
//            Looper.prepare();
//
//            SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US);
//            Date now = new Date();
//            String fileName = formatter.format(now) + ".muse";
//
//            fileHandler.set(new Handler());
//            final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
//            final File file = new File(dir, fileName);
//            // MuseFileWriter will append to an existing file.
//            // In this case, we want to start fresh so the file
//            // if it exists.
//            if (file.exists()) {
//                file.delete();
//            }
//            Log.i(TAG, "Writing data to: " + file.getAbsolutePath());
//            fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
//            Looper.loop();
//        }
//    };
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
            (dialog).getButton(AlertDialog.BUTTON_POSITIVE)
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
                break;
            case ACCELEROMETER:
                assert (accelBuffer.length >= n);
                getAccelValues(p);
                break;
            case ALPHA_RELATIVE:
                assert (alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer, p);
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
