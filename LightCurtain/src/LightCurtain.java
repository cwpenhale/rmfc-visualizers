import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.Stream;

import peasy.PeasyCam;
import processing.core.PApplet;
import processing.core.PVector;
import processing.event.KeyEvent;
import SimpleOpenNI.SimpleOpenNI;
import ddf.minim.AudioInput;
import ddf.minim.Minim;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;


public class LightCurtain extends PApplet {

	/**
	 * @author cpenhale - cwpenhale@gmail.com
	 */
	private static final long serialVersionUID = -5022282393686973266L;

	// APP CONFIG
	private static final int FRAME_RATE = 30;

	private static Properties prefs = new Properties();

	private SimpleOpenNI context;
	private static int colorCounter = 0;
	private static int coloredPointer = 0;
	private PeasyCam cam;
	private Minim minim;
	private AudioInput lineIn;
	private FFT fft;
	private BeatDetect beatDetect;

	private static float bgBright;

	private int bgColor;

	private float threshold = 1300;
	private static int dHeight;
	private static int dWidth;
	private static int specCounter;
	private static int specSize;
	private static float[] bandEnergies;
	private static int specDiv;
	private static int steps;
	

	public void setup() {
		defaultConfig();
		size(1280, 720, OPENGL);
		steps = 4;
		// peasy
		cam = new PeasyCam(this, 100);
		loadPrefs();
		context = new SimpleOpenNI(this);
		if (context.isInit() == false) {
			println("Can't init SimpleOpenNI, maybe the camera is not connected!");
			exit();
			return;
		}
		// enable depthMap generation
		context.enableDepth();
		dHeight = context.depthHeight();
		dWidth = context.depthHeight();
		// color
		colorMode(HSB, 100);
		frameRate(new Float(FRAME_RATE));
		//sound activation
		minim = new Minim(this);
		lineIn = minim.getLineIn();
	    fft = new FFT(lineIn.bufferSize(), lineIn.sampleRate());
	    beatDetect = new BeatDetect(lineIn.bufferSize(), lineIn.sampleRate());
	    beatDetect.setSensitivity(10);
		background(0);
	}
	
	@Override
	protected void handleKeyEvent(KeyEvent e) {
		System.out.println("KEYS: "+e.getKeyCode());
		if(e.isControlDown()&&e.getKeyCode()==83){
			System.out.println("SAVING");
			savePrefs();
		}
		if(e.isControlDown()&&e.getKeyCode()==76){
			System.out.println("LOADING");
			loadPrefs();
		}
		if(e.getKeyCode()==38){
			threshold+=100;
			System.out.println("tUp: "+ threshold);
		}
		if(e.getKeyCode()==40){
			threshold-=100;
			System.out.println("tDown: "+ threshold);
		}
		super.handleKeyEvent(e);
	}
	
	private void loadPrefs() {
		try {
			FileInputStream input = new FileInputStream("prefs.properties");
			prefs.load(input);
			loadCameraState();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void savePrefs() {
		saveCameraState();
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

	private void saveCameraState() {
		float[] rots = cam.getRotations();
		float[] pozs = cam.getPosition();
		prefs.setProperty("camX", String.valueOf(pozs[0]));
		prefs.setProperty("camY", String.valueOf(pozs[1]));
		prefs.setProperty("camZ", String.valueOf(pozs[2]));
		prefs.setProperty("rotX", String.valueOf(rots[0]));
		prefs.setProperty("rotY", String.valueOf(rots[1]));
		prefs.setProperty("rotZ", String.valueOf(rots[2]));
		prefs.setProperty("zoom", String.valueOf(cam.getDistance()));
	}
	
	private void loadCameraState() {
		if(cam!=null){
			cam.setRotations(
					new Float(prefs.getProperty("rotX")),
					new Float(prefs.getProperty("rotY")),
					new Float(prefs.getProperty("rotZ"))
			);
			cam.lookAt(
				Double.valueOf(prefs.getProperty("camX")),
				Double.valueOf(prefs.getProperty("camY")),
				Double.valueOf(prefs.getProperty("camZ"))
			);
			cam.setDistance(Double.valueOf(prefs.getProperty("zoom")));
		}
	}

	private void defaultConfig() {
		prefs.setProperty("camX", "0");
		prefs.setProperty("camY", "0");
		prefs.setProperty("camZ", "0");
		prefs.setProperty("rotX", "0");
		prefs.setProperty("rotY", "0");
		prefs.setProperty("rotZ", "0");
		prefs.setProperty("zoom", "0");
	}

	public void draw() {
		camera();
		noLights();
		hint(DISABLE_DEPTH_TEST);
		fill(0, 20);
		rect(0,0,1280,720);
		hint(ENABLE_DEPTH_TEST);
		loadCameraState();
		lights();
		// update the cam
		context.update();
		strokeWeight((float) steps);
		beatDetect.detect(lineIn.mix);
		if(beatDetect.isKick()){
			bgBright = 100;
		}else{
			bgBright -= 5;
		}
		if(beatDetect.isHat()){
			bgBright+=5;
		}
		if(beatDetect.isSnare()){
			bgBright+=10;
		}
		fft.forward(lineIn.mix);
		specSize = fft.specSize();
		bandEnergies = new float[specSize];
		for(int i = 0 ; i<specSize; i++){
			bandEnergies[i] = (int) fft.getBand(i) / (int) 10;
		}
		specDiv = (int) (27065/specSize);
		Stream.of(context.depthMapRealWorld())
			.parallel()
			.filter(v -> (int) v.x % steps == 0 && (int) v.y % steps == 0 && v.z <= threshold)
			.map(LightCurtain::getNextColor)
			.sequential()
			.forEach(this::forEachPoint);
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
	

	public static ColoredVector getNextColor(PVector v) {
//		//iterate from 0 to 100 for HUE (HSB)
		if (colorCounter >= 100) {
			colorCounter = 0;
		}
		if (coloredPointer >= specDiv) {
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
}
