package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class GameModelImpl implements GameModel {

    private final GameState gameState = new GameState();
    private final Player player = new Player(400, 600, 50, 50);
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final Random random = new Random();
    private double spawnTimer = 0;

    @Override
    public void initGame() {
        gameState.resetScore();
        gameState.setStatus(GameStatus.PLAYING);
        player.heal(player.getMaxHealth());
        player.setAlive(true);
        enemies.clear();
        bullets.clear();
        items.clear();
        spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime) {
        if (gameState.getStatus() != GameStatus.PLAYING) {
            return;
        }
        
        player.update(deltaTime);
        
        if (!player.isAlive()) {
            gameState.setStatus(GameStatus.GAME_OVER);
            return;
        }
        
        spawnTimer += deltaTime;
        if (spawnTimer >= 2.0) {
            spawnEnemy();
            spawnTimer = 0;
        }
        
        updateEntities(enemies, deltaTime);
        updateEntities(bullets, deltaTime);
        updateEntities(items, deltaTime);
        
        removeDeadEntities();
        
        checkCollisions();
        
        if (gameState.getScore() >= 500) {
            gameState.setStatus(GameStatus.VICTORY);
        }
    }

    private void spawnEnemy() {
        double x = random.nextDouble() * (GameConfig.WINDOW_WIDTH - 50);
        EnemyType type = EnemyType.NORMAL;
        Enemy enemy = new NormalEnemy(x, -50, 40, 40, type, 2, 50);
        enemies.add(enemy);
    }

    private void updateEntities(List<? extends Entity> entities, double deltaTime) {
        for (Entity entity : entities) {
            entity.update(deltaTime);
        }
    }

    private void removeDeadEntities() {
        enemies.removeIf(enemy -> !enemy.isAlive());
        bullets.removeIf(bullet -> !bullet.isAlive());
        items.removeIf(item -> !item.isAlive());
    }

    private void checkCollisions() {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            if (!bullet.isPlayerBullet()) continue;
            
            Iterator<Enemy> enemyIter = enemies.iterator();
            while (enemyIter.hasNext()) {
                Enemy enemy = enemyIter.next();
                if (isColliding(bullet, enemy)) {
                    enemy.takeDamage(bullet.getDamage());
                    bullet.setAlive(false);
                    
                    if (!enemy.isAlive()) {
                        addScore(enemy.getScore());
                    }
                    break;
                }
            }
        }
    }

    private boolean isColliding(Entity a, Entity b) {
        return a.getX() < b.getX() + b.getWidth() &&
               a.getX() + a.getWidth() > b.getX() &&
               a.getY() < b.getY() + b.getHeight() &&
               a.getY() + a.getHeight() > b.getY();
    }

    @Override
    public void movePlayer(double dx, double dy) {
        player.move(dx, dy);
    }

    @Override
    public Player getPlayer() { return player; }

    @Override
    public List<Enemy> getEnemies() { return enemies; }

    @Override
    public List<Bullet> getBullets() { return bullets; }

    @Override
    public List<Item> getItems() { return items; }

    @Override
    public int getScore() { return (int) gameState.getScore(); }

    @Override
    public int getHealth() { return player.getHealth(); }

    @Override
    public int getLevel() { return gameState.getLevel(); }

    @Override
    public GameStatus getStatus() { return gameState.getStatus(); }

    @Override
    public void addScore(int amount) {
        gameState.addScore(amount);
    }
}
