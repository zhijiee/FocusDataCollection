package controllers;

/**
 * Created by ohzj on 2/26/2018.
 */

public class MuseConnectionHelper {
    // Check for Good connection before allowing the user to proceed
//    private Runnable update_hsi_for_dialog = new Runnable() {
//        @Override
//        public void run() {
//
//            String msg = getString(R.string.dialog_arith_training_instruction, (int)hsiBuffer[0], (int)hsiBuffer[1], (int)hsiBuffer[2], (int)hsiBuffer[3]);
//            dialog.setMessage(msg);
//
//            if (is_muse_stable) {
//                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
//            }
//
//            //Stop when recording started
//            if (!is_recording){
//                handler.postDelayed(update_hsi_for_dialog, 1000 / 100);
//            }
//        }
//    };
//
//    private void cdt_muse_stable() {
//        cdt_muse_stable = new CountDownTimer(MUSE_STABLE_TIME, MUSE_STABLE_TIME_CD_INTERVAL) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                for (int i = 0; i < hsiBuffer.length; i++) {
//                    if (hsiBuffer[i] != 1) {
//                        restart_muse_stable();
//                    }
//                }
//            }
//
//            @Override
//            public void onFinish() {
//                is_muse_stable = true;
//            }
//        }.start();
//    }
//
//    private void restart_muse_stable(){
//        Log.d(TAG, "RESTART");
//        if(cdt_muse_stable != null){
//            cdt_muse_stable.cancel();
//            cdt_muse_stable = null;
//        }
//
//        cdt_muse_stable();
//    }
}
