package it.eng.msp.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Angelo Marguglio
 *
 */
public class TextUtils {

	public static SimpleDateFormat time = new SimpleDateFormat("HH:mm");
	public static SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy");
	public static SimpleDateFormat dateSimple 	= new SimpleDateFormat("dd/MM/yy");
	public static SimpleDateFormat dateTime		= new SimpleDateFormat("dd/MM/yy HH:mm");
	public static SimpleDateFormat dateLiteral 	= new SimpleDateFormat("dd MMMM yyyy");
	public static SimpleDateFormat dateLiteralReverse 		= new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat dateTimeLiteralReverse 	= new SimpleDateFormat("yyyyMMddHHmm");
	
	public static DecimalFormat importoFormat = new DecimalFormat("#,##0.00");

	public static boolean isValidString(String str) {
		return (str!=null && !str.equals(""));
	}
	
	public static boolean isValidStringPlus(String str) {
		return (str!=null && !str.trim().equals(""));
	}
	
	public static boolean isValidDate(String str) {
		if(isValidString(str)) {
			try {
				date.parse(str);
				return true;
			} catch (ParseException e) {}
		}
		
		return false;
	}

	public static boolean isValidDate(Date data) {
		try {
			return TextUtils.isValidDate(TextUtils.dateToString(data));
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isValidInteger(String str) {
		if(isValidString(str)) {
			try {
				Integer.valueOf(str);
				return true;
			} catch (Exception e) {}
		}
		
		return false;
	}
	
	public static boolean isValidLong(String str) {
		if(isValidString(str)) {
			try {
				Long.valueOf(str);
				return true;
			} catch (Exception e) {}
		}
		
		return false;
	}
	
	public static boolean isValidFloat(String str) {
		if(isValidString(str)) {
			try {
				Float.valueOf(str);
				return true;
			} catch (Exception e) {}
		}
		
		return false;
	}
	
	public static boolean isValidDouble(String str) {
		if(isValidString(str)) {
			try {
				Double.valueOf(str);
				return true;
			} catch (Exception e) {}
		}
		
		return false;
	}
	
	/**
	 * @return Trasformo in maiuscole tutte le lettere iniziali della frase passata
	 */
	public static String toCapitalLetter(String in) {
		return toCapitalLetter(in, false);
	}
	
	/**
	 * @return Trasformo in maiuscola la prima lettera della frase passata
	 */
	public static String toInitialCapitalLetter(String in) {
		return toCapitalLetter(in, true);
	}
	
	/**
	 * @return Trasformo in maiuscola la prima lettera della frase passata
	 */
	public static String toInitialCapitalLetter(String in, boolean forceLower) {
		return toCapitalLetter(in, true, forceLower);
	}
	
	/**
	 * @return Trasformo in maiuscola la prima lettera o tutte le lettere iniziali
	 * della frase passata in base al parametro passato
	 */
	protected static String toCapitalLetter(String in, boolean onlyInitial) {
		return toCapitalLetter(in, onlyInitial, true);
	}
	
	/**
	 * @return Trasformo in maiuscola la prima lettera o tutte le lettere iniziali
	 * della frase passata in base al parametro passato
	 */
	protected static String toCapitalLetter(String in, boolean onlyInitial, boolean forceLower) {
		in = forceLower ? in.trim().toLowerCase() : in.trim();
		
		if(in.length()<=0) return "";
		
		String initial = in.substring(0, 1).toUpperCase();
		String out = initial.concat(in.substring(1));

		if(!onlyInitial && out.contains(" ")) {
			char[] str = out.toCharArray();
			if(str.length>0) {
				for (int i = 0; i < str.length-1; i++) {
					if(str[i]==' ')
						str[i+1] = toCapitalLetter(
								in.substring(i+1, i+2)).toCharArray()[0];
				}
			}
			return String.copyValueOf(str);
		}
		
		return out;
	}
	
	/**
	 * @return Restituisce il numero di giorni che intercorre tra
	 * le due date passate sottraendo alla seconda la prima.
	 */
	public static int dateDifference(long ldate1, long ldate2) {
		int hr1   = (int)(ldate1/3600000);
		int hr2   = (int)(ldate2/3600000);
		int days1 = (int)hr1/24;
		int days2 = (int)hr2/24;

		return (days2 - days1);
	}
	
	/**
	 * @return Restituisce il numero di giorni che intercorre tra
	 * le due date passate.
	 */
	public static Date modifyData(Date time, int what, int howMuch) {
		Date date = new Date(time.getTime());
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(what, howMuch);
		return new Date(c.getTime().getTime());
	}
	
	public static String dateToString(Date date, SimpleDateFormat format) {
		return format.format(date);
	}

	public static Date stringToDate(String str, SimpleDateFormat format) {
		try {
			if(isValidString(str))
				return new Date(format.parse(str).getTime());
		} catch (ParseException e) {}
		
		return null;
	}

	public static String dateLiteralReverseToString(Date data) {
		return (data!=null) ? toCapitalLetter(dateLiteralReverse.format(data)) : null;
	}

	public static String dateTimeLiteralReverseToString(Date data) {
		return (data!=null) ? toCapitalLetter(dateTimeLiteralReverse.format(data)) : null;
	}

	public static String dateToString(Date data) {
		return (data!=null) ? date.format(data) : null;
	}
	
	public static String dateSimpleToString(Date data) {
		return (data!=null) ? dateSimple.format(data) : null;
	}

	public static String dateTimeToString(Date data) {
		return (data!=null) ? dateTime.format(data) : null;
	}
	
	public static String dateLiteralToString(Date data) {
		return (data!=null) ? toCapitalLetter(dateLiteral.format(data)) : null;
	}
	
	public static String stringDataReverseToString(String str) {
		return dateToString(stringToDate(str, dateLiteralReverse));
	}
	
	public static String timeToString(Date data) {
		return (data!=null) ? time.format(data) : null;
	}
	
	public static String timeNotEmptyToString(Date data) {
		String time = TextUtils.timeToString(data);
		
		if(TextUtils.isValidString(time) && time.equals("00:00"))
			time = "";
		
		return time;
	}
	
	public static Date stringToDate(String str) {
		try {
			if(isValidString(str))
				return new Date(date.parse(str).getTime());
		} catch (ParseException e) {}
		
		return null;
	}

	public static String getReverseDateAsString() {
		return dateLiteralReverseToString(new Date());
	}

	public static String getReverseDateTimeAsString() {
		return dateTimeLiteralReverseToString(new Date());
	}
	
	public static String importoToString(Double importo) {
		if(importo==null) return null;
		
		return importoFormat.format(importo);
	}
	
	public static String importoToString(Float importo) {
		if(importo==null) return null;
		
		return importoFormat.format(importo);
	}

	public static String mergeArrayObject(Object[] desc) {
		String str = "";
		
		for (Object o : desc) {
			if(o!=null)
				str += o + "; ";
		}
		
		return str;
	}

	public static String mergeArrayString(String[] desc) {
		String str = "";
		
		for (String string : desc) {
			if(isValidString(string))
				str += string + "; ";
		}
		
		return str;
	}
	
	public static String[] sortArrayString(String[] rs) {
		// Ordine alfabetico
		List<String> rsL = Arrays.asList(rs);
		Collections.sort(rsL);
		
		for (int i = 0; i < rs.length; i++)
			rs[i] = rsL.get(i);
		
		return rs;
	}
	
	/**
	 * @param str - La stringa da parsare
	 * @return  Un set di stringhe contenente tutte le stringhe valide
	 * 			separate dal ';' nella stringa passata
	 */
	@SuppressWarnings("unchecked")
	public static Set<String> getSetFromString(String str) {
		// Effettuo il trim della stringa
		str = str.trim();
		
		String separator = ";";
		if(!str.contains(separator)) separator = "\n";
		
		// Se la stringa passata non e' valida restituisco un set vuoto
		if(!isValidString(str)) 
			return Collections.EMPTY_SET;
		
		// Splitto la stringa passata
		String[] array = str.split(separator);
		
		// Creo il set di uscita
		Set<String> set = new HashSet<String>();
		
		// Aggiungo ogni stringa valida al set di uscita
		for (String s : array) {
			if(isValidString(s.trim()))
				set.add(s.trim());
		}
		
		// Restituisco il set creato
		return set;
	}
	
	/**
	 * @param set - Il set da parsare
	 * @return  La stringa contenente tutte le stringhe valide
	 * 			contenute nel set separate dal ';'
	 */
	public static String getStringFromSet(Set<String> set) {
		if(set==null || set.isEmpty()) return "";
		
		// Creo un array di stringhe in ordine alfabetico
		String[] array = new String[set.size()];
		set.toArray(array);
		Arrays.sort(array);
		
		String str = "";
		
		if(array.length>0) {
			for (int i = 0; i < array.length-1; i++) {
				String s = array[i];
				if(isValidString(s))
					str += s + "; ";
			}
			
			// Tratto separatamente l'ultimo elemento per non aggiungere
			// alla fine di tutta la stringa il carattere ';'
			String last = array[array.length-1];
			if(isValidString(last))
				str += last;
		}

		return str;
	}
	
	public static String addIntraSpaces(String str) {
		if(!TextUtils.isValidString(str)) return str;

		String result = "";
		for (int i = 0; i < str.length(); i++) {
			if(i+1<str.length())
				result += str.substring(i, i+1);
			else
				result += str.substring(i);
			result += " ";
		}
		
		return result;
	}
	
	public static String addPadding(String str, int len) {
		if(!TextUtils.isValidString(str)) return str;
		
		int pad = len - str.length();

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < pad; i++){
			sb.append("0");
		}

		sb.append(str);

		return sb.toString();
	}
	
	public static String removePadding(String str) {
		if(isValidLong(str))
			return Long.toString(Long.valueOf(str));
		
		return str;
	}
	
	public static String trim(String str, int lenght) {
		if(str==null) return null;
		
		if(lenght<=0)
			return str;
		
		if(str.length()<lenght)
			return str;
		
		return str.substring(0, lenght) + ".";
	}
	
	public static String exceptionToString(Throwable e) {
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		e.printStackTrace(ps);
		
		return os.toString();
	}
	
}