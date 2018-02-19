import java.util.ArrayList;

public class Column {
	private String name;
	private String type;
	private ArrayList<Object> values;
	
	public Column() {
		values = new ArrayList<Object>();
	}
	
public Column(String name, String type, ArrayList<Object> values) {
		this.setName(name);
		this.setType(type);
		this.setValues(values);
	}

public ArrayList<Object> getValues() {
	return values;
}

public void setValues(ArrayList<Object> values) {
	this.values = values;
}

public String getType() {
	return type;
}

public void setType(String type) {
	this.type = type;
}

public String getName() {
	return name;
}

public void setName(String name) {
	this.name = name;
}

public void addValue(Object value) {
	values.add(value);
}

}
