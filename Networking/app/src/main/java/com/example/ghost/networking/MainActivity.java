package com.example.ghost.networking;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    Button startServer, stopServer, sendbtn;
    Server server;
    TextView serverStatus, clients, clientChat, serverMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectGadgets();
        init();
        gadgetActionListener();
    }

    private void connectGadgets()
    {
        startServer = (Button)findViewById(R.id.StartServer);
        stopServer = (Button)findViewById(R.id.StopServer);
        serverStatus = (TextView)findViewById(R.id.ServerStatus);
        clients = (TextView)findViewById(R.id.Clients);
        clientChat = (TextView)findViewById(R.id.Chat);
        serverMsg = (TextView)findViewById(R.id.ServerMsg);
        sendbtn = (Button)findViewById(R.id.SendBtn);


        sendbtn.setEnabled(false);
        clients.setMovementMethod(new ScrollingMovementMethod());
        clientChat.setMovementMethod(new ScrollingMovementMethod());
    }

    private void init()
    {
        server = new Server(MainActivity.this);
    }

    private void gadgetActionListener()
    {
        startServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startServerAction();
            }
        });

        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                server.sendToClient(serverMsg.getText().toString());
                serverMsg.setText("");
            }
        });

        stopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopServerAction();
            }
        });
    }


    private void startServerAction()
    {
        if(server.startServer(9876))
        {
            serverStatus.setText("Server Online");
            stopServer.setEnabled(true);
            startServer.setEnabled(false);
        }
        else
            serverStatus.setText("Server Offline");
    }

    private void stopServerAction()
    {
        if(server.stop())
        {
            sendbtn.setEnabled(false);
            serverStatus.setText("Server Offline");
            clients.setText("");
            stopServer.setEnabled(false);
            startServer.setEnabled(true);
        }
        else
            serverStatus.setText("Failed To Close Try Again");
        sendbtn.setEnabled(false);
    }

    public void disableWidgets()
    {
        sendbtn.setEnabled(false);
        clientChat.setEnabled(false);
    }

}
