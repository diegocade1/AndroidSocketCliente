package com.atc.androidsocketcliente;

import android.app.Activity;
import android.content.Context;
import android.drm.DrmStore;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity
{

    private String IP;
    private int Port = 0;
    private EditText etIP,etPort,etTexto;
    private Button btnConnect, btnEnviar,btnDisconnect;
    private TextView tvConsole;
    private Thread Thread1 = null;
    private Context context;
    private boolean isThread1Active;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        etIP = (EditText) findViewById(R.id.etIP);
        etTexto = (EditText) findViewById(R.id.etTexto);
        etPort = (EditText) findViewById(R.id.etPort);
        tvConsole = (TextView) findViewById(R.id.tvConsole);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnEnviar = (Button) findViewById(R.id.btnEnviar);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        ActionButtonConectar(btnConnect);
        ActionButtonEnviar(btnEnviar);
        ActionButtonDesconectar(btnDisconnect);
    }

    private void ActionButtonConectar(Button conectar)
    {
        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IP = etIP.getText().toString();
                String tempPort = etPort.getText().toString().trim();
                Port = TryParse(tempPort);
                if((IP != "")&&(Port != 0))
                {
                    Thread1 = new Thread(new Thread1());
                    Thread1.start();
                }
            }
        });
    }

    private void ActionButtonEnviar(Button conectar)
    {
        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mensaje = etTexto.getText().toString();
                if(mensaje != null)
                {
                    new Thread(new Thread3(mensaje)).start();
                }
            }
        });
    }

    private void ActionButtonDesconectar(Button conectar)
    {
        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    tvConsole.setText("");
                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    socket.close();
                    Thread1.interrupt();
                    isThread1Active = false;
                    etIP.setEnabled(true);
                    etPort.setEnabled(true);
                    etTexto.setEnabled(false);
                    btnEnviar.setEnabled(false);
                    btnDisconnect.setVisibility(View.GONE);
                    btnConnect.setVisibility(View.VISIBLE);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    //showError(e.getMessage());
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void ShowMensage(String titulo, String mensage)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(titulo);
        builder.setMessage(mensage);
        builder.show();
    }

    private int TryParse(String string)
    {
        if(isParsable(string))
        {
            return Integer.valueOf(string);
        }
            else
        {
         return 0;
        }
    }

    boolean isParsable(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /*---------------------------------Thread 1--------------------------------------------------*/

    private static BufferedReader input;
    private static PrintWriter output;
    private Socket socket;

    class Thread1 implements Runnable
    {
        @Override
        public void run() {
            try
            {
                socket = new Socket();
                SocketAddress direccion = new InetSocketAddress(IP, Port);
                socket.connect(direccion,5*1000);
                output = new PrintWriter(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                isThread1Active = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvConsole.setText("Connected\r\n");
                        etIP.setEnabled(false);
                        etPort.setEnabled(false);
                        etTexto.setEnabled(true);
                        btnEnviar.setEnabled(true);
                        btnDisconnect.setVisibility(View.VISIBLE);
                        btnConnect.setVisibility(View.GONE);
                    }
                });
                    new Thread(new Thread2()).start();
            }
            catch (IOException e)
            {
                isThread1Active = false;
                e.printStackTrace();
                showError(e.getMessage());
                Thread1.interrupt();
            }

        }
    }

    class Thread2 implements Runnable
    {
        @Override
        public void run() {
            while(isThread1Active)
            {
                try
                {
                        final String mensaje = input.readLine();
                        if(mensaje!= null)
                        {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvConsole.append("Server: "+ mensaje + "\r\n");
                                }
                            });
                        }
                        else
                        {
                            Thread1 = new Thread(new Thread1());
                            Thread1.start();
                        }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    //showError(e.getMessage());
                }
            }
        }
    }

    class Thread3 implements Runnable
    {
        private String mensaje;
        Thread3 (String menj)
        {
            this.mensaje = menj;
        }
        @Override
        public void run() {
            try
            {
                output.write(mensaje);
                output.flush();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvConsole.append("Client: " + mensaje +"\r\n");
                        etTexto.setText("");
                    }
                });
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

        }
    }

    private void showError(final String message) {
        ((MainActivity)context).runOnUiThread(new Runnable() {
            public void run() {

                new AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Ok", null)
                        .show();
            }
        });
    }

}
