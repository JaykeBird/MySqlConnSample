import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ColumnData
{

	private String name;
	private String datatype;
	private ColumnDataType type;
	private int index;
	private int vallength;
	private boolean prikey = false;
	private boolean nullable = true;
	private boolean unsigned = false;
	private List<String> enumvalues = new ArrayList<String>();
	
	public ColumnData(String name, String datatype, ColumnDataType type, int index, int vallength, boolean nullable, boolean unsigned, boolean prikey)
	{
		this.name = name;
		this.datatype = datatype;
		this.type = type;
		this.index = index;
		this.vallength = vallength;
		this.nullable = nullable;
		this.unsigned = unsigned;
		this.prikey = prikey;
	}
	
	@SuppressWarnings("unused")
	private void setUpEnumValues(ArrayList<String> values)
	{
		enumvalues = values;
	}
	
	private void setUpEnumValues(String valuedata)
	{
		// put the list of values for the ENUM or SET into a List for accessing later
		valuedata = valuedata.replace("'", "");
		String[] vals = valuedata.split(",");
		enumvalues = Arrays.asList(vals);
	}
	
	/**
	 * Get the name of the column.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the string detailing the data type of this column.
	 */
	public String getDataType() {
		return datatype;
	}
	
	/**
	 * Get the data type of the column, as a ColumnDataType option. Unsupported data types will be returned as VarChar.
	 */
	public ColumnDataType getType() {
		return type;
	}
	
	/**
	 * Get the one-based index of the column within its table.
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Get the max length (in characters) an entry can be within this column. If 0, no max length was defined.
	 */
	public int getValLength() {
		return vallength;
	}
	
	/**
	 * Get whether a null value is allowed in the column. If false, null values are not permitted.
	 */
	public boolean getNullable() {
		return nullable;
	}
	
	/**
	 * Get whether the data for this column is unsigned or not. This only applies for columns with numeric data; otherwise, returns false.
	 */
	public boolean isUnsigned() {
		return unsigned;
	}
	
	/**
	 * Get whether this column is the primary key for this table.
	 */
	public boolean isPrimaryKey() {
		return prikey;
	}
	
	/**
	 * If this is an ENUM or SET type, will return an ArrayList containing the list of valid values for this column.
	 * Otherwise, will only return an empty ArrayList.
	 */
	public List<String> getEnumValues()
	{
		return enumvalues;
	}
	
	/**
	 * Checks to see if this string representation of a value is valid for this column's data type.
	 * @param value The value to check/
	 * @return True if this is a valid value. False if it cannot be used.
	 */
	public boolean isValidValue(String value)
	{
		// check the length first
		// (vallength == 0 means there is no length)
		if (this.vallength > 0)
		{
			if (value.length() > this.vallength)
			{
				return false; // the input string is longer than what the column allows
			}
		}
		
		// check if null
		if (value.isEmpty())
		{
			if (nullable == false)
			{
				return false; // the input string is null, and the column is set as NOT NULL
			}
		}
		
		switch (type)
		{
		case Binary:
			// a binary string
			// any string will do, will just need to be converted to a byte array
			return true;
		case Bit:
			// a bit is a binary string of 0s and 1s. 
			// if the string contains more than ones and zeros, it is not valid
			for (char ch : value.toCharArray())
			{
				if (!(ch == '0' || ch == '1'))
				{
					return false;
				}
			}
			return true;
		case Blob:
			// a large string
			// any string will do
			return true;
		case Byte:
			if (unsigned)
			{
				return checkNumeric(value, NumericType.SHORT, true);
			}
			else
			{
				return checkNumeric(value, NumericType.BYTE, false);
			}
		case Date:
			// dates can be written in four different ways
			// YYYY-MM-DD, YY-MM-DD, YYYYMMDD, and YYMMDD.
			// The string lengths are 10, 8, 8, and 6 respectively.
			if (!(value.length() == 10 || value.length() == 8 || value.length() == 6))
			{
				return false;
				// very quickly determined to be an invalid date string  because there's
				// no format that this string could fit into based on the string's length alone 
			}
			
			// For this first step, let's assume it is a valid date string.
			// It will either have the punctuation or not.
			boolean haspunc = false;
			
			try { Integer.parseInt(value); }
			catch (NumberFormatException e) { haspunc = true; } // cannot be parsed as an integer
			
			// moved these to separate functions because I'll need them in another place too
			if (haspunc == false)
			{ // If it has no punctuation
				
				if (value.length() == 6)
				{
					// YYMMDD
					return checkDate(value, 0, 2, 2, 4, 4);
					
				}
				else
				{
					// YYYYMMDD
					return checkDate(value, 0, 4, 4, 6, 6);
				}
			} else {
				// punctuation or other non-numeral characters are in here
				// first, let's see if the string could be YY-MM-DD, or YYYY-MM-DD
				// afterwards, we'll check to see if it is a valid date
				
				if (value.length() == 8)
				{
					// YY-MM-DD
					return checkDate(value, 0, 2, 3, 5, 6);
				}
				else
				{
					// YYYY-MM-DD
					return checkDate(value, 0, 4, 5, 7, 8);
				}
			}
			
			// I realized after the fact that formats such as YYYY-M-D are acceptable if the Month and Day are under 10
			// this code does not check for that. Month and Day must both always be 2 digits
			// I'm going to keep it this way for the time being though
			
		case DateTime:
			// this can be written in two distinct ways, with the 1st having 3 variations, and the 2nd having 1:
			// With punctuation (punctuated) - YYYY-MM-DD HH:MM:SS
			// Without punctuation (numeral) - YYYYMMDDHHMMSS
			
			// For the punctuated format, the letter "T" can be used in place of a space
			// For both formats, the year value can be 2 digits (YY).
			
			// The punctuated format can have a length of 19 or 17.
			// The numeral format can have a length of 14 or 12.
			
			if (value.length() == 19) {
				// YYYY-MM-DD HH:MM:SS
				
				// First, check the date portion
				if (!(checkDate(value.substring(0, 10), 0, 2, 3, 5, 6)))
				{ return false; }
				
				// Secondly, check the character between the date and the time
				// This character must be a space or the letter T
				if (!(value.charAt(10) == ' ' || value.charAt(10) == 'T'))
				{ return false; }
				
				// Finally, check the time portion
				if (!(checkTime(value.substring(11), true, false, false)))
				{ return false; }
				
			} else if (value.length() == 17) {
				// YY-MM-DD HH:MM:SS
				
				// First, check the date portion
				if (!(checkDate(value.substring(0, 8), 0, 2, 3, 5, 6)))
				{ return false; }
				
				// Secondly, check the character between the date and the time
				// This character must be a space or the letter T
				if (!(value.charAt(8) == ' ' || value.charAt(8) == 'T'))
				{ return false; }
				
				// Finally, check the time portion
				if (!(checkTime(value.substring(9), true, false, false)))
				{ return false; }
				
			} else if (value.length() == 14) {
				// YYYYMMDDHHMMSS
				
				// First, check the date portion
				if (!(checkDate(value.substring(0, 8), 0, 4, 4, 6, 6)))
				{ return false; }
				
				// Finally, check the time portion
				if (!(checkTime(value.substring(8), false, false, false)))
				{ return false; }
				
			} else if (value.length() == 12) {
				// YYMMDDHHMMSS
				
				// First, check the date portion
				if (!(checkDate(value.substring(0, 6), 0, 2, 2, 4, 4)))
				{ return false; }
				
				// Finally, check the time portion
				if (!(checkTime(value.substring(6), false, false, false)))
				{ return false; }
				
			} else {
				// does not match any of the possible lengths
				// this is a quick sign that the string isn't valid
				return false;
			}
			
			return true; // if it gets to this point, I think it checks out
		case Decimal:
			
			DecimalFormat df = new DecimalFormat();
			df.setParseBigDecimal(true);
			
			try { df.parse(value); }
			catch (ParseException e) { return false; } // cannot be parsed as this value
			
			return true; // if it reached this point, that means it's valid
		case Double:
			return checkNumeric(value, NumericType.DOUBLE, false);
		case Enum:
			// check to see if this is a valid enum value
			return enumvalues.contains(value);
		case Float:
			return checkNumeric(value, NumericType.FLOAT, false);
		case Integer:
			if (unsigned)
			{
				return checkNumeric(value, NumericType.LONG, true);
			}
			else
			{
				return checkNumeric(value, NumericType.INT, false);
			}
		case Long:
			if (unsigned)
			{ // An unsigned long can be a BigInteger, so I will compare the value against that here
				BigInteger bi = new BigInteger("0");
				try { bi = new BigInteger(value); }
				catch (NumberFormatException e) { return false; }
				
				if (bi.compareTo(BigInteger.ZERO) == -1) { return false; } // it is less than zero
				else { return true; }
			}
			else { return checkNumeric(value, NumericType.LONG, false); }
		case Set:
			// similar to an ENUM type, except a SET value can have multiple elements set per item
			String[] setvals = value.split(","); // each SET element is separated by a comma
			
			// now to check if each element is in the SET's values
			for (String str : setvals)
			{
				if (enumvalues.contains(str) == false)
				{
					return false; // one of these items was not actually in the SET's values
				}
			}
			
			return true; // if it got to this point, I think the string checks out
		case Short:
			if (unsigned)
			{
				return checkNumeric(value, NumericType.INT, true);
			}
			else
			{
				return checkNumeric(value, NumericType.SHORT, false);
			}
		case Text:
			// a string is a string
			return true;
		case Time:
			// TIME can be written in a variety of ways:
			// D HH:MM:SS format (with variations HH:MM:SS, D HH:MM, HH:MM, D HH)
			// HHMMSS format (with variations MMSS, SS)
			// More variations exist (D HH:MM:SS.LLLLLL, HH:MM:SS.LLLLLL, and HHMMSS.LLLLLL),
			// but those allow microsecond precision, and that is discarded when stored in TIME columns. Thus, we will not check that here.
			
			// First, let's check to see if it's HHMMSS format, by seeing if the entire string is a number
			
			try {
				int time = Integer.parseInt(value);
				
				// if the code did not cause an exception, it will end up here
				// thus, we can assume there's no punctuation and it's in the HHMMSS format
				// let's parse it
				if (value.length() == 2)
				{ // SS
					if (time > 60) { return false; }
				}
				else if (value.length() == 6)
				{ // HHMMSS
					return checkTime(value, false, false, true);
				}
				
				// MMSS only applies if the data being entered is not being entered as a string
				// it is assumed that the data being entered will be formatted as a string
//				else if (value.length() == 4)
//				{ // MMSS
//					return checkTime(value, false, true);
//				}

			} catch (NumberFormatException e) { } // has punctuation or something, we'll check this in the next block 

			String tims;
			boolean hasday = false;
			// first, check to see if the D value is assigned
			// by checking to see if there's a space in index 1 or 2
			if (value.charAt(1) == ' ' || value.charAt(2) == ' ') {
				// assume there's the D value
				
				// before anything else, let's see if it's the D HH format
				try
				{ // D HH
					int day = Integer.parseInt(value.substring(0, value.indexOf(' ')));
					int hour = Integer.parseInt(value.substring(value.indexOf(' ') + 1));
					
					if (day > 34 || day < 0) { return false; }
					else if (hour > 24 || hour < 0) { return false; }
					else { return true; } // seems to be D HH and seems to check out
				}
				catch (NumberFormatException e)
				{ // okay, not D HH format, let's move on
					tims = value.substring(value.indexOf(' ') + 1);
					hasday = true;
				}
			} else { // assume there's no D value
				tims = value;
				hasday = false;
			}
			
			if (hasday == true) { // check day value
				try {
					int day = Integer.parseInt(value.substring(0, value.indexOf(' ')));
					
					if (day > 34 || day < 0) { return false; }
				} catch (NumberFormatException e)
				{
					return false;
					// the day value isn't valid (has a not-numeric character)
					// that alone is enough to make this invalid
				}
			}
			
			if (tims.length() == 8) { // HH:MM:SS
				return checkTime(tims, true, false, true); }
			else if (tims.length() == 5) { // HH:MM
				return checkTime(tims, true, true, true); }
			else { return false; } // something about this string just isn't right, it's not a valid time format
			
		case VarChar:
			// a string is a string
			return true;
		default:
			return false; // I don't think this line will ever be reached
			
		}
	}
	
	/**
	 * Gets if the data type of this column is a number type (INT, DOUBLE, etc.)
	 */
	public boolean isNumberType()
	{
		switch (type)
		{
		case Byte:
		case Decimal:
		case Double:
		case Float:
		case Integer:
		case Long:
		case Short:
			return true;
		case Bit:
		case Binary:
		case Blob:
		case Date:
		case DateTime:
		case Enum:
		case Text:
		case Time:
		case VarChar:
			return false;
		default:
			return false;
		
		}
	}
	
	/**
	 * Create an ArrayList with a ColumnData object for each column in the table
	 * @param data The ResultSet data this function reads. This must be the ResultSet from a "DESCRIBE" command.
	 * @return An ArrayList with a ColumnData object for each column.
	 * @throws SQLException If there is an issue reading the ResultSet data, this exception will be raised.
	 */
	public static ArrayList<ColumnData> buildColumnData(ResultSet data) throws SQLException
	{
		ArrayList<ColumnData> cols = new ArrayList<ColumnData>();
		
		try
		{
			int rn = 1;
			
			while (data.next())
			{
				// Determine if nullable
				boolean nn = false;
				if (data.getString(3).equals("NO"))
				{
					nn = true; // this column is not nullable (null values not allowed)
				}
				
				// Determine if primary key
				boolean pk = false;
				if (data.getString(4).equals("PRI"))
				{
					pk = true; // this column is primary key
				}
				
				// Parse/determine type
				String typedata = data.getString(2).toUpperCase(); // Data about the type is stored in the 2nd column
				
				boolean us = false;
				if (typedata.endsWith("UNSIGNED") || typedata.endsWith("UNSIGNED ZEROFILL"))
				{
					us = true; // Is unsigned
				}
				
				int length = 0;
				boolean isenum = false;
				ColumnDataType type;
				// If there is an opening parenthesis, this means there might be a length parameter
				if (!(typedata.startsWith("ENUM") || typedata.startsWith("SET")))
				{
					
					if (typedata.contains("("))
					{
						String lth = typedata.substring(typedata.indexOf("(") + 1, typedata.indexOf(")"));
						
						if (!(typedata.startsWith("TIME") || typedata.startsWith("DATE") || typedata.startsWith("DATETIME")))
						{
							
							if (lth.contains(",")) // some data types allow commas within the parentheses to define decimal points
							{
								lth = lth.substring(0, lth.indexOf(","));
							}
							length = Integer.parseInt(lth); // there really isn't a conceivable way that this will not be a number
							
						}
						else
						{
							length = 0;
							// for the TIME, DATE, and DATETIME types, the value inside the parentheses
							// are not to define length, and thus will not be counted
						}
						
						// data type will be defined immediately before the opening parenthesis
						String dt = typedata.substring(0, typedata.indexOf('('));
						
						type = getTypeFromString(dt);
					}
					else
					{
						// no length parameter defined, so it is left as 0.
						
						// unless it's a BIT value. the max BIT length is 64
						if (typedata.startsWith("BIT"))
						{
							length = 64;
						}
						// or unless it's a YEAR value. the max YEAR length is 4
						if (typedata.startsWith("YEAR"))
						{
							length = 4;
						}
						
						if (typedata.contains(" ")) // meaning UNSIGNED, ZEROFILL, or other special data was written on the end
						{
							String dt = typedata.substring(0, typedata.indexOf(" "));
							
							type = getTypeFromString(dt);
						}
						else
						{
							// only contains data type and that's it. no further parsing necessary
							type = getTypeFromString(typedata);
						}
					}
				}
				else
				{
					// is an ENUM or SET type
					length = 0;
					isenum = true;
					
					if (typedata.startsWith("ENUM"))
					{
						type = ColumnDataType.Enum;
					}
					else
					{
						type = ColumnDataType.Set;
					}
				}
				
				if (isenum)
				{
					// in order to make the 'typedata' string look pretty
					// while keeping the casing of the original enum values
					String lth = data.getString(2).substring(typedata.indexOf("(") + 1, typedata.indexOf(")"));
					typedata = typedata.substring(0, typedata.indexOf("(")) + "(" + lth + ")";
				}
				
				ColumnData col = new ColumnData(data.getString(1), typedata, type, rn, length, nn, us, pk);
				
				if (isenum) // set up enumerator/set values
				{
					String lth = data.getString(2).substring(typedata.indexOf("(") + 1, typedata.indexOf(")"));
					col.setUpEnumValues(lth);
				}
				
				// Add to column list
				cols.add(col);
				
				rn++;
			}
		}
		catch (SQLException e)
		{
			throw e;
		}
		
		return cols;
	}
	
	private static ColumnDataType getTypeFromString(String str)
	{
		// used source: http://docs.oracle.com/cd/E17952_01/connector-j-en/connector-j-reference-type-conversions.html
		// used source: http://dev.mysql.com/doc/refman/5.6/en/create-table.html
		
		switch (str)
		{
		case "BIT":
			return ColumnDataType.Bit;
		case "BOOL":
		case "BOOLEAN": // MySQL does not have a defined BOOLEAN type. BOOL and BOOLEAN are synonyms of TINYINT(1).
		case "TINYINT":
			return ColumnDataType.Byte;
		case "SMALLINT":
			return ColumnDataType.Short;
		case "MEDIUMINT":
		case "INT":
		case "INTEGER":
			return ColumnDataType.Integer;
		case "BIGINT":
			return ColumnDataType.Long;
		case "DOUBLE":
			return ColumnDataType.Double;
		case "REAL":
		case "FLOAT":
			return ColumnDataType.Float;
		case "DECIMAL":
		case "NUMERIC": // the two data types are synonymous, from what it seems
			return ColumnDataType.Decimal;
		case "DATE":
		case "YEAR": // configured by default to return a DATE, of the specified year, on Jan 1st at midnight
			return ColumnDataType.Date;
		case "TIME":
			return ColumnDataType.Time;
		case "DATETIME":
		case "TIMESTAMP": // Timestamp differs from DateTime in that the data is stored in UTC time
			return ColumnDataType.DateTime;
		case "CHAR":
		case "VARCHAR":
		case "LONGVARCHAR": // lumping LONGVARCHAR with CHAR and VARCHAR isn't the best, no, but it's enough for this program's purposes
			return ColumnDataType.VarChar;
		case "TINYBLOB":
		case "BLOB":
		case "MEDIUMBLOB":
		case "LONGBLOB": // blobs are used for storing data, such as images, etc. rather than just plain text
			return ColumnDataType.Blob;
		case "TINYTEXT":
		case "TEXT":
		case "MEDIUMTEXT":
		case "LONGTEXT": // text is stored separately from the table itself; the table just references the text resource
			return ColumnDataType.Text;
		case "BINARY":
		case "VARBINARY":
			return ColumnDataType.Binary;
		default:
			return ColumnDataType.VarChar;
		}
		
		//return ColumnDataType.VarChar;
	}

		
	
	private boolean checkDate(String value, int y1, int y2, int m1, int m2, int d)
	{
		try
		{
			/*int year = */ Integer.parseInt(value.substring(y1, y2));
			int month = Integer.parseInt(value.substring(m1, m2));
			int day = Integer.parseInt(value.substring(d));
			
			if (month > 12)
			{
				return false; // only 12 months
			}
			
			if (month == 1 || month == 3 || month == 5 || month == 7 || month == 8 || month == 10 || month == 12)
			{
				if (day > 31)
				{
					return false; // these 7 months have 31 days
				}
			}
			else if (month == 2)
			{
				if (day > 29)
				{
					return false; // February only has 28
					// to save time, we do not check to see if February is a leap year on the specified year
				}
			}
			else
			{
				if (day > 30)
				{
					return false; // the remaining months have 30 days
				}
			}
		}
		catch (NumberFormatException e)
		{
			// if we're here
			// that means one of the numbers above really isn't a number
			// so... it's not a valid date string
			return false;
		}
		
		return true;
	}
	
	/**
	 * Check a string representation of a time value to see if it is valid.
	 * @param value The value to check.
	 * @param punctuation Check if there is punctuation or not. For example, if checking for "HH:MM:SS", input "true".
	 * If checking for "HHMMSS", input false.
	 * @param twovalues Check for two values or three. For example, if checking for "HH:MM", input "true".
	 * If checking for "HH:MM:SS", input "false".
	 * @param allowlargerthan24hrs Allow the max hour value to be 99 or 24. If "true", the max value will be 99.
	 * @return "true" if the string is valid. "false" if the string is invalid in some way.
	 */
	private boolean checkTime(String value, boolean punctuation, boolean twovalues, boolean allowlargerthan24hrs)
	{	
		int hour = 0;
		int min = 0;
		int sec = 0;
		
		if (punctuation == true)
		{
			// HH:MM:SS or HH:MM
			try
			{
				
				if (twovalues == false)
				{
					// HH:MM:SS
					hour = Integer.parseInt(value.substring(0, 2));
					min = Integer.parseInt(value.substring(3, 5));
					sec = Integer.parseInt(value.substring(6));
				}
				else
				{
					// HH:MM
					hour = Integer.parseInt(value.substring(0, 2));
					min = Integer.parseInt(value.substring(3));
				}
				
			}
			catch (NumberFormatException e)
			{
				// if we're here, that means one of the numbers above really isn't a number
				// so... it's not a valid time string
				return false;
			}
		}
		else
		{
			// HHMMSS or MMSS
			try
			{		
				if (twovalues == false)
				{
					// HHMMSS
					hour = Integer.parseInt(value.substring(0, 2));
					min = Integer.parseInt(value.substring(2, 4));
					sec = Integer.parseInt(value.substring(4));
				}
				else
				{
					// MMSS
					min = Integer.parseInt(value.substring(0, 2));
					sec = Integer.parseInt(value.substring(2));
				}
			}
			catch (NumberFormatException e)
			{
				// if we're here, that means one of the numbers above really isn't a number
				// so... it's not a valid time string
				return false;
			}
		}
		
		// parse time values
		if (allowlargerthan24hrs)
		{
			if (hour > 99 || hour < -9)
			{
				return false;
			}
		}
		else
		{
			if (hour > 24 || hour < 0)
			{
				return false;
			}
		}

		if (min > 59 || min < 0)
		{
			return false;
		}
		
		if (sec > 59 || min < 0)
		{
			return false;
		}
		
		return true; // seems fine to me
		
	}
	
	private enum NumericType { BYTE, SHORT, INT, LONG, FLOAT, DOUBLE }
	
	private boolean checkNumeric(String value, NumericType type, boolean checknegatives)
	{
		Number n;
		
		try
		{
			switch (type)
			{
			case BYTE:
				n = Byte.parseByte(value);
				break;
			case DOUBLE:
				n = Double.parseDouble(value);
				break;
			case FLOAT:
				n = Float.parseFloat(value);
				break;
			case INT:
				n = Integer.parseInt(value);
				break;
			case LONG:
				n = Long.parseLong(value);
				break;
			case SHORT:
				n = Short.parseShort(value);
				break;
			default:
				n = 0;
				return false;
			}
			
		}
		catch (NumberFormatException e)
		{
			return false; // cannot be parsed as this value
		}
		
		// check if unsigned
		if (checknegatives)
		{
			if (n.longValue() < 0)
			{
				return false;
			}
		}
		
		return true;  // if it reached this point, that means it's valid
	}
	
}
