package org.blackaho.misc.datagencli;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    static final String OPTION_OUT = "--out=";
    static final String USAGE = "Usage: java -jar datagencli.jar --listfields | [--rows=<number of rows to gen>] [-mbs=<megabytes to gen>] "
        + "--separator=<fields separator char> --header=<header line to gen> --fields=<comma separated list of fields to gen> "
        + "--out=<output filename>";

    // fields set with options
    boolean listFieldsOnly = false;
    int nRows = 0;
    int nMbytes = 0;
    String separator = ",";
    String header = null;
    String outputFilename = null;
    List<String> fields = new ArrayList<>();

    // vars needed for run()
    private PrintWriter err = new PrintWriter(System.err,true); //NOSONAR - we really want to write to stderr
    private PrintWriter out = new PrintWriter(System.out); //NOSONAR - we really want to write to stdout, buffered
    private RowGenerator rowGenerator = null;


    public static void main(String[] args) {
        DataGenCLI dataGenCLI = new DataGenCLI();
        dataGenCLI.parseOptions(args);
        if (!dataGenCLI.checkOptions()) {
            return;
        }
        dataGenCLI.run();
    }


    public void run() {
        rowGenerator = RowGeneratorFactory.createDefaultRowGenerator()
            .setFields(fields)
            .setFieldsSeparator(separator)
            .setHeader(header);

        boolean closeOutAtEnd = false;
        // sets output to a specific file, instead of console
        try {
            if (outputFilename != null) {
                out = new PrintWriter(new FileOutputStream(outputFilename));
                closeOutAtEnd = true;
            }
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
        if (header != null) out.println(rowGenerator.generateHeaderLine());
        IntStream.range(1,nRows+1).parallel().forEach( rowNum -> { // do in parallel
            out.println(rowGenerator.generateRowLine());
            if (rowNum%1000==0) out.flush(); // flush output every 1000 lines
        });
        out.flush();
    }

    protected void runWithNumberOfMegabytes() {
        // get a sample of 1000 rows get average size per line. then, use it to calc number of rows needed.
        // number of bytes will an approach, but we avoid having a sync/shared counter between threads
        long totalBytes = LongStream.range(0,1000).parallel()
            .map( n -> rowGenerator.generateRowLine().getBytes().length+1 )
            .reduce( (a,b) -> a+b ).getAsLong();
        long avgBytesPerRow = totalBytes/1000L;
        long rowsToGenerate = (nMbytes*1024L*1024L)/avgBytesPerRow;
        if (header != null) out.println(rowGenerator.generateHeaderLine());
        LongStream.range(1, rowsToGenerate+1).parallel().forEach(rowNum -> {
            out.println(rowGenerator.generateRowLine());
            if (rowNum%1000==0) out.flush();
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
            } else if (arg.startsWith(OPTION_HEADER)) {
                header = arg.substring(OPTION_HEADER.length(), arg.length());
            } else if (arg.startsWith(OPTION_OUT)) {
                outputFilename = arg.substring(OPTION_OUT.length(), arg.length());
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
            err.println("ERROR: Options --rows=numberOfRows and --mbs=megabytesToGenerate cannot be used together.");
            err.println(USAGE);
            return false;
        } else if (fields.isEmpty() || (nRows == 0 && nMbytes == 0)) {
            err.println(USAGE);
            return false;
        }
        return true;
    }


}
