# DataGenCLI - Data Generator CLI 

A CLI (Command Line Interface) utility for generating data, based on Java Faker library  - https://github.com/DiUS/java-faker

License: MIT

Building

*  mvn clean install

Resulting (executable) jar file will be on target/datagencli-0.1-SNAPSHOT-jar-with-dependencies.jar

Usage:
* List available fields (from Java Faker API)
  * java -jar datagencli.jar --listfields
* Generate Data 
  * java -jar datagencli.jar --rows=100 --fields='name.fullName,date.birthday'
  * java -jar datagencli.jar --mbs=10 --separator=, --fields='randomLong(1:10000),lorem.sentence'

Generated lines (rows) are written to stdout. Error messages, usage options or fields (with --listfields) option are written to stderr.

Available command line options
* --listfields : list all available fields from Java Faker API
* --fields=<fields-list> : comma separated list of fields to generate (see available fields with --listfields)  
* --rows=<number-of-rows> : number of rows (lines) to generate. Cannot be used in conjunction with --mbs.
* --mbs=<number-of-megabytes> : (approximate) number of Megabytes of data to generate. Cannot be used in conjunction with --rows
* --separator=<separator-string> - char/string separating each field in each generated row/line
* --header=<header-line> : header to add to output, before generating rows

Examples:


> java -jar datagencli.jar --mbs=10 --separator=\; --fields=internet.uuid,name.fullName,date.birthDate,address.fullAddress --header='id;name;birthDate;address' > customers.csv

Generates 10Mb of CSV data with ';' as separator, a header and fields id, name, birthDate, address.

> java -jar datagencli.jar --rows=1000 --separator=, --fields='internet.uuid,commerce.productName,randomLong(10:1000),randomDouble(2:100:700),randomString(AA###??ZR)' --header='productId,name,itemsInStock,price,promotionCode' > products.csv

Generates 1000 rows of CSV data with header and fields id, productName, itemsInStock (random number between 10 and 1000), price (random double between 100 and 700 and 2 decimal places) and promotionCode (string starting with AA, 3 letters, 2 numbers and ending with ZR)


> java -jar datagencli.jar --rows=5 --fields=superhero.name

Generate 5 random superhero names (you never know when you will need it)

