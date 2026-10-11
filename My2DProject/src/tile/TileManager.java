package tile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import main.GamePanel;
import main.UtilityTool;

/**
 * Carga los tiles desde la carpeta /res/tiles y dibuja el mapa.
 *
 * Formato de la carpeta (el mismo que usa el TileEditor):
 *
 * res/tiles/
 * 00_grass/ -> grupo 0, sin colision
 * grass_v0_s0.png -> variacion 0, forma 0
 * grass_v1_s3.png
 * 01_brick_solid/ -> grupo 1, SOLIDO (colision)
 * brick_v0_s0.png
 *
 * ID del tile en el mapa = grupo * 100 + variacion * 10 + forma
 * grass_v0_s0 = 0, grass_v1_s3 = 13, brick_v0_s0 = 100
 */
public class TileManager {

    static final String TILES_PATH = "/res/tiles";

    // IDs posibles: grupo (0-99) * 100 + variacion (0-9) * 10 + forma (0-9) ->
    // 0..9999
    static final int MAX_TILES = 10000;

    static final Pattern GROUP_RE = Pattern.compile("^(\\d{1,2})_([A-Za-z0-9-]+?)(_solid)?$", Pattern.CASE_INSENSITIVE);
    static final Pattern FILE_RE = Pattern.compile("^(.+)_v(\\d)_s(\\d)\\.png$", Pattern.CASE_INSENSITIVE);

    GamePanel gp;

    // Se indexa directamente por ID. Los IDs sin imagen apuntan a un tile
    // "faltante" (magenta),
    // asi que nunca hay null y CollisionChecker puede seguir usando
    // tile[id].collision.
    public Tile[] tile;
    public int mapTileNum[][];

    Tile missingTile;
    UtilityTool uTool = new UtilityTool();

    public TileManager(GamePanel gp) {
        this.gp = gp;

        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];

        loadTiles();
        loadMap("/res/maps/world01.txt");
    }

    // ===================================================================
    // Carga de tiles
    // ===================================================================
    public void loadTiles() {
        tile = new Tile[MAX_TILES];
        missingTile = createMissingTile();
        int loaded = 0;
        int warnings = 0;

        try {
            URL url = getClass().getResource(TILES_PATH);
            if (url == null) {
                System.err.println("TileManager: no se encontro la carpeta " + TILES_PATH);
            } else {
                Path root = pathOf(url);
                List<Path> groups;
                try (Stream<Path> s = Files.list(root)) {
                    groups = s.sorted().collect(Collectors.toList());
                }
                for (Path dir : groups) {
                    String dirName = nameOf(dir);
                    if (!Files.isDirectory(dir)) {
                        continue; // archivos sueltos en res/tiles: se ignoran
                    }
                    Matcher gm = GROUP_RE.matcher(dirName);
                    if (!gm.matches()) {
                        System.err.println("TileManager: carpeta ignorada (se esperaba NN_nombre): " + dirName);
                        warnings++;
                        continue;
                    }
                    int group = Integer.parseInt(gm.group(1));
                    boolean solid = gm.group(3) != null;

                    List<Path> files;
                    try (Stream<Path> s = Files.list(dir)) {
                        files = s.sorted().collect(Collectors.toList());
                    }
                    for (Path file : files) {
                        String fileName = nameOf(file);
                        if (!fileName.toLowerCase().endsWith(".png")) {
                            continue;
                        }
                        Matcher fm = FILE_RE.matcher(fileName);
                        if (!fm.matches()) {
                            System.err.println("TileManager: archivo ignorado (se esperaba nombre_v#_s#.png): "
                                    + dirName + "/" + fileName);
                            warnings++;
                            continue;
                        }
                        int id = group * 100 + Integer.parseInt(fm.group(2)) * 10 + Integer.parseInt(fm.group(3));
                        if (tile[id] != null) {
                            System.err.println(
                                    "TileManager: ID repetido (" + id + "), se ignora " + dirName + "/" + fileName);
                            warnings++;
                            continue;
                        }
                        if (loadTile(id, file, solid)) {
                            loaded++;
                        } else {
                            warnings++;
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        // Los IDs que no existen quedan apuntando al tile "faltante"
        for (int i = 0; i < tile.length; i++) {
            if (tile[i] == null) {
                tile[i] = missingTile;
            }
        }
        System.out.println("TileManager: " + loaded + " tiles cargados"
                + (warnings > 0 ? " (" + warnings + " advertencias)" : ""));
    }

    private boolean loadTile(int id, Path file, boolean solid) {
        try (InputStream is = Files.newInputStream(file)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null) {
                System.err.println("TileManager: no se pudo leer la imagen " + file);
                return false;
            }
            Tile t = new Tile();
            t.image = uTool.scaleImage(img, gp.tileSize, gp.tileSize);
            t.collision = solid;
            tile[id] = t;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tile magenta con una cruz: se dibuja cuando el mapa usa un ID que no tiene
     * imagen.
     */
    private Tile createMissingTile() {
        Tile t = new Tile();
        BufferedImage img = new BufferedImage(gp.tileSize, gp.tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.MAGENTA);
        g2.fillRect(0, 0, gp.tileSize, gp.tileSize);
        g2.setColor(Color.BLACK);
        g2.drawLine(0, 0, gp.tileSize - 1, gp.tileSize - 1);
        g2.drawLine(gp.tileSize - 1, 0, 0, gp.tileSize - 1);
        g2.dispose();
        t.image = img;
        t.collision = false;
        return t;
    }

    /**
     * Devuelve la carpeta de tiles como Path, tanto si esta en disco como dentro de
     * un .jar.
     */
    private Path pathOf(URL url) throws IOException, URISyntaxException {
        URI uri = url.toURI();
        if ("jar".equals(uri.getScheme())) {
            FileSystem fs;
            try {
                fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            } catch (FileSystemAlreadyExistsException e) {
                fs = FileSystems.getFileSystem(uri);
            }
            return fs.getPath(TILES_PATH);
        }
        return Paths.get(uri);
    }

    private String nameOf(Path p) {
        // dentro de un .jar los nombres de carpeta pueden terminar en "/"
        return p.getFileName().toString().replaceAll("/$", "");
    }

    // ===================================================================
    // Mapa
    // ===================================================================
    public void loadMap(String filePath) {
        InputStream is = getClass().getResourceAsStream(filePath);
        if (is == null) {
            System.err.println("TileManager: no se encontro el mapa " + filePath);
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            boolean widthWarned = false;

            for (int row = 0; row < gp.maxWorldRow; row++) {
                String line = br.readLine();
                while (line != null && line.trim().isEmpty()) {
                    line = br.readLine(); // saltear lineas en blanco
                }
                if (line == null) {
                    System.err.println("TileManager: al mapa " + filePath + " le faltan filas (tiene " + row
                            + ", el juego espera " + gp.maxWorldRow + ")");
                    break;
                }

                String[] numbers = line.trim().split("\\s+");
                if (numbers.length != gp.maxWorldCol && !widthWarned) {
                    System.err.println("TileManager: la fila " + row + " tiene " + numbers.length
                            + " columnas, el juego espera " + gp.maxWorldCol);
                    widthWarned = true;
                }

                for (int col = 0; col < gp.maxWorldCol && col < numbers.length; col++) {
                    int num = Integer.parseInt(numbers[col]);
                    if (num < 0 || num >= MAX_TILES) {
                        System.err.println(
                                "TileManager: ID fuera de rango (" + num + ") en col " + col + ", fila " + row);
                        num = 0;
                    }
                    mapTileNum[col][row] = num;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===================================================================
    // Dibujo
    // ===================================================================
    public void draw(Graphics2D g2) {
        int worldCol = 0;
        int worldRow = 0;
        while (worldCol < gp.maxWorldCol && worldRow < gp.maxWorldRow) {

            int tileNum = mapTileNum[worldCol][worldRow];

            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;
            int screenX = worldX - gp.player.worldX + gp.player.screenX;
            int screenY = worldY - gp.player.worldY + gp.player.screenY;

            if (worldX + gp.tileSize > gp.player.worldX - gp.player.screenX &&
                    worldX - gp.tileSize < gp.player.worldX + gp.player.screenX &&
                    worldY + gp.tileSize > gp.player.worldY - gp.player.screenY &&
                    worldY - gp.tileSize < gp.player.worldY + gp.player.screenY) {

                g2.drawImage(tile[tileNum].image, screenX, screenY, null);
            }
            worldCol++;

            if (worldCol == gp.maxWorldCol) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}