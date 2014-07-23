import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import processing.core.PApplet;
import promidi.Controller;
import promidi.MidiIO;
import promidi.Note;

public class K2Controller {

	/**
	 * @author cpenhale - cwpenhale@gmail.com
	 */
	private static final String DEVICE_NAME = "XONE:K2";
	private static final int MIDI_CHANNEL = 4;
	private static final int  NUMBER_OF_FADERS = 4;
	private static final int FADER_FIRST_NOTE = 16;
	private static final int ROTARY_FIRST_NOTE = 4;
	private static final int POT_FIRST_NOTE = 0;
	private static final int NUMBER_OF_COLUMNS = 4;
	private static final int NUMBER_OF_ROTARIES_PER_COLUMN = 3;

	private MidiIO midiIO;
	private Collection<Fader> faders;
	private Collection<ControlSurfaceItem> pots;
	private Collection<ControlSurfaceItem> controls;
	private Collection<ControlSurfaceItem> rotaries;
	private int inputDevice;
	private int outputDevice;

	public K2Controller(MidiIO instance){
		midiIO = instance;
		setRotaries(new ArrayList<K2Controller.ControlSurfaceItem>());
		setPots(new ArrayList<K2Controller.ControlSurfaceItem>());
		setFaders(new ArrayList<K2Controller.Fader>());
		setControls(new ArrayList<K2Controller.ControlSurfaceItem>());
		findDevice();
		initializeControl();
	}

	public void handleNote(Note note) {
		System.out.println("PITCH: "+note.getPitch());
		System.out.println("LEN: "+note.getLength());
		System.out.println("d1: "+note.getData1());
		System.out.println("VEL "+note.getVelocity());
		
	}
	
	public void handleController(Controller event){
		getControls().stream().parallel().filter(i -> i.getId() == event.getNumber()).forEach(i -> i.setValue(event.getValue()));
		getControls().stream().parallel().filter(i -> i.getId() == event.getNumber()).forEach(i -> i.getAction().accept(i));
	}
	

	private void findDevice() {
		midiIO.printDevices();
		for (int i = 0; i < midiIO.numberOfInputDevices(); i++) {
			if (midiIO.getInputDeviceName(i).equals(DEVICE_NAME)) {
				setInputDevice(i);
				System.out.println("INPUT SET: "+ i);
			}
		}
		for (int i = 0; i < midiIO.numberOfOutputDevices(); i++) {
			if (midiIO.getOutputDeviceName(i).equals(DEVICE_NAME)) {
				setOutputDevice(i);
				System.out.println("OUTPUT SET: "+ i);
			}
		}
		midiIO.plug(this, "handleNote", getInputDevice(), MIDI_CHANNEL-1);
		midiIO.plug(this, "handleController", getInputDevice(), MIDI_CHANNEL-1);
	}
	
	private void initializeControl(){
		initializeFaders();
		initializePots();
		initializeRotaries();
	}
	
	private void initializeFaders(){
		Consumer<ControlSurfaceItem> defaultAction = new Consumer<ControlSurfaceItem>() {
			@Override
			public void accept(ControlSurfaceItem f) {
				System.out.println("COLUMN #"+f.getColumn()+" FADER #"+f.getId()+"'s Value -> "+ f.getValue());
			}
		};
		Fader[] faders =  new Fader[NUMBER_OF_FADERS];
		for(int i = 0; i< NUMBER_OF_FADERS; i++){
			faders[i] = new Fader();
			faders[i].setId(FADER_FIRST_NOTE+i);
			faders[i].setValue(0);
			faders[i].setDescription("Fader for Column #"+i);
			faders[i].setColumn(i);
			faders[i].setAction(defaultAction);
		}
		Collection<Fader> faderList = Arrays.asList(faders);
		getFaders().addAll(faderList);
		getControls().addAll(faderList);
	}
	
	private void initializePots(){
		Consumer<ControlSurfaceItem> defaultAction = new Consumer<ControlSurfaceItem>() {
			@Override
			public void accept(ControlSurfaceItem f) {
				System.out.println("COLUMN #"+f.getColumn()+" POT #"+f.getId()+"'s Value -> "+ f.getValue());
			}
		};
		ControlSurfaceItem[] pots =  new ControlSurfaceItem[NUMBER_OF_COLUMNS];
		for(int i = 0; i< NUMBER_OF_COLUMNS; i++){
			pots[i] = new ControlSurfaceItem();
			pots[i].setId(POT_FIRST_NOTE+i);
			pots[i].setValue(0);
			pots[i].setColumn(i);
			pots[i].setAction(defaultAction);
		}
		Collection<ControlSurfaceItem> potList = Arrays.asList(pots);
		getPots().addAll(potList);
		getControls().addAll(potList);
	}
	
	private void initializeRotaries(){
		Consumer<ControlSurfaceItem> defaultAction = new Consumer<ControlSurfaceItem>() {
			@Override
			public void accept(ControlSurfaceItem f) {
				System.out.println("COLUMN #"+f.getColumn()+" Rotary #"+f.getId()+"'s Value -> "+ f.getValue());
			}
		};
		ControlSurfaceItem[] rotaries =  new ControlSurfaceItem[NUMBER_OF_COLUMNS*NUMBER_OF_ROTARIES_PER_COLUMN];
		System.out.println("LEN"+ rotaries.length);
		int i = 0;
		for(int column = 0; column < NUMBER_OF_COLUMNS; column++){
			for(int row = 1; row <= NUMBER_OF_ROTARIES_PER_COLUMN; row++){
					rotaries[i] = new ControlSurfaceItem();
					rotaries[i].setId((ROTARY_FIRST_NOTE*row)+column);
					rotaries[i].setValue(0);
					rotaries[i].setColumn(column);
					rotaries[i].setAction(defaultAction);
					i++;
				}
		}
		Collection<ControlSurfaceItem> rotariesList = Arrays.asList(rotaries);
		getRotaries().addAll(rotariesList);
		getControls().addAll(rotariesList);
	}

	public void output() {

	}

	public static void main(String args[]) {
		PApplet.main(new String[] { "--bgcolor=#ECE9D8", "LightCurtainz" });
	}

	public int getInputDevice() {
		return inputDevice;
	}

	public void setInputDevice(int inputDevice) {
		this.inputDevice = inputDevice;
	}

	public int getOutputDevice() {
		return outputDevice;
	}

	public void setOutputDevice(int outputDevice) {
		this.outputDevice = outputDevice;
	}
	
	public Collection<Fader> getFaders() {
		return faders;
	}

	public void setFaders(Collection<Fader> faders) {
		this.faders = faders;
	}

	public Collection<ControlSurfaceItem> getControls() {
		return controls;
	}

	public void setControls(Collection<ControlSurfaceItem> controls) {
		this.controls = controls;
	}

	public Collection<ControlSurfaceItem> getPots() {
		return pots;
	}

	public void setPots(Collection<ControlSurfaceItem> pots) {
		this.pots = pots;
	}

	public Collection<ControlSurfaceItem> getRotaries() {
		return rotaries;
	}

	public void setRotaries(Collection<ControlSurfaceItem> rotaries) {
		this.rotaries = rotaries;
	}

	public class Fader extends ControlSurfaceItem {
		private static final int MIN_VAL = 0;
		private static final int MAX_VAL = 127;
		
		private String description;
		
		public Fader(){ }
		
		
		
		public String getDescription() {
			return description;
		}
		
		public void setDescription(String description) {
			this.description = description;
		}
		
	}
	
	public class ControlSurfaceItem{
		private int id;
		private int value;
		private int column;
		private Consumer<ControlSurfaceItem> action;
		
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
		
		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public int getColumn() {
			return column;
		}

		public void setColumn(int column) {
			this.column = column;
		}

		public Consumer<ControlSurfaceItem> getAction() {
			return action;
		}

		public void setAction(Consumer<ControlSurfaceItem> action) {
			this.action = action;
		}
	}
}
