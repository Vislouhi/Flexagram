package org.flexatar;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class VPUtil {
    public static float[] concatenateFloatArrays(List<float[]> floatArrays) {
        int totalLength = 0;

        // Calculate the total length of the concatenated array
        for (float[] arr : floatArrays) {
            totalLength += arr.length;
        }

        // Create a new float array with the total length
        float[] result = new float[totalLength];
        int currentIndex = 0;

        // Copy elements from each float array to the result array
        for (float[] arr : floatArrays) {
            System.arraycopy(arr, 0, result, currentIndex, arr.length);
            currentIndex += arr.length;
        }

        return result;
    }

    public static short[] concatenateShortArrays(List<short[]> floatArrays) {
        int totalLength = 0;

        // Calculate the total length of the concatenated array
        for (short[] arr : floatArrays) {
            totalLength += arr.length;
        }

        // Create a new float array with the total length
        short[] result = new short[totalLength];
        int currentIndex = 0;

        // Copy elements from each float array to the result array
        for (short[] arr : floatArrays) {
            System.arraycopy(arr, 0, result, currentIndex, arr.length);
            currentIndex += arr.length;
        }

        return result;
    }
    public static short[] floatToShortPCM(float[] floatPCM) {
        short[] shortPCM = new short[floatPCM.length];
        for (int i = 0; i < floatPCM.length; i++) {
            // Scale float value to short value range
            float scaledValue = floatPCM[i] * 32767.0f;

            // Clip values to fit within the range of short integers
            if (scaledValue > Short.MAX_VALUE) {
                shortPCM[i] = Short.MAX_VALUE;
            } else if (scaledValue < Short.MIN_VALUE) {
                shortPCM[i] = Short.MIN_VALUE;
            } else {
                shortPCM[i] = (short) scaledValue;
            }
        }
        return shortPCM;
    }

    public static byte[] floatArrayToByteArray(float[] floatArray) {
        // Create a ShortBuffer and put the short array into it


        // Create a ByteBuffer from the ShortBuffer and retrieve the byte array
        ByteBuffer byteBuffer = ByteBuffer.allocate(floatArray.length * 4); // 2 bytes per short
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asFloatBuffer().put(floatArray);
        byteBuffer.position(0);
        // Retrieve the byte array

        return byteBuffer.array();
    }
    public static byte[] shortArrayToByteArray(short[] shortArray) {
        // Create a ShortBuffer and put the short array into it


        // Create a ByteBuffer from the ShortBuffer and retrieve the byte array
        ByteBuffer byteBuffer = ByteBuffer.allocate(shortArray.length * 2); // 2 bytes per short
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.asShortBuffer().put(shortArray);
        byteBuffer.position(0);
        // Retrieve the byte array

        return byteBuffer.array();
    }
    public static InputStream shortArrayToInputStream(short[] shortArray) {
        // Convert short array to byte array
        byte[] byteArray = new byte[shortArray.length * 2]; // 2 bytes per short
        for (int i = 0; i < shortArray.length; i++) {
            byteArray[i * 2] = (byte) (shortArray[i] & 0xFF);          // Least significant byte
            byteArray[i * 2 + 1] = (byte) ((shortArray[i] >> 8) & 0xFF); // Most significant byte
        }

        // Create ByteArrayInputStream from byte array
        return new ByteArrayInputStream(byteArray);
    }
    public static float[] shortToFloat(short[] shortBuffer) {
        float[] floatBuffer = new float[shortBuffer.length];
        for (int i = 0; i < shortBuffer.length; i++) {
            floatBuffer[i] = (float)shortBuffer[i] / 32767.0f; // Normalize short value to range [-1.0, 1.0]
        }
        return floatBuffer;
    }
}
