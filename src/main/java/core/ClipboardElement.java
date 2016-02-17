package core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Set;

import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.input.DataFormat;

public class ClipboardElement implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1164391321598868110L;
	
	final static String NO_ELEMENT_WRITTEN = "Nothing is written here";
	final static String ELEMENT_WRITTEN = "Here is an element";
	
	private transient ReadOnlyStringProperty recordTime;
	private transient StringProperty name;
	private transient MapProperty<DataFormat, Object> contents;
	
	public ClipboardElement(String name, long recordedTime) {
		this(recordedTime);
		setName(name);
	}
	
	public ClipboardElement(long recordTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(recordTime);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String recordTimeString = sdf.format(cal.getTime());
		
		setRecordTimeProperty(recordTimeString);
	}
	
	// private constructor needed for deserialization
	@SuppressWarnings("unused")
	private ClipboardElement() {
		
	}
	
	public ReadOnlyStringProperty recordTimeProperty() {
		if(recordTime == null) {
			throw new IllegalStateException("No record time specified. Clipboard element was created faulty");
		}
		return recordTime;
	}
	
	private void setRecordTimeProperty(String recordTimeString) {
		this.recordTime = new SimpleStringProperty(this, "recordTime", recordTimeString);
	}
	
	public String getRecordTime() {
		return recordTimeProperty().get();
	}
	
	public StringProperty nameProperty() {
		if(name == null) {
			name = new SimpleStringProperty(this, "name", "-unnamed-");
		}
		
		return name;
	}
	
	public void setName(String name) {
		nameProperty().set(name);
	}
	
	public String getName() {
		return nameProperty().get();
	}
	
	public MapProperty<DataFormat, Object> contentsProperty() {
		if(contents == null) {
			contents = new SimpleMapProperty<DataFormat, Object>(this, "contents", FXCollections.observableHashMap());
		}
		
		return contents;
	}
	
	/**
	 * This method add clipboard content of a specified format to the element.
	 * 
	 * @param format - The DataFormat of this content
	 * @param content - The data to be stored for later uses
	 * @throws IllegalArgumentException - if there is already some content with the specified DataFormat
	 */
	public void addContent(DataFormat format, Object content) throws IllegalArgumentException {
		if(!contentsProperty().containsKey(format)) {
			contentsProperty().put(format, content);
		} else {
			throw new IllegalArgumentException("There are already contents with this DataFormat in this element");
		}
	}
	
	public int getMapEntryCount() {
		return contentsProperty().getSize();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		LinkedList<Serializable> writeObjects = new LinkedList<Serializable>();
		
		writeObjects.add(name.get());
		writeObjects.add(recordTime.get());
		int writableDataFormats = 0;
		
		for(DataFormat df: contents.keySet()) {
			Object value = contents.get(df);
			if(value instanceof Serializable || value instanceof ByteBuffer) {
				writableDataFormats++;
				
				Set<String> tmp = df.getIdentifiers();
				writeObjects.add(tmp.size());
				for(String s: tmp) {
					writeObjects.add(s);
				}
				
				if(value instanceof ByteBuffer) {
					byte[] b = new byte[((ByteBuffer) value).capacity()];
					Integer pos = ((ByteBuffer) value).position();
					((ByteBuffer) value).get(b);
					writeObjects.add(b);
					writeObjects.add(pos);
					((ByteBuffer) value).position(pos);
				} else
					writeObjects.add((Serializable) value);
			} else {
				System.err.println("Value was " + value.getClass() +". Not serializable. Skipping value.");
			}
		}
		writeObjects.addFirst(writableDataFormats);
		
		if(writableDataFormats > 0) {
			oos.writeObject(ELEMENT_WRITTEN);
			oos.defaultWriteObject();
			while(!writeObjects.isEmpty()) {
				oos.writeObject(writeObjects.getFirst());
				writeObjects.removeFirst();
			}
		} else {
			oos.writeObject(NO_ELEMENT_WRITTEN);
			System.err.println("No writable DataFormat was found for this ClipboardElement. Writing nothing!");
		}
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		Object elementHeader = ois.readObject();
		if(elementHeader instanceof String && elementHeader.equals(NO_ELEMENT_WRITTEN))
			return;
		else if(elementHeader instanceof String && elementHeader.equals(ELEMENT_WRITTEN)) {
			ois.defaultReadObject();
			
			Object writtenDataFormats = ois.readObject();
			if(writtenDataFormats instanceof Integer) {
				Object name = ois.readObject();
				if(name instanceof String)
					nameProperty().set((String) name);
				else
					throw new StreamCorruptedException("Expected String as name. Recieved: " + name.getClass());
				
				Object recordTime = ois.readObject();
				if(recordTime instanceof String)
					setRecordTimeProperty((String) recordTime);
				else
					throw new StreamCorruptedException("Expected String as record time. Recieved: " + recordTime.getClass());
				
				for(int i = 0; i < (Integer) writtenDataFormats; i++) {
					Object dataFormatIdentifiers = ois.readObject();
					
					if(dataFormatIdentifiers instanceof Integer) {
						String[] identifiers = new String[(Integer) dataFormatIdentifiers];
						DataFormat correspondingDataFormat = null;
						for(int j = 0; j < identifiers.length; j++) {
							Object identifier = ois.readObject();
							if(identifier instanceof String) {
								identifiers[j] = (String) identifier;
								if(correspondingDataFormat == null) {
									correspondingDataFormat = DataFormat.lookupMimeType((String) identifier);
									if(correspondingDataFormat == null)
										correspondingDataFormat = new DataFormat((String) identifier);
								} else if(correspondingDataFormat != DataFormat.lookupMimeType((String) identifier))
									correspondingDataFormat = new DataFormat(Arrays.copyOfRange(identifiers, 0, j + 1));
							}
							else
								throw new StreamCorruptedException("Expected String as DataFormat identifier. Recieved: " + identifier.getClass());
						}
						
						Object value = ois.readObject();
						if(value instanceof byte[]) {
							value = ByteBuffer.wrap((byte[]) value);
							((ByteBuffer) value).position((Integer) ois.readObject());
						}
						
						contentsProperty().put(correspondingDataFormat, value);
					} else {
						throw new StreamCorruptedException("Excepted Integer as DataFormat identifier count. Recieved: " + dataFormatIdentifiers.getClass());
					}
				}
			} else {
				throw new StreamCorruptedException("Excepted Integer as written DataFormat count. Recieved: " + writtenDataFormats.getClass());
			}
		} else
			throw new StreamCorruptedException("Missing element header");
	}
	
	/*public Map<DataFormat, Object> getContents() {
		return Collections.unmodifiableMap(contentsProperty());
	}*/
}
