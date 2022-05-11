package cl.imfd.benchmark;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Iterator;

public class QueryIterator implements Iterable<String> {

	static String INPUT_FOLDER = "data/";

	static String[] INPUT_FILES = {
			INPUT_FOLDER + "I1_status500_Joined.tsv",
			INPUT_FOLDER + "I2_status500_Joined.tsv",
			INPUT_FOLDER + "I3_status500_Joined.tsv",
			INPUT_FOLDER + "I4_status500_Joined.tsv",
			INPUT_FOLDER + "I5_status500_Joined.tsv",
			INPUT_FOLDER + "I6_status500_Joined.tsv",
			INPUT_FOLDER + "I7_status500_Joined.tsv",
	};

	@Override
	public Iterator<String> iterator() {
		Iterator<String> it = new Iterator<String>() {
			private BufferedReader br = null;

			private int currentInput = 0;

            private String currentLine;

            private String currentQuery;

            @Override
            public boolean hasNext() {
            	try {
            		while (currentInput < INPUT_FILES.length) {
            			if (br == null) {
            				br = new BufferedReader(new FileReader(INPUT_FILES[currentInput]));
            				currentLine = br.readLine(); // skip header
            			}
            			if ((currentLine = br.readLine()) != null) {
            				String[] cols = currentLine.trim().split("\t");
            				if (cols.length != 4) {
            					throw new UnsupportedOperationException();
            				}
        					currentQuery = URLDecoder.decode(cols[0], "UTF-8").replaceAll("\n", " ");
            				return true;
            			} else {
            				currentInput++;
            				br = null;
            			}
            		}
            		return false;
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
            }

            @Override
            public String next() {
                return currentQuery;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
        return it;
	}
}
