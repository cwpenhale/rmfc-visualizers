import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Observable;
import java.util.function.Consumer;

import processing.core.PApplet;
import promidi.Controller;
import promidi.MidiIO;
import promidi.MidiOut;
import promidi.Note;

public class K2Controller {

	/**
	 * @author cpenhale - cwpenhale@gmail.com
	 */
	private static final String DEVICE_NAME = "boxputer";
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
	private Collection<ControlSurfaceItem> buttons;
	private int inputDevice;
	private int outputDevice;
	private MidiOut midiOut;

	public K2Controller(MidiIO instance){
		midiIO = instance;
		setButtons(new ArrayList<ControlSurfaceItem>());
		setRotaries(new ArrayList<ControlSurfaceItem>());
		setPots(new ArrayList<ControlSurfaceItem>());
		setFaders(new ArrayList<Fader>());
		setControls(new ArrayList<ControlSurfaceItem>());
		findDevice();
		initializeControl();
	}
	
	public void lightUp(ControlSurfaceItem item, int color){
		System.out.println("LIGHT");
		int note = item.getId();
		if(color==1){
			note = note*(color*4);
		}

	}
	
	public void bpmFlash(){
		getMidiOut().sendNote(new Note(12, 127, 6));
		getMidiOut().sendNote(new Note(24, 127, 6));
		getMidiOut().sendNote(new Note(0, 127, 6));
		getMidiOut().sendNote(new Note(28, 127, 6));
		getMidiOut().sendNote(new Note(32, 127, 6));
		getMidiOut().sendNote(new Note(36, 127, 6));
		getMidiOut().sendNote(new Note(12, 0, 6));
		getMidiOut().sendNote(new Note(24, 0, 6));
		getMidiOut().sendNote(new Note(0, 0, 6));
		getMidiOut().sendNote(new Note(28, 0, 6));
		getMidiOut().sendNote(new Note(32, 0, 6));
		getMidiOut().sendNote(new Note(36, 0, 6));
	}

	public void handleNote(Note note) {
		getButtons().stream().filter(b -> b.getId() == note.getPitch()).peek(b -> b.setValue(note.getVelocity())).forEach(b -> b.notifyObservers());
	}
	
	public void handleController(Controller event){
		getControls().stream().parallel().filter(i -> i.getId() == event.getNumber()).forEach(i -> i.setValue(event.getValue()));
		//getControls().stream().parallel().filter(i -> i.getId() == event.getNumber()).forEach(i -> i.getAction().accept(i));
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
		setMidiOut(midiIO.getMidiOut(MIDI_CHANNEL-1, getOutputDevice()));
	}
	
	private void initializeControl(){
		initializeFaders();
		initializePots();
		initializeRotaries();
		initializeButtons();
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

	private void initializeButtons(){
		ControlSurfaceItem[] buttons =  new ControlSurfaceItem[18];
		int i = 0;
		//big one left
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(12);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("BL");
		//big one right
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(15);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("BR");
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(36);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("A");
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(37);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("B");
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(38);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("C");
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(39);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("D");
		//E
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(32);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("E");
		//F
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(33);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("F");
		//G
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(34);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("G");
		//H
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(35);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("H");
		//I
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(28);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("I");
		//J
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(29);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("J");
		//K
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(30);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("K");
		//L
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(31);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("L");
		//M
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(24);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("M");
		//N
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(25);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("N");
		//O
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(26);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("O");
		//P
		i++;
		buttons[i] = new ControlSurfaceItem();
		buttons[i].setId(27);
		buttons[i].setValue(0);
		buttons[i].setColumn(0);
		buttons[i].setLabel("P");
		Collection<ControlSurfaceItem> buttonsList = Arrays.asList(buttons);
		getButtons().addAll(buttonsList);
		getControls().addAll(buttonsList);
	}
	
	private void initializeRotaries(){
		Consumer<ControlSurfaceItem> defaultAction = new Consumer<ControlSurfaceItem>() {
			@Override
			public void accept(ControlSurfaceItem f) {
				System.out.println("COLUMN #"+f.getColumn()+"Row #"+f.getRow()+" Rotary #"+f.getId()+"'s Value -> "+ f.getValue());
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
					rotaries[i].setRow(row);
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
	
	public Collection<ControlSurfaceItem> getButtons() {
		return buttons;
	}

	public void setButtons(Collection<ControlSurfaceItem> buttons) {
		this.buttons = buttons;
	}

	public MidiOut getMidiOut() {
		return midiOut;
	}

	public void setMidiOut(MidiOut midiOut) {
		this.midiOut = midiOut;
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
	

}
