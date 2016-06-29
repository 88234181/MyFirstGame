package me.june.myfirstgame;

import android.graphics.Bitmap;
import android.graphics.Canvas;

/**
 * Created by j8823_000 on 6/27/2016.
 */
public class BotBorder extends GameObject {
    private Bitmap image;

    public BotBorder(Bitmap res, int x, int y){
        height = 200;
        width = 20;

        this.x = x;
        this.y = y;

        dx = GamePanel.MOVESPEED; //set it to be the same speed as the background
        image = Bitmap.createBitmap(res, 0, 0, width, height);
    }

    public void update(){
        x+=dx;
    }

    public void draw(Canvas canvas){
        try{
            canvas.drawBitmap(image, x, y, null);
        }catch (Exception e){

        }
    }
}
