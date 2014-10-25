package ru.feamor.aliasserver.utils;

import io.netty.buffer.ByteBuf;

public class TextUtils {
	public static boolean isEmpty(String string) {
		return string == null || string.length()==0;
	}
	
	public static boolean isNotEmpty(String string) {
		return string != null && string.length()>0;
	}
	
	public static void writeToBuf(String string, ByteBuf out) {
		//TODO: add -1 as null string
		if (isEmpty(string)) {
			out.writeInt(0);
		} else {
			byte [] byteName = string.getBytes();
			out.writeInt(byteName.length);		 
			out.writeBytes(byteName);
		}
	}
	
	public static String readFromBuf(ByteBuf in) {
		//TODO: add null if receive -1  
		int length = in.readInt();
		if (length > 0) {
			byte [] data = new byte[length];
			in.readBytes(data, 0, length);
			String result =  String.valueOf(data);
			return result;
		} else {
			return "";
		}
	}
}
