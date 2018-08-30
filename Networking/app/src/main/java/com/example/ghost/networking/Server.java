package com.example.ghost.networking;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by ghost on 11-Aug-18.
 */

public class Server
{
    private MainActivity mainActivity;
    private int port;
    private String fromClientMsg;
    private String clientInfo;
    private boolean ServerShutdownReq;
    private boolean serverStartedStatus;
    private boolean isClientConnected;
    private ServerSocket serverSocket = null;
    private Socket currClientSocket = null;
    private Lock threadLocker = new ReentrantLock();
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private final Object wait_notify = new Object();

    public Server(MainActivity activity)
    {
        this.mainActivity = activity;
        this.ServerShutdownReq = false;
        this.serverStartedStatus = false;
        this.isClientConnected = false;
    }

    public boolean startServer(int serverPort)
    {
        this.port = serverPort;
        Thread serverThread = new Thread(new SocketServerThread());
        serverThread.start();
        synchronized (wait_notify)
        {
            try
            {
                if(!ServerShutdownReq)
                {
                    Log.i("Server", "Waiting 3 Seconds For Server To Launch...");
                    wait_notify.wait(3000);
                }
            }
            catch (Exception e)
            {
                Log.i("Server", "Synchronized wait Error: " + e);
            }
        }

        Log.i("Server", "Server Launched: " + serverStartedStatus);
        return serverStartedStatus;
    }

    public void sendToClient(final String msgToClient)
    {
        Thread serverWriteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    if(isClientConnected && !currClientSocket.isClosed())
                    {
                        output.writeObject(msgToClient);
                        output.flush();
                        toChatWindow("Server: ", msgToClient);
                        Log.i("Server", "Msg Sent To Client Successfully" );
                    }
                    else
                        Log.i("Server", "Sending Failed Not Connected To Client");
                }
                catch (Exception e)
                {
                    Log.i("Server", "Writing Error: " + e);
                }
            }
        });
        serverWriteThread.start();
    }

    private void toChatWindow(final String ClientOrServer, final String msgToChat)
    {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.clientChat.append(ClientOrServer + msgToChat + "\n");
            }
        });
    }

    public boolean stop()
    {
        Log.i("Server", "Shutting Down");
        try
        {
            ServerShutdownReq = true;
            serverSocket.close();   //the associated Socket and Streams will close
            Log.i("Server", "Server Closed Successfully");
            return true;
        }
        catch (Exception e)
        {
            Log.i("Server", "Server Failed To Close: " + e);
            return false;
        }
    }

    public class SocketServerThread extends Thread
    {

        @Override
        public void run() {
            try
            {
                serverSocket = new ServerSocket(port);
                serverStartedStatus = true;
                ServerShutdownReq = false;
                synchronized (wait_notify)
                {
                    Log.i("Server", "Notifying Main Thread To Take Positive Action");
                    wait_notify.notify();
                }
                while(serverStartedStatus && !ServerShutdownReq)
                {
                    try
                    {
                        if(!waitForConnection() || !setupStreams())
                            break;
                        readFromClient();
                    }
                    catch (Exception e)
                    {
                        Log.i("Server", "Client Socket Error: " + e);
                    }
                }
                Server.this.stop();
            }
            catch (Exception e)
            {
                synchronized (wait_notify)
                {
                    Log.i("Server", "Notifying Main Thread To Take Negative Action");
                    wait_notify.notify();
                }
                Log.i("Server", "Server Error: " + e);
            }
        }

        private boolean waitForConnection()
        {
            try
            {
                isClientConnected = false;
                currClientSocket = serverSocket.accept();
                isClientConnected = true;
                String clientHostName = "";
                String clientIpAddress = currClientSocket.getLocalAddress().getHostAddress();
                int clientPort = currClientSocket.getPort();
                clientInfo = "Client: " + clientHostName + ", IP: " + clientIpAddress + ", Port: " + clientPort;
                Log.i("Server", clientInfo + " Connected");
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.clients.append(clientInfo + " Connected\n");
                    }
                });
                return true;
            }
            catch (Exception e)
            {
                if(ServerShutdownReq && e.getClass() == SocketException.class)
                    Log.i("Server", "Client Socket Is Closing: " + e);
                Log.i("Server", "Accepting Connection Error: " + e);
                return false;
            }
        }

        //Streams Should Be Declared In Order Output Then Input
        private boolean setupStreams()
        {
            try
            {
                output = new ObjectOutputStream(currClientSocket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(currClientSocket.getInputStream());
                Log.i("Server", "Streams Setup Successfully");
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.sendbtn.setEnabled(true);
                    }
                });
                return true;
            }
            catch (Exception e)
            {
                Log.i("Server", "Streams Setup Error: " + e);
                clientDisconnected();
                return false;
            }
        }

        private void readFromClient()
        {
            while (isClientConnected && !ServerShutdownReq)
            {
                try
                {
                    Log.i("Server", "Trying To Read Client Message...");
                    fromClientMsg = (String)input.readObject();
                    toChatWindow("Client: ", fromClientMsg);
                    Log.i("Server", "Message Received Successfully: " + fromClientMsg);
                }
                catch (Exception e)
                {
                    if(e.getClass() == EOFException.class)
                    {
                        Log.i("Server", "Client Disconnected");
                        clientDisconnected();
                        isClientConnected = false;

                    }
                    else
                        Log.i("Server", "Reading From Client Error: " + e);
                }
            }
        }
        private void clientDisconnected()
        {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mainActivity.clients.append(clientInfo + " Disconnected\n");
                    mainActivity.disableWidgets();
                }
            });
        }
    }
}

