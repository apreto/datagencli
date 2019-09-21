package org.blackaho.misc.datagencli;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

/**
 * Unit tests for DataGenCli.
 */
public class DataGenCLITest
{

    private DataGenCLI dataGenCLI;

    private final ByteArrayOutputStream systemOut = new ByteArrayOutputStream();
    private final ByteArrayOutputStream systemErr = new ByteArrayOutputStream();
    private final PrintStream originalSystemOut = System.out;
    private final PrintStream originalSystemErr = System.err;

    @Before
    public void before() {
        dataGenCLI = new DataGenCLI();
        //faker = new Faker();
        System.setOut(new PrintStream(systemOut));
        System.setErr(new PrintStream(systemErr));
    }

    @After
    public void after() {
        System.setOut(originalSystemOut);
        System.setErr(originalSystemErr);
    }


    // test command line options parsing

    @Test
    public void testParseOptionsListFields() {
        String[] args = new String[] {"--listfields"};
        dataGenCLI.parseOptions(args);
        assertTrue(dataGenCLI.listFieldsOnly);
    }

    @Test
    public void testParseOptionsNRows() {
        String[] args = new String[] {"--rows=10"};
        dataGenCLI.parseOptions(args);
        assertEquals(10, dataGenCLI.nRows);
    }

    @Test
    public void testParseOptionsNMBytes() {
        String[] args = new String[] {"--mbs=1000"};
        dataGenCLI.parseOptions(args);
        assertEquals(1000, dataGenCLI.nMbytes);
    }

    @Test
    public void testParseOptionsSeparatorSingleChar() {
        String[] args = new String[] {"--separator=;"};
        dataGenCLI.parseOptions(args);
        assertEquals(";", dataGenCLI.separator);
    }

    @Test
    public void testParseOptionsSeparatorString() {
        String[] args = new String[] {"--separator=SEPARATOR-STRING"};
        dataGenCLI.parseOptions(args);
        assertEquals("SEPARATOR-STRING", dataGenCLI.separator);
    }

    @Test
    public void testParseOptionsFieldsWithHeader() {
        String[] args = new String[] {"--header=firstName,lastName"};
        dataGenCLI.parseOptions(args);
        assertEquals("firstName,lastName", dataGenCLI.header);
    }

    @Test
    public void testParseOptionsFieldsWithList() {
        String[] args = new String[] {"--fields=name.firstName,name.lastName"};
        dataGenCLI.parseOptions(args);
        Object[] expected = new String[] {"name.firstName","name.lastName"};
        assertArrayEquals(dataGenCLI.fields.toArray(), expected);
    }

    @Test
    public void testParseOptionsFieldsWithSingleItem() {
        String[] args = new String[] {"--fields=name.firstName"};
        dataGenCLI.parseOptions(args);
        Object[] expected = new String[] {"name.firstName"};
        assertArrayEquals(dataGenCLI.fields.toArray(), expected);
    }

    @Test
    public void testParseOptionsFieldsWithSpacesAndMissingFields() {
        String[] args = new String[] {"--fields= name.firstName , name.lastName , ,"};
        dataGenCLI.parseOptions(args);
        Object[] expected = new String[] {"name.firstName","name.lastName"};
        assertArrayEquals(dataGenCLI.fields.toArray(), expected);
    }

    // test argument logic checking

    @Test
    public void testCheckArgumentsValidUsingListFields() {
        dataGenCLI.listFieldsOnly=true;
        assertTrue(dataGenCLI.checkOptions());
    }

    @Test
    public void testCheckArgumentsValidUsingRowsAndFields() {
        dataGenCLI.nRows = 1;
        dataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
        assertTrue(dataGenCLI.checkOptions());
    }

    @Test
    public void testCheckArgumentsValidUsingNMbytesAndFields() {
        dataGenCLI.nMbytes = 1;
        dataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
        assertTrue(dataGenCLI.checkOptions());
    }

    @Test
    public void testCheckArgumentsInvalidMissingRowsOrNMbytes() {
        dataGenCLI.fields = Arrays.asList(new String[] {"name.firstName"});
        assertFalse(dataGenCLI.checkOptions());
    }

    @Test
    public void testCheckArgumentsInvalidMissingFields() {
        dataGenCLI.nRows = 1;
        assertFalse(dataGenCLI.checkOptions());
    }

    @Test
    public void testCheckArgumentsInvalidUsingBothRowsAndNMbytes() {
        dataGenCLI.nRows = 1;
        dataGenCLI.nMbytes = 1;
        assertFalse(dataGenCLI.checkOptions());
    }


    // test run method

    @Test
    public void testRunWithListFieldsOnly() {
        dataGenCLI.listFieldsOnly = true;
        dataGenCLI.run();
        // check if we have some known fields on our redirected system error stream
        String systemErrAsString = systemErr.toString();

        // check for custom/built-in fields
        assertTrue(systemErrAsString.contains("randomString("));
        assertTrue(systemErrAsString.contains("randomLong("));
        assertTrue(systemErrAsString.contains("randomDouble("));

        // check for some known java faker fields
        assertTrue(systemErrAsString.contains("name.firstName"));
        assertTrue(systemErrAsString.contains("address.fullAddress"));
    }

    @Test
    public void testRunWithRowsCount() {
        dataGenCLI.nRows = 1;
        dataGenCLI.fields = Arrays.asList(new String[] {"randomLong(1:10)","randomString(???)" }) ;
        dataGenCLI.header = "number;text";
        dataGenCLI.separator = ";";
        dataGenCLI.run();
        String[] outputLines = systemOut.toString().split("\n");
        assertEquals(2, outputLines.length); // header\nline1\n
        assertEquals( "number;text", outputLines[0]); // first line is our header
        assertEquals(2, outputLines[1].split(";").length); // second line contains 2 fields separated by ;
    }

    @Test
    public void testRunWithMBytesCount() {
        dataGenCLI.nMbytes = 1;
        dataGenCLI.fields = Arrays.asList(new String[] {"lorem.sentence" }) ;
        dataGenCLI.header = "text";
        dataGenCLI.separator = ";";
        dataGenCLI.run(); // generates 1Mb of data
        // let's just check it generates more than 1Mb but less than 2Mb of data;
        int outSizeInBytes = systemOut.size();
        assertTrue(outSizeInBytes >= 1*1024*1024 &&  outSizeInBytes < 2*1024*1024);
    }



    @Test
    public void testMainWithListFields() {
        dataGenCLI.main(new String[] {"--listfields"} );
        // output goes only to err
        assertTrue(systemErr.size() >= 0);
        assertTrue(systemOut.size() == 0);
    }

    @Test
    public void testMainWithGenerateRows() {
        dataGenCLI.main(new String[] {"--rows=1","--fields=randomLong(1:10)"} );
        // output goes only to out
        assertTrue(systemErr.size() == 0);
        assertTrue(systemOut.size() >= 0);
    }

    @Test
    public void testMainWithInvalidArguments() {
        dataGenCLI.main(new String[] {} );
        // output goes only to err
        assertTrue(systemOut.size() == 0);
        assertTrue(systemErr.toString().contains("Usage: "));
    }


}
