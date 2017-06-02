package com.example.wanbinwang.smartlogger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private BroadcastReceiver broadcastReceiver;
    private final String action_bugreport_finish = "android.intent.action.BUGREPORT_FINISHED";
    private final String action_systrace_finish = "android.intent.action.SYSTRACE_FINISHED";
    private final String action_logcat_finish = "android.intent.action.LOGCAT_FINISHED";
    //private final String action_bugreport_finish = "android.intent.action.BUGREPORT_FINISHED";
    private static final String TAG = "SML:Main";
    private Button btn_bugreport;
    private Button btn_systrace;
    private Button btn_logcat;
    private Button btn_all;
    private Button btn_wrsk;
    private static LocalSocket smldCmdSocket;
    private static OutputStream smldCmdOutputStream;
    private static InputStream smldCmdInputStream;
    private static LocalSocket smldRspSocket;
    private static OutputStream smldRspOutputStream;
    //private static InputStream smldInputStream;
    private boolean isprogressfinish = false;
    private ProgressDialog proDialog = null;

    static final byte ATM_START_SYSTRACE = 0;
    static final byte ATM_START_BGREPORT = 1;
    static final byte ATM_START_LOGCAT = 2;
    static final byte ATM_START_ALL = 3;
    static final byte ATM_GET_WRSK = 4;
    static final byte ATM_ENABLE_WRSK = 5;
    static final byte ATM_MAX_CMD = 6;

    static final byte ATM_FINISH_SYSTRACE = 0;
    static final byte ATM_FINISH_BGREPORT = 1;
    static final byte ATM_FINISH_LOGCAT = 2;
    static final byte ATM_FINISH_ALL = 3;
    static final byte ATM_MAX_RSP = 4;
    private int isfinish_all = 0;

    private LocalSocket receiver;
    private LocalServerSocket lss;
    private static final int BUFFER_SIZE = 8;
    private final String JSsocketName = "atrace_monitor_wr_sk";
    private boolean isexit = false;

    private void write_rsp(byte CMD, int info){
        ByteBuffer buf = ByteBuffer.allocate(2 * 4);
        buf.putInt(CMD);
        buf.putInt(info);
        for (int i = 0; i < 3; i++) {
            if (smldRspSocket == null) {
                if (openSmldRspSocket() == false) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                    continue;
                }
            }

            try {
                smldRspOutputStream.write(buf.array(), 0, buf.position());
                return;
            } catch (IOException ex) {
                Log.d(TAG, "Error writing to smld socket");

                try {
                    smldRspSocket.close();
                } catch (IOException ex2) {
                }

                smldRspSocket = null;
            }
        }
    }

    private void write_cmd(byte CMD, int info){
        ByteBuffer buf = ByteBuffer.allocate(2 * 4);
        buf.putInt(CMD);
        buf.putInt(info);
        for (int i = 0; i < 3; i++) {
            if (smldCmdSocket == null) {
                if (openSmldCmdSocket() == false) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                    continue;
                }
            }

            try {
                smldCmdOutputStream.write(buf.array(), 0, buf.position());
                return;
            } catch (IOException ex) {
                Log.d(TAG, "Error writing to smld socket");

                try {
                    smldCmdSocket.close();
                } catch (IOException ex2) {
                }

                smldCmdSocket = null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (action_bugreport_finish.equals(intent.getAction())) {
                    Log.d(TAG, "recv bugreport finish!!");
                    isprogressfinish = true;

                    if (isfinish_all == 3){
                        write_rsp(ATM_FINISH_ALL, 0);
                    }

                    if(smldCmdOutputStream!=null){
                        write_rsp(ATM_FINISH_BGREPORT, 0);
                        isfinish_all += 1;
                    }
                } else if (action_logcat_finish.equals(intent.getAction())){
                    Log.d(TAG, "recv bugreport finish!!");
                    isprogressfinish = true;

                    if (isfinish_all == 3){
                        write_rsp(ATM_FINISH_ALL, 0);
                    }

                    if(smldCmdOutputStream!=null){
                        write_rsp(ATM_FINISH_LOGCAT, 0);
                        isfinish_all += 1;
                    }
                } else if (action_systrace_finish.equals(intent.getAction())){
                    Log.d(TAG, "recv bugreport finish!!");
                    isprogressfinish = true;

                    if (isfinish_all == 3){
                        write_rsp(ATM_FINISH_ALL, 0);
                    }

                    if(smldCmdOutputStream!=null){
                        write_rsp(ATM_FINISH_SYSTRACE, 0);
                        isfinish_all += 1;
                    }
                }
            }
        };

        IntentFilter smlFilter = new IntentFilter();
        smlFilter.addAction(action_bugreport_finish);
        smlFilter.addAction(action_systrace_finish);
        smlFilter.addAction(action_logcat_finish);

        if (broadcastReceiver!=null && smlFilter!=null) {
            registerReceiver(broadcastReceiver,smlFilter);
        }

        btn_bugreport = (Button) findViewById(R.id.btn_bugreport);
        btn_bugreport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write_cmd(ATM_START_BGREPORT, 0);
                isprogressfinish = true;
                show_progressDialog("Bugreport capturing...");
            }
        });

        btn_systrace = (Button) findViewById(R.id.btn_systrace);
        btn_systrace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write_cmd(ATM_START_SYSTRACE, 0);
                isprogressfinish = true;
                show_progressDialog("systrace capturing...");
            }
        });

        btn_logcat = (Button) findViewById(R.id.btn_logcat);
        btn_logcat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write_cmd(ATM_START_LOGCAT, 0);
                isprogressfinish = true;
                show_progressDialog("logcat buffer capturing...");
            }
        });

        btn_all = (Button) findViewById(R.id.btn_all);
        btn_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write_cmd(ATM_START_ALL, 0);
                isprogressfinish = true;
                show_progressDialog("all log capturing...");
            }
        });

        btn_wrsk = (Button) findViewById(R.id.btn_wrsk);
        btn_wrsk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write_cmd(ATM_ENABLE_WRSK, 0);
            }
        });

        new Thread (local_receive).start();
    }

    private static boolean openSmldCmdSocket() {
        try {
            smldCmdSocket = new LocalSocket(LocalSocket.SOCKET_SEQPACKET);
            smldCmdSocket.connect(
                    new LocalSocketAddress("atrace_monitor_cmd_sk",
                            LocalSocketAddress.Namespace.RESERVED));
            smldCmdOutputStream = smldCmdSocket.getOutputStream();
            smldCmdInputStream = smldCmdSocket.getInputStream();
        } catch (IOException ex) {
            Log.d(TAG, "smld daemon socket open failed");
            smldCmdSocket = null;
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    private static boolean openSmldRspSocket() {
        try {
            smldRspSocket = new LocalSocket(LocalSocket.SOCKET_SEQPACKET);
            smldRspSocket.connect(
                    new LocalSocketAddress("atrace_monitor_rsp_sk",
                            LocalSocketAddress.Namespace.RESERVED));
            smldRspOutputStream = smldRspSocket.getOutputStream();
            //smldInputStream = smldRspSocket.getInputStream();
        } catch (IOException ex) {
            Log.d(TAG, "smld daemon socket open failed");
            smldRspSocket = null;
            ex.printStackTrace();
            return false;
        }

        return true;
    }

    private void show_progressDialog(String title){
        proDialog = android.app.ProgressDialog.show(MainActivity.this, title, "Please pay attention...");
        Thread thread = new Thread() {
            public void run()
            {
                while(!isprogressfinish);
                if (isprogressfinish) proDialog.dismiss();
            }
        };
        thread.start();
    }

    Thread local_receive = new Thread(){
        public void run(){

            Log.d(TAG, "local_receive run.....");

            try {
                lss = new LocalServerSocket(JSsocketName);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                receiver = lss.accept();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            try {
                receiver.setReceiveBufferSize(4);
            } catch (IOException e) {
                e.printStackTrace();
            }

            int receiveLen = 16;
            InputStream m_Rece = null;

            try {
                m_Rece = receiver.getInputStream();
                byte[] data;
                while(true) {
                    if (receiveLen > 0) {
                        data = new byte[receiveLen];
                        m_Rece.read(data);
                        Log.d(TAG, "receiveLen: " + receiveLen + " --- "+new String(data) + " ---");
                        Log.d(TAG, "cmd = " + byte2int(data));
                    }
                    Thread.sleep(500);
                    if(isexit){
                        m_Rece.close();
                        receiver.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isexit = true;

        if (receiver != null) try {
            receiver.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(lss != null) try {
            lss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] int2byte(int res) {
        byte[] targets = new byte[4];

        targets[0] = (byte) (res & 0xff);
        targets[1] = (byte) ((res >> 8) & 0xff);
        targets[2] = (byte) ((res >> 16) & 0xff);
        targets[3] = (byte) (res >>> 24);
        return targets;
    }

    public static int byte2int(byte[] res) {

        int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) | ((res[2] << 24) >>> 8) | (res[3] << 24);
        return targets;
    }
}
