package org.blackaho.misc.datagencli;

import com.github.javafaker.Faker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Main class for DataGen CLI
 *
 */
public class DataGenCLI {

    // options
    static final String OPTION_LISTFIELDS = "--listfields";
    static final String OPTION_NROWS = "--rows=";
    static final String OPTION_NMBYTES = "--mbs=";
    static final String OPTION_SEPARATOR = "--separator=";
    static final String OPTION_FIELDS = "--fields=";
    static final String OPTION_HEADER = "--header=";
    static final String USAGE = "Usage: java -jar datagencli.jar --listfields | [--rows=<number of rows to gen>] [-mbs=<megabytes to gen>] "
        + "--separator=<fields separator char> --header=<header line to gen> --fields=<comma separated list of fields to gen>";

    // special/custom functions
    static final Pattern RANDOMSTRING_REGEXP = Pattern.compile("randomString\\(([\\w\\#\\?]*)\\)");
    static final Pattern RANDOMLONGFUNC_REGEXP = Pattern.compile("randomLong\\((\\d+):(\\d+)\\)");
    static final Pattern RANDOMDOUBLEFUNC_REGEXP = Pattern.compile("randomDouble\\((\\d+):(\\d+):(\\d+)\\)"); //"randomDouble\\((\\d+):(\\d+\\.?\\d*):(\\d+\\.?\\d*)\\)"

    // fields set with options
    boolean listFieldsOnly = false;
    int nRows = 0;
    int nMbytes = 0;
    String separator = ",";
    String header = null;
    List<String> fields = new ArrayList<>();


    public static void main(String[] args) {
        DataGenCLI dataGenCLI = new DataGenCLI();
        dataGenCLI.parseOptions(args);
        if (!dataGenCLI.checkOptions()) {
            return;
        }
        dataGenCLI.run();
    }



    public void run() {
        Faker faker = new Faker();

        if (listFieldsOnly) {
            for (String field : getAvailableFields(faker)) {
                System.err.println(field);
            }
        } else if (nRows != 0) {
            if (header != null) System.out.println(header);
            for (int nGenRows = 0; nGenRows < nRows; nGenRows++) {
                System.out.println(generateRowLine(faker, separator, fields));
            }
        } else if (nMbytes != 0) {
            long maxBytes = nMbytes * 1024L * 1024L;
            long genBytes = 0L;
            if (header != null) System.out.println(header);
            while (genBytes < maxBytes) {
                String line = generateRowLine(faker, separator, fields);
                genBytes += line.getBytes().length + 1L; // plus one, count for new line char
                System.out.println(line);
            }
        }

    }


    protected static String generateRowLine(Faker faker, String separator, List<String> fields) {
        StringBuilder line = new StringBuilder();
        boolean firstField = true;
        for (String field : fields) {
            if (!firstField) {
                line.append(separator);
            } else {
                firstField = false;
            }
            line.append(getFieldValue(faker, field));
        }
        return line.toString();
    }

    /**
     * Get a value for a field using reflection on Faker object.
     * E.g., field "name.firstName" will call faker.name().firstName() and return its value
     * @param faker
     * @param field
     * @return
     */
    protected static String getFieldValue(Faker faker, String field) {
        // yeah, I could use some Expression Language lib, just don't want to add another dependency just for this
        // TBD/Improve: cache in a Map field names (key),faker objects/methods and parsed parameters (value) for faster performance
        try {
            // handle custom fields
            if (RANDOMSTRING_REGEXP.matcher(field).find()) {
                Matcher m = RANDOMSTRING_REGEXP.matcher(field); m.find();
                return faker.bothify(m.group(1));
            } else if (RANDOMLONGFUNC_REGEXP.matcher(field).find()) {
                Matcher m = RANDOMLONGFUNC_REGEXP.matcher(field); m.find();
                return "" + faker.number().numberBetween(Long.parseLong(m.group(1)),Long.parseLong(m.group(2)));
            } else if (RANDOMDOUBLEFUNC_REGEXP.matcher(field).find()) {
                Matcher m = RANDOMDOUBLEFUNC_REGEXP.matcher(field); m.find();
                return "" + faker.number().randomDouble(Integer.parseInt(m.group(1)), Long.parseLong(m.group(2)),Long.parseLong(m.group(3)));
            } else {
                // handle other fields in field1.name1 format with reflection
                String[] methodCalls = field.split("\\.");
                Object curObj = faker;
                Object retVal = null;
                for (int i = 0; i < methodCalls.length; i++) {
                    retVal = curObj.getClass().getDeclaredMethod(methodCalls[i]).invoke(curObj);
                    curObj = retVal;
                }
                return retVal != null ? retVal.toString() : "";
            }
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Get all supported fields from faker object using reflection
     * @param fakerObject - Faker object
     * @return
     */
    protected static List<String> getAvailableFields(Object fakerObject) {
        List<String> results = new ArrayList<>();
        // add fields we have special handling, faker.number.numberBetween(min,max),faker.number.randomDouble(maxDecimals,min,max) and faker.bothify()
        results.add("randomString(bothifyFormatting)");
        results.add("randomLong(min,max)");
        results.add("randomDouble(maxDecimals,min,max)");
        getAvailableFieldsRecursive(fakerObject.getClass(), null, results, 6);
       return results;
    }

    protected static void getAvailableFieldsRecursive(Class fakerObjectClass, String prefix, List<String> results, int maxRecursion) {
        final List<String> ignoreMethods = Arrays.asList("instance", "toString");

        if (maxRecursion <= 0) return; // reached max recursion level. just in case...

        Method[] methods = fakerObjectClass.getDeclaredMethods();

        for (Method m : methods) {
            if (m.getParameterCount() == 0 && !ignoreMethods.contains(m.getName())) {
                String currentFieldName = prefix == null ? m.getName() : prefix + "." + m.getName();
                try {
                    Class callResultClass = m.getReturnType();
                    // check if we will call recursively
                    if (callResultClass.getPackage().getName().startsWith("com.github.javafaker")) {
                        // java faker domain object, lets dig into this
                        getAvailableFieldsRecursive(callResultClass, currentFieldName, results, maxRecursion - 1);
                    } else {
                        results.add(currentFieldName);
                    }
                } catch (Exception ex) {
                    // do nothing
                }
            }
        }
    }

    public void parseOptions(String[] args) {

        for (String arg : args) {
            if (arg.startsWith(OPTION_LISTFIELDS)) {
                listFieldsOnly = true;
            } else if (arg.startsWith(OPTION_NROWS)) {
                nRows = Integer.parseInt(arg.substring(OPTION_NROWS.length(), arg.length()));
            } else if (arg.startsWith(OPTION_NMBYTES)) {
                nMbytes = Integer.parseInt(arg.substring(OPTION_NMBYTES.length(), arg.length()));
            } else if (arg.startsWith(OPTION_SEPARATOR)) {
                separator = arg.substring(OPTION_SEPARATOR.length(), arg.length());
            } else if (arg.startsWith(OPTION_HEADER)) {
                header = arg.substring(OPTION_HEADER.length(), arg.length());
            } else if (arg.startsWith(OPTION_FIELDS)) {
                fields = Arrays.asList(arg.substring(OPTION_FIELDS.length(), arg.length()).split(","))
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            }
        }
    }

    public boolean checkOptions() {
        // error checking
        if (listFieldsOnly) {
            return true;
        }
        if (nRows != 0 && nMbytes != 0) {
            System.err.println("ERROR: Options --rows=numberOfRows and --mbs=megabytesToGenerate cannot be used together.");
            System.err.println(USAGE);
            return false;
        } else if (fields.isEmpty() || (nRows == 0 && nMbytes == 0)) {
            System.err.println(USAGE);
            return false;
        }
        return true;
    }


}
