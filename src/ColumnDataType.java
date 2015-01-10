// Java conventions say Enum values should be in all caps, don't they?
// Whoops! So used to C# conventions.
// It's an easy fix to change this to the conventional way, but it has no affect on the program itself, so...

public enum ColumnDataType {
	VarChar, Text, Blob, Bit, Binary, Short, Integer, Double, Long, Float, Byte, Decimal, Date, Time, DateTime, Enum, Set;
}
