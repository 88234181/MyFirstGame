package me.june.myfirstgame;

import android.graphics.Bitmap;

/**
 * Created by j8823_000 on 6/24/2016.
 */
public class Animation {
    private Bitmap[] frames;
    private int currentFrame;
    private long startTime;
    private long delay;
    private boolean playedOnce; //some animation only needs to be played once

    public void setFrames(Bitmap[] frames){
        this.frames = frames;
        currentFrame = 0;
        startTime = System.nanoTime();
    }

    public void setDelay(long d){
        delay = d;
    }

    //in case you need to manually set the frame
    public void setFrames(int i){
        currentFrame = i;
    }

    public Bitmap getImage(){
        return frames[currentFrame];
    }

    public void update(){
        long elapsed = (System.nanoTime()-startTime)/1000000;

        if(elapsed>delay){
            currentFrame++;
            startTime = System.nanoTime();
        }
        if(currentFrame == frames.length){
            currentFrame = 0;
            playedOnce = true;
        }
    }


    public int getFrames(){
        return currentFrame;
    }

    public boolean playedOnce(){
        return playedOnce;
    }
}
