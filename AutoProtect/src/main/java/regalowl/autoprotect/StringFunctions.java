package regalowl.autoprotect;

import java.util.ArrayList;

public class StringFunctions {
	
	public ArrayList<String> explode(String string, String delimiter) {
			ArrayList<String> array = new ArrayList<String>();
			if (string == null || delimiter == null || !string.contains(delimiter)) {return array;}
			if (string.indexOf(delimiter) == 0) {string = string.substring(1, string.length());}
			if (!string.substring(string.length() - 1, string.length()).equalsIgnoreCase(delimiter)) {string += delimiter;}
			while (string.contains(delimiter)) {
				array.add(string.substring(0, string.indexOf(delimiter)));
				if (string.indexOf(delimiter) == string.lastIndexOf(delimiter)) {break;}
				string = string.substring(string.indexOf(delimiter) + 1, string.length());
			}
			return array;
	}

	public String implode(ArrayList<String> array, String delimiter) {
		if (array == null || delimiter == null) {return "";}
		String string = "";
		for (String cs:array) {
			string += cs + delimiter;
		}
		return string;
	}

}
