package org.blackaho.misc.datagencli;

import java.util.List;

/**
 * Interface for classes that implement a Row Generator
 */
public interface RowGenerator {

  /**
   * Get all supported fields by RowGenerator implementation
   * @return a list of supported fields (to be used in setFields())
   */
  List<String> getAvailableFields();

  /**
   * Sets header, used on generateHeaderLine(), a list of column names
   * @param header
   * @return this object, to help on construction
   */
  RowGenerator setHeader(List<String> header);

  /**
   * Sets the string returned by generateHeaderLine(), used instead of setHeader() to allow specify specific
   * formatting to this header line (e.g., adding some generation date/time, etc)
   * @param header
   * @return this object, to help on construction
   */
  RowGenerator setHeaderLine(String header);


  /**
   * Sets the fields definition RowGenerator will use to generate rows
   * @param fields
   * @return this object, to help on construction
   */
  RowGenerator setFields(List<String> fields);

  /**
   * Sets the separator char to separate each field, when generating lines (strings) with generateRowLine
   * @param separator
   * @return
   */
  RowGenerator setFieldsSeparator(String separator);

  /**
   * Generates a row, according to fields definition set with setFieds()
   * @return List of objects (column) values according to fields definition set with setFields()
   */
  List generateRow();

  /**
   * Generate a header line, according to header definition set with setHeader()
   * @return
   */
  String generateHeaderLine();

  /**
   * Generates a row in string (line) format, according to fields definition set with setFieds()
   * @return
   */
  String generateRowLine();
}
