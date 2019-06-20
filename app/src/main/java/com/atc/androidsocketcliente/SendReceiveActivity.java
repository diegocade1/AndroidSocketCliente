package com.atc.androidsocketcliente;

import android.content.Context;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class SendReceiveActivity extends AppCompatActivity {

    private String IP;
    private int Port = 0;
    private EditText etIP,etPort;
    private Button btnRecibirArchivo,btnEnviarArchivo;
    private TextView tvInfo;
    private Thread Thread_SendFile,Thread_ReceiveFile = null;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_receive);
        context = this;

        tvInfo = findViewById(R.id.tvInfo2);
        etIP = (EditText) findViewById(R.id.etIP2);
        etIP.setText("192.168.1.135");
        etPort = (EditText) findViewById(R.id.etPort2);
        etPort.setText("100");
        btnRecibirArchivo = (Button) findViewById(R.id.btnRecibirArchivo);
        btnEnviarArchivo = (Button) findViewById(R.id.btnEnviarArchivo);

        ActionButtonEnviarArchivo(btnEnviarArchivo);
        ActionButtonRecibirArchivo(btnRecibirArchivo);
        tvInfo.setText(getIPAddress(true));
    }

    private void ActionButtonEnviarArchivo(Button conectar)
    {
        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IP = etIP.getText().toString();
                String tempPort = etPort.getText().toString().trim();
                Port = TryParse(tempPort);
                if((IP != "")&&(Port != 0))
                {
                    etIP.setEnabled(false);
                    etPort.setEnabled(false);
                    Thread_SendFile = new Thread(new Thread_SendFile());
                    Thread_SendFile.start();
                }
            }
        });
    }

    private void ActionButtonRecibirArchivo(Button conectar)
    {
        conectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IP = etIP.getText().toString();
                String tempPort = etPort.getText().toString().trim();
                Port = TryParse(tempPort);
                if((IP != "")&&(Port != 0))
                {
                    etIP.setEnabled(false);
                    etPort.setEnabled(false);
                    Thread_ReceiveFile = new Thread(new Thread_ReceiveFile());
                    Thread_ReceiveFile.start();
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

    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    /*---------------------------------Threads --------------------------------------------------*/

    private static BufferedReader input;
    private static PrintWriter output;
    private Socket socket;

    class Thread_SendFile implements Runnable
    {
        @Override
        public void run() {
            try
            {
                socket = new Socket();
                SocketAddress direccion = new InetSocketAddress(IP, Port);
                socket.connect(direccion,5*1000);
                File file = new File(
                        Environment.getExternalStorageDirectory(),
                        "Maestro.txt");

                byte[] bytes = new byte[(int) file.length()];
                BufferedInputStream bis;

                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(bytes, 0, bytes.length);
                OutputStream os = socket.getOutputStream();
                os.write(bytes, 0, bytes.length);
                os.flush();
                os.close();
                socket.close();

                final String sentMsg = "Archivo enviado a: " + socket.getInetAddress();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, sentMsg, Toast.LENGTH_LONG).show();
                        Thread_SendFile.interrupt();
                        etIP.setEnabled(true);
                        etPort.setEnabled(true);
                    }
                });
            }
            catch (IOException e)
            {
                e.printStackTrace();
                showError(e.getMessage());
                enabledEditText();
            }
        }
    }

    class Thread_ReceiveFile implements Runnable
    {
        private String mensaje;
        @Override
        public void run() {

                try
                {
                    int actual = 0;
                    socket = new Socket();
                    SocketAddress direccion = new InetSocketAddress(IP, Port);
                    socket.connect(direccion,5*1000);
                    InputStream is = socket.getInputStream();
                    final File file = new File(Environment.getExternalStorageDirectory(),"Maestro.txt");
                    byte[] bytes = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int byteRead;

                    while((byteRead = is.read(bytes,0,bytes.length)) != -1)
                    {
                        bos.write(bytes,0,byteRead);
                    }

                    bos.flush();
                    bos.close();
                    socket.close();
                    mensaje = "Archivo Recibido";

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                                Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show();
                                Thread_ReceiveFile.interrupt();
                            etIP.setEnabled(true);
                            etPort.setEnabled(true);
                        }
                    });
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    showError(e.getMessage());
                    enabledEditText();
                }
            }
    }



    private void showError(final String message) {
        ((SendReceiveActivity)context).runOnUiThread(new Runnable() {
            public void run() {

                new AlertDialog.Builder(context)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Ok", null)
                        .show();
            }
        });
    }

    private void enabledEditText() {
        ((SendReceiveActivity)context).runOnUiThread(new Runnable() {
            public void run() {

                etIP.setEnabled(true);
                etPort.setEnabled(true);
            }
        });
    }
}
