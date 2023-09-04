import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

public class SoundManager {

	private static HashMap<String, Clip> clips;
	private static int gap;
	private static boolean mute = false;
	private static FloatControl gainControl;
	
	public static void init() {
		clips = new HashMap<String, Clip>();
		gap = 0;
	}
	
	public static void load(String s, String n) {
		if(clips.get(n) != null) {
			return;
		}
		
		Clip clip;
		try {
			InputStream in = SoundManager.class.getClass().getResourceAsStream(s);
			InputStream bufferedIn = new BufferedInputStream(new FileInputStream(new File(s)));
			AudioInputStream ais = AudioSystem.getAudioInputStream(bufferedIn);
			AudioFormat baseFormat = ais.getFormat();
			AudioFormat decodeFormat = new AudioFormat(
					AudioFormat.Encoding.PCM_SIGNED,
					baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
					baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
					false);
			AudioInputStream dais = AudioSystem.getAudioInputStream(decodeFormat, ais);
			AudioFormat format = dais.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format, AudioSystem.NOT_SPECIFIED);
			clip = (Clip) AudioSystem.getLine(info);
			
			clip.open(dais);
			
			gainControl = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
			gainControl.setValue(-40);
			
			clips.put(n, clip);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static FloatControl getGainControl() {
		return gainControl;
	}
	
	public static void play(String s) {
		play(s, gap);
	}
	
	public static void play(String s, int i) {
		if(mute) {
			return;
		}
		
		Clip c = clips.get(s);
		if(c == null) {
			return;
		}
		if(c.isRunning()) {
			c.stop();
		}
		c.setFramePosition(i);
		while(!c.isRunning()) {
			c.start();
		}
	}
	
	public static void stop(String s) {
		if(clips.get(s) == null) {
			return;
		}
		if(clips.get(s).isRunning()) {
			clips.get(s).stop();
		}
	}
	
	public static void resume(String s) {
		if(mute) {
			return;
		}
		if(clips.get(s).isRunning()) {
			return;
		}
		clips.get(s).start();
	}
	
	public static void setVolume(float value) {
		float newValue = gainControl.getValue() + value;
		
		if(newValue > gainControl.getMaximum()) {
			newValue = gainControl.getMaximum();
		}
		
		if(newValue < gainControl.getMinimum()) {
			newValue = gainControl.getMinimum();
		}
		
		gainControl.setValue(newValue);
	}
	
	public static void loop(String s) {
		loop(s, gap, gap, clips.get(s).getFrameLength() - 1);
	}
	
	public static void loop(String s, int frame) {
		loop(s, frame, gap, clips.get(s).getFrameLength() - 1);
	}
	
	public static void loop(String s, int start, int end) {
		loop(s, gap, start, end);
	}
	
	public static void loop(String s, int frame, int start, int end) {
		stop(s);
		if(mute) {
			return;
		}
		clips.get(s).setLoopPoints(start, end);
		clips.get(s).setFramePosition(frame);
		clips.get(s).loop(Clip.LOOP_CONTINUOUSLY);
	}
	
	public static void setPosition(String s, int frame) {
		clips.get(s).setFramePosition(frame);
	}
	
	public static int getFrames(String s) {
		return clips.get(s).getFrameLength();
	}
	
	public static int getPosition(String s) {
		return clips.get(s).getFramePosition();
	}
	
	public static void close(String s) {
		stop(s);
		clips.get(s).close();
	}
}
