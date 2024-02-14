package org.flexatar;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class SrializerJson {

    public void formJson(String str) throws JSONException {
//        Log.d("FLX_INJECT","serialization got json :"+str);
        JSONObject ticket = new JSONObject(str);
        Class<SrializerJson> clazz = (Class<SrializerJson>) this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            if(field.getType() == String.class){


//                Log.d("FLX_INJECT","serialization field :"+fieldName);
                if (fieldName.equals("f_class")) fieldName = "class";
//                Log.d("FLX_INJECT","ticket.has(fieldName): "+ticket.has(fieldName));
                if (ticket.has(fieldName)) {
                    try {
                        field.set(this, ticket.getString(fieldName));
                    } catch (IllegalAccessException e) {
//                        throw new RuntimeException(e);
                    }
                }
            }
        }

//        return this;
    }
    public JSONObject toJson(String ticketString){

        Class<SrializerJson> clazz = (Class<SrializerJson>) this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        JSONObject jsonTicket = null;
        try {
            jsonTicket = new JSONObject(ticketString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        // Loop through each field
        for (Field field : fields) {

            try {

                String value = (String) field.get(this);
                if (value!=null && !Modifier.isFinal(field.getModifiers()))
                    jsonTicket.put(field.getName(),value);

//                Log.d("FLX_INJECT","Field Name: " + field.getName() + ", Value: " + value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return jsonTicket;
    }
    public JSONObject toJson(){

        Class<SrializerJson> clazz = (Class<SrializerJson>) this.getClass();
        Field[] fields = clazz.getDeclaredFields();
        JSONObject jsonTicket = null;

        jsonTicket = new JSONObject();

        // Loop through each field
        for (Field field : fields) {

            try {

                String value = (String) field.get(this);
                if (value!=null && !Modifier.isFinal(field.getModifiers()))
                    jsonTicket.put(field.getName(),value);

//                Log.d("FLX_INJECT","Field Name: " + field.getName() + ", Value: " + value);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        return jsonTicket;
    }
}
