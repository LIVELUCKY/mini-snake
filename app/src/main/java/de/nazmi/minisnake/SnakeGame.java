package de.nazmi.minisnake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends SurfaceView implements Runnable, SurfaceHolder.Callback, GestureDetector.OnGestureListener {
    private Thread gameThread;
    private boolean isRunning;
    private final Paint paint = new Paint();
    private final List<Point> snakeBody = new ArrayList<>();
    private Point food;
    private int gridSize, maxX, maxY, score, highScore;
    private Direction direction = Direction.RIGHT;
    private final Random random = new Random();
    private final SharedPreferences sharedPreferences;
    private GameState gameState = GameState.INITIAL;
    private static final String HIGH_SCORE_KEY = "HighScore";
    private final GestureDetector gestureDetector;

    private static final float SWIPE_THRESHOLD = 100;
    private static final float SWIPE_VELOCITY_THRESHOLD = 100;


    public SnakeGame(Context context) {
        super(context);
        gestureDetector = new GestureDetector(context, this);
        sharedPreferences = context.getSharedPreferences("SnakeGame", Context.MODE_PRIVATE);
        highScore = sharedPreferences.getInt(HIGH_SCORE_KEY, 0);
        getHolder().addCallback(this);
        paint.setTextSize(40);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        gridSize = Math.min(getWidth(), getHeight()) / 20;
        maxX = getWidth() / gridSize;
        maxY = getHeight() / gridSize;
        resetGame();
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
        if (snakeBody.isEmpty()) return;

        Point head = new Point(snakeBody.get(0));
        head.offset(direction.dx * gridSize, direction.dy * gridSize);

        if (isGameOver(head)) {
            gameState = GameState.GAME_OVER;
            saveHighScore();
            return;
        }

        snakeBody.add(0, head);

        if (head.equals(food)) {
            score++;
            randomizeFood();
        } else {
            snakeBody.remove(snakeBody.size() - 1);
        }
    }

    private void drawGame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            canvas.drawColor(Color.BLACK);
            paint.setColor(Color.GREEN);
            for (Point point : snakeBody) {
                canvas.drawRect(point.x, point.y, point.x + gridSize, point.y + gridSize, paint);
            }
            if (gameState == GameState.RUNNING) {
                paint.setColor(Color.RED);
                canvas.drawRect(food.x, food.y, food.x + gridSize, food.y + gridSize, paint);
            }
            paint.setColor(Color.WHITE);
            String text = "";
            if (gameState == GameState.INITIAL) {
                text = "Tap to Start";
            } else if (gameState == GameState.GAME_OVER) {
                text = "Game Over! Score: " + score + (highScore > 0 ? ". High Score: " + highScore : "") + ". Tap to Restart";
            }
            drawCenterText(canvas, text);
            getHolder().unlockCanvasAndPost(canvas);
        }
    }

    private void drawCenterText(Canvas canvas, String text) {
        float textWidth = paint.measureText(text);
        float x = (getWidth() - textWidth) / 2;
        float y = getHeight() / 2;
        canvas.drawText(text, x, y, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void randomizeFood() {
        food = new Point(random.nextInt(maxX) * gridSize, random.nextInt(maxY) * gridSize);
    }

    private void resetGame() {
        snakeBody.clear();
        snakeBody.add(new Point(gridSize * 5, gridSize * 5));
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

    private boolean isGameOver(Point head) {
        return head.x < 0 || head.x >= maxX * gridSize || head.y < 0 || head.y >= maxY * gridSize ||
                snakeBody.subList(1, snakeBody.size()).contains(head);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (gameState == GameState.INITIAL || gameState == GameState.GAME_OVER) {
            resetGame();
            gameState = GameState.RUNNING;
            return true;
        }

        return true;
    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getY() - e1.getY();
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (Math.abs(deltaX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (deltaX > 0 && direction != Direction.LEFT) {
                    direction = Direction.RIGHT;
                } else if (deltaX < 0 && direction != Direction.RIGHT) {
                    direction = Direction.LEFT;
                }
            }
        } else {
            if (Math.abs(deltaY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (deltaY > 0 && direction != Direction.UP) {
                    direction = Direction.DOWN;
                } else if (deltaY < 0 && direction != Direction.DOWN) {
                    direction = Direction.UP;
                }
            }
        }
        return true;
    }


    private enum Direction {
        LEFT(-1, 0), RIGHT(1, 0), UP(0, -1), DOWN(0, 1);

        final int dx, dy;

        Direction(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private enum GameState {INITIAL, RUNNING, GAME_OVER}
}

class Point {
    int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point p) {
        this.x = p.x;
        this.y = p.y;
    }

    public void offset(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point)) return false;
        Point other = (Point) obj;
        return x == other.x && y == other.y;
    }
}
