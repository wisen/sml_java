package com.example.wanbinwang.smartlogger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
    private static LocalSocket smldSocket;
    private static OutputStream smldOutputStream;
    private static InputStream smldInputStream;
    private boolean isprogressfinish = false;
    private ProgressDialog proDialog = null;

    static final byte ATM_START_SYSTRACE = 0;
    static final byte ATM_FINISH_SYSTRACE = 1;
    static final byte ATM_START_BGREPORT = 2;
    static final byte ATM_FINISH_BGREPORT = 3;
    static final byte ATM_START_LOGCAT = 4;
    static final byte ATM_FINISH_LOGCAT = 5;
    static final byte ATM_START_ALL = 6;
    static final byte ATM_FINISH_ALL = 7;
    static final byte ATM_MAX_CMD = 8;
    private int isfinish_all = 0;

    private void write_cmd(byte CMD, int info){
        ByteBuffer buf = ByteBuffer.allocate(2 * 4);
        buf.putInt(CMD);
        buf.putInt(info);
        for (int i = 0; i < 3; i++) {
            if (smldSocket == null) {
                if (openSmldSocket() == false) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                    continue;
                }
            }

            try {
                smldOutputStream.write(buf.array(), 0, buf.position());
                return;
            } catch (IOException ex) {
                Log.d(TAG, "Error writing to smld socket");

                try {
                    smldSocket.close();
                } catch (IOException ex2) {
                }

                smldSocket = null;
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
                        write_cmd(ATM_FINISH_ALL, 0);
                    }

                    if(smldOutputStream!=null){
                        write_cmd(ATM_FINISH_BGREPORT, 0);
                        isfinish_all += 1;
                    }
                } else if (action_logcat_finish.equals(intent.getAction())){
                    Log.d(TAG, "recv bugreport finish!!");
                    isprogressfinish = true;

                    if (isfinish_all == 3){
                        write_cmd(ATM_FINISH_ALL, 0);
                    }

                    if(smldOutputStream!=null){
                        write_cmd(ATM_FINISH_LOGCAT, 0);
                        isfinish_all += 1;
                    }
                } else if (action_systrace_finish.equals(intent.getAction())){
                    Log.d(TAG, "recv bugreport finish!!");
                    isprogressfinish = true;

                    if (isfinish_all == 3){
                        write_cmd(ATM_FINISH_ALL, 0);
                    }

                    if(smldOutputStream!=null){
                        write_cmd(ATM_FINISH_SYSTRACE, 0);
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

    }

    private static boolean openSmldSocket() {
        try {
            smldSocket = new LocalSocket(LocalSocket.SOCKET_SEQPACKET);
            smldSocket.connect(
                    new LocalSocketAddress("atrace_monitor_sk",
                            LocalSocketAddress.Namespace.RESERVED));
            smldOutputStream = smldSocket.getOutputStream();
            smldInputStream = smldSocket.getInputStream();
        } catch (IOException ex) {
            Log.d(TAG, "smld daemon socket open failed");
            smldSocket = null;
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
}
