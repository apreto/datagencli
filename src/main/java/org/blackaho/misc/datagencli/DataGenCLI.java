package org.blackaho.misc.datagencli;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Main class for DataGen CLI.
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
  static final String OPTION_HEADERLINE = "--headerline=";
  static final String OPTION_OUT = "--out=";
  static final String OPTION_SLEEP = "--sleep=";

  static final String USAGE = "Usage: java -jar datagencli.jar [ --listfields | --rows=<number of rows to gen> | --mbs=<megabytes to gen> ] "
      + "[ --header=<comma separated list of field names>  | --headerline=<header line to generate> ] "
      + "--separator=<fields separator char> --fields=<comma separated list of fields to gen> "
      + "--sleep=<milisecs to sleep after generating each row> --out=<output filename>";

  // fields set with options
  boolean listFieldsOnly = false;
  int nRows = 0;
  int nMbytes = 0;
  String separator = ",";
  String headerLine = null;
  List<String> header = null;
  String outputFilename = null;
  List<String> fields = new ArrayList<>();
  long sleepInMilisecs = 0;

  // vars needed for run()
  protected PrintWriter err = new PrintWriter(System.err,true); //NOSONAR - we really want to write to stderr
  protected PrintWriter out = new PrintWriter(System.out); //NOSONAR - we really want to write to stdout, buffered
  protected RowGenerator rowGenerator = null;


  public static void main(String[] args) {
    DataGenCLI dataGenCLI = new DataGenCLI();
    dataGenCLI.parseOptions(args);
    if (!dataGenCLI.checkOptions()) {
      return;
    }
    dataGenCLI.run();
  }


  /**
   * If applicable, sets output to specific file instead of console.
   * @return true if file was created for output, false case not.
   */
  protected boolean prepareOutputFile() throws FileNotFoundException {
    if (outputFilename!=null) {
      out = new PrintWriter(new FileOutputStream(outputFilename));
      return true;
    } else {
      return false;
    }
  }

  public void run() {
    rowGenerator = RowGeneratorFactory.createDefaultRowGenerator()
        .setFields(fields)
        .setFieldsSeparator(separator)
        .setHeader(header)
        .setHeaderLine(headerLine);

    boolean closeOutAtEnd = false;
    try {
      closeOutAtEnd = prepareOutputFile(); // if applicable sets output to a specific file, instead of console.
    } catch (FileNotFoundException ex) {
      err.println("ERROR opening output file "+outputFilename + ", stop processing.");
      return;
    }

    if (listFieldsOnly) {
      runWithListFields();
    } else if (nRows != 0) {
      runWithNumberOfRows();
    } else if (nMbytes != 0) {
      runWithNumberOfMegabytes();
    }

    if (closeOutAtEnd) {
      out.close();
    }
  }

  protected void runWithNumberOfRows() {
    runWithNumberOfRows(nRows);
  }

  protected void runWithNumberOfMegabytes() {
    // get a sample of 1000 rows get average size per line. then, use it to calc number of rows needed.
    // number of bytes will an approach, but we avoid having a sync/shared counter between threads
    double avgBytesPerRow = LongStream.range(0,1000).parallel()
        .map( n -> rowGenerator.generateRowLine(n).getBytes().length+1 )
        .average().getAsDouble();
    long rowsToGenerate = (long) ((nMbytes*1024L*1024L)/avgBytesPerRow);
    runWithNumberOfRows(rowsToGenerate);
  }

  protected void runWithNumberOfRows(long numberOfRowsToGenerate) {
    if (header != null || headerLine != null) out.println(rowGenerator.generateHeaderLine());
    LongStream.range(1L,numberOfRowsToGenerate+1L).parallel().forEach( rowNum -> { // do in parallel
      out.println(rowGenerator.generateRowLine(rowNum));
      if (rowNum%1000==0) out.flush(); // flush output every 1000 lines
      try {
        if (sleepInMilisecs > 0) {
          out.flush(); // with sleep, we flush every line
          Thread.sleep(sleepInMilisecs);
        }
      } catch (Exception ex) {
        // just get back to our life
      }
    });
    out.flush();
  }

  protected void runWithListFields() {
    for (String field : rowGenerator.getAvailableFields()) {
      err.println(field);
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
      } else if (arg.startsWith(OPTION_HEADERLINE)) {
        headerLine = arg.substring(OPTION_HEADERLINE.length(), arg.length());
      } else if (arg.startsWith(OPTION_OUT)) {
        outputFilename = arg.substring(OPTION_OUT.length(), arg.length());
      } else if (arg.startsWith(OPTION_FIELDS)) {
        fields = parseCSVStringAsList(arg.substring(OPTION_FIELDS.length(), arg.length()));
      } else if (arg.startsWith(OPTION_HEADER)) {
        header = parseCSVStringAsList(arg.substring(OPTION_HEADER.length(), arg.length()));
      } else if (arg.startsWith(OPTION_SLEEP)) {
        sleepInMilisecs = Long.parseLong(arg.substring(OPTION_SLEEP.length(), arg.length()));
      }
    }
  }

  protected List<String> parseCSVStringAsList(String csvStr) {
    return Arrays.asList(csvStr.split(","))
        .stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  public boolean checkOptions() {
    // error checking
    if (listFieldsOnly) {
      return true;
    }
    if (nRows != 0 && nMbytes != 0) {
      err.println("ERROR: Options --rows=numberOfRows and --mbs=megabytesToGenerate cannot be used together.");
      err.println(USAGE);
      return false;
    } else if (fields.isEmpty() || (nRows == 0 && nMbytes == 0)) {
      err.println(USAGE);
      return false;
    } else if (headerLine==null && header!=null && (fields.size()!=header.size())) {
      err.println("ERROR: Number of fields on --fields different than on --header");
      err.println(USAGE);
      return false;
    } else if (nRows < 0 || nMbytes < 0 || sleepInMilisecs < 0) {
      err.println("ERROR: Either --rows, --mbs or --sleep have negative or invalid values");
      err.println(USAGE);
      return false;
    }
    return true;
  }


}
