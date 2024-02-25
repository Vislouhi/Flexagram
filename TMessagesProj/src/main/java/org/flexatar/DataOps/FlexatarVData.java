package org.flexatar.DataOps;



import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class FlexatarVData {

    public final ByteBuffer markerBB;
    public Map<String, Map<String, List<byte[]>>> flxData = new HashMap<>();
    public FlexatarVData(LengthBasedFlxUnpack dataLB){
        String currentPartName = "exp0";
        Map<String, List<byte[]>> currentPart = new HashMap<>();
        flxData.put(currentPartName,currentPart);

        for (int i = 0; i < dataLB.hPacks.size(); i++) {
            String headerName = dataLB.hPacks.get(i);
            byte[] body = dataLB.bPacks.get(i);
            if (headerName.equals("Delimiter")){
                String str = new String(body, StandardCharsets.UTF_8);
                try {
                    JSONObject jsonObject = new JSONObject(str);
                    currentPartName = jsonObject.getString("type");
                    if (!currentPartName.equals("end")) {
                        currentPart = new HashMap<>();
                        flxData.put(currentPartName, currentPart);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
//                Log.d("====DEB====", " delimiter " + str);

            }else{
                if (currentPart.containsKey(headerName)){
                    currentPart.get(headerName).add(body);
                }else{
                    List<byte[]> dataList = new ArrayList<>();
                    dataList.add(body);
                    currentPart.put(headerName,dataList);

                }
            }
        }
        markerBB = Data.dataToBuffer(flxData.get("exp0").get("markers").get(0));

    }
    public FileDescriptor getVideoFile(){
        String name = "your.package.name-" + UUID.randomUUID();

// Bind a server to the socket.
        final LocalServerSocket server;
        try {
            server = new LocalServerSocket(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

// Connect a client to the socket.
        LocalSocket client = new LocalSocket(LocalSocket.SOCKET_STREAM);
        try {
            client.connect(new LocalSocketAddress(name, LocalSocketAddress.Namespace.ABSTRACT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

// Start a thread to read from the server socket.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalSocket socket = server.accept();
                    byte[] byteArray = flxData.get("exp0").get("video").get(0);
                    Log.d("FLX_INJECT","video fd byteArray "+byteArray.length);
                    OutputStream outputStream = socket.getOutputStream();
                    outputStream.write(byteArray);
                    outputStream.close();
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }


            }
        }).start();




        return client.getFileDescriptor();
    }
}
