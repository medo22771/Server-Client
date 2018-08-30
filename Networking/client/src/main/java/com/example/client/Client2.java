package com.example.client;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.Lock;

/**
 * Created by ghost on 13-Aug-18.
 */

public class Client2 extends Thread
{

    private MainActivity mainActivity;
    private String serverAddress = null;
    private int serverPort = 0;
    private boolean serverOnline;
    private String fromServerMsg = "";
    private String ErrorMsg = "";
    String determineFreeIP = "";
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private boolean connectedToServer;
    private boolean clientShutdownReq;
    private Socket mainClientSocket;
    private Thread clientReadThread;
    private final Object wait_notify = new Object();


    public Client2(MainActivity mActivity)
    {
        this.mainActivity = mActivity;
    }

    public boolean connect(String addr, int port)
    {
        this.serverAddress = addr;
        this.serverPort = port;
        connectedToServer = false;
        clientShutdownReq = false;
        clientReadThread = new Thread(new SocketClientThread());
        clientReadThread.start();
        synchronized (wait_notify)
        {
            try
            {
                if(!connectedToServer)
                {
                    Log.i("Client2", "Waiting 3 Seconds For Client To Connect To Server...");
                    wait_notify.wait(3000);
                }
            }
            catch (Exception ie)
            {
                Log.i("Client2", "Synchronized wait Error: " + ie);
            }
        }
        Log.i("Client2", "Connected To Server: " + connectedToServer);
        return connectedToServer;
    }

    public void sendToServer(String msgToServer)
    {
        Thread clientWriteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    if(connectedToServer && !mainClientSocket.isClosed())
                    {
                        output.writeObject(msgToServer);
                        output.flush();
                        toChatWindow("Client: " + msgToServer);
                        Log.i("Client2", "Message Sent To Server Successfully");
                    }
                    else
                        Log.i("Client2", "Sending Failed Not Connected To Server");
                }
                catch (Exception e)
                {
                    Log.i("Client2", "Writing To Server Error: " + e);
                    runDisconnectFromMain();
                }
            }
        });
        clientWriteThread.start();
    }

    private void toChatWindow(final String msgToChat)
    {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.chat.append(msgToChat + "\n");
            }
        });
    }
    
    private void runDisconnectFromMain()
    {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.disconnectFromServerAction();
            }
        });
    }

    public boolean disconnect()
    {
        Log.i("Client2", "Disconnecting");
        try
        {
            clientShutdownReq = true;
            clientReadThread.interrupt();
            mainClientSocket.close();   //the associated streams will close
            return true;
        }
        catch (Exception e)
        {
            Log.i("Client2", "Client Failed To Close");
            return false;
        }
    }

    public String checkIpAvailable(String IpAddr)
    {
        Thread thr = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    if(InetAddress.getByName(IpAddr).isReachable(3000))
                    {
                        Log.i("Client2", "Ip Address (HOST)Is Available");
                        determineFreeIP = IpAddr;
                    }
                    else
                    {
                        Log.i("Client2", "Ip Address (HOST) Is Not Available");
                        determineFreeIP = "Not Available IP";
                    }
                }
                catch (Exception e)
                {
                    if(e.getClass() == IOException.class)
                        Log.i("Client2", "Network Error: " + e);
                    else if(e.getClass() == IllegalArgumentException.class)
                        Log.i("Client2", "Timeout: " + e);
                    else
                        Log.i("Client2", "Unknown Error" + e);
                }
                synchronized (wait_notify)
                {
                    Log.i("Client2", "Notifying Main Thread");
                    wait_notify.notify();
                }
            }
        });
        thr.start();
        synchronized (wait_notify)
        {
            try
            {
                Log.i("Client2", "Checking If IP Is Free....");
                wait_notify.wait(3000);
            }
            catch (Exception e)
            {
                Log.i("Client2", "Checking IP Error: " + e);
            }
        }
        return determineFreeIP;
    }

    private class SocketClientThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                if(clientShutdownReq || !connectToServer() || !setupStreams())
                    return;

                if(!clientShutdownReq || !connectedToServer)
                    readFromServer();
            }
            catch (Exception e)
            {
                Log.i("Client2", "Error: " + e);
                runDisconnectFromMain();
            }
        }

        private boolean connectToServer()
        {
            try
            {
                mainClientSocket = new Socket(serverAddress, serverPort);
                final String serverHostName = mainClientSocket.getInetAddress().getHostName();
                final int serverConnectedToPort = mainClientSocket.getPort();
                connectedToServer = true;
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.clientStatus.setText("Connected To: " + serverHostName + ":" + serverConnectedToPort);
                    }
                });
                Log.i("Client2", "Client2 Connected Successfully: " + mainClientSocket.getInetAddress().getHostName());
                synchronized (wait_notify)
                {
                    Log.i("Client2", "Notifying Main Thread To Take Positive Action");
                    wait_notify.notify();
                }
                return true;
            }
            catch (Exception e)
            {
                connectedToServer = false;
                synchronized (wait_notify)
                {
                    Log.i("Client2", "Notifying Main Thread To Take Negative Action");
                    wait_notify.notify();
                }
                if(e.getClass() == ConnectException.class)
                    Log.i("Client2", "Can't Connect To Server: No Internet Connection");
                else
                    Log.i("Client2", "Can't Connect To Server: " + e);
                return false;
            }
        }

        private boolean setupStreams()
        {
            try
            {
                output = new ObjectOutputStream(mainClientSocket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(mainClientSocket.getInputStream());
                Log.i("Client2", "Setting Up The Streams Successful");
                return true;
            }
            catch (Exception e)
            {
                Log.i("Client2", "Error Setting Up The Streams: " + e);
                runDisconnectFromMain();
                return false;
            }
        }

        private void readFromServer()
        {
            while (!clientShutdownReq && connectedToServer && !mainClientSocket.isClosed())
            {
                try
                {
                    if(Thread.interrupted())
                        throw new InterruptedException();
                    Log.i("Client2", "Trying To Read Server Message... ");
                    fromServerMsg = (String)input.readObject();
                    toChatWindow("Server: " + fromServerMsg);
                    Log.i("Client2", "Message Received Successfully: " + fromServerMsg);
                }
                catch (Exception e)
                {
                    Log.i("Client2", "Message Receiving Error: " + e);
                    connectedToServer = false;

                    if(e.getClass() == SocketException.class)       //Means Socket is Closed
                        Log.i("Client2", "Socket Closed Disconnecting...");
                    else if(e.getClass() == EOFException.class)     //EOF Means Server Is Off
                        runDisconnectFromMain();
                    else if (e.getClass() == InterruptedException.class)
                        Log.i("Client2", "Reading Interrupted");
                }
            }
        }
    }

























}
