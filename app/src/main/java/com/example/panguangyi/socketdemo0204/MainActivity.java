package com.example.panguangyi.socketdemo0204;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static final int RECEIVE_NEW_MESSAGE = 1;
    public static final int SOCKET_CONNECTED = 2;

    private PrintWriter mPrintWriter;
    private Socket mClient;
    TextView tvInfo;
    EditText etMsg;
    Button btnSend;

    private static class SafeHandle extends Handler {
        private WeakReference<Context> mRef;
        public SafeHandle(Context context){
            mRef = new WeakReference<Context>(context);
        }
    }

    private SafeHandle mHandle = new SafeHandle(this){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case RECEIVE_NEW_MESSAGE:
                    tvInfo.setText(tvInfo.getText().toString() + (String)msg.obj);
                    break;
                case SOCKET_CONNECTED:
                    btnSend.setEnabled(true);
                    break;
                default:break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        etMsg = (EditText) findViewById(R.id.etMsg);
        btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String msg = etMsg.getText().toString();
                if (!TextUtils.isEmpty(msg) && mPrintWriter != null){
                    new Thread(){
                        @Override
                        public void run() {
                            mPrintWriter.println(msg);
                        }
                    }.start();
                    etMsg.setText("");
                    String time = formateDateTime(System.currentTimeMillis());
                    final String info = "client " + time + ":" + msg + "\n";
                    tvInfo.setText(tvInfo.getText().toString() + info);
                }
            }
        });
        Intent service = new Intent(this,TCPServerService.class);
        service.setAction(TCPServerService.ACTION_START_SERVER);
        startService(service);
        new Thread() {
            @Override
            public void run() {
                connectTCPServer();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (mClient != null) {
            try {
                mClient.shutdownInput();
                mClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    private String formateDateTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(new Date(time));
    }

    private void connectTCPServer() {
        Socket socket = null;
        while (socket == null) {
            try {
                socket = new Socket("localhost",8688);
                mClient = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                mHandle.sendEmptyMessage(SOCKET_CONNECTED);
                Log.d(TAG,"connect server successful");
            } catch (IOException e) {
                SystemClock.sleep(1000);
                Log.d(TAG,"connect tcp server failed.retrying...");
                e.printStackTrace();
            }
        }

        //接收服务器端响应
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while(!MainActivity.this.isFinishing()){
                String msg = br.readLine();
                Log.d(TAG,"receive msg:" + msg);
                if (msg != null){
                    String time = formateDateTime(System.currentTimeMillis());
                    final String info = "server " + time + ":" + msg + "\n";
                    mHandle.obtainMessage(RECEIVE_NEW_MESSAGE,info).sendToTarget();
                }
            }
            Log.d(TAG,"quit...");
            mPrintWriter.close();
            br.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
