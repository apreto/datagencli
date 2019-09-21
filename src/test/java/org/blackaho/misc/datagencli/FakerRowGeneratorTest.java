package org.blackaho.misc.datagencli;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for DataGenCli.
 */
public class FakerRowGeneratorTest {

  FakerRowGenerator rowGenerator;


  @Before
  public void before() {
    rowGenerator = new FakerRowGenerator();
  }


  @Test
  public void testGetAvailableFields() {
    // joining a few distinct tests into one to avoid cost of recursion done by this method call
    List<String> availableFields = rowGenerator.getAvailableFields();


    // check if our custom fields are present (randomLong, randomString, randomDouble)
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("randomLong(")));
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("randomDouble(")));
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("randomString(")));

    // check if we have a few well known classes of faker API
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("name.firstName")));
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("name.lastName")));
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("address.fullAddress")));
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("internet.emailAddress")));
    assertTrue(availableFields.stream().anyMatch(s -> s.startsWith("lorem.sentence"))); // we love this one

  }


  // test getFieldValue - custom functions and faker API call

  @Test
  public void testGetFieldValueForCustomRandomLong() {
    long result = (Long) rowGenerator.getFieldValue("randomLong(1:10)");
    assertTrue(result >= 1 && result <= 10);
  }

  @Test
  public void testGetFieldValueForCustomRandomDouble() {
    double result = (Double) rowGenerator.getFieldValue("randomDouble(2:1:10)");
    assertTrue(result >= 1.0D && result <= 10.0D);
  }

  @Test
  public void testGetFieldValueForCustomRandomString() {
    String result = (String) rowGenerator.getFieldValue("randomString(aaa????bb###)"); // faker bothify -  4 chars (?), 3 numbers
    assertTrue(result.matches("aaa[a-zA-Z]{4}bb[0-9]{3}"));
  }

  @Test
  public void testGetFieldValueForFakerAPICall() {
    String result = (String) rowGenerator.getFieldValue("name.fullName");
    assertTrue(result.trim().length() > 0);
  }

  @Test
  public void testGetFieldValueForInvalidCall() {
    String result = (String) rowGenerator.getFieldValue("some.invalid.name");
    assertTrue(result.equals(""));
  }

  // test generate Row Line (with separator in right places)

  @Test
  public void testGenerateRowLine() {
    List<String> fields = Arrays.asList(new String[] {"randomLong(1:10)","randomString(###)","randomLong(10:20)"});
    rowGenerator.setFieldsSeparator(";").setFields( fields );
    String result = rowGenerator.generateRowLine();
    String[] resultCols = result.split(";");
    // check if result has 3 columns separated by ";"
    assertTrue(resultCols.length == 3);
    // check if they are what we expected.
    long col1  = Long.parseLong(resultCols[0]);
    assertTrue(col1 >= 1 && col1 <= 10);
    assertTrue(resultCols[1].matches("[0-9]{3}"));
    long col3  = Long.parseLong(resultCols[2]);
    assertTrue(col3 >= 10 && col3 <= 20);
  }

  @Test
  public void testGenerateRow() {
    List<String> fields = Arrays.asList(new String[] {"randomLong(1:10)","randomString(###)","randomLong(10:20)"});
    rowGenerator.setFieldsSeparator(";").setFields( fields );
    List result = rowGenerator.generateRow();
    // check if result has 3 columns
    assertTrue(result.size() == 3);
    // check if they are what we expected.
    long col1  = (Long) result.get(0);
    assertTrue(col1 >= 1 && col1 <= 10);
    String col2 = (String) result.get(1);
    assertTrue(col2.matches("[0-9]{3}"));
    long col3  = (Long) result.get(2);
    assertTrue(col3 >= 10 && col3 <= 20);

  }

  @Test
  public void testGenerateHeader() {
    String header = "col1,col2,col3";
    rowGenerator.setHeader(header);
    assertEquals(header, rowGenerator.generateHeaderLine());
  }




}
