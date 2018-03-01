package com.zhijie.focus;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import java.util.concurrent.atomic.AtomicReference;

import controllers.ArithmeticTest;
import controllers.ArithmeticTraining;
import controllers.GenericArithmetic;

import static constants.Constants.ARITH_TEST_TIMEOUT;
import static constants.Constants.ARITH_TRAINING_TIMEOUT;
import static constants.Constants.GUIDED_MEDITATION_TRACK;
import static constants.Constants.MUSE_STABLE_TIME;
import static constants.Constants.MUSE_STABLE_TIME_CD_INTERVAL;
import static constants.Constants.USE_MUSE;
import static constants.Constants.cd_interval;

//import static constants.Constants.ARITH_TEST_TIMEOUT;

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
    private final double[] hsiBuffer = new double[4];

    private GenericArithmetic arith_session;

    AlertDialog dialog;
    private Muse muse;
    private MuseManagerAndroid manager;
    private DataListener dataListener; // Receive packets from connected band
    private ConnectionListener connectionListener; //Headband connection Status
    private String muse_status;

    private boolean is_recording = false;
    private boolean is_muse_stable = false;
    private boolean EEG_data_collection_not_finished = true;

    private Context context;

    private TextView tv_current_activity_instr;
    private TextView tv_muse_status;
    private ProgressBar pb_timer;
    private ProgressBar pb_qsn_timeout;

    private String name;
    private long avg_time_taken;

    private CountDownTimer cdt_muse_stable;
    MediaPlayer mp; //Garbage collector will destroy

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        arith_session = new ArithmeticTraining();

        context = this;
        // Load the Muse Library
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Intent intent = getIntent();
        name = intent.getStringExtra("name");

        // Setup Callback
        WeakReference<Activity_Record_Data> weakActivity =
                new WeakReference<Activity_Record_Data>(this);
        connectionListener = new ConnectionListener(weakActivity); //Status of Muse Headband
        dataListener = new DataListener(weakActivity); //Get data from EEG

        // Connect Muse Activity
        if (USE_MUSE) {
            Intent i = new Intent(this, Activity_Connect_Muse.class);
            startActivityForResult(i, R.integer.SELECT_MUSE_REQUEST);
        } else {
            start_arithmetic_training_dialog();
        }
        initUI(); // Init UI Elements
        muse_status = getString(R.string.undefined);

    }

    /* Begin Arithmetic Training Session Dialogue */
    private void start_arithmetic_training_dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_arith_training_title)
                .setMessage(R.string.dialog_arith_training_instruction);

        builder.setPositiveButton(R.string.dialog_arith_training_btn_pos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                // Start Recording Data
                initFileWriter();

                fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_training_begin));
                is_recording = true;
                handler.post(arith_training_session);
            }
        });

        // 3. Get the AlertDialog from create()
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        if (USE_MUSE) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            cdt_muse_stable();
            handler.post(update_hsi_for_dialog);
        }
    }

    // Check for Good connection before allowing the user to proceed
    private Runnable update_hsi_for_dialog = new Runnable() {
        @Override
        public void run() {

            String msg = getString(R.string.dialog_arith_training_instruction, muse_status, (int) hsiBuffer[0], (int) hsiBuffer[1], (int) hsiBuffer[2], (int) hsiBuffer[3]);
            dialog.setMessage(msg);

            if (is_muse_stable) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            }

            //Stop when recording started
            if (!is_recording) {
                handler.postDelayed(update_hsi_for_dialog, 1000 / 100);
            }
        }
    };

    private void cdt_muse_stable() {
        cdt_muse_stable = new CountDownTimer(MUSE_STABLE_TIME, MUSE_STABLE_TIME_CD_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                for (int i = 0; i < hsiBuffer.length; i++) {
                    if (hsiBuffer[i] != 1) {
                        restart_muse_stable();
                    }
                }
            }

            @Override
            public void onFinish() {
                is_muse_stable = true;
            }
        }.start();
    }

    private void restart_muse_stable() {
        Log.d(TAG, "RESTART");
        if (cdt_muse_stable != null) {
            cdt_muse_stable.cancel();
            cdt_muse_stable = null;
        }

        cdt_muse_stable();
    }


    /**
     * Start of Training Session
     */
    private Runnable arith_training_session = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Arithmetic Training Session Started!");
            tv_current_activity_instr.setText(R.string.tv_activity_training_instr);

            // Set Timer for Training session, When time ended --> Break
            int time = ARITH_TRAINING_TIMEOUT * 1000;
            pb_timer.setMax(time / cd_interval);

            //Training Count down timer
            new CountDownTimer(time, cd_interval) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int progress = (int) (millisUntilFinished / cd_interval);
                    pb_timer.setProgress(progress);
                }

                @Override
                public void onFinish() {

                    fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_training_end));
                    Log.d(TAG, getString(R.string.anno_arith_training_end));
                    pb_timer.setProgress(0);

                    avg_time_taken = arith_session.get_avg_time();

                    // Break Session
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.dialog_break_msg)
                            .setTitle(R.string.dialog_break_title);
                    builder.setPositiveButton(R.string.dialog_break_btn_positive, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //Begin Guided Meditation
                            handler.post(guided_meditation_session);
                        }
                    });
                    dialog = builder.create();
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();

                }
            }.start();

            arith_session.generate_questions();

        }
    };

    /**
     * Guided Meditation Session
     */
    private Runnable guided_meditation_session = new Runnable() {
        @Override
        public void run() {
            fileWriter.get().addAnnotationString(0, getString(R.string.anno_guided_meditation_begin));
            Log.d(TAG, getString(R.string.anno_guided_meditation_begin));
            setContentView(R.layout.guided_meditation);
            tv_muse_status = findViewById(R.id.tv_muse_status);
            tv_muse_status.setText(muse_status);


            mp = MediaPlayer.create(context, GUIDED_MEDITATION_TRACK);
            mp.start();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "Finish playing!");
                    fileWriter.get().addAnnotationString(0, getString(R.string.anno_guided_meditation_ended));
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle(R.string.dialog_guided_meditation_title)
                            .setMessage(R.string.dialog_guided_meditation_msg);
                    builder.setPositiveButton(R.string.dialog_guided_meditation_btn_pos, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Start Recording Data
                            setContentView(R.layout.arithmetic_task);
                            handler.post(arith_test_session);

                        }
                    });

                    // 3. Get the AlertDialog from create()
                    dialog = builder.create();
                    dialog.setCancelable(false);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.show();
                    mp.stop();
                    mp.release();

                }
            });
            pb_timer = findViewById(R.id.pb_timer);
            long time = mp.getDuration();
            pb_timer.setMax(mp.getDuration() / cd_interval);

            new CountDownTimer(time, cd_interval) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int progress = (int) millisUntilFinished / cd_interval;
                    pb_timer.setProgress(progress);
                }

                @Override
                public void onFinish() {

                }
            }.start();

//            mp.start();
        }
    };

    /**
     * Start of Arithmetic Test Session
     */
    private Runnable arith_test_session = new Runnable() {
        @Override
        public void run() {
            arith_session = new ArithmeticTest();
            initUI();
            arith_session.setTv_test_score(findViewById(R.id.tv_score));
            pb_qsn_timeout.setVisibility(View.VISIBLE);

            arith_session.setPb_qsn_timeout(pb_qsn_timeout);
            arith_session.setAvg_time_taken(avg_time_taken);

            fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_test_begin));
            Log.d(TAG, getString(R.string.anno_arith_test_begin));
            tv_muse_status.setText(muse_status);
            tv_current_activity_instr.setText(R.string.tv_activity_test_instr);

            arith_session.generate_questions();
            arith_session.cdt_repeat();

            int time = ARITH_TEST_TIMEOUT * 1000;
            pb_timer.setMax(time / cd_interval);
            // Timer the test session
            new CountDownTimer(time, cd_interval) {

                @Override
                public void onTick(long millisUntilFinished) {
                    // Do nothing on tick
                    int progress = (int) (millisUntilFinished / cd_interval);
                    pb_timer.setProgress(progress);
                }

                public void onFinish() {
                    saveFile();
                    pb_qsn_timeout.setProgress(0);
                    arith_session.cdt_qsn.cancel();
                    fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_test_end));
                    Log.d(TAG, getString(R.string.anno_arith_test_end));
                    start_arithmetic_test_completed();

                }
            }.start();
        }
    };

    private void start_arithmetic_test_completed() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_arith_complete_title)
                .setMessage(R.string.dialog_arith_complete_message);

        builder.setPositiveButton(R.string.dialog_arith_complete_btn_pos, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                if (muse != null)
                    muse.disconnect();

                finish();
            }
        });

        // 3. Get the AlertDialog from create()
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

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
        arith_session.answer_question(user_input);
    }


    private void initUI() {
        setContentView(R.layout.arithmetic_task);
        tv_current_activity_instr = findViewById(R.id.current_activity_instr);
        tv_muse_status = findViewById(R.id.tv_muse_status);
        pb_timer = findViewById(R.id.pb_task_timer);
        pb_qsn_timeout = findViewById(R.id.pb_qsn_timeout);

        arith_session.setTv_qn_feedback(findViewById(R.id.tv_qsn_feedback));
        arith_session.setTv_question(findViewById(R.id.arith_question));

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

                start_arithmetic_training_dialog();
            } else {
                finish();
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
        muse.registerDataListener(dataListener, MuseDataPacketType.HSI_PRECISION);
        muse.registerDataListener(dataListener, MuseDataPacketType.IS_GOOD);

        // Initiate a connection to the headband and stream the data asynchronously.
        muse.runAsynchronously();

    }

    /*
     * -------------------- Begin File I/O --------------------------
     */
    private void initFileWriter() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.US);
        Date now = new Date();
        String fileName = name + "_" + formatter.format(now) + ".muse";

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
        muse_status = current.toString();
        Log.i(TAG, "Muse Connection Status: " + muse_status);

        handler.post(new Runnable() {
            @Override
            public void run() {
                // Update the UI with the change in connection state.
//                final TextView statusText = findViewById(R.id.con_status);
                tv_muse_status.setText(muse_status);

            }
        });

        if (current == ConnectionState.DISCONNECTED) {
            //TODO Settle for another logic for disconnection!
            Log.i(TAG, "Muse disconnected:" + muse.getName());

            // Save the data file once streaming has stopped.


            // We have disconnected from the headband, so set our cached copy to null.
//            this.muse = null;

            if (EEG_data_collection_not_finished) {
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
                    }, 500);
                }
            }


        } else if (current == ConnectionState.CONNECTED) {

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

        // Write to file when is_recording started
        if (is_recording) {
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
            case HSI_PRECISION:
                assert (alphaBuffer.length >= n);
                getHSIPrecision(hsiBuffer, p);
                break;
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:

            default:
                break;
        }
    }

    private void getHSIPrecision(double[] buffer, MuseDataPacket p) {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
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
        if (is_recording) {
            Toast.makeText(context, "Back button is disabled while recording!", Toast.LENGTH_SHORT).show();
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
