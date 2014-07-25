import java.awt.Rectangle;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.stream.IntStream;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.serial.Serial;
import promidi.MidiIO;
import ddf.minim.AudioInput;
import ddf.minim.Minim;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;
import dmxP512.DmxP512;


public class LightCurtain extends PApplet implements Observer{

	/**
	 * @author cpenhale - cwpenhale@gmail.com
	 */
	private static final long serialVersionUID = -5022282393686973266L;

	// APP CONFIG
	private static final int FRAME_RATE = 60;
	private static Properties prefs = new Properties();
	private static int colorCounter = 0;
	private static int coloredPointer = 0;
	private Minim minim;
	private AudioInput lineIn;
	private FFT fft;
	private BeatDetect beatDetect;
	private static float bgBright;
	private float threshold = 1300;
	private static int dHeight;
	private static int dWidth;
	private static int specCounter;
	private static int specSize;
	private static float[] bandEnergies;
	private static int specDiv;
	private static int steps;
	
	//PANEL
	private static int errorCount = 0;
	private static int[] gammatable = new int[256];
	private float gamma = (float) 1.7;
	private static int numPorts = 0;  // the number of serial ports in use
	private static int maxPorts=24; // maximum number of serial ports
	private static Serial[] ledSerial = new Serial[maxPorts];     // each port's actual Serial port
	private static Rectangle[] ledArea = new Rectangle[maxPorts]; // the area of the movie each port gets, in % (0-100)
	private static boolean[] ledLayout = new boolean[maxPorts];   // layout of rows, true = even is left->right
	private static PImage[] ledImage = new PImage[maxPorts];      // image sent to each port
	//END PANEL
	
	//DMX
	private static String DMXPRO_PORT = "/dev/tty.usbserial-A7XSBYHC";
	private static int DMXPRO_BAUDRATE = 115000;
	private DmxP512 dmxOutput;
	int universeSize=128;
	//END DMX

	private K2Controller k2;
	private boolean disableMotionBlur;
	private int motionBlurKey;
	
	//BPM counter
	private float averageBpm;
	private int nearestWhole;
	private int[] bpmHits;
	private int hitPointer;

	private float averageDistanceBetweenBeatsInMillis;

	private int counter = 1;
	private static final int BPM_HIT_HISTORY = 8;
	//
	
	public void setup() {
		defaultConfig();
		size(1280, 720);
		steps = 4;
		loadPrefs();
		colorMode(HSB, 100);
		frameRate(new Float(FRAME_RATE));
		//sound activation
		minim = new Minim(this);
		lineIn = minim.getLineIn();
	    fft = new FFT(lineIn.bufferSize(), lineIn.sampleRate());
	    beatDetect = new BeatDetect(lineIn.bufferSize(), lineIn.sampleRate());
	    beatDetect.setSensitivity(150);
		background(0);
		k2 = new K2Controller(MidiIO.getInstance(this));
		
		// PANEL
		serialConfigure("/dev/tty.usbmodem461521");  // change these to your port names
		serialConfigure("/dev/tty.usbmodem465661");
		if (errorCount > 0) exit();
		for (int i=0; i < 256; i++) {
		  gammatable[i] = (int)(pow((float) ((float)i / 255.0), gamma) * 255.0 + 0.5);
		}
		//END PANEL
		
		//DMX
		dmxOutput=new DmxP512(this);
	    dmxOutput.setupDmxPro(DMXPRO_PORT,DMXPRO_BAUDRATE );
		//END DMX
	    ControlSurfaceItem bl = k2.getButtons().stream().parallel().filter(b->b.getLabel().equals("BL")).findFirst().get();
	    if(bl!=null){
	    	System.out.println("BUTTTS");
	    	bl.addObserver(this);
	    }
	    
	    //BPM
	    bpmHits = new int[BPM_HIT_HISTORY];
	    //BPM
	}
	
	public void draw() {
		background(0);
		bgBright = k2.getFaders().stream().filter(v->v.getColumn()==0).findFirst().get().getValue();
		beatDetect.detect(lineIn.mix);
		fft.forward(lineIn.mix);
		soundDetection();
		specSize = 60;
		bandEnergies = new float[specSize];
		for(int i = 0 ; i<specSize; i++){
			bandEnergies[i] = (int) fft.getBand(i);
		}
		specDiv = (int) (1280/specSize);
		noStroke();
		rectMode(CENTER);
		for(int i = 0; i < specSize; i++) {
			ColoredVector v = getNextColor(new PVector(1,1));
			fill(v.getColor(), v.getSaturation(), bgBright);
			rect((i*60)+30,360,40,10+(bandEnergies[i]/2)*bandEnergies[i]);
		}
		drawLightCurtain(get());
	}
	
	private void soundDetection(){
		if(millis()%averageDistanceBetweenBeatsInMillis<=20){
			bgBright = 100;
			System.out.println(counter++);
			if(counter>4){
				counter = 1;
			}
		}
		if(beatDetect.isHat()){
			dmx();
			bgBright+=5;
		}
		if(beatDetect.isSnare()){
			bgBright+=10;
		}
	}
	

	private void serialConfigure(String portName){
		if (numPorts > maxPorts) {
			println("too many serial ports, please increase maxPorts");
			errorCount++;
			return;
		}
		try {
			ledSerial[numPorts] = new Serial(this, portName);
			if (ledSerial[numPorts] == null)
				throw new NullPointerException();
			ledSerial[numPorts].write('?');
		} catch (Throwable e) {
			e.printStackTrace();
			println("Serial port " + portName
					+ " does not exist or is non-functional");
			errorCount++;
			return;
		}
		delay(50);
		String line = ledSerial[numPorts].readStringUntil(10);
		if (line == null) {
			println("Serial port " + portName + " is not responding.");
			println("Is it really a Teensy 3.0 running VideoDisplay?");
			errorCount++;
		}
		String param[] = line.split(",");
		if (param.length != 12) {
			println("Error: port " + portName + " did not respond to LED config query");
			errorCount++;
			return;
		}
		// only store the info and increase numPorts if Teensy responds properly
		ledImage[numPorts] = new PImage(Integer.parseInt(param[0]), Integer.parseInt(param[1]), RGB);
		ledArea[numPorts] = new Rectangle(Integer.parseInt(param[5]), Integer.parseInt(param[6]),
		Integer.parseInt(param[7]), Integer.parseInt(param[8]));
		ledLayout[numPorts] = (Integer.parseInt(param[5]) == 0);
		numPorts++;
	}
	
	@Override
	protected void handleKeyEvent(KeyEvent e) {
		if(e.isControlDown()&&e.getKeyCode()==77){
			System.out.println("MOTION BLUR TOGGLE");
			if(motionBlurKey%2==0){
				if(isDisableMotionBlur()){
					motionBlurKey = 0;
					setDisableMotionBlur(false);
				}else{
					motionBlurKey = 0;
					setDisableMotionBlur(true);
				}
			}
			motionBlurKey++;
		}
		if(e.isControlDown()&&e.getKeyCode()==83){
			System.out.println("SAVING");
			savePrefs();
		}
		if(e.isControlDown()&&e.getKeyCode()==68){
			System.out.println("DEFAULTING");
			defaultConfig();
			savePrefs();
		}
		if(e.isControlDown()&&e.getKeyCode()==76){
			System.out.println("LOADING");
			loadPrefs();
		}
		if(e.getKeyCode()==38){
			threshold+=25;
			System.out.println("tUp: "+ threshold);
		}
		if(e.getKeyCode()==40){
			threshold-=25;
			System.out.println("tDown: "+ threshold);
		}
		super.handleKeyEvent(e);
	}
	
	private void loadPrefs() {
		try {
			FileInputStream input = new FileInputStream("prefs.properties");
			prefs.load(input);
//			loadCameraState();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void savePrefs() {
//		saveCameraState();
		try {
			FileOutputStream output = new FileOutputStream("prefs.properties");
			prefs.store(output, null);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void defaultConfig() {
		prefs.setProperty("camX", "100");
		prefs.setProperty("camY", "100");
		prefs.setProperty("camZ", "1000");
		prefs.setProperty("rotX", "1000");
		prefs.setProperty("rotY", "1000");
		prefs.setProperty("rotZ", "1000");
		prefs.setProperty("zoom", "50");
	}
	
	private void dmx(){
		dmxOutput.set(1, 60); // 6 channel mode
		dmxOutput.set(3, 0); //STROBe
		dmxOutput.set(4, (int) red(color(colorCounter, 100, bgBright))); 
		dmxOutput.set(5, (int) green(color(colorCounter, 100, bgBright)));
		dmxOutput.set(6, (int) blue(color(colorCounter, 100, bgBright)));
	}
	
	private void forEachPoint(ColoredVector vector){
		beginShape(POINTS);
		stroke(vector.getColor(), vector.getSaturation(), vector.getBrightness(), vector.getAlpha());
		vertex(vector.getV().x, vector.getV().y, vector.getV().z);
		endShape();
	}

	public static float inverseScale(float zValue) {
		return bgBright+20 + (-1 * zValue/100);
	}
	
	public static int scaleMidiToDMX(int midiValue){
		return (int) (((float) midiValue/ (float) 127) * (float) 255);
	}
	
	private void drawLightCurtain(PImage image){
		  for (int i=0; i < numPorts; i++) {    
			    // copy a portion of the movie's image to the LED image
			    int xoffset = percentage((int) 1280, ledArea[i].x);
			    int yoffset = percentage((int) 720, ledArea[i].y);
			    int xwidth =  percentage((int) 1280, ledArea[i].width);
			    int yheight = percentage((int) 720, ledArea[i].height);
			    ledImage[i].copy(image, xoffset, yoffset, xwidth, yheight,
			                     0, 0, ledImage[i].width, ledImage[i].height);
			    // convert the LED image to raw data
			    byte[] ledData =  new byte[(ledImage[i].width * ledImage[i].height * 3) + 3];
			    image2data(ledImage[i], ledData, ledLayout[i]);
			    if (i == 0) {
			      ledData[0] = '*';  // first Teensy is the frame sync master
			      int usec = (int)((1000000.0 / FRAME_RATE) * 0.75);
			      ledData[1] = (byte)(usec);   // request the frame sync pulse
			      ledData[2] = (byte)(usec >> 8); // at 75% of the frame time
			    } else {
			      ledData[0] = '%';  // others sync to the master board
			      ledData[1] = 0;
			      ledData[2] = 0;
			    }
			    // send the raw data to the LEDs  :-)
			    ledSerial[i].write(ledData); 
			  }
	}
	
	// scale a number by a percentage, from 0 to 100
	private int percentage(int num, int percent) {
	  double mult = percentageFloat(percent);
	  double output = num * mult;
	  return (int)output;
	}

	// scale a number by the inverse of a percentage, from 0 to 100
	private int percentageInverse(int num, int percent) {
	  double div = percentageFloat(percent);
	  double output = num / div;
	  return (int)output;
	}
	
	// convert an integer from 0 to 100 to a float percentage
	// from 0.0 to 1.0.  Special cases for 1/3, 1/6, 1/7, etc
	// are handled automatically to fix integer rounding.
	double percentageFloat(int percent) {
	  if (percent == 33) return 1.0 / 3.0;
	  if (percent == 17) return 1.0 / 6.0;
	  if (percent == 14) return 1.0 / 7.0;
	  if (percent == 13) return 1.0 / 8.0;
	  if (percent == 11) return 1.0 / 9.0;
	  if (percent ==  9) return 1.0 / 11.0;
	  if (percent ==  8) return 1.0 / 12.0;
	  return (double)percent / 100.0;
	}
	
	private void image2data(PImage image, byte[] data, boolean layout) {
		  int offset = 3;
		  int x, y, xbegin, xend, xinc, mask;
		  int linesPerPin = image.height / 8;
		  int pixel[] = new int[8];
		  
		  for (y = 0; y < linesPerPin; y++) {
		    if ((y & 1) == (layout ? 0 : 1)) {
		      // even numbered rows are left to right
		      xbegin = 0;
		      xend = image.width;
		      xinc = 1;
		    } else {
		      // odd numbered rows are right to left
		      xbegin = image.width - 1;
		      xend = -1;
		      xinc = -1;
		    }
		    for (x = xbegin; x != xend; x += xinc) {
		      for (int i=0; i < 8; i++) {
		        // fetch 8 pixels from the image, 1 for each pin
		        pixel[i] = image.pixels[x + (y + linesPerPin * i) * image.width];
		        pixel[i] = colorWiring(pixel[i]);
		      }
		      // convert 8 pixels to 24 bytes
		      for (mask = 0x800000; mask != 0; mask >>= 1) {
		        byte b = 0;
		        for (int i=0; i < 8; i++) {
		          if ((pixel[i] & mask) != 0) b |= (1 << i);
		        }
		        data[offset++] = b;
		      }
		    }
		  } 
		}
	
	// translate the 24 bit color from RGB to the actual
	// order used by the LED wiring.  GRB is the most common.
	private int colorWiring(int c) {
	  int red = (c & 0xFF0000) >> 16;
	  int green = (c & 0x00FF00) >> 8;
	  int blue = (c & 0x0000FF);
	  red = gammatable[red];
	  green = gammatable[green];
	  blue = gammatable[blue];
	  return (green << 16) | (red << 8) | (blue); // GRB - most common wiring
	}

	public static ColoredVector getNextColor(PVector v) {
//		//iterate from 0 to 100 for HUE (HSB)
		if (colorCounter >= 100) {
			colorCounter = 0;
		}
		if (coloredPointer >= 2) {
			coloredPointer = 0;
			specCounter++;
			// reset spec counter when it has cycled through the spectrums
			if(specCounter>=specSize){
				specCounter = 0;
			}
			colorCounter += (int) bandEnergies[specCounter];
		}
		coloredPointer++;
		float inverse = inverseScale(v.z);
		return new ColoredVector(v, colorCounter , 100, bgBright, inverse);
	}
	
	public static void main(String args[]) {
		PApplet.main(new String[] { "--bgcolor=#ECE9D8", "LightCurtainz" });
	}

	public boolean isDisableMotionBlur() {
		return disableMotionBlur;
	}

	public void setDisableMotionBlur(boolean disableMotionBlur) {
		this.disableMotionBlur = disableMotionBlur;
	}

	@Override
	public void update(Observable o, Object arg) {
		if(o instanceof ControlSurfaceItem){
			ControlSurfaceItem item = (ControlSurfaceItem) o;
			//LOGIC
			if(item.getLabel().equals("BL")){
				if(item.getValue()==127){
					if(hitPointer==8)
						hitPointer = 0;
					bpmHits[hitPointer++] = millis();
					int historyCounter = 0;
					int[] history = new int[BPM_HIT_HISTORY/2];
					for(int i=0; i<BPM_HIT_HISTORY/2; i++){
						history[historyCounter++] 
								= 
								bpmHits[i+1]
										- bpmHits[i];
					}
					averageDistanceBetweenBeatsInMillis = Math.abs((float) Arrays.stream(history).average().getAsDouble());
					averageBpm = Math.abs(60000/(float) Arrays.stream(history).average().getAsDouble());
					System.out.println(averageBpm);
					System.out.println(averageDistanceBetweenBeatsInMillis);
				}
			}
		}
	}
	
	
}
