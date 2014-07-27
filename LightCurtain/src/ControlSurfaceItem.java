import java.util.Observable;
import java.util.function.Consumer;

public class ControlSurfaceItem extends Observable {
	private int id;
	private int value;
	private int row;
	private int column;
	private String label;
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
		setChanged();
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

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}
}