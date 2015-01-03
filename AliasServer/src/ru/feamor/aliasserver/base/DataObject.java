package ru.feamor.aliasserver.base;

import gnu.trove.map.hash.TIntObjectHashMap;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import org.apache.jcs.utils.struct.DoubleLinkedList;
import org.apache.jcs.utils.struct.DoubleLinkedListNode;

import ru.feamor.aliasserver.utils.TextUtils;

public class DataObject {
	
	public static class DataObjectType {
		public static final byte TYPE_BYTE = 0;
		public static final byte TYPE_SHORT = 1;
		public static final byte TYPE_INTEGER = 2;
		public static final byte TYPE_FLOAT = 3;
		public static final byte TYPE_DOUBLE = 4;
		public static final byte TYPE_STRING = 5;
		public static final byte TYPE_ARRAY_LIST = 7;
		public static final byte TYPE_LONG = 8;
		public static final byte TYPE_LINKED_LIST = 9;
		public static final byte TYPE_HASH_MAP = 10;
		public static final byte TYPE_BOOLEAN = 11;
		public static final byte TYPE_NULL = 12;
	}
	
	private byte type;
	private Object data;
	
	public DataObject() {
		type = DataObjectType.TYPE_NULL;
		data = null;
	}
		
	public Object getData() {
		return data;
	}
	
	public int getType() {
		return type;
	}
	
	public void changeType(byte newType) {
		this.type = newType;
	}
		
	public void setData(Object newData) {
		data = newData;
	}
	
	public int asInteger() {
		return ((Number)data).intValue();
	}
	
	public byte asByte() {
		return ((Number)data).byteValue();
	}
	
	public short asShort() {
		return ((Number)data).shortValue();
	}
	
	public float asFloat() {
		return ((Number)data).floatValue();
	}
	
	public double asDouble() {
		return ((Number)data).doubleValue();
	}
	
	public long asLong() {
		return ((Number)data).longValue();
	}
	
	public boolean asBoolean() {
		return ((Boolean)data).booleanValue();
	}
	
	public String asString() {
		return (String)data;
	}
	
	public boolean isNull() {
		return type == DataObjectType.TYPE_NULL;
	}
	
	public ArrayList<DataObject> asArrayList() {
		return (ArrayList<DataObject>)data;
	}
	
	public DoubleLinkedList asLinkedList() {
		return (DoubleLinkedList)data;
	}
	
	public DoubleLinkedList asDataObject() {
		return (DoubleLinkedList)data;
	}
	
	public TIntObjectHashMap<DataObject> asHashMap() {
		return (TIntObjectHashMap<DataObject>)data;
	}
	
	public DataObject setInteger(int value) {
		type = DataObjectType.TYPE_INTEGER;
		data = Integer.valueOf(value);
		return this;
	}
	
	public DataObject setByte(byte value) {
		type = DataObjectType.TYPE_BYTE;
		data = Byte.valueOf(value);
		return this;
	}
	
	public DataObject setShort(short value) {
		type = DataObjectType.TYPE_SHORT;
		data = Short.valueOf(value);
		return this;
	}
	
	public DataObject setFloat(float value) {
		type = DataObjectType.TYPE_FLOAT;
		data = Float.valueOf(value);
		return this;
	}
	
	public DataObject setDouble(double value) {
		type = DataObjectType.TYPE_DOUBLE;
		data = Double.valueOf(value);
		return this;
	}
	
	public DataObject setLong(long value) {
		type = DataObjectType.TYPE_LONG;
		data = Long.valueOf(value);
		return this;
	}
	
	public DataObject setString(String value) {
		type = DataObjectType.TYPE_STRING;
		data = value;
		return this;
	}
	
	public DataObject setBoolean(boolean value) {
		type = DataObjectType.TYPE_BOOLEAN;
		data = Boolean.valueOf(value);
		return this;
	}
	
	public DataObject setArrayList(ArrayList<DataObject> value) {
		type = DataObjectType.TYPE_ARRAY_LIST;
		data = value;
		return this;
	}
	
	public DataObject setAsArraList(int startLength) {
		type = DataObjectType.TYPE_ARRAY_LIST;
		data = new ArrayList<DataObject>(startLength);
		return this;
	}
	
	public DataObject addToArrayList(DataObject item) {
		asArrayList().add(item);
		return this;
	}
	
	public DataObject setLinkedList(DoubleLinkedList value) {
		type = DataObjectType.TYPE_LINKED_LIST;
		data = value;
		return this;
	}
		
	public DataObject setHashMap(TIntObjectHashMap<DataObject> value) {
		type = DataObjectType.TYPE_HASH_MAP;
		data = value;
		return this;
	}
	
	public DataObject putToHashMap(DataObject item, int key) {
		asHashMap().put(key, item);
		return this;
	}
	
	public DataObject setNull() {
		type = DataObjectType.TYPE_NULL;
		data = null;
		return this;
	}
	
	public static void write(DataObject object, ByteBuf out) {
		if (object == null) {
			out.writeByte(DataObjectType.TYPE_NULL);
		} else {
			out.writeByte(object.type);
			switch(object.type) {
			case DataObjectType.TYPE_BYTE :
				out.writeByte(object.asByte());
				break;
			case DataObjectType.TYPE_SHORT :
				out.writeShort(object.asShort());
				break;
			case DataObjectType.TYPE_INTEGER :
				out.writeInt(object.asInteger());
				break;
			case DataObjectType.TYPE_FLOAT :
				out.writeFloat(object.asFloat());
				break;
			case DataObjectType.TYPE_DOUBLE :
				out.writeDouble(object.asDouble());
				break;
			case DataObjectType.TYPE_STRING  :
				TextUtils.writeToBuf(object.asString(), out);
				break;
			case DataObjectType.TYPE_ARRAY_LIST :
				ArrayList<DataObject> arrayList = object.asArrayList();
				out.writeInt(arrayList.size());
				for (int i=0; i<arrayList.size(); i++) {
					DataObject item = arrayList.get(i);
					if (item == null) {
						out.writeByte(DataObjectType.TYPE_NULL);
					} else {
						write(item, out);
					}
				}
				break;
			case DataObjectType.TYPE_LONG :
				out.writeLong(object.asLong());
				break;
			case DataObjectType.TYPE_LINKED_LIST :
				DoubleLinkedList linkedList = object.asLinkedList();
				out.writeInt(linkedList.size());
				for (DoubleLinkedListNode i=linkedList.getFirst(); i!=null; i=i.next) {
					DataObject item = (DataObject) i.getPayload();
					if (item == null) {
						out.writeByte(DataObjectType.TYPE_NULL);
					} else {
						write(item, out);
					}
				}
				break;
			case DataObjectType.TYPE_HASH_MAP :
				TIntObjectHashMap<DataObject> hashMap = object.asHashMap();
				out.writeInt(hashMap.size());
				for (int i = 0; i<hashMap.size(); i++) {
					out.writeInt(hashMap.keys()[i]);
					DataObject item = (DataObject) hashMap.values()[i];
					if (item == null) {
						out.writeByte(DataObjectType.TYPE_NULL);
					} else {
						write(item, out);
					}
				}
				break;
			case DataObjectType.TYPE_BOOLEAN :
				out.writeBoolean(object.asBoolean());
				break;
			case DataObjectType.TYPE_NULL:
				//do nothin! beacuse NULL!
			default:
				break;
			}
		}
	}
	
	public static DataObject read(ByteBuf in) {
		DataObject result= new DataObject();
		result.type = in.readByte();
		switch(result.type) {
		case DataObjectType.TYPE_BYTE :
			result.setData(Byte.valueOf(in.readByte()));
			break;
		case DataObjectType.TYPE_SHORT :
			result.setData(Short.valueOf(in.readShort()));
			break;
		case DataObjectType.TYPE_INTEGER :
			result.setData(Integer.valueOf(in.readInt()));
			break;
		case DataObjectType.TYPE_FLOAT :
			result.setData(Float.valueOf(in.readFloat()));
			break;
		case DataObjectType.TYPE_DOUBLE :
			result.setData(Double.valueOf(in.readDouble()));
			break;
		case DataObjectType.TYPE_STRING  :
			result.setData(TextUtils.readFromBuf(in));
			break;
		case DataObjectType.TYPE_ARRAY_LIST :
			ArrayList<DataObject> arrayList = new ArrayList<>();
			int arrayLenght = in.readInt();
			for (int i=0; i < arrayLenght; i++) {
				DataObject item = new DataObject();
				item = read(in);
				arrayList.add(item);
			}
			result.setData(arrayList);
			break;
		case DataObjectType.TYPE_LONG :
			result.setData(Long.valueOf(in.readLong()));
			break;
		case DataObjectType.TYPE_LINKED_LIST :
			DoubleLinkedList linkedList = new DoubleLinkedList();
			int listLength = in.readInt();
			for (int i = 0; i<listLength; i++) {
				DataObject item = new DataObject();
				item = read(in);
				linkedList.addLast(new DoubleLinkedListNode(item));
			}
			result.setData(linkedList);
			break;
		case DataObjectType.TYPE_HASH_MAP :
			TIntObjectHashMap<DataObject> hashMap = new TIntObjectHashMap<DataObject>();
			int hashLength = in.readInt();
			for (int i = 0; i<hashLength; i++) {
				int key = in.readInt();
				DataObject item = new DataObject();
				item = read(in);
				hashMap.put(key, item);
			}
			result.setData(hashMap);
			break;
		case DataObjectType.TYPE_BOOLEAN :
			result.setData(Boolean.valueOf(in.readBoolean()));
			break;
		case DataObjectType.TYPE_NULL:
			result.setData(null);
		default:
			result.setData(null);
			break;
		}
		return result;
	}
	
}
