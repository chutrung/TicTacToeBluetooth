package trungchu.com.tictactoebluetooth.constant;

import android.util.Log;

public class BoardConstant {
    public static int NUM_ROW = 20;
    public static int NUM_COL = 20;
    public static int CELL_SIZE = 40;
    public static boolean DEBUG = true;
    public static String TAG = "TTT";
    public static void LOG_E(String content){
        Log.e(TAG,content);
    }
}
