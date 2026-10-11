package object;

import main.GamePanel;

public class OBJ_Key extends SuperObject {
    GamePanel gp;

    public OBJ_Key(GamePanel gp) {
        this.gp = gp;
        name = "Key";
        try {
            image = javax.imageio.ImageIO.read(getClass().getResourceAsStream("/res/objects/Key.png"));
            uTool.scaleImage(image, gp.tileSize, gp.tileSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
