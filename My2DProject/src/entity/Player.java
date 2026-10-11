package entity;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import main.GamePanel;
import main.KeyHandler;
import main.UtilityTool;

public class Player extends Entity {

    GamePanel gp;
    KeyHandler keyH;

    public final int screenX;
    public final int screenY;

    public int hasKey = 0;

    int standCounter = 0;

    public Player(GamePanel gp, KeyHandler keyH) {

        this.gp = gp;
        this.keyH = keyH;

        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        solidArea = new Rectangle();
        solidArea.x = 25;
        solidArea.y = 50;
        solidAreaDefaultX = solidArea.x;
        solidAreaDefaultY = solidArea.y;
        solidArea.width = 45;
        solidArea.height = 42;

        setDefaultValues();
        getPlayerImage();
    }

    public void setDefaultValues() {
        worldX = gp.tileSize * 23;
        worldY = gp.tileSize * 22;
        speed = 4;
        direction = "down";
        spriteNum = 0;
    }

    // Orden = índice de spriteNum: 0 = idle, 1..n = caminata
    private static final String[] FRAME_NAMES = { "Idle", "Walk1", "Walk2" };

    public void getPlayerImage() {
        up = loadFrames("Up");
        down = loadFrames("Down");
        left = loadFrames("Left");
        right = loadFrames("Right");
    }

    private BufferedImage[] loadFrames(String dir) {
        UtilityTool uTool = new UtilityTool();
        BufferedImage[] frames = new BufferedImage[FRAME_NAMES.length];
        try {
            for (int i = 0; i < frames.length; i++) {
                String path = "/res/player/" + dir + FRAME_NAMES[i] + "-Char.png";
                InputStream is = getClass().getResourceAsStream(path);
                if (is == null) {
                    throw new IOException("No se encontró el sprite: " + path);
                }
                frames[i] = ImageIO.read(is);
                frames[i] = uTool.scaleImage(frames[i], gp.tileSize, gp.tileSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frames;
    }

    public void update() {
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            if (keyH.upPressed)
                direction = "up";
            if (keyH.downPressed)
                direction = "down";
            if (keyH.leftPressed)
                direction = "left";
            if (keyH.rightPressed)
                direction = "right";

            standCounter = 0;
            if (spriteNum == 0) { // venía de idle: arrancar la caminata ya
                spriteNum = 1;
                spriteCounter = 0;
            }

            // CHECK TILE COLLISION
            collisionOn = false;
            gp.cChecker.checkTile(this);

            // CHECK OBJECT COLLISION
            int objIndex = gp.cChecker.checkObject(this, true);
            pickUpObject(objIndex);

            // IF COLLISION IS FALSE, PLAYER CAN MOVE
            if (!collisionOn) {
                switch (direction) {
                    case "up":
                        worldY -= speed;
                        break;
                    case "down":
                        worldY += speed;
                        break;
                    case "left":
                        worldX -= speed;
                        break;
                    case "right":
                        worldX += speed;
                        break;
                }
            }

            spriteCounter++;
            if (spriteCounter > 12) {
                spriteNum++;
                if (spriteNum >= FRAME_NAMES.length) {
                    spriteNum = 1; // vuelve al primer frame de caminata, nunca al idle
                }
                spriteCounter = 0;
            }
        } else {
            standCounter++;
            if (standCounter >= 20) {
                spriteNum = 0; // idle
                standCounter = 0;
            }
        }
    }

    public void pickUpObject(int i) {
        if (i != 999) {
            String objectName = gp.obj[i].name;

            switch (objectName) {
                case "Key":
                    gp.playSE(1);
                    gp.obj[i] = null;
                    hasKey++;
                    gp.ui.showMessage("You got a key! Keys: " + hasKey);
                    break;
                case "Door":
                    if (hasKey > 0) {
                        gp.playSE(4);
                        gp.obj[i] = null;
                        hasKey--;
                        gp.ui.showMessage("You opened the door! Keys left: " + hasKey);
                    } else {
                        gp.ui.showMessage("You need a key to open this door.");
                    }
                    break;
                case "Chest":
                    gp.ui.gameFinished = true;
                    gp.stopMusic();
                    gp.playSE(7);
                    break;
                case "Boots":
                    gp.playSE(2);
                    gp.obj[i] = null;
                    speed += 2;
                    gp.ui.showMessage("You put on the boots! Your speed has increased.");
                    break;
            }
        }
    }

    public void draw(Graphics2D g2) {
        BufferedImage[] frames = switch (direction) {
            case "up" -> up;
            case "down" -> down;
            case "left" -> left;
            default -> right;
        };

        g2.drawImage(frames[spriteNum], screenX, screenY, null);
        g2.setColor(Color.red);

        if (keyH.checkDrawTime)
            g2.drawRect(screenX + solidAreaDefaultX, screenY + solidAreaDefaultY, solidArea.width, solidArea.height);
    }
}
