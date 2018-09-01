package trungchu.com.tictactoebluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    // Member object for the chat services
    private BluetoothService mChatService = null;

    // Name of the connected device
    private String mConnectedDeviceName = null;

    private BoardGame mBoardGame;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mChatService == null) setupBoard();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mChatService != null) {
            if (mChatService.getState() == BluetoothService.STATE_NONE) {
                mChatService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
    }

    private void setupBoard() {
        mBoardGame = findViewById(R.id.boardGame);
        mBoardGame.setSendMessageListener(sendMessageListener);
        // Initialize the BluetoothService to perform bluetooth connections
        mChatService = new BluetoothService( mHandler);

    }

    public void connect(View v) {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void discoverable(View v) {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBoard();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    // The Handler that gets information back from the BluetoothService
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WRITE: {
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
//                    mAdapter.notifyDataSetChanged();
//                    messageList.add(new androidRecyclerView.Message(counter++, writeMessage, "Me"));
                    String[] p = writeMessage.split(",");
                    int x = Integer.parseInt(p[0]);
                    int y = Integer.parseInt(p[1]);
                    Point point = new Point(x, y);
                    mBoardGame.setMinePointToBoard(point);
                    BoardGame.myTurn = false;
                    if(mBoardGame.checkWin(point)){ //ban thang
                        showAlertDialog(true);
                    }
                    break;
                }
                case MESSAGE_READ: {
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
//                    mAdapter.notifyDataSetChanged();
//                    messageList.add(new androidRecyclerView.Message(counter++, readMessage, mConnectedDeviceName));
                    String[] p = readMessage.split(",");
                    int x = Integer.parseInt(p[0]);
                    int y = Integer.parseInt(p[1]);
                    Point point = new Point(x, y);
                    mBoardGame.setEnemyPointToBoard(point);
                    BoardGame.myTurn = true;
                    if(mBoardGame.checkWin(point)){ //doi thu thang
                        showAlertDialog(false);
                    }
                    break;
                }
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private SendMessageListener sendMessageListener = new SendMessageListener() {
        @Override
        public void sendMessage(Point point) {
            String message = point.x+","+point.y;
            // Check that we're actually connected before trying anything
            if (mChatService.getState() != BluetoothService.STATE_CONNECTED) {
                showToast(getString(R.string.not_connected));
                return;
            }
            // Check that there's actually something to send
            if (message.length() > 0) {
                // Get the message bytes and tell the BluetoothService to write
                byte[] send = message.getBytes();
                mChatService.write(send);
            }
        }
    };

    private void showToast(String s){
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void showAlertDialog(boolean youWin){
        String mes;
        if(youWin){
            mes = "Ban thang";
        }
        else {
            mes = "Ban thua";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tran dau ket thuc");
        builder.setMessage(mes);
        builder.setCancelable(false);
//        builder.setPositiveButton("Co", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                Toast.makeText(MainActivity.this, "Co", Toast.LENGTH_SHORT).show();
//            }
//        });
        builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //nothing, dismiss alert
                dialogInterface.dismiss();
            }
        });
        builder.setIcon(R.drawable.gameover);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

}
