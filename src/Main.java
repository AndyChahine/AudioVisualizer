import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main {

	boolean playCompleted;
	int frameLength;
	FloatControl gainControl;
	float mouseX, mouseY, percent;
	boolean dragging;

	public Main() {
		String fileName = "resources/music/makememove.wav";
		File file = new File(fileName);
		try {
			InputStream bufferedIn = new BufferedInputStream(new FileInputStream(file));
			AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);

			frameLength = (int) audioInputStream.getFrameLength();
			int frameSize = (int) audioInputStream.getFormat().getFrameSize();

			byte[] bytes = new byte[frameLength * frameSize];

			int result = audioInputStream.read(bytes);

			int numChannels = audioInputStream.getFormat().getChannels();
			int[][] toReturn = new int[numChannels][frameLength];

			int sampleIndex = 0;

			for (int t = 0; t < bytes.length;) {
				for (int channel = 0; channel < numChannels; channel++) {
					int low = (int) bytes[t];
					t++;
					int high = (int) bytes[t];
					t++;
					int sample = (high << 8) + (low & 0x00ff);
					toReturn[channel][sampleIndex] = sample;
				}

				sampleIndex++;
			}

//			Sample[] samples = new Sample[frameLength / 41000];

			SoundManager.init();
			SoundManager.load(fileName, "clip1");

			JPanel panel = new JPanel() {
				@Override
				public void paint(Graphics g) {
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, this.getWidth(), this.getHeight());

					g.setColor(Color.LIGHT_GRAY.darker());
					g.drawLine(0, this.getHeight() / 2, this.getWidth(), this.getHeight() / 2);

					g.setColor(Color.GREEN);

					int samplesPerPixel = frameLength / this.getWidth() + 100;

					for (int i = 0; i < frameLength / samplesPerPixel; i++) {
						int[] blockSamples = Arrays.copyOfRange(toReturn[0], i * samplesPerPixel,
								samplesPerPixel + i * samplesPerPixel);
//						int min = (int)(((-(getMin(blockSamples) / 32768.0f)) + 1) * this.getHeight() / 2);
//						int max = (int)(((-(getMax(blockSamples) / 32768.0f)) + 1) * this.getHeight() / 2);

						int min2 = (int) (getMin(blockSamples) / 300);
						int max2 = (int) (getMax(blockSamples) / 300);

						g.drawLine(i + 10, min2 + this.getHeight() - 200, i + 10, max2 + this.getHeight() - 200);
					}
//
//					g.setColor(Color.MAGENTA);
//					for (int i = 0; i < frameLength / samplesPerPixel; i++) {
//						int[] blockSamples = Arrays.copyOfRange(toReturn[1], i * samplesPerPixel,
//								samplesPerPixel + i * samplesPerPixel);
////						int min = (int)(((-(getMin(blockSamples) / 32768.0f)) + 1) * this.getHeight() / 2);
////						int max = (int)(((-(getMax(blockSamples) / 32768.0f)) + 1) * this.getHeight() / 2);
//
//						int min2 = getMin(blockSamples) / 300;
//						int max2 = getMax(blockSamples) / 300;
//
//						g.drawLine(i + 10, min2 + this.getHeight() - 200, i + 10, max2 + this.getHeight() - 200);
//
//					}
					
					int freqs = 1024;
					Complex[] x = new Complex[freqs];
					for(int j = 0; j < SoundManager.getPosition("clip1") / 1024; j++) {
						int[] blockSamples = Arrays.copyOfRange(toReturn[0], j * freqs, freqs + j * freqs);
						for(int k = 0; k < freqs; k++) {
							x[k] = new Complex(blockSamples[k], 0);
						}
//						System.out.println(SoundManager.getPosition("clip1") / 1024 + j);
//						x[j] = new Complex(toReturn[0][j * SoundManager.getPosition("clip1") / 1024], 0);
					}
					Complex[] y = FFT.fft(x);

					int oldX = this.getWidth() / 2 - freqs / 2;
					int oldY = this.getHeight() / 2;
					int oldX2 = this.getWidth() / 2 - freqs / 2;
					int oldY2 = this.getHeight() / 2;
					for(int i = 0; i < freqs; i++) {
						g.setColor(Color.MAGENTA);
						double re = y[i].re();
						double im = y[i].im();
						double mag = Math.sqrt((re * re) + (im * im)) / 10;
//						g.drawLine(i + this.getWidth() / 2 - 1024 / 2, this.getHeight() / 2, i + this.getWidth() / 2 - 1024 / 2, (int)(mag / this.getHeight()));
//						g.drawLine(i + this.getWidth() / 2 - 1024 / 2, this.getHeight() - this.getHeight() / 2, i + this.getWidth() / 2 - 1024 / 2, (int)(this.getHeight() - this.getHeight() / 2 - mag / 1000));
						
						g.setColor(Color.YELLOW);
						g.drawLine(oldX, oldY, i + this.getWidth() / 2 - freqs / 2, (int)(this.getHeight() - this.getHeight() / 2 - mag / 1000));
						oldX = i + this.getWidth() / 2 - freqs / 2;
						oldY = (int)(this.getHeight() - this.getHeight() / 2 - mag / 1000);
					}

					g.setColor(Color.CYAN);
					int increment = SoundManager.getPosition("clip1") / samplesPerPixel;
					g.drawLine(increment + 10, this.getHeight() / 2, increment + 10, this.getHeight());
				}
			};
			panel.setPreferredSize(new Dimension(1366, 768));
			Mouse mouse = new Mouse();
			panel.addMouseListener(mouse);
			panel.addMouseMotionListener(mouse);
			panel.addMouseWheelListener(mouse);

			JFrame frame = new JFrame("Audio Visualizer");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setResizable(false);
			frame.add(panel);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);

			SoundManager.play("clip1");

			while (true) {
				
				if(Mouse.isScrolling()) {
					SoundManager.setVolume(Mouse.getScrollAmount());
				}
				
				if(Mouse.getMouseDown(MouseEvent.BUTTON1)) {
					mouseX = Mouse.getMouseX();
					mouseY = Mouse.getMouseY();
					percent = (float)mouseX / (float)(panel.getWidth());
					SoundManager.stop("clip1");
//					SoundManager.setPosition("clip1", (int)(percent * SoundManager.getFrames("clip1")));
					SoundManager.play("clip1", (int)(percent * SoundManager.getFrames("clip1")));
				}
				Mouse.update();
				panel.repaint();
				
				Thread.sleep(16);
			}
		} catch (UnsupportedAudioFileException | IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public int getMax(int[] array) {
		int maxValue = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] > maxValue) {
				maxValue = array[i];
			}
		}

		return maxValue;
	}

	public int getMin(int[] array) {
		int minValue = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] < minValue) {
				minValue = array[i];
			}
		}

		return minValue;
	}
	
	public double getMax(double[] array) {
		double maxValue = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] > maxValue) {
				maxValue = array[i];
			}
		}

		return maxValue;
	}

	public double getMin(double[] array) {
		double minValue = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] < minValue) {
				minValue = array[i];
			}
		}

		return minValue;
	}

	public static void main(String[] args) {
		Main main = new Main();
	}
}
