package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class FlexatarServerAccess {
    public static void makeFlexatarRequest(byte[] sendData){
        try {
            URL url = new URL("https://26ntp3aaifsmiiz2qxoi4ynboe0ncqby.lambda-url.us-east-1.on.aws/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Authorization","Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMjA1OTY2MjIsIm5iZiI6MTcwNTIzNjYyMiwiVGFnIjoibm9uZSIsIlVzZXIiOiJkZWZhdWx0IiwiQXBwIjoidGctYW5yb2lkIiwiVmVyIjoiMS4wIn0._PxtlSUHWk0TDmz7Q84V-c4oxfDuSYXiWCDkwqwJXE4");
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(sendData);
            outputStream.close();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                /*ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                byteArrayOutputStream.close();
                inputStream.close();
                connection.disconnect();*/
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                // Read the response line by line
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Close the reader
                reader.close();

                // Now 'response' contains the JSON data
                String jsonResponse = response.toString();

                // Do something with the JSON data
                Log.d("FLX_INJECT","Received JSON: " + jsonResponse);
//                Log.d("FLX_INJECT","Server responce :" + new String(buffer, StandardCharsets.UTF_8));
            }else{
                Log.d("FLX_INJECT","Server responce error :"+responseCode );
            }
        } catch (IOException e) {
//            return null;
//            throw new RuntimeException(e);
        }
//        return null;
    }
}
