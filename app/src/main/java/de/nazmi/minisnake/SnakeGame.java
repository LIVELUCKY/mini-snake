package de.nazmi.minisnake;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.Random;

public class SnakeGame extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private final Paint paint;
    private Thread thread;
    private boolean running, gameOver;
    private final LinkedList<PointF> snakeBody;
    private final float padding;
    private float foodX;
    private float foodY;
    private float initialTouchX;
    private float initialTouchY;
    private float gridSize;
    private float textSize;
    private long lastPressTime;
    private int maxX, maxY;
    private Direction direction;
    private Random random;

    public SnakeGame(Context context) {
        super(context);
        paint = new Paint();
        running = false;
        gameOver = false;
        snakeBody = new LinkedList<>();
        snakeBody.add(new PointF(10f, 10f));
        padding = 2f;
        direction = Direction.RIGHT;
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gridSize = Math.min(getWidth(), getHeight()) / 26f;
        textSize = Math.min(getWidth(), getHeight()) / 20f;
        maxX = (int) (getWidth() / gridSize) - 1;
        maxY = (int) (getHeight() / gridSize) - 1;
        random = new Random();
        randomizeFood();
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // No-op
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pause();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getX();
                initialTouchY = event.getY();
                lastPressTime = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_UP:
                if (System.currentTimeMillis() - lastPressTime >= 1000L) {
                    gameOver = false;
                    snakeBody.clear();
                    snakeBody.add(new PointF(10f, 10f));
                    direction = Direction.RIGHT;
                    randomizeFood();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getX() - initialTouchX;
                float deltaY = event.getY() - initialTouchY;
                direction = Math.abs(deltaX) > Math.abs(deltaY)
                        ? (deltaX > 0 ? Direction.RIGHT : Direction.LEFT)
                        : (deltaY > 0 ? Direction.DOWN : Direction.UP);
                break;
        }
        return true;
    }

    @Override
    public void run() {
        while (running) {
            if (!gameOver) {
                update();
                draw();
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void update() {
        PointF head = snakeBody.getFirst();
        PointF newHead = new PointF(head.x, head.y);

        switch (direction) {
            case RIGHT:
                newHead.x += gridSize;
                break;
            case LEFT:
                newHead.x -= gridSize;
                break;
            case UP:
                newHead.y -= gridSize;
                break;
            case DOWN:
                newHead.y += gridSize;
                break;
        }

        if (newHead.x < 0 || newHead.y < 0 || newHead.x >= getWidth() || newHead.y >= getHeight()) {
            gameOver = true;
            return;
        }

        snakeBody.addFirst(newHead);

        if (Math.abs(newHead.x - foodX) < gridSize && Math.abs(newHead.y - foodY) < gridSize) {
            randomizeFood();
        } else {
            snakeBody.removeLast();
        }
    }


    private void draw() {
        if (getHolder().getSurface().isValid()) {
            Canvas canvas = getHolder().lockCanvas();
            canvas.drawColor(Color.BLACK);
            paint.setColor(Color.GREEN);

            for (PointF point : snakeBody) {
                RectF rect = new RectF(point.x + padding, point.y + padding, point.x + gridSize - padding, point.y + gridSize - padding);
                canvas.drawRect(rect, paint);
            }

            paint.setColor(Color.RED);
            RectF foodRect = new RectF(foodX, foodY, foodX + gridSize, foodY + gridSize);
            canvas.drawRect(foodRect, paint);

            if (gameOver) {
                paint.setColor(Color.WHITE);
                paint.setTextSize(textSize);
                String text = "Game Over"; // replace with context.getString(R.string.game_over_text) if you are using string resources
                float textWidth = paint.measureText(text);
                canvas.drawText(text, (getWidth() - textWidth) / 2, getHeight() / 2, paint);
            }

            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void randomizeFood() {
        foodX = random.nextInt(maxX) * gridSize;
        foodY = random.nextInt(maxY) * gridSize;
    }

    public void pause() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        thread = null;
    }

    public void resume() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    private enum Direction {RIGHT, LEFT, DOWN, UP}
}
