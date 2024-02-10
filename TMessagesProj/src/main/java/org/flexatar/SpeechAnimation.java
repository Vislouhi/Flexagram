package org.flexatar;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpeechAnimation {
    private static Interpreter wav2melInterpreter;
    private static Interpreter mel2phoneInterpreter;
    private static Interpreter phon2avecInterpreter;

    private static MappedByteBuffer loadModelFile(Context context,String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        MappedByteBuffer ret = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        fileChannel.close();
        inputStream.close();
        fileDescriptor.close();
        return ret;
    }
    public static void loadModels(Context context){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (wav2melInterpreter != null && mel2phoneInterpreter != null && phon2avecInterpreter != null) return;
            try {
                if (wav2melInterpreter == null)
                    wav2melInterpreter = new Interpreter(loadModelFile(context,"flexatar/nn/wav2mel.tflite"), new Interpreter.Options());
                Log.d("====DEB====","Successfuli loaded tf model1");
                if (mel2phoneInterpreter == null)
                    mel2phoneInterpreter = new Interpreter(loadModelFile(context,"flexatar/nn/mel2phon.tflite"), new Interpreter.Options());
                Log.d("====DEB====","Successfuli loaded tf model2");
                if (phon2avecInterpreter == null)
                    phon2avecInterpreter = new Interpreter(loadModelFile(context,"flexatar/nn/phon2avec.tflite"), new Interpreter.Options());
                Log.d("====DEB====","Successfuli loaded tf model3");
            } catch (IOException e) {
                Log.d("====DEB====","failed loaded tf model");
                e.printStackTrace();
            }
        });

    }
    public static void loadModelsSync(Context context){

            if (wav2melInterpreter != null && mel2phoneInterpreter != null && phon2avecInterpreter != null) return;
            try {
                if (wav2melInterpreter == null)
                    wav2melInterpreter = new Interpreter(loadModelFile(context,"flexatar/nn/wav2mel.tflite"), new Interpreter.Options());
                Log.d("====DEB====","Successfuli loaded tf model1");
                if (mel2phoneInterpreter == null)
                    mel2phoneInterpreter = new Interpreter(loadModelFile(context,"flexatar/nn/mel2phon.tflite"), new Interpreter.Options());
                Log.d("====DEB====","Successfuli loaded tf model2");
                if (phon2avecInterpreter == null)
                    phon2avecInterpreter = new Interpreter(loadModelFile(context,"flexatar/nn/phon2avec.tflite"), new Interpreter.Options());
                Log.d("====DEB====","Successfuli loaded tf model3");
            } catch (IOException e) {
                Log.d("====DEB====","failed loaded tf model");
                e.printStackTrace();
            }


    }
    public static void checkModel(){
        float[][] inputBuffer = new float[1][800];
        float[][][][] outputBuffer = new float[1][5][80][1];
        wav2melInterpreter.run(inputBuffer,outputBuffer);

        float[][][][] inputBuffer1 = new float[1][100][80][1];
        float[][][][] outputBuffer1 = new float[1][20][8][1];
        mel2phoneInterpreter.run(inputBuffer1,outputBuffer1);

        float[][][][] inputBuffer2 = new float[1][20][7][1];
        float[][][][] outputBuffer2 = new float[1][20][7][1];
        phon2avecInterpreter.run(inputBuffer2,outputBuffer2);

    }
    static private List<float[][][][]> mels = new ArrayList<>();
    static private float[] zeroAnimState = {0.5f,0.48f,0.52f,0.43f,0.46f};
    static public void prepareAnimator(){
        mels.clear();
        float[][] inputBuffer = new float[1][800];
        for (int i = 0; i < 19; i++) {
            float[][][][] outputBuffer = new float[1][5][80][1];
            wav2melInterpreter.run(inputBuffer,outputBuffer);

            mels.add(outputBuffer);
        }

    }

    public static float[] processAudio(float[] audioBuffer){

        float[] result = {0f,0f,0f,0f,0f};
//        try {
            float[][] inputBuffer = new float[1][800];
            inputBuffer[0] = audioBuffer;
            float[][][][] outputBuffer = new float[1][5][80][1];
            if (wav2melInterpreter != null) {
                wav2melInterpreter.run(inputBuffer, outputBuffer);

                mels.add(outputBuffer);
            }
            if (mels.size() == 20) {
                float[][][][] inputBuffer1 = new float[1][100][80][1];

                for (int i = 0; i < 20; i++) {
                    System.arraycopy(mels.get(i)[0], 0, inputBuffer1[0],  i * 5, 5);
                }
                float[][][][] outputBuffer1 = new float[1][20][8][1];
                if (mel2phoneInterpreter != null)
                    mel2phoneInterpreter.run(inputBuffer1, outputBuffer1);


                float[][][][] inputBuffer2 = new float[1][20][7][1];
                float[][][][] outputBuffer2 = new float[1][20][7][1];
                for (int i = 0; i < 20; i++) {
                    System.arraycopy(outputBuffer1[0][i], 0, inputBuffer2[0][i], 0, 7);

                }
                if (phon2avecInterpreter != null)
                    phon2avecInterpreter.run(inputBuffer2, outputBuffer2);
                for (int i = 0; i < 5; i++) {
                    result[i] = -7f * (outputBuffer2[0][9][i][0] - zeroAnimState[i]);
                }

//            Log.d("====DEB====","val " +outputBuffer2[0][10][0][0] + " " +outputBuffer2[0][10][1][0]+ " " +outputBuffer2[0][10][2][0]+ " " +outputBuffer2[0][10][3][0]+ " " +outputBuffer2[0][10][4][0]);
//            Log.d("====DEB====","val " +outputBuffer[0][2][20][0] );
                mels.remove(0);
            }
//            if (mels.size() > 20) {mels.clear();}
//        }catch (NullPointerException ignored){}
        return result;

    }

    public static void dropModels() {
//        phon2avecInterpreter.setCancelled(true);
//        mel2phoneInterpreter.setCancelled(true);
//        wav2melInterpreter.setCancelled(true);

//            phon2avecInterpreter = null;
//            mel2phoneInterpreter = null;
//            wav2melInterpreter = null;
            mels.clear();

    }
}
