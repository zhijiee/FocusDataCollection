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

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

/*
    TODO: Create Spinner while connecting
    TODO: Better connection animation
 */
public class Activity_Record_Data extends Activity implements View.OnClickListener {
    //Global Variables

    // TODO Change the timing Variables, 3 Min, 3 Min
    private static final int ARITH_TRAINING_TIMEOUT = 1;
    private static final int GUIDED_MEDITATION_TRACK = R.raw.ting;  //TODO R.raw.guided_meditation
    private static final int ARITH_TEST_TIMEOUT = 180;
    private static final int cd_interval = 1;
    private static final int MUSE_STABLE_TIME = 3;

    private final String TAG = "Activity_Record_Data";
    private final Handler handler = new Handler();

    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();

    private final double[] eegBuffer = new double[6];
    private final double[] alphaBuffer = new double[6];
    private final double[] accelBuffer = new double[3];
    private final double[] hsiBuffer = new double[4];

    AlertDialog dialog;
    private Muse muse;
    private MuseManagerAndroid manager;
    private DataListener dataListener; // Receive packets from connected band
    private ConnectionListener connectionListener; //Headband connection Status
    //    private ArithmeticTraining arithmeticTraining;
    String muse_status;

    private boolean is_recording = false;
    private boolean is_arith_test = false;
    private boolean is_muse_stable = false;

    private Context context;

    private TextView tv_current_activity_instr;
    private TextView tv_arith_question;
    private TextView tv_qn_feedback;
    private TextView tv_muse_status;
    private ProgressBar pb_timer;
    private ProgressBar pb_qsn_timeout;

    private int answer;
    private int num_consecutive_correct = 0;
    private List<Long> userTimeTaken;
    private long question_start_time;
    private long avg_time_taken;

    private CountDownTimer cdt_muse_stable;
    private CountDownTimer cdt_qsn;

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

        //TODO Uncomment, MUSE CONNECTION
        // Connect Muse Activity
        Intent i = new Intent(this, Activity_Connect_Muse.class);
        startActivityForResult(i, R.integer.SELECT_MUSE_REQUEST);

//        start_arithmetic_training_dialog(); //TODO REMOVE, MUSE CONNECTION

        initUI(); // Init UI Elements

    }

    /* Begin Arithmetic Training Session Dialogue */
    private void start_arithmetic_training_dialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_arith_training_title)
                .setMessage(R.string.dialog_arith_training_instruction);

        builder.setPositiveButton("Begin Test", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                is_arith_test = false; //Set false
                // Start Recording Data
                initFileWriter();
                fileWriter.get().addAnnotationString(0, "Recording Started");
                is_recording = true;
                handler.post(arith_training_session);
            }
        });

        // 3. Get the AlertDialog from create()
        dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        cdt_muse_stable();
        handler.post(update_hsi_for_dialog);
        //TODO UNCOMMENT THIS MUSE CONNECTION

    }

    private Runnable update_hsi_for_dialog = new Runnable() {
        @Override
        public void run() {
            int a[] = new int[6];
            boolean muse_good_connection = true;

            for (int i = 0; i < hsiBuffer.length; i++) {
                a[i] = (int) hsiBuffer[i];
                if (a[i] != 1) {
                    muse_good_connection = false;
                }
            }

            if (muse_good_connection) {
                cdt_muse_stable();
            } else {
                if (cdt_muse_stable != null) {
                    cdt_muse_stable.cancel();
                    cdt_muse_stable = null;
                }
            }


            String msg = getString(R.string.dialog_arith_training_instruction, a[0], a[1], a[2], a[3]);
            dialog.setMessage(msg);


            if (is_muse_stable) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            } else {
                handler.postDelayed(update_hsi_for_dialog, 1000 / 100);
            }
        }
    };

    private void cdt_muse_stable() {
        long time = MUSE_STABLE_TIME * 1000;
        cdt_muse_stable = new CountDownTimer(time, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                is_muse_stable = true;
            }
        }.start();
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

                    //Calculate time taken for each question for the Test later.
                    long total_time_taken = 0;
                    for (int i = 0; i < userTimeTaken.size(); i++) {
                        total_time_taken += userTimeTaken.get(i);
                    }

                    if (userTimeTaken.size() == 0) {
                        avg_time_taken = 9000;
                    } else {
                        avg_time_taken = total_time_taken / userTimeTaken.size();
                    }
                    Log.d(TAG, "Avg time: " + avg_time_taken);

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

            tv_arith_question.setText(generate_questions());


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


            MediaPlayer mp = MediaPlayer.create(context, GUIDED_MEDITATION_TRACK);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_test_begin));
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

                }
            });
            pb_timer = findViewById(R.id.pb_timer);
            long time = mp.getDuration();
            Log.d(TAG, "Dur" + mp.getDuration());
            pb_timer.setMax(mp.getDuration() / cd_interval);

            // TODO confirm this is correct
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

            mp.start();
        }
    };

    /**
     * Start of Arithmetic Test Session
     */
    private Runnable arith_test_session = new Runnable() {
        @Override
        public void run() {
            fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_test_begin));
            Log.d(TAG, getString(R.string.anno_arith_test_begin));
            initUI();
            tv_muse_status.setText(muse_status);
            tv_current_activity_instr.setText(R.string.tv_activity_test_instr);
            is_arith_test = true;

            pb_qsn_timeout.setVisibility(View.VISIBLE);
//            pb_qsn_timeout.setProgress(100);
            tv_arith_question.setText(generate_questions());
            cdt_repeat();


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
                    pb_qsn_timeout.setProgress(0);
                    cdt_qsn.cancel();
                    fileWriter.get().addAnnotationString(0, getString(R.string.anno_arith_test_end));
                    Log.d(TAG, getString(R.string.anno_arith_test_end));
                }
            }.start();
        }
    };

    private void cdt_repeat() {

        if (cdt_qsn != null)
            cdt_qsn.cancel();

        pb_qsn_timeout.setMax((int) avg_time_taken / cd_interval);
        pb_qsn_timeout.setProgress((int) avg_time_taken);
        cdt_qsn = new CountDownTimer(avg_time_taken, cd_interval) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / cd_interval);
                pb_qsn_timeout.setProgress(progress);
            }

            @Override
            public void onFinish() {
                tv_qn_feedback.setText(R.string.tv_feedback_timeout);
                tv_qn_feedback.setTextColor(RED);
                answered_consecutive_helper(-1);
                tv_arith_question.setText(generate_questions());
                cdt_repeat();
            }
        };

        cdt_qsn.start();
    }

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
        Log.d(TAG, eqn + " = " + answer);

        question_start_time = System.currentTimeMillis(); //Set Start time after question generation

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
            bound = 100;
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
            tv_qn_feedback.setTextColor(GREEN);
            tv_qn_feedback.setText(R.string.tv_feedback_correct);
        } else {
            tv_qn_feedback.setTextColor(RED);
            tv_qn_feedback.setText(R.string.tv_feedback_wrong);

        }
        // For Arithmetic Test
        if (is_arith_test) {

            answered_consecutive_helper(user_input);
            cdt_repeat();

        }

        // For Arithmetic Training
        if (!is_arith_test) {
            // Record user time taken in milliseconds.
            userTimeTaken.add(System.currentTimeMillis() - question_start_time);
            Log.d(TAG, "Time: " + (System.currentTimeMillis() - question_start_time));
        }

        tv_arith_question.setText(generate_questions());

    }

    private void answered_consecutive_helper(int user_input) {
        if (user_input == answer) { // User answer question correctly

            if (num_consecutive_correct < 0) //If previous answered wrongly
                num_consecutive_correct = 1;
            else
                num_consecutive_correct++; // Previously correct, increment

        } else { //User answered the question wrongly
            if (num_consecutive_correct > 0) { //If previously answered correctly
                Log.d(TAG, "num:" + num_consecutive_correct);
                num_consecutive_correct = -1;
            } else {
                Log.d(TAG, "num:" + num_consecutive_correct);
                num_consecutive_correct--;
            }
        }
        // Increase or decrease the time taken with 3 consecutively correct or wrong.
        long timeChange = avg_time_taken / 10;
        if (num_consecutive_correct <= -3) {
            avg_time_taken += timeChange;

        } else if (num_consecutive_correct >= 3) {
            avg_time_taken -= timeChange;
        }

    }


    private void initUI() {
        setContentView(R.layout.arithmetic_task);
        tv_current_activity_instr = findViewById(R.id.current_activity_instr);
        tv_arith_question = findViewById(R.id.arith_question);
        tv_muse_status = findViewById(R.id.tv_muse_status);
        pb_timer = findViewById(R.id.pb_task_timer);
        tv_qn_feedback = findViewById(R.id.qsn_feedback);
        pb_qsn_timeout = findViewById(R.id.pb_qsn_timeout);

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
//                if (is_recording) {
//                    // Stop Recording and save file
//                    saveFile();
//                    is_recording = false;
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

                start_arithmetic_training_dialog();
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
            //todo remove this soon
//            (dialog).getButton(AlertDialog.BUTTON_POSITIVE)
//                    .setEnabled(true);

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
//        if (is_recording) {
//            Toast.makeText(context, "Recording in Progress", Toast.LENGTH_SHORT).show();
//        } else {
//            // Disconnect Muse when returning to previous activity.
//            if (muse != null)
//                muse.disconnect();
//            muse = null;
//            finish();
//        }

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
