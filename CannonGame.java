//GROUP MEMBERS
//1. ALIM ISMAEL KAMARA            32114194
//2. ABDUL KEHMORKAI KAMARA        32113442
//3. ANNETTE SANNOH                32117039

package application;

import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.text.Font;
import javafx.scene.text.Text;



import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CannonGame extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TARGET_COUNT = 9;
    private static final double TARGET_WIDTH = 10;
    private static final double TARGET_HEIGHT = 65;
    private static final double BARREL_LENGTH = 40;
    private static final double MIN_SEPARATION_DISTANCE = 80;
    
    private int shotsFired = 0;
    
    private int targetsDestroyed = 0;
    
    private long timeBonus = 3000; // 
    private long timePenalty = 3000;


    private double cannonAngle = 0;
    private double mouseX, mouseY;
    private boolean firing = false;
    private Text timerText;

    private double aimLineStartX, aimLineStartY;

    private List<Cannonball> cannonballs = new ArrayList<>();
    private List<Rectangle> targets = new ArrayList<>();
    private AudioClip explosionSound;
    private AudioClip blockerHitSound;
    private AudioClip cannonFireSound;
    private AudioClip blockerHitTopBottomSound;
    private AudioClip targetHitTopBottomSound;
    private AudioClip gameOverSound;
    private AudioClip levelCompletionSound;
    private MediaPlayer backgroundMusicPlayer;
    
    private Rectangle blocker;
    
    private long startTime;
    private long gameTimeLimit = 10 * 1000; // 10 seconds in milliseconds
    private Timeline timerTimeline;
    private AnimationTimer gameTimeline;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Pane root = new Pane();
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        
     // Load background music
        String backgroundMusicFile = getClass().getResource("game_level.mp3").toExternalForm();
        Media backgroundMusicMedia = new Media(backgroundMusicFile);
        backgroundMusicPlayer = new MediaPlayer(backgroundMusicMedia);
        backgroundMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop the music indefinitely
        
        
     // Load level completion sound
        String levelCompletionSoundFile = getClass().getResource("game_completed.mp3").toExternalForm();
        levelCompletionSound = new AudioClip(levelCompletionSoundFile);
        
        
     // Load game-over sound
        String gameOverSoundFile = getClass().getResource("game_over.mp3").toExternalForm();
        gameOverSound = new AudioClip(gameOverSoundFile);
        
        
     // Load target hit sound
        String targetHitTopBottomSoundFile = getClass().getResource("target_top.mp3").toExternalForm();
        targetHitTopBottomSound = new AudioClip(targetHitTopBottomSoundFile);
        
     // Load blocker hit sound
        String blockerHitTopBottomSoundFile = getClass().getResource("blocker_top.mp3").toExternalForm();
        blockerHitTopBottomSound = new AudioClip(blockerHitTopBottomSoundFile);

        
     // Load firing sound
        String cannonFireSoundFile = getClass().getResource("cannon_fire.wav").toExternalForm();
        cannonFireSound = new AudioClip(cannonFireSoundFile);

        // Load explosion sound
        String explosionSoundFile = getClass().getResource("target_hit.wav").toExternalForm();
        explosionSound = new AudioClip(explosionSoundFile);

        // Load hit sound for the blocker
        String blockerHitSoundFile = getClass().getResource("blocker_hit.wav").toExternalForm();
        blockerHitSound = new AudioClip(blockerHitSoundFile);
        
     // Start playing the background music
        backgroundMusicPlayer.play();
        
        timerText = new Text();
        timerText.setFont(new Font(20));
        timerText.setX(10);
        timerText.setY(30);
        root.getChildren().add(timerText);

        blocker = createBouncingRectangle((WIDTH - 20) / 2, (HEIGHT - 90) / 2, 20, 90, Color.BLACK, 2.0);
        root.getChildren().add(blocker);

        for (int i = 0; i < TARGET_COUNT; i++) {
            double targetX;
            double targetY;

            do {
                targetX = (WIDTH - 20) / 2 + 30 + Math.random() * (WIDTH - (WIDTH - 20) / 2 - 40);
                targetY = Math.random() * (HEIGHT - 40) + 20;
            } while (collidesWithOtherTargets(targetX, targetY, MIN_SEPARATION_DISTANCE) || isOutsideStageBounds(targetX, targetY));

            Rectangle target = createTarget(targetX, targetY, TARGET_WIDTH, TARGET_HEIGHT);
            root.getChildren().add(target);
            targets.add(target);
        }

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        root.getChildren().add(canvas);

        scene.setOnMousePressed(this::handleMousePressed);
        scene.setOnMouseDragged(this::handleMouseDragged);
        scene.setOnMouseReleased(this::handleMouseReleased);

        primaryStage.setScene(scene);
        primaryStage.setTitle("Merged Cannon Game");
        primaryStage.show();

        cannonAngle = 180;

        // Initialize the timer
        timerTimeline = new Timeline(
                new KeyFrame(Duration.millis(100), event -> updateTimer())
        );
        timerTimeline.setCycleCount(Timeline.INDEFINITE);

        // Initialize the game timeline
        gameTimeline = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                draw(canvas.getGraphicsContext2D());
            }
        };
        gameTimeline.start();

        // Start the timer
        startTime = System.currentTimeMillis();
        timerTimeline.play();
    }

    private Rectangle createTarget(double x, double y, double width, double height) {
    	Rectangle target = new Rectangle(x, y, width, height);

        if (targets.size() < 5) {
            target.setFill(Color.BLUE);
        } else {
            target.setFill(Color.YELLOW);
        }

        target.setStroke(Color.BLACK);

        final double speed = 2.0;
        final double[] direction = {-1};
        
        final boolean[] canPlaySound = {true};

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double newY = target.getY() + speed * direction[0];

                if (newY < 0 || newY + height > HEIGHT) {
                    direction[0] *= -1;

                    
                    if (canPlaySound[0]) {
                        targetHitTopBottomSound.play();
                        canPlaySound[0] = false;
                        // Introduce a cooldown (e.g., 500 milliseconds)
                        new Timeline(new KeyFrame(Duration.millis(500), event -> canPlaySound[0] = true)).play();
                    }
                } else {
                    target.setY(newY);
                 // Reset the cooldown when the target is in a valid position
                    canPlaySound[0] = true;
                }
            }
        }.start();


        return target;
    }

    private Rectangle createBouncingRectangle(double x, double y, double width, double height, Color color, double speed) {
    	Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setFill(color);
        rectangle.setStroke(Color.BLACK);

        final double[] direction = {-1};
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double newY = rectangle.getY() + speed * direction[0];

                if (newY < 0 || newY + height > HEIGHT) {
                    direction[0] *= -1;
                    blockerHitTopBottomSound.play(); // Play the sound when hitting the top or bottom
                } else {
                    rectangle.setY(newY);
                }
            }
        }.start();

        return rectangle;
    }

    private void handleMousePressed(MouseEvent event) {
    	mouseX = event.getX();
        mouseY = event.getY();
        aimLineStartX = 0;
        aimLineStartY = HEIGHT / 2;
        firing = false;
    }

    private void handleMouseDragged(MouseEvent event) {
    	 mouseX = event.getX();
         mouseY = event.getY();
    }

    private void handleMouseReleased(MouseEvent event) {
    	mouseX = event.getX();
        mouseY = event.getY();
        firing = true;
        fireCannonball();
    }

    private void update() {
        if (firing) {
            double angleToMouse = Math.toDegrees(Math.atan2(mouseY - HEIGHT / 2, mouseX - 0));
            cannonAngle = angleToMouse;
            fireCannonball();
            firing = false;
        }

        Iterator<Cannonball> iterator = cannonballs.iterator();
        while (iterator.hasNext()) {
            Cannonball cannonball = iterator.next();
            cannonball.update();

            for (Rectangle target : targets) {
                if (!cannonball.hasHitTarget() && cannonball.collidesWithTarget(target)) {
                    cannonball.setHitTarget(true);
                    resetTarget(target);
                    targetsDestroyed++;

                    Explosion explosion = new Explosion(target.getX() + target.getWidth() / 2,
                            target.getY() + target.getHeight() / 2, 30);
                    cannonball.setExplosion(explosion);
                    explosionSound.play();

                    // Apply time bonus
                    startTime += timeBonus;
                    break;
                }
            }

            
         // Check collision with the blocker
            if (!cannonball.hasHitTarget() && cannonball.collidesWithBlocker(blocker)) {
                cannonball.setHitTarget(true);
                cannonball.reflectOffBlocker();
                blockerHitSound.play();

                // Apply time penalty
                startTime -= Math.min(timePenalty, startTime);
            }


            if (cannonball.isOutOfBounds() || cannonball.hasHitTarget()) {
                iterator.remove();
            }
        }

        targets.removeIf(target -> target.getX() < 0 || target.getY() < 0);

        for (Cannonball cannonball : cannonballs) {
            Explosion explosion = cannonball.getExplosion();
            if (explosion != null && explosion.isActive()) {
                explosion.update();
            }
        }
        
        if (targetsDestroyed == TARGET_COUNT) {
            endGame();
        }
    }


    private void resetTarget(Rectangle target) {
    	target.setX(-100);
        target.setY(-100);
    }

    private void fireCannonball() {
        double cannonStartX = 0;
        double cannonStartY = HEIGHT / 2;

        double cannonEndX = mouseX;
        double cannonEndY = mouseY;

        Cannonball cannonball = new Cannonball(cannonStartX, cannonStartY, cannonEndX, cannonEndY);
        cannonball.setVelocity(15.0);
        cannonball.setFired(true); // Set the fired flag to true
        cannonballs.add(cannonball);

        // Increment the shotsFired count for every fired cannonball
        shotsFired++;
        
     // Play the firing sound
        cannonFireSound.play();
    }
 
    // Inside the CannonGame class, in the draw method
    
    private void draw(GraphicsContext gc) {
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        boolean targetHit = false;

        for (Rectangle target : targets) {
            gc.setFill((Color) target.getFill());
            gc.fillRect(target.getX(), target.getY(), target.getWidth(), target.getHeight());
            gc.setStroke(Color.BLACK);
            gc.strokeRect(target.getX(), target.getY(), target.getWidth(), target.getHeight());

            // Check if any cannonball has hit the target
            boolean cannonballHitTarget = cannonballs.stream().anyMatch(Cannonball::hasHitTarget);

            if (cannonballHitTarget) {
                // Set the targetHit variable to true if a cannonball hits the target
                targetHit = true;
                break; // Exit the loop since we don't need to check further
            }
        }

        // Draw the aim line only if no target has been hit
        if (!targetHit) {
            gc.setStroke(Color.BLACK);
            gc.strokeLine(aimLineStartX, aimLineStartY, mouseX, mouseY);
        }

        gc.save();
        gc.translate(0, HEIGHT / 2);
        gc.rotate(cannonAngle);

        gc.setFill(Color.GRAY);
        gc.fillRect(-25, -10, 50, 20);

        gc.setFill(Color.BROWN);
        gc.fillRect(25, -5, BARREL_LENGTH, 10);

        gc.restore();

        for (Cannonball cannonball : cannonballs) {
            cannonball.draw(gc);
        }
    }
    

    private boolean collidesWithOtherTargets(double x, double y, double separationDistance) {
    	for (Rectangle target : targets) {
            if (x < target.getX() + target.getWidth() + separationDistance &&
                    x + TARGET_WIDTH + separationDistance > target.getX() &&
                    y < target.getY() + target.getHeight() + separationDistance &&
                    y + TARGET_HEIGHT + separationDistance > target.getY()) {
                return true;
            }
        }
        return false;
    }

    private boolean isOutsideStageBounds(double x, double y) {
    	return x < 0 || y < 0 || x + TARGET_WIDTH > WIDTH || y + TARGET_HEIGHT > HEIGHT;
    }

    class Cannonball {
    	private double x, y;
        private double velocity = 5;
        private double angle;
        private boolean hasHitTarget = false;
        private Explosion explosion;
        
        private boolean fired = false;
        
        public void setFired(boolean fired) {
            this.fired = fired;
        }

        public boolean isFired() {
            return fired;
        }
        
        public Cannonball(double startX, double startY, double endX, double endY) {
            this.x = startX;
            this.y = startY;
            this.angle = Math.toDegrees(Math.atan2(endY - startY, endX - startX));
            this.explosion = null;
        }

        public void setVelocity(double velocity) {
            this.velocity = velocity;
        }

        public void setExplosion(Explosion explosion) {
            this.explosion = explosion;
        }

        public Explosion getExplosion() {
            return explosion;
        }

        public void update() {
            if (hasHitTarget) {
                // If the cannonball hits a target, update the associated explosion
                if (explosion != null) {
                    explosion.update();
                }
                return;
            }

            x += Math.cos(Math.toRadians(angle)) * velocity;
            y += Math.sin(Math.toRadians(angle)) * velocity;
        }

        public boolean isOutOfBounds() {
            return x < 0 || x > WIDTH || y < 0 || y > HEIGHT;
        }

        public boolean collidesWithTarget(Rectangle target) {
            if (hasHitTarget) {
                return false;
            }

            double targetX = target.getX();
            double targetY = target.getY();
            double targetWidth = target.getWidth();
            double targetHeight = target.getHeight();

            return x < targetX + targetWidth && x + 10 > targetX && y < targetY + targetHeight && y + 10 > targetY;
        }

        public boolean collidesWithBlocker(Rectangle blocker) {
            double blockerX = blocker.getX();
            double blockerY = blocker.getY();
            double blockerWidth = blocker.getWidth();
            double blockerHeight = blocker.getHeight();

            return x < blockerX + blockerWidth && x > blockerX && y < blockerY + blockerHeight && y > blockerY;
        }
        
        
        
        
        public void reflectOffBlocker() {
            // Reverse the direction by adjusting the angle
            angle = (angle + 180) % 360;
        }


        public void draw(GraphicsContext gc) {
        	if (hasHitTarget) {
                // Remove the aim line when the cannonball hits the target
                aimLineStartX = 0;
                aimLineStartY = HEIGHT / 2;
                return;
            }

            gc.setFill(Color.BLACK);
            gc.fillOval(x - 5, y - 5, 10, 10);

            if (explosion != null && explosion.isActive()) {
                explosion.draw(gc);
            }
        }

        public void setHitTarget(boolean hitTarget) {
            hasHitTarget = hitTarget;
        }

        public boolean hasHitTarget() {
            return hasHitTarget;
        }
    }

    class Explosion {
        private double x, y;
        private double maxRadius;
        private double currentRadius;
        private boolean active;

        public Explosion(double x, double y, double maxRadius) {
            this.x = x;
            this.y = y;
            this.maxRadius = maxRadius;
            this.currentRadius = 1;
            this.active = true;
        }

        public void update() {
            if (active) {
                currentRadius += 5; // Increase the radius for a more noticeable effect

                if (currentRadius > maxRadius) {
                    active = false;
                }
            }
        }

        public void draw(GraphicsContext gc) {
            if (active) {
                double alpha = 1.0 - (currentRadius / maxRadius);
                gc.setFill(new Color(1, 0, 0, alpha));

                // Draw a filled circle for a more realistic explosion effect
                gc.fillOval(x - currentRadius, y - currentRadius, 2 * currentRadius, 2 * currentRadius);
            }
        }

        public boolean isActive() {
            return active;
        }
    }

    

    private void updateTimer() {
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;

        long remainingTime = gameTimeLimit - elapsedTime;

        if (remainingTime <= 0) {
            timerTimeline.stop();
            endGame();
        }

        // Update the timer display
        timerText.setText("Time: " + remainingTime / 1000 + " seconds");
    }

    private void endGame() {
        gameTimeline.stop();
        
     // Stop the background music
        backgroundMusicPlayer.stop();
        
        if (targetsDestroyed == TARGET_COUNT) {
            // Play the level completion sound
            levelCompletionSound.play();
        } else {
            // Play the game-over sound
            gameOverSound.play();
        }

        
        displayGameOverDialog();
    }
    
    

    private void displayGameOverDialog() {
        Platform.runLater(() -> {
            // Use JavaFX Alert to display a dialog with game results
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("Game Over");

            // Display appropriate message based on whether the player won or lost
            if (targetsDestroyed == TARGET_COUNT) {
                alert.setContentText("Congratulations! You won!\nShots fired: " + shotsFired +
                        "\nElapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
            } else {
                alert.setContentText("Sorry, you lost.\nShots fired: " + shotsFired +
                        "\nElapsed Time: " + (System.currentTimeMillis() - startTime) / 1000 + " seconds");
            }

            alert.showAndWait();
        });
    }
}