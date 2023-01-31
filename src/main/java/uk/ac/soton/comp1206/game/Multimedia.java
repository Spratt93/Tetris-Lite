package uk.ac.soton.comp1206.game;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Multimedia {
    private static final Logger logger = LogManager.getLogger(Multimedia.class);
    private static MediaPlayer audioPlayer;
    private static MediaPlayer backgroundMusic;

    public static void playAudio(String file) {
        String toPlay = Multimedia.class.getResource("/sounds/" + file).toExternalForm();
        try {
            Media audio = new Media(toPlay);
            audioPlayer = new MediaPlayer(audio);
            logger.info("Playing: " + file);
            audioPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Unable to play " + file);
        }
    }

    public static void playBackgroundMusic(String file) {
        String toPlay = Multimedia.class.getResource("/music/" + file).toExternalForm();
        try {
            Media bg_audio = new Media(toPlay);
            backgroundMusic = new MediaPlayer(bg_audio);
            logger.info("Playing: " + file);
            //probs a neater way of doing this
            backgroundMusic.setCycleCount(200);
            backgroundMusic.play();
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("Unable to play " + file);
        }
    }

    public static void closeBackgroundMusic() {
        backgroundMusic.stop();
        logger.info("Closing" + backgroundMusic.toString());
    }
}
