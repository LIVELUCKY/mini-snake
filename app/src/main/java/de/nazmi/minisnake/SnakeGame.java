package de.nazmi.minisnake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.Random;

public class SnakeGame extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private Thread gameThread;
    private boolean isRunning;
    private final Paint paint;
    private final LinkedList<PointF> snakeBody;
    private float foodX, foodY, gridSize;
    private int maxX, maxY, score, highScore;
    private Direction direction = Direction.RIGHT;
    private final Random random = new Random();
    private final SharedPreferences sharedPreferences;
    private GameState gameState = GameState.INITIAL;
    private long lastBlinkTime;
    private boolean isFoodVisible = true;
    private static final long BLINK_INTERVAL = 900;
    private static final String HIGH_SCORE_KEY = "HighScore";
    private float initialTouchX;
    private float initialTouchY;
    private long lastPressTime;

    public SnakeGame(Context context) {
        super(context);
        paint = new Paint();
        snakeBody = new LinkedList<>();
        sharedPreferences = context.getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        highScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gridSize = Math.min(getWidth(), getHeight()) / 26f;
        maxX = (int) (getWidth() / gridSize) - 1;
        maxY = (int) (getHeight() / gridSize) - 1;
        randomizeFood();
        startGame();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopGame();
    }

    @Override
    public void run() {
        while (isRunning) {
            if (gameState == GameState.RUNNING) {
                updateGame();
            }
            drawGame();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateGame() {
        if (snakeBody.isEmpty()) {
            return;
        }

        PointF head = new PointF(snakeBody.getFirst().x, snakeBody.getFirst().y);
        switch (direction) {
            case RIGHT:
                head.x += gridSize;
                break;
            case LEFT:
                head.x -= gridSize;
                break;
            case UP:
                head.y -= gridSize;
                break;
            case DOWN:
                head.y += gridSize;
                break;
        }

        if (head.x < 0 || head.y < 0 || head.x >= getWidth() || head.y >= getHeight() || contains(snakeBody, head)) {
            gameState = GameState.GAME_OVER;
            saveHighScore();
            return;
        }

        snakeBody.addFirst(head);

        if (Math.abs(head.x - foodX) < gridSize && Math.abs(head.y - foodY) < gridSize) {
            score++;
            randomizeFood();
        } else {
            snakeBody.removeLast();
        }
    }

    private void drawGame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.BLACK);
            paint.setColor(Color.GREEN);

            for (PointF point : snakeBody) {
                float padding = 2f;
                float left = point.x + padding;
                float top = point.y + padding;
                float right = point.x + gridSize - padding;
                float bottom = point.y + gridSize - padding;
                canvas.drawRect(left, top, right, bottom, paint);
            }

            // Blinking food logic
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastBlinkTime > BLINK_INTERVAL) {
                isFoodVisible = !isFoodVisible;
                lastBlinkTime = currentTime;
            }

            if (isFoodVisible && gameState == GameState.RUNNING) {
                paint.setColor(Color.RED);
                canvas.drawRect(foodX, foodY, foodX + gridSize, foodY + gridSize, paint);
            }

            paint.setColor(Color.WHITE);
            paint.setTextSize(60); // Increased text size

            if (gameState == GameState.INITIAL || gameState == GameState.GAME_OVER) {
                String text = gameState == GameState.INITIAL ? "Hold to start game" :
                        "Game Over. Score: " + score + ". High Score: " + highScore;
                // Calculate x and y to center the text
                float textWidth = paint.measureText(text);
                float x = (getWidth() - textWidth) / 2;
                float y = (float) getHeight() / 2;
                canvas.drawText(text, x, y, paint);

                if (gameState == GameState.GAME_OVER) {
                    String restartText = "Touch to restart";
                    float restartTextWidth = paint.measureText(restartText);
                    float restartX = (getWidth() - restartTextWidth) / 2;
                    canvas.drawText(restartText, restartX, y + 70, paint); // Adjust y position for restart text
                }
            }

            getHolder().unlockCanvasAndPost(canvas);
        }
    }


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
                    if (gameState != GameState.RUNNING) {
                        resetGame();
                        gameState = GameState.RUNNING;
                    }
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

    private void randomizeFood() {
        foodX = random.nextInt(maxX) * gridSize;
        foodY = random.nextInt(maxY) * gridSize;
    }

    private void resetGame() {
        snakeBody.clear();
        snakeBody.add(new PointF(gridSize * 5, gridSize * 5));
        direction = Direction.RIGHT;
        score = 0;
        randomizeFood();
    }

    private void startGame() {
        isRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    private void stopGame() {
        isRunning = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void saveHighScore() {
        if (score > highScore) {
            highScore = score;
            sharedPreferences.edit().putInt(HIGH_SCORE_KEY, highScore).apply();
        }
    }


    private boolean contains(LinkedList<PointF> list, PointF point) {
        for (int i = 1; i < list.size(); i++) {
            PointF p = list.get(i);
            if (p.x == point.x && p.y == point.y) {
                return true;
            }
        }
        return false;
    }

    private enum Direction {LEFT, RIGHT, UP, DOWN}

    private enum GameState {INITIAL, RUNNING, GAME_OVER}
}
