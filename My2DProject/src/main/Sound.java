package main;

import javax.sound.sampled.Clip;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class Sound {
    Clip clip;
    URL soundURL[] = new URL[30];

    public Sound() {
        soundURL[0] = getClass().getResource("/res/sound/Track 5 - Ithyphallophobie.wav");
        soundURL[1] = getClass().getResource("/res/sound/Coin.wav");
        soundURL[2] = getClass().getResource("/res/sound/PowerUp.wav");
        soundURL[3] = getClass().getResource("/res/sound/DoorUnlocked.wav");
        soundURL[4] = getClass().getResource("/res/sound/DoorOpen.wav");
        soundURL[5] = getClass().getResource("/res/sound/Jump.wav");
        soundURL[6] = getClass().getResource("/res/sound/Stairs.wav");
        soundURL[7] = getClass().getResource("/res/sound/Victorious.wav");
    }

    public void setFile(int i) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL[i]);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void play() {
        clip.start();
    }

    public void loop() {
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stop() {
        clip.stop();
    }
}
