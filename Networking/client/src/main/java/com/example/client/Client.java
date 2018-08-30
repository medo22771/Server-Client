package com.example.client;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by ghost on 11-Aug-18.
 */

public class Client extends AsyncTask<Void, Void, Void>
{
    MainActivity mainActivity;
    String dstAddress;
    int dstPort;
    String serverMsg;
    String response = "";
    ObjectInputStream input;
    ObjectOutputStream output;
    Socket connection;


    public Client(String addr, int port, MainActivity mActivity)
    {
        this.dstAddress = addr;
        this.dstPort = port;
        this.mainActivity = mActivity;
    }

    public void connect()
    {
        try
        {
            connection = new Socket(dstAddress, dstPort);
            Log.i("Client", "Connecte d To: " + connection.getInetAddress().getHostName());
            setupStreams();
        }
        catch (Exception e)
        {
            Log.i("Client", "Client Failed To Connect To Server: " + e);
        }
    }

    public void setupStreams()
    {
        try
        {
            input = new ObjectInputStream(connection.getInputStream());
            output = new ObjectOutputStream(connection.getOutputStream());
            Log.i("Client", "Done Setup Streams");
            whileChatting();
        }
        catch (Exception e)
        {
            Log.i("Client", "Failed To Setup Streams");
        }
    }

    public void whileChatting()
    {
        try
        {
            serverMsg = (String)input.readObject();
            Log.i("Client", "Server Said: "+ serverMsg);
        }
        catch (Exception e)
        {
            Log.i("Client", "Error While Chatting");
        }
    }

    public void sendToServer(String msg)
    {
        try
        {
            output.writeObject(msg);
            output.flush();
            Log.i("Client", "Message Sent To Server");
        }
        catch (Exception e)
        {
            Log.i("Client", "Couldn't Send Msg To Server: "+ e);
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Socket clientSocket;

        try {
            clientSocket = new Socket(dstAddress, dstPort);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
                    1024);
            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream inputStream = clientSocket.getInputStream();

			/*
             * notice: inputStream.read() will block if no data return
			 */
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                response += byteArrayOutputStream.toString("UTF-8");
            }
        }
        catch (Exception e)
        {
            Log.i("Client", "Error: " + e);
            response = e.toString();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        //textResponse.setText(response);
        super.onPostExecute(result);
    }
}
