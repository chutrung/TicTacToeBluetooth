package trungchu.com.tictactoebluetooth.constant;

import android.util.Log;

public class BoardConstant {
    public static int NUM_ROW = 20;
    public static int NUM_COL = 20;
    public static int CELL_SIZE = 40;
    private static boolean DEBUG = true;
    private static String TAG = "TTT";
    public static void LOG_E(String content){
        if(DEBUG) {
            Log.e(TAG, content);
        }
    }
    public static int COUNT_DOWN_TIME = 30; //30 seconds
    public static int DISCOVERABLE_DURATION = 300; //300 seconds
}
