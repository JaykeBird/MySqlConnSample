MySql Server Connection Sample
==============================
Created by Jacob R. Huempfner, January 2015

Documentation for using the program

Command-line arguments
----------------------

_"MySqlConnSample.jar database username password"_

database - name of the database/schema to connect with.
username - name of the user accessing the database.
password - password for the user accessing the database.

For example,

_"MySqlConnSample.jar myDb JohnSmith pass123"_

Available commands
------------------

- *Add*: Add a new record to the table.
- *About*: Learn more about this program.
- *Display*: Show all records and data within a table.
- *Delete*: Delete records in a table that match a single condition.
- *Describe*: Describe the columns of the table.
- *Exit*: Quit the program.
- *Help*: Display this help screen.
- *SQL*: Enter Direct SQL mode to send commands straight to the server.
- *Table*: Select which table in the database to use.

This can be displayed by using the 'Help' command. Commands are not case-sensitive. (Typing 'help' or 'HELP' will work as well.)

About adding values
-------------------

When using the 'Add' command to add a value to the table, the single-quotes around strings and bit values do not need to be added.

For example, when entering a value for a VARCHAR column named "LastName", just typing `Johnson` is an acceptable and the expected way data will be entered.

For SET columns, be sure to separate each SET element by a comma, and do not use spaces after the commas, similar to how you'd enter data using the MySQL command line interface itself. For example, for a column of data type `SET('a','b','c','d')`, enter in a new value as `a,d`. Type `a` to just select only one element.

For BIT columns, enter in the data in binary format. For example, to enter the number 5 into a BIT column, type `101`. Do not type it as `b'101'` or `5`.

Direct SQL mode
---------------

Type the command 'Sql' to enter Direct SQL mode, which will allow you to send SQL commands straight to the MySQL server itself. Typing the semicolon at the end of each command is not necessary.

For example, you can type commands such as `SELECT LastName,BirthDate FROM employeelist` or `ALTER TABLE employeelist ADD PRIMARY KEY (ID)`.

If there are any errors or warnings while in Direct SQL mode, the program will display them, and continue to run.

Commands that update a table (INSERT, DELETE, ALTER, UPDATE) will return the value "x rows updated" if successful, with the number of rows added/removed/changed in place of "x".

Commands that query a table (such as SHOW or SELECT) will display results in the form of a table output.

Type 'exit' to leave Direct SQL mode.

Known issues
------------

1. When entering a value into a record with the data type TIME, DATE, DATETIME, and TIMESTAMP, the program may incorrectly state some strings are invalid . These are strings where leading zeros are not necessary in the month, day, hour, minute, or second values because punctuation is added between units of time. (For example, 13:04:09 and 13:4:9 are both valid strings for MySQL, but the program will not accept the latter.)
2. Handling is not made for leap years. If "2007-02-29" is entered for a DATE value, the program will not consider the string invalid, but MySQL will not allow the value to be entered.
3. TIME values larger than 99 hours or less than -9 hours will be incorrectly stated as invalid, due to how the program checks the time value. Use Direct SQL mode to insert or update these values correctly.

Contact:
--------

Feel free to contact me at my Twitter profile @JaykeBird or email me (my email is on my GitHub profile).
