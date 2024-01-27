package org.flexatar;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LocaleDicts {
    public static JSONObject getLocaleData(Context context, String shortName){
        String filePath = "flexatar/locale/";
        try {
            InputStream inputStream;
            try {
                inputStream = context.getAssets().open(filePath+shortName + ".json");
            } catch (IOException e) {
                inputStream = context.getAssets().open( filePath+"en.json");
            }

            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            return new JSONObject(new String(buffer, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            return null;
        }
    }
    /*

I have following json

{
  "FlexatarMenuName": "Flexatar",
  "NewFlexatarByPhoto": "New Flexatar by Photo",
  "FlexatarFromCloud": "Load Flexatar From Cloud",
  "FlexatarCapture": "Flexatar Capture",
  "FlexatarsInProgress": "Flexatars in progress",
  "FlexatarsAvailableForCalls": "Flexatars available for calls",
  "Instructions": "Instructions",
  "MakeFlexatarHelp": "1. Take photos turning your head as assistant shows.\\n2. Don\\'t turn your head too much.\\n3. Keep your mouth closed.",
  "DontShowAgain": "Don\\'t show again.",
  "MakeMouthTitle": "Make mouth (optional) ",
  "MakeMouthTitle1": "Make Mouth",
  "MakeFlexatarMouthHelp": "1. Bite the orange bar taking first picture.\\n2. 2. Keep mouth opened.\\n3. Take photos turning your head as assistant shows.",
  "FlexatarName": "Flexatar Name",
  "EnterFlexatarsName": "Enter flexatars name"
}
I need to translate values in this json to ukraine. Do not translate the keys.
I have word flexatar in texts replace it with transliteration.

    */
}
