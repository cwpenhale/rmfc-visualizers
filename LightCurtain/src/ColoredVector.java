import processing.core.PVector;


public class ColoredVector {
	
	private PVector v;
	private int color;
	private float saturation;
	private float brightness;
	private float alpha;
	
	public ColoredVector(){};
	
	public ColoredVector(PVector v, int color, float saturation, float brightness, float alpha){
		setV(v);
		setColor(color);
		setSaturation(saturation);
		setBrightness(brightness);
		setAlpha(alpha);
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	public PVector getV() {
		return v;
	}

	public void setV(PVector v) {
		this.v = v;
	}

	public float getSaturation() {
		return saturation;
	}

	public void setSaturation(float saturation) {
		this.saturation = saturation;
	}

	public float getBrightness() {
		return brightness;
	}

	public void setBrightness(float brightness) {
		this.brightness = brightness;
	}

	public float getAlpha() {
		return alpha;
	}

	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}
}
