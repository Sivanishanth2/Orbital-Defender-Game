import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


 // Orbital Defender Game
 // The main class that initializes the game window and uses a CardLayout
 // to manage the different game states (Menu, Game, GameOver).
 
public class OrbitalDefender {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Orbital Defender");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout);

            GamePanel gamePanel = new GamePanel(mainPanel, cardLayout);
            MenuPanel menuPanel = new MenuPanel(mainPanel, cardLayout, gamePanel);
            GameOverPanel gameOverPanel = new GameOverPanel(mainPanel, cardLayout, gamePanel);

            mainPanel.add(menuPanel, "menu");
            mainPanel.add(gamePanel, "game");
            mainPanel.add(gameOverPanel, "gameover");

            frame.add(mainPanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            cardLayout.show(mainPanel, "menu");
        });
    }
}


 // Represents any object in the game world with a position and velocity.

abstract class SpaceObject {
    protected double x, y;
    protected double velX, velY;
    protected double size;

    public abstract void draw(Graphics2D g2d);
    public void update() {
        x += velX;
        y += velY;
    }
    public Point.Double getPosition() {
        return new Point.Double(x, y);
    }
}


//  Represents the player's ship. It handles orbital movement and aiming.

class PlayerShip extends SpaceObject {
    private double angle; // Angle on the orbit in radians
    private final double ORBIT_RADIUS = 200;
    private final double ROTATION_SPEED = 0.05;
    private int health = 100;
    public Point turretTarget;

    public PlayerShip() {
        this.angle = 0;
        this.size = 20;
        this.turretTarget = new Point(GamePanel.WIDTH / 2, 0);
        updatePosition();
    }
    
    private void updatePosition() {
        this.x = GamePanel.WIDTH / 2 + ORBIT_RADIUS * Math.cos(angle);
        this.y = GamePanel.HEIGHT / 2 + ORBIT_RADIUS * Math.sin(angle);
    }
    
    public void rotateLeft() {
        angle -= ROTATION_SPEED;
        updatePosition();
    }
    
    public void rotateRight() {
        angle += ROTATION_SPEED;
        updatePosition();
    }

    public void takeDamage(int amount) {
        health -= amount;
        if (health < 0) health = 0;
    }
    
    public int getHealth() { return health; }

    @Override
    public void draw(Graphics2D g2d) {
        // Draw the turret first (underneath the ship)
        double targetAngle = Math.atan2(turretTarget.y - y, turretTarget.x - x);
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(4));
        g2d.drawLine((int)x, (int)y, 
                     (int)(x + 30 * Math.cos(targetAngle)), 
                     (int)(y + 30 * Math.sin(targetAngle)));

        // Draw the ship's body
        Path2D.Double shipShape = new Path2D.Double();
        shipShape.moveTo(x + size * Math.cos(angle), y + size * Math.sin(angle));
        shipShape.lineTo(x + (size/2) * Math.cos(angle + Math.PI / 2), y + (size/2) * Math.sin(angle + Math.PI / 2));
        shipShape.lineTo(x + (size/2) * Math.cos(angle - Math.PI / 2), y + (size/2) * Math.sin(angle - Math.PI / 2));
        shipShape.closePath();
        
        g2d.setColor(new Color(0, 191, 255));
        g2d.fill(shipShape);
        g2d.setColor(Color.CYAN);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(shipShape);
    }
}


//  Represents a projectile fired by the player.

class Projectile extends SpaceObject {
    public Projectile(double startX, double startY, double targetX, double targetY) {
        this.x = startX;
        this.y = startY;
        this.size = 5;
        double angle = Math.atan2(targetY - startY, targetX - startX);
        double speed = 15;
        this.velX = speed * Math.cos(angle);
        this.velY = speed * Math.sin(angle);
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
        g2d.fillOval((int)(x - size / 2), (int)(y - size / 2), (int)size, (int)size);
    }
}


//  Represents an asteroid enemy.
 
class Asteroid extends SpaceObject {
    private final Shape shape;
    private final double rotationSpeed;
    private double currentAngle = 0;

    public Asteroid(double x, double y, double size) {
        this.x = x;
        this.y = y;
        this.size = size;
        
        double angleToCenter = Math.atan2(GamePanel.HEIGHT / 2.0 - y, GamePanel.WIDTH / 2.0 - x);
        double speed = 1.0 + new Random().nextDouble() * 2.0;
        this.velX = speed * Math.cos(angleToCenter);
        this.velY = speed * Math.sin(angleToCenter);
        this.shape = createAsteroidShape();
        this.rotationSpeed = (new Random().nextDouble() - 0.5) * 0.05;
    }

    private Shape createAsteroidShape() {
        Path2D.Double path = new Path2D.Double();
        Random rand = new Random();
        int points = 8 + rand.nextInt(5);
        double angleStep = 2 * Math.PI / points;
        
        path.moveTo(size * 0.8, 0);
        for (int i = 1; i < points; i++) {
            double radius = size * (0.7 + rand.nextDouble() * 0.3);
            path.lineTo(radius * Math.cos(i * angleStep), radius * Math.sin(i * angleStep));
        }
        path.closePath();
        return path;
    }
    
    @Override
    public void draw(Graphics2D g2d) {
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(currentAngle);
        
        g2d.setColor(new Color(139, 69, 19)); // Brown
        g2d.fill(shape);
        g2d.setColor(new Color(92, 64, 51));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(shape);
        
        g2d.setTransform(oldTransform);
    }
    
    @Override
    public void update() {
        super.update();
        currentAngle += rotationSpeed;
    }
}


 
// The main panel where gameplay occurs. Manages game state, rendering, and updates.
 
class GamePanel extends JPanel implements ActionListener {
    public static final int WIDTH = 900;
    public static final int HEIGHT = 700;
    
    private Timer timer;
    private PlayerShip player;
    private List<Projectile> projectiles;
    private List<Asteroid> asteroids;
    private Random random = new Random();
    private boolean isPaused = false;
    private int planetHealth = 1000;
    private int score = 0;
    private int spawnRate = 100; // Lower is faster
    private int frameCount = 0;
    
    private boolean moveLeft, moveRight;
    
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public GamePanel(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        
        addKeyBindings();
        addMouseListeners();
    }
    
    public void startGame() {
        player = new PlayerShip();
        projectiles = new ArrayList<>();
        asteroids = new ArrayList<>();
        score = 0;
        planetHealth = 1000;
        isPaused = false;
        moveLeft = false;
        moveRight = false;
        spawnRate = 100;
        
        requestFocusInWindow();
        timer = new Timer(1000 / 60, this);
        timer.start();
    }
    
    private void addKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "moveLeft_pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "moveLeft_released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "moveRight_pressed");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "moveRight_released");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "pause");

        am.put("moveLeft_pressed", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveLeft = true; } });
        am.put("moveLeft_released", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveLeft = false; } });
        am.put("moveRight_pressed", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveRight = true; } });
        am.put("moveRight_released", new AbstractAction() { public void actionPerformed(ActionEvent e) { moveRight = false; } });
        am.put("pause", new AbstractAction() { public void actionPerformed(ActionEvent e) { isPaused = !isPaused; repaint(); } });
    }

    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isPaused && player != null) {
                    projectiles.add(new Projectile(player.x, player.y, e.getX(), e.getY()));
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (player != null) {
                    player.turretTarget = e.getPoint();
                }
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Stars
        drawStars(g2d);
        
        // Draw Planet
        g2d.setColor(new Color(100, 150, 255));
        g2d.fillOval(WIDTH/2 - 50, HEIGHT/2 - 50, 100, 100);
        g2d.setColor(new Color(173, 216, 230));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawOval(WIDTH/2 - 50, HEIGHT/2 - 50, 100, 100);

        if (player == null) return;
        
        player.draw(g2d);
        for (Projectile p : projectiles) p.draw(g2d);
        for (Asteroid a : asteroids) a.draw(g2d);
        
        drawHUD(g2d);

        if (isPaused) {
            drawPauseScreen(g2d);
        }
    }
    
    private void drawStars(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        Random starRand = new Random(0); // Seeded for consistent starfield
        for (int i=0; i < 100; i++) {
            int x = starRand.nextInt(WIDTH);
            int y = starRand.nextInt(HEIGHT);
            g2d.fillOval(x, y, 2, 2);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setFont(new Font("Consolas", Font.BOLD, 20));
        g2d.setColor(Color.CYAN);
        g2d.drawString("SCORE: " + score, 20, 30);

        g2d.setColor(Color.GREEN);
        g2d.drawString("SHIP HEALTH: " + player.getHealth(), 20, 60);

        g2d.setColor(Color.ORANGE);
        g2d.drawString("PLANET HEALTH: " + planetHealth, 20, 90);

        g2d.setColor(Color.GRAY);
        g2d.drawString("Press 'P' to Pause", WIDTH - 200, 30);
    }
    
    private void drawPauseScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 50));
        String text = "PAUSED";
        int width = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, WIDTH/2 - width/2, HEIGHT/2);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isPaused) {
            updateGame();
        }
        repaint();
    }
    
    private void updateGame() {
        frameCount++;
        if (moveLeft) player.rotateLeft();
        if (moveRight) player.rotateRight();

        // Spawn Asteroids
        if (frameCount % spawnRate == 0) {
            spawnAsteroid();
            if (spawnRate > 30) spawnRate--; // Increase spawn rate over time
        }

        // Update all game objects
        player.update();
        for (Projectile p : projectiles) p.update();
        for (Asteroid a : asteroids) a.update();
        
        checkCollisions();
        cleanupObjects();
    }
    
    private void spawnAsteroid() {
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = Math.max(WIDTH, HEIGHT) / 2.0;
        double x = WIDTH / 2.0 + radius * Math.cos(angle);
        double y = HEIGHT / 2.0 + radius * Math.sin(angle);
        double size = 20 + random.nextDouble() * 30;
        asteroids.add(new Asteroid(x, y, size));
    }
    
    private void checkCollisions() {
        // Projectiles with Asteroids
        Iterator<Projectile> p_iter = projectiles.iterator();
        while(p_iter.hasNext()) {
            Point.Double p_pos = p_iter.next().getPosition();
            Iterator<Asteroid> a_iter = asteroids.iterator();
            while(a_iter.hasNext()) {
                Asteroid asteroid = a_iter.next();
                Point.Double a_pos = asteroid.getPosition();
                if (p_pos.distance(a_pos) < asteroid.size) {
                    a_iter.remove();
                    p_iter.remove();
                    score += 100;
                    break;
                }
            }
        }
        
        // Asteroids with Player
        Iterator<Asteroid> a_iter = asteroids.iterator();
        while(a_iter.hasNext()) {
            Asteroid asteroid = a_iter.next();
            if (player.getPosition().distance(asteroid.getPosition()) < asteroid.size) {
                a_iter.remove();
                player.takeDamage(25);
                if (player.getHealth() <= 0) {
                    gameOver();
                    return;
                }
            }
        }

        // Asteroids with Planet
        a_iter = asteroids.iterator();
        while(a_iter.hasNext()) {
            Asteroid asteroid = a_iter.next();
            if (asteroid.getPosition().distance(WIDTH/2, HEIGHT/2) < 50 + asteroid.size) {
                a_iter.remove();
                planetHealth -= 50;
                 if (planetHealth <= 0) {
                    gameOver();
                    return;
                }
            }
        }
    }

    private void cleanupObjects() {
        projectiles.removeIf(p -> p.x < 0 || p.x > WIDTH || p.y < 0 || p.y > HEIGHT);
    }
    
    private void gameOver() {
        timer.stop();
        GameOverPanel gameOverPanel = (GameOverPanel) mainPanel.getComponent(2);
        gameOverPanel.setFinalScore(score);
        cardLayout.show(mainPanel, "gameover");
    }
    
    public void resetGame() {
        startGame();
    }
}


//  The main menu panel with title and start button.
 
class MenuPanel extends JPanel {
    public MenuPanel(JPanel mainPanel, CardLayout cardLayout, GamePanel gamePanel) {
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        
        JLabel title = new JLabel("ORBITAL DEFENDER");
        title.setFont(new Font("Consolas", Font.BOLD, 70));
        title.setForeground(Color.CYAN);
        add(title, gbc);
        
        JLabel instructions1 = new JLabel("Use LEFT/RIGHT arrows to move. Use MOUSE to aim. CLICK to shoot.");
        instructions1.setFont(new Font("Consolas", Font.PLAIN, 18));
        instructions1.setForeground(Color.WHITE);
        add(instructions1, gbc);
        
        JLabel instructions2 = new JLabel("Protect the planet from the asteroid belt!");
        instructions2.setFont(new Font("Consolas", Font.PLAIN, 18));
        instructions2.setForeground(Color.WHITE);
        gbc.insets = new Insets(15, 15, 50, 15);
        add(instructions2, gbc);

        JButton startButton = new JButton("START MISSION");
        startButton.setFont(new Font("Consolas", Font.BOLD, 24));
        startButton.setBackground(new Color(0, 191, 255));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.setBorder(BorderFactory.createEmptyBorder(15, 50, 15, 50));
        startButton.addActionListener(e -> {
            gamePanel.startGame();
            cardLayout.show(mainPanel, "game");
        });
        gbc.insets = new Insets(15, 15, 15, 15);
        add(startButton, gbc);
    }
}


 // The game over panel, displaying the final score and a play again button.
 
class GameOverPanel extends JPanel {
    private JLabel finalScoreLabel;

    public GameOverPanel(JPanel mainPanel, CardLayout cardLayout, GamePanel gamePanel) {
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel("MISSION FAILED");
        title.setFont(new Font("Consolas", Font.BOLD, 70));
        title.setForeground(Color.RED);
        add(title, gbc);
        
        finalScoreLabel = new JLabel("FINAL SCORE: 0");
        finalScoreLabel.setFont(new Font("Consolas", Font.BOLD, 30));
        finalScoreLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(15, 15, 50, 15);
        add(finalScoreLabel, gbc);

        JButton playAgainButton = new JButton("RETRY MISSION");
        playAgainButton.setFont(new Font("Consolas", Font.BOLD, 24));
        playAgainButton.setBackground(new Color(0, 200, 100));
        playAgainButton.setForeground(Color.BLACK);
        playAgainButton.setFocusPainted(false);
        playAgainButton.setBorder(BorderFactory.createEmptyBorder(15, 50, 15, 50));
        playAgainButton.addActionListener(e -> {
            gamePanel.resetGame();
            cardLayout.show(mainPanel, "game");
        });
        gbc.insets = new Insets(15, 15, 15, 15);
        add(playAgainButton, gbc);
    }
    
    public void setFinalScore(int score) {
        finalScoreLabel.setText("FINAL SCORE: " + score);
    }
}
