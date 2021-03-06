package me.june.myfirstgame;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Random;


public class GamePanel extends SurfaceView implements SurfaceHolder.Callback
{
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private Random rand = new Random();
    private long smokeStartTime;
    private long missileStartTime;
    private MainThread thread;
    private Background bg;
    private  Player player;
    private ArrayList<Smokepuff> smoke;
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topBorders;
    private ArrayList<BotBorder> botBorders;
    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean topDown = true;
    private boolean botDown = true;
    private boolean newGameCreated;

    //increase to slow down difficulty progression, decrease to speed up difficulty progression
    private int progressDenom = 20;

    private Explosion explosion;
    private long startReset;
    private boolean reset;
    private boolean disappear;
    private boolean started;
    private int best;

    public GamePanel(Context context)
    {
        super(context);


        //add the callback to the surface holder to intercept events
        getHolder().addCallback(this);



        //make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        int counter = 0; //prevent infinite loop
        while(retry && counter <1000){
            counter++;
            try{thread.setRunning(false);
                thread.join();
                retry = false;
                thread = null;
            }catch(InterruptedException e){e.printStackTrace();}

        }

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){

        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        smoke = new ArrayList<>();
        missiles = new ArrayList<>();
        botBorders = new ArrayList<>();
        topBorders = new ArrayList<>();

        smokeStartTime = System.nanoTime();
        missileStartTime = System.nanoTime();

        thread = new MainThread(getHolder(), this);

        //we can safely start the game loop
        thread.setRunning(true);
        thread.start();

    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        //when pressing down
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!player.getPlaying() && newGameCreated && reset) {
                player.setPlaying(true);
                player.setUp(true);
            }

            if(player.getPlaying()){
                if(!started){
                    started = true;
                }
                reset = false;
                player.setUp(true);
            }
            return true;

        }

        if(event.getAction()==MotionEvent.ACTION_UP){
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update(){
        if(player.getPlaying()){
            if(botBorders.isEmpty()){
                player.setPlaying(false);
                return;
            }
            if(topBorders.isEmpty()){
                player.setPlaying(false);
                return;
            }

            bg.update();
            player.update();

            //calculate the threshold of height the border can have based on the score max and min border
            // heart are updated, and the border switched direction when either max or min is met
            maxBorderHeight = 30+player.getScore()/progressDenom;
            //cap max border height so that borders can only take up a total of 1/2 of the screen
            if(maxBorderHeight > HEIGHT/4){
                maxBorderHeight = HEIGHT/4;
            }
            minBorderHeight = 5+player.getScore()/progressDenom;

            //check bottom border collision
            for(int i = 0; i < botBorders.size(); i++){
                if(collision(botBorders.get(i), player)){
                    player.setPlaying(false);
                }
            }

            //check top border collision
            for(int i = 0; i < topBorders.size(); i++){
                if(collision(topBorders.get(i), player)){
                    player.setPlaying(false);
                }
            }

            //create top border
            this.updateTopBorder();

            //create bottom border
            this.updateBottomBorder();

            //generating missiles with time delay
            long missilesElapsed = (System.nanoTime() - missileStartTime)/1000000;
            if(missilesElapsed > (2000 - player.getScore()/4)){
                //first missile always goes down the middle
                if(missiles.size() == 0){
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH+10, HEIGHT/2, 45,15, player.getScore(), 13));
                }
                else{
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile),
                            WIDTH+10, (int) (rand.nextDouble()*(HEIGHT-(maxBorderHeight*2))+maxBorderHeight), 45,15, player.getScore(), 13));
                }
                //reset time
                missileStartTime = System.nanoTime();
            }

            //loop through every missile and check collision and remove
            for(int i = 0; i < missiles.size(); i++){
                missiles.get(i).update();
                if(collision(missiles.get(i), player)){
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }

                if(missiles.get(i).getX() < -100){
                    missiles.remove(i);
                    break;
                }
            }

            //add delay to smoke appearing
            long elapsed = (System.nanoTime() - smokeStartTime)/1000000;
            if(elapsed > 120){
                smoke.add(new Smokepuff(player.getX(), player.getY() + 10));
                smokeStartTime = System.nanoTime();
            }

            //remove the smoke if it get out of the screen
            for(int i = 0; i < smoke.size(); i++){
                smoke.get(i).update();
                if(smoke.get(i).getX() < -10){
                    smoke.remove(i);
                }
            }
        }
        else{
            player.resetDY();

            if(!reset){
                newGameCreated = false;
                startReset = System.nanoTime();
                reset = true;
                disappear = true;
                explosion = new Explosion(BitmapFactory.decodeResource(getResources(), R.drawable.explosion),
                        player.getX(), player.getY()-30, 100, 100,25);
            }

            explosion.update();
            long resetElapsed = (System.nanoTime()-startReset)/1000000;

            if(resetElapsed > 2500 && !newGameCreated){
                newGame();
            }
        }
    }

    public boolean collision(GameObject a, GameObject b){
        if(Rect.intersects(a.getRectangle(), b.getRectangle())){
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);
        final float scaleFactorX = getWidth()/(WIDTH*1.f);//width of phone screen
        final float scaleFactorY = getHeight()/(HEIGHT*1.f);//height of phone screen

        if(canvas!=null){
            final int savedSate = canvas.save();

            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            if(!disappear){
                player.draw(canvas);
            }

            //draw smoke
            for(Smokepuff sp: smoke){
                sp.draw(canvas);
            }

            //draw missiles
            for(Missile m: missiles){
                m.draw(canvas);
            }

            //draw top border
            for(TopBorder tb: topBorders){
                tb.draw(canvas);
            }

            //draw bottom border
            for(BotBorder bb: botBorders){
                bb.draw(canvas);
            }

            //draw explosion
            if(started){
                explosion.draw(canvas);
            }

            drawText(canvas);
            canvas.restoreToCount(savedSate);
        }
    }

    public void updateTopBorder(){
        //every 50 point, insert randomly placed top blocks that break the pattern
        if(player.getScore()%50 ==0){
            topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    topBorders.get(topBorders.size()-1).getX()+20, 0, (int) (rand.nextDouble()*maxBorderHeight)+1));
        }
        for(int i = 0; i < topBorders.size(); i++){
            topBorders.get(i).update();
            if(topBorders.get(i).getX() < -20){
                //remove element of arraylist, replace it by adding a new one
                topBorders.remove(i);
                //calculate topdown which determines the direction the border is moving (up or down)
                if(topBorders.get(topBorders.size() - 1).getHeight() >= maxBorderHeight){
                    topDown = false;
                }
                if(topBorders.get(topBorders.size() - 1).getHeight() <= minBorderHeight){
                    topDown = true;
                }

                if(topDown){
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            topBorders.get(topBorders.size()-1).getX()+20, 0, topBorders.get(topBorders.size() - 1).getHeight()+1));
                }
                //new border added will have smaller height
                else{
                    topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            topBorders.get(topBorders.size()-1).getX()+20, 0, topBorders.get(topBorders.size() - 1).getHeight()-1));
                }
            }
        }
    }

    public void updateBottomBorder(){

        //every 40 point, insert randomly placed bottom blocks that break the pattern
        if(player.getScore()%40 ==0){
            botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    botBorders.get(botBorders.size()-1).getX()+20, (int) ((rand.nextDouble()*maxBorderHeight)+(HEIGHT-maxBorderHeight))));
        }
        //update bottom border
        for(int i = 0; i < botBorders.size(); i++){
            botBorders.get(i).update();
            if(botBorders.get(i).getX() < -20){
                //remove element of arraylist, replace it by adding a new one
                botBorders.remove(i);

                //determine if border will be moving up or down
                if(botBorders.get(botBorders.size() - 1).getY() <= HEIGHT - maxBorderHeight){
                    botDown = true;
                }
                if(botBorders.get(botBorders.size() - 1).getY() >= HEIGHT - minBorderHeight){
                    botDown = false;
                }

                if(botDown){
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botBorders.get(botBorders.size()-1).getX()+20, botBorders.get(botBorders.size()-1).getY()+1));
                }
                //new border added will have smaller height
                else{
                    botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            botBorders.get(botBorders.size()-1).getX()+20, botBorders.get(botBorders.size()-1).getY()-1));
                }
            }
        }

    }

    public void newGame(){
        disappear = false;

        botBorders.clear();
        topBorders.clear();
        missiles.clear();
        smoke.clear();

        minBorderHeight = 5;
        maxBorderHeight = 30;
        player.resetDY();
        player.resetScore();
        player.setY(HEIGHT/2);

        if(player.getScore() > best){
            best = player.getScore();
        }

        //initial top borders
        for(int i = 0; i*20<WIDTH+40;i++){
            //first border ever created
            if(i == 0){
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, 0, 10));
            }
            else{
                topBorders.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, 0, topBorders.get(i-1).getHeight()+1));
            }

        }

        //initial bottom border
        for(int i = 0; i*20<WIDTH+40;i++){

            //first border ever created
            if(i == 0){
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, HEIGHT-minBorderHeight));
            }
            //adding borders until the initial screen is filled
            else{
                botBorders.add(new BotBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), i*20, botBorders.get(i-1).getY()-1));
            }

        }
        newGameCreated = true;
    }

    public void drawText(Canvas canvas){
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(30);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("DISTANCE: " + (player.getScore()*3), 10, HEIGHT-10, paint);
        canvas.drawText("BEST: " +  best, WIDTH-215, HEIGHT-10, paint);

        if(!player.getPlaying() && newGameCreated && reset){
            Paint paint1 = new Paint();
            paint1.setTextSize(40);
            paint1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("PRESS to START", WIDTH/2-50, HEIGHT/2, paint1);

            paint1.setTextSize(20);
            canvas.drawText("PRESS AND HOLD TO GO UP", WIDTH/2-50, HEIGHT/2 + 20, paint1);
            canvas.drawText("RELEASE TO GO DOWN", WIDTH/2-50, HEIGHT/2 + 40, paint1);
        }
    }
}