package org.flexatar;

import java.util.Timer;
import java.util.TimerTask;

public class TimerAutoDestroy<T> {


    private  Timer timer;
    public interface OnTimerListener<U>{
         U onTic(U val);
    }
    public OnTimerListener<T> onTimerListener;
    private T value;
    public int usageCounter = 0;
    public int counter5 = 0;
    private static final Object mutexObject = new Object();
    public void setValue(T val){
        value = val;
    }
    
    public T getValue(){
        synchronized (mutexObject) {
            if (timer == null) {
                timer = new Timer();

                TimerTask task = new TimerTask() {


                    @Override
                    public void run() {
                        if (onTimerListener!=null){
                            value = onTimerListener.onTic(value);
                        }

                        counter5++;
                        if (counter5 > 5) {
                            counter5 = 0;
                            if (usageCounter == 0) {
                                destroy();
                            }
                            usageCounter = 0;
                        }
                        // Code to be executed repeatedly
//                System.out.println("Task executed at regular interval.");
                    }
                };
                timer.scheduleAtFixedRate(task, 0, 40);
            }
        }
        return value;
    }
    public void destroy(){
        timer.cancel();
        timer.purge();
        timer = null;
    }
}
