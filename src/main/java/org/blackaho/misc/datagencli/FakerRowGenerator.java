package org.blackaho.misc.datagencli;

import com.github.javafaker.Faker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FakerRowGenerator implements RowGenerator {

  // special/custom functions
  protected static final Pattern RANDOMSTRING_REGEXP = Pattern.compile("randomString\\(([\\w\\#\\?]*)\\)");
  protected static final Pattern RANDOMLONGFUNC_REGEXP = Pattern.compile("randomLong\\((\\d+):(\\d+)\\)");
  protected static final Pattern RANDOMDOUBLEFUNC_REGEXP = Pattern.compile("randomDouble\\((\\d+):(\\d+):(\\d+)\\)"); //"randomDouble\\((\\d+):(\\d+\\.?\\d*):(\\d+\\.?\\d*)\\)"
  protected static final Pattern SEQUENCEFUNC_REGEXP = Pattern.compile("sequence\\((\\d+):(\\d+)\\)");


  protected Faker faker;
  protected List<String> fields;
  protected List<String> header;
  protected String headerLine;
  protected String separator;
  protected  HashMap<String,RowGeneratorMetaEntry> rowGeneratorCache;


  public FakerRowGenerator() {
    this.faker = new Faker();
    this.rowGeneratorCache = new HashMap<>();
    this.separator = ","; // defaults to comma
  }


  /**
   * Get all supported fields by Faker API using reflection on Faker object
   * @return a list of supported fields (to be used in setFields())
   */
  @Override
  public List<String> getAvailableFields() {
    List<String> results = new ArrayList<>();
    // add fields we have special handling, faker.number.numberBetween(min,max),faker.number.randomDouble(maxDecimals,min,max) and faker.bothify()
    results.add("rowNumber");
    results.add("sequence(start:increment)");
    results.add("randomString(bothifyFormatting)");
    results.add("randomLong(min:max)");
    results.add("randomDouble(maxDecimals:min:max)");
    getAvailableFieldsRecursive(faker.getClass(), null, results, 6);
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



  @Override
  public RowGenerator setHeaderLine(String headerLine) {
    this.headerLine = headerLine;
    return this;
  }

  @Override
  public RowGenerator setHeader(List<String> header) {
    this.header = header;
    return this;
  }


  @Override
  public RowGenerator setFields(List<String> fields) {
    this.fields = fields;
    return this;
  }

  @Override
  public RowGenerator setFieldsSeparator(String separator) {
    this.separator = separator;
    return this;
  }


  @Override
  public String generateHeaderLine() {
    if (this.headerLine!=null) {
      return this.headerLine;
    } else {
      return header.stream().collect(Collectors.joining(this.separator));
    }
  }

  @Override
  public List generateRow(Long lineNumber) {
    return fields.stream().map( field -> this.getFieldValue(field, lineNumber) ).collect(Collectors.toList());
  }

  @Override
  public String generateRowLine(Long lineNumber) {
    StringBuilder line = new StringBuilder();
    boolean firstField = true;
    for (String field : fields) {
      if (!firstField) {
        line.append(this.separator);
      } else {
        firstField = false;
      }
      line.append(this.getFieldValue(field, lineNumber));
    }
    return line.toString();
  }


  static class RowGeneratorMetaEntry {
    String fieldKey;
    Object generatorObject;
    Method generatorMethod;
    Object[] generatorArguments;
    enum EntryType { ROWNUMBER, SEQUENCE, FAKER_METHOD}
    EntryType entryType;

    public RowGeneratorMetaEntry(String field,EntryType et, Object object, Method method, Object[] args) {
      fieldKey = field;
      entryType = et;
      generatorObject = object;
      generatorMethod = method;
      generatorArguments = args;
    }
  }

  protected RowGeneratorMetaEntry createRowGeneratorMetaEntry(String field) {
    try {
      // handle custom fields
      if (RANDOMSTRING_REGEXP.matcher(field).find()) {
        Matcher m = RANDOMSTRING_REGEXP.matcher(field); m.find();
        // simulate faker.bothify(m.group(1))
        return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.FAKER_METHOD, faker,
            faker.getClass().getDeclaredMethod("bothify", String.class),
            new Object[] {m.group(1)} );
      } else if (RANDOMLONGFUNC_REGEXP.matcher(field).find()) {
        Matcher m = RANDOMLONGFUNC_REGEXP.matcher(field); m.find();
        // simulate faker.number().numberBetween(Long.parseLong(m.group(1)),Long.parseLong(m.group(2)))
        return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.FAKER_METHOD, faker.number(),
            faker.number().getClass().getDeclaredMethod("numberBetween", long.class, long.class),
            new Object[] { Long.parseLong(m.group(1)), Long.parseLong(m.group(2)) } );
      } else if (RANDOMDOUBLEFUNC_REGEXP.matcher(field).find()) {
        Matcher m = RANDOMDOUBLEFUNC_REGEXP.matcher(field); m.find();
        // simulate faker.number().randomDouble(Integer.parseInt(m.group(1)), Long.parseLong(m.group(2)),Long.parseLong(m.group(3)))
        return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.FAKER_METHOD, faker.number(),
            faker.number().getClass().getDeclaredMethod("randomDouble", int.class, long.class, long.class),
            new Object[] { Integer.parseInt(m.group(1)), Long.parseLong(m.group(2)),Long.parseLong(m.group(3)) } );
      } else if (SEQUENCEFUNC_REGEXP.matcher(field).find()) {
        Matcher m = SEQUENCEFUNC_REGEXP.matcher(field); m.find();
        return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.SEQUENCE,null, null,
            new Object[] { Long.parseLong(m.group(1)), Long.parseLong(m.group(2)) } );
      } else if (field.equals("rowNumber")) {
        return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.ROWNUMBER,null, null, null);
      } else {
        // handle other fields in field1.name1 format with reflection
        String[] methodCalls = field.split("\\.");
        Object objToCall = faker;
        for (int i = 0; i < methodCalls.length-1; i++) {
          objToCall = objToCall.getClass().getDeclaredMethod(methodCalls[i]).invoke(objToCall);
        }
        Method methodToCall = objToCall.getClass().getDeclaredMethod(methodCalls[methodCalls.length-1]);
        return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.FAKER_METHOD, objToCall, methodToCall, new Object[] {} );
      }
    } catch (Exception ex) {
      return new RowGeneratorMetaEntry(field, RowGeneratorMetaEntry.EntryType.FAKER_METHOD, null, null, null);
    }
  }

  /**
   * Get a value for a field using reflection on Faker object.
   * E.g., field "name.firstName" will call faker.name().firstName() and return its value
   * @param field
   * @return
   */
  protected Object getFieldValue(String field, Long lineNumber) {
    // get value from cache, init cache if needed
    RowGeneratorMetaEntry fieldCacheEntry = rowGeneratorCache.get(field);
    if (fieldCacheEntry == null) {
      fieldCacheEntry = createRowGeneratorMetaEntry(field);
      rowGeneratorCache.put(field, fieldCacheEntry);
    }

    // handles stuff we can handle directly, w/o faker, line rowNumber and sequence
    if (fieldCacheEntry.entryType == RowGeneratorMetaEntry.EntryType.ROWNUMBER) {
      return lineNumber;
    } else if (fieldCacheEntry.entryType == RowGeneratorMetaEntry.EntryType.SEQUENCE) {
      return (long) fieldCacheEntry.generatorArguments[0] + ((lineNumber-1L) * (long) fieldCacheEntry.generatorArguments[1]);
    } else { // RowGeneratorMetaEntry.EntryType.SEQUENCE
      try { // call generator object/method in cache using reflection API
        return fieldCacheEntry.generatorMethod.invoke(fieldCacheEntry.generatorObject, fieldCacheEntry.generatorArguments);
      } catch (Exception ex) {
        return ""; // review - error out instead of return empty string.
      }
    }
  }






}
