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
import java.util.ArrayList;
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

    private final String TAG = "Activity_Record_Data";
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
    private TextView tv_qsn_feedback;
    private ProgressBar pb_timer;

    private int answer;
    private List<Long> userTimeTaken;
    private long question_start_time;
    private long avg_time_taken;

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

    //TODO Called when question timeout
    private void timeout() {

    }

    /* Test Sequences */
    private void start_arithmetic_test_dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(R.string.arith_practice_instruction)
                .setTitle("Instructions");
        builder.setPositiveButton("Begin Test", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Start Recording Data
                initFileWriter();
                fileWriter.get().addAnnotationString(0, "Recording Started");
                recording = true;
                handler.post(arith_training_session);
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

    /**
     * Start of Training Session
     */
    private Runnable arith_training_session = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Arithmetic Training Session Started!");
            handler.post(arith_test_ui_update); //Begin UI Updates
            tv_current_activity_instr.setText("Activity: Practice Session 3 Min!");

            // Set Timer for Training session, When time ended --> Break
            int time = 3 * 1000; //TODO CHANGE THE TIME BACK!!!!--------------------------------
            pb_timer.setMax(time / 1000);
            new CountDownTimer(time, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int progress = (int) (millisUntilFinished / 1000);
                    pb_timer.setProgress(progress);
                    //TODO Create new text timer display
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Arithmetic Training Session Ended");
                    Log.d(TAG, "Break Session Started!");
                    pb_timer.setProgress(0);
                    fileWriter.get().addAnnotationString(0, "Practice -> Break");

                    //Calculate time taken for each question for the Test later.
                    long total_time_taken = 0;
                    for (int i = 0; i < userTimeTaken.size(); i++) {
                        total_time_taken += userTimeTaken.get(i);
                    }
                    avg_time_taken = total_time_taken / userTimeTaken.size();
                    Log.d(TAG, "Avg time: " + avg_time_taken);

                    // Break Session
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.break_instruction)
                            .setTitle("Break Instructions");
                    builder.setPositiveButton("Begin Relaxation", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            fileWriter.get().addAnnotationString(0, "Break -> Test");
                            question_start_time = System.currentTimeMillis();

                            //Begin Test Session
                            handler.post(arith_test_session);
                        }
                    });
                    dialog = builder.create();
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                }
            }.start();

            tv_arith_question.setText(generate_questions());
            question_start_time = System.currentTimeMillis();


        }
    };

    /**
     * Arith Test Session
     */
    private Runnable arith_test_session = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Begin Arithmetic Test Session!!");

        }
    };


    private String generate_questions() {
        Random r = new Random();

        answer = 0; //r.nextInt(9);
        String eqn;
        int curr;
        int a = r.nextInt(100);
        String b = gen_next_num(a, false);
        curr = eval(a + b);
        String c = gen_next_num(curr, false);
        curr = eval(a + b + c);
        String d = gen_next_num(curr, true);

        eqn = a + b + c + d;
        answer = eval(a + b + c + d);
        Log.d(TAG, eqn + "\n" + "Ans: " + answer);
        return eqn;
    }

    /**
     * Generate Next number with operator e.g. "+ 1" or "- 1". Generated number will be kept between 0 - 99.
     * TODO Add multiplications into the next number generation
     *
     * @param curr      Current sum of all digits.
     * @param final_num Next number is final, end result will make equation answer range from 0 - 9
     * @return "+ 1" or "- 1". Generated number will keep the sum between 0 - 99
     */
    private String gen_next_num(int curr, Boolean final_num) {

        int bound;
        String new_num = "";
        Random r = new Random();
        int opt = r.nextInt(2);

        if (final_num) {
            bound = 10;
        } else {
            bound = 99;
        }

        if (curr < 10) {

            if (opt == 0) {
                new_num = "+" + r.nextInt(bound - curr);
            } else if (opt == 1)
                new_num = "-" + r.nextInt(1 + curr);

        } else {
            if (final_num) { //Answer must be between 0 - 10
                new_num = "-" + (r.nextInt(10) + curr - 9);
            } else {
                if (opt == 0) {
                    new_num = "+" + r.nextInt(bound - curr);
                } else if (opt == 1)
                    new_num = "-" + r.nextInt(1 + curr);
            }

        }

        return new_num;
    }

    /**
     * Evaluate the provided equation and returns int.
     * @param eqn String of equation to be solved.
     * @return Result of equation
     */
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


    @Override
    public void onClick(View v) {
        int user_input = -1;
        switch (v.getId()) {
            case R.id.ans0:
                user_input = 0;
                break;
            case R.id.ans1:
                user_input = 1;
                break;
            case R.id.ans2:
                user_input = 2;
                break;
            case R.id.ans3:
                user_input = 3;
                break;
            case R.id.ans4:
                user_input = 4;
                break;
            case R.id.ans5:
                user_input = 5;
                break;
            case R.id.ans6:
                user_input = 6;
                break;
            case R.id.ans7:
                user_input = 7;
                break;
            case R.id.ans8:
                user_input = 8;
                break;
            case R.id.ans9:
                user_input = 9;
                break;
            default:
                Toast.makeText(this, "MISSING BREAK STATEMENT/ NO SUCH BUTTON", Toast.LENGTH_LONG).show();

        }
        answer_question(user_input);
    }

    private void answer_question(int user_input) {
        if (user_input == answer) {
            tv_qsn_feedback.setText("Correct!!");
        } else {
            tv_qsn_feedback.setText("Wrong!!!!");
        }

        userTimeTaken.add(System.currentTimeMillis() - question_start_time); // Record user time taken in milliseconds.
        Log.d(TAG, "Time: " + (System.currentTimeMillis() - question_start_time));

        tv_arith_question.setText(generate_questions());
        question_start_time = System.currentTimeMillis();

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
        tv_qsn_feedback = findViewById(R.id.qsn_feedback);

        userTimeTaken = new ArrayList<>();

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
