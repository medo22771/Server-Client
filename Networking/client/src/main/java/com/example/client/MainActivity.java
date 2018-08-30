package com.example.client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    Button ConnectBtn, DisconnectBtn, SendBtn;
    TextView clientStatus, chat;
    EditText clientInput, serverIp;
    Client2 Ahmed;
    String dstAddress = "";
    int dstPort = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectGadgets();
        init();
        gadgetActions();
    }

    public void connectGadgets()
    {
        ConnectBtn = (Button)findViewById(R.id.ClientConnect) ;
        DisconnectBtn = (Button)findViewById(R.id.ClientDisconnect);
        clientStatus = (TextView)findViewById(R.id.ClientStatus);
        chat = (TextView)findViewById(R.id.Chat);
        clientInput = (EditText)findViewById(R.id.ClientInput);
        SendBtn = (Button)findViewById(R.id.SendBtn);
        serverIp = (EditText)findViewById(R.id.ServerIp);

        chat.setMovementMethod(new ScrollingMovementMethod());
    }

    public void init()
    {
        Ahmed = new Client2(MainActivity.this);
        ConnectBtn.setEnabled(true);
        DisconnectBtn.setEnabled(false);
    }

    public void gadgetActions()
    {
        ConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToServerAction();
            }
        });

        SendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    Ahmed.sendToServer(clientInput.getText().toString());
                    clientInput.setText("");
            }
        });

        DisconnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnectFromServerAction();
            }
        });
    }

    private void connectToServerAction()
    {
        dstAddress = serverIp.getText().toString();
        dstPort = 9876;
        if(!dstAddress.isEmpty())
        {
            if(Ahmed.connect(dstAddress, dstPort))
            {
                Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                clientInput.setEnabled(true);
                ConnectBtn.setEnabled(false);
                DisconnectBtn.setEnabled(true);
                SendBtn.setEnabled(true);
            }
            else
                Toast.makeText(MainActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(MainActivity.this, "Enter Server Ip", Toast.LENGTH_SHORT).show();
    }

    public void disconnectFromServerAction()
    {
        if(Ahmed.disconnect())
        {
            clientStatus.setText("Not Connected");
            ConnectBtn.setEnabled(true);
            DisconnectBtn.setEnabled(false);
            SendBtn.setEnabled(false);
        }
        else
            clientStatus.setText("Failed To Disconnect Try Again");
        clientInput.setEnabled(false);
    }



}
