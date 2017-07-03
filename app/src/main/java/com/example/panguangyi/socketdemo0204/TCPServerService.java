package com.example.panguangyi.socketdemo0204;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class TCPServerService extends IntentService {
    private static final String TAG = "TCPServerService";
    public static final String ACTION_START_SERVER = "ACTION_START_SERVER";
    private boolean isServiceDestroyed = false;
    private ServerSocket serverSocket = null;
    private String[] defaultResponseMessages = new String[] {
            "Hello,guys!",
            "Nice to meet you!",
            "hahahaha,you are stupid!",
            "goodbye,see you tomorrow!",
            "just a joke,don't worry,be happy!"
    };
    public TCPServerService() {
        super("TCPServerService");
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"[TCPServerService] onCreate");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"[TCPServerService] onDestroy");
        isServiceDestroyed = true;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,"[TCPServerService] onHandleIntent");
        if (intent != null) {
            final String action = intent.getAction();
            if (action.equals(ACTION_START_SERVER)) {
                handleActionStartServer();
            }
        }
    }

    private void handleActionStartServer() {
        try {
            serverSocket = new ServerSocket(8688);
        } catch (IOException e) {
            Log.d(TAG,"establish tcp server failed,port:8688");
            e.printStackTrace();
        }

        while (!isServiceDestroyed) {
            try {
                final Socket client = serverSocket.accept();
                Log.d(TAG,"server accept");
                new Thread() {
                    @Override
                    public void run() {
                        try{
                            responseClient(client);
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void responseClient(Socket client) throws IOException {
        //用于接收客户端信息
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        //用于向客户端发送信息
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);
        out.println("Welcome to the chat room!");
        while(!isServiceDestroyed) {
            String str = in.readLine();
            Log.d(TAG,"msg from client:" + str);
            if (str == null) {
                //客户端断开连接
                break;
            }
            String msg = getRandomResponseMsg();
            out.println(msg);
            Log.d(TAG,"server send msg:" + msg);
        }
        Log.d(TAG,"client quit.");
        out.close();
        in.close();
        client.close();
    }

    private String getRandomResponseMsg() {
        return defaultResponseMessages[new Random().nextInt(defaultResponseMessages.length)];
    }
}
