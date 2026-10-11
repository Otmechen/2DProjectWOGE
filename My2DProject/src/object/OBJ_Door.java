package object;

import main.GamePanel;

public class OBJ_Door extends SuperObject {
    GamePanel gp;

    public OBJ_Door(GamePanel gp) {
        this.gp = gp;
        name = "Door";
        try {
            image = javax.imageio.ImageIO.read(getClass().getResourceAsStream("/res/objects/Door.png"));
            uTool.scaleImage(image, gp.tileSize, gp.tileSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        collision = true;
    }

}
