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


  // test distinct field types - custom functions and faker API call

  @Test
  public void testGenerateRowWithFieldRowNumber() {
    rowGenerator.setFields(Arrays.asList("rowNumber"));
    long result = (Long) rowGenerator.generateRow(123L).get(0);
    assertEquals(123L, result);
  }

  @Test
  public void testGenerateRowWithFieldSequence() {
    rowGenerator.setFields(Arrays.asList("sequence(10:3)"));
    long result = (Long) rowGenerator.generateRow(2L).get(0);
    assertEquals(13L, result);
  }

  @Test
  public void testGenerateRowWithFieldRandomLong() {
    rowGenerator.setFields(Arrays.asList("randomLong(1:10)"));
    long result = (Long) rowGenerator.generateRow(1L).get(0);
    assertTrue(result >= 1 && result <= 10);
  }

  @Test
  public void testGenerateRowWithFieldRandomDouble() {
    rowGenerator.setFields(Arrays.asList("randomDouble(2:1:10)"));
    double result = (Double) rowGenerator.generateRow(1L).get(0);
    assertTrue(result >= 1.0D && result <= 10.0D);
  }

  @Test
  public void testGenerateRowWithFieldRandomString() {
    rowGenerator.setFields(Arrays.asList("randomString(aaa????bb###)"));    // faker bothify -  4 chars (?), 3 numbers
    String result = (String) rowGenerator.generateRow(1L).get(0);
    assertTrue(result.matches("aaa[a-zA-Z]{4}bb[0-9]{3}"));
  }

  @Test
  public void testGenerateRowWithFieldRandomStringUsingSpecialChars() {
    rowGenerator.setFields(Arrays.asList("randomString(a?-b #.+%:#@z&/\\[]=$;)"));
    String result = (String) rowGenerator.generateRow(1L).get(0);
    assertTrue(result.matches("a[a-zA-Z]-b\\s[0-9]\\.\\+%:[0-9]@z&/\\\\\\[\\]=\\$;"));
  }


  @Test
  public void testGenerateRowWithFieldMappedToFakerAPICall() {
    rowGenerator.setFields(Arrays.asList("name.fullName"));
    String result = (String) rowGenerator.generateRow(1L).get(0);
    assertTrue(result.trim().length() > 0);
  }

  @Test
  public void testGenerateRowWithFieldUnknown() {
    rowGenerator.setFields(Arrays.asList("some.unknown.name"));
    String result = (String) rowGenerator.generateRow(1L).get(0);
    assertTrue(result.equals("")); // current behavior is not error out, return "" and continue generating other fields
  }


  // test generate Row Line (with separator in right places)

  @Test
  public void testGenerateRowLine() {
    List<String> fields = Arrays.asList(new String[] {"randomLong(1:10)","randomString(###)","randomLong(10:20)"});
    rowGenerator.setFieldsSeparator(";").setFields( fields );
    String result = rowGenerator.generateRowLine(1L);
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
    List result = rowGenerator.generateRow(1L);
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
  public void testGenerateHeaderLineWithHeader() {
    List<String> header = Arrays.asList( new String[] {"col1","col2","col3"});
    rowGenerator.setHeader(header);
    rowGenerator.setFieldsSeparator(";");
    assertEquals("col1;col2;col3", rowGenerator.generateHeaderLine());
  }

  @Test
  public void testGenerateHeaderLineWithHeaderLine() {
    String header = "col1,col2,col3";
    rowGenerator.setHeaderLine(header);
    rowGenerator.setFieldsSeparator(";"); // checks if separator is ignored
    rowGenerator.setHeader(Arrays.asList(new String[] {"col5,col6"})); // check header is ignored when using headerLine
    assertEquals(header, rowGenerator.generateHeaderLine());
  }



}
