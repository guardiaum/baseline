package com.johny.baseline.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author jms5
 *
 */
public class CSVUtil {
	
	private static final char DEFAULT_SEPARATOR = ',';
	private static final char DEFAULT_QUOTE = '"';
	
	/**
	 * 
	 * @param dir
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static OutputStreamWriter createCSVfile(String dir, String filename) throws IOException {

		String canonicalPath = dir + "/" + filename +".csv";
		File file = new File(canonicalPath);
		
		if(!file.exists()) {
			if(!Files.exists(Paths.get(dir)))
				Files.createDirectories(Paths.get(dir));
			
			file.createNewFile();
		}
		
		return new OutputStreamWriter(new FileOutputStream(canonicalPath), StandardCharsets.UTF_8);
	}
	
	/**
	 * 
	 * @param value
	 * @return
	 */
	public static String followCSVformat(String value) {
		String result = value;
		if(result.contains("\""))
			result = result.replace("\"", "\"\"");
		return result;
	}
	
	/**
	 * 
	 * @param w
	 * @param values
	 * @throws IOException
	 */
	public static void writeLine(Writer w, List<String> values) throws IOException{
		writeLine(w, values, DEFAULT_SEPARATOR, ' ');
	}
	
	/**
	 * 
	 * @param csvLine
	 * @return 
	 */
	public static List<String> readLine(String csvLine) {
		return readLine(csvLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
	}
	
	/**
	 * 
	 * @param w
	 * @param values
	 * @param separators
	 * @throws IOException
	 */
	public static void writeLine(Writer w, List<String> values, char separators) throws IOException{
		writeLine(w, values, separators, ' ');
	}
	
	/**
	 * 
	 * @param csvLine
	 * @param separators
	 * @return 
	 */
	public static List<String> readLine(String csvLine, char separators) {
		return readLine(csvLine, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
	}
	
	/**
	 * 
	 * @param w
	 * @param values
	 * @param separators
	 * @param customQuote
	 * @throws IOException
	 */
	public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException{
		
		boolean first = true;
		
		if(separators == ' ')
			separators = DEFAULT_SEPARATOR;
		
		StringBuilder sb = new StringBuilder();
		
		for (String value : values) {
			if(!first)
				sb.append(separators);
			
			if(customQuote == ' ')
				sb.append(followCSVformat(value));
			else{
				if(value==null){
					sb.append(customQuote).append(followCSVformat(" ")).append(customQuote);
				}else{
					sb.append(customQuote).append(followCSVformat(value)).append(customQuote);
				}
			}
			first = false;
		}
		sb.append("\n");
		
		//ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(sb.toString());
		
		w.append(sb.toString());
	}
	
	public static List<String> readLine(String csvLine, char separators, char customQuote) {
		
		List<String> result = new ArrayList<>();
		
		//if empty, return!
        if (csvLine == null && csvLine.isEmpty()) {
            return result;
        }
		
        if (customQuote == ' ') {
            customQuote = DEFAULT_QUOTE;
        }

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }
        
        StringBuffer curVal = new StringBuffer();
        
        boolean inQuotes = false;
        boolean startCollectChar = false;
        boolean doubleQuotesInColumn = false;

        char[] chars = csvLine.toCharArray();

        for (char ch : chars) {
        	
            if (inQuotes) {
                startCollectChar = true;
                if (ch == customQuote) {
                    inQuotes = false;
                    doubleQuotesInColumn = false;
                } else {

                    //Fixed : allow "" in custom quote enclosed
                    if (ch == '\"') {
                        if (!doubleQuotesInColumn) {
                            curVal.append(ch);
                            doubleQuotesInColumn = true;
                        }
                    } else {
                        curVal.append(ch);
                    }

                }
            } else {
                if (ch == customQuote) {

                    inQuotes = true;

                    //Fixed : allow "" in empty quote enclosed
                    if (chars[0] != '"' && customQuote == '\"') {
                        curVal.append('"');
                    }

                    //double quotes in column will hit this!
                    if (startCollectChar) {
                        curVal.append('"');
                    }

                } else if (ch == separators) {

                    result.add(curVal.toString());

                    curVal = new StringBuffer();
                    startCollectChar = false;

                } else if (ch == '\r') {
                    //ignore LF characters
                    continue;
                } else if (ch == '\n') {
                    //the end, break!
                    break;
                } else {
                    curVal.append(ch);
                }
            }
        }

        result.add(curVal.toString());

        return result;
	}
	
}
