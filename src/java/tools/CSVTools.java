package tools;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * A couple of CSV tools
 * @author Noé Saffaf
 */
public class CSVTools {

    public static Map<String, String> readCSVtoMap(String resource) {
        Map<String, String> map = new HashMap();
        InputStream in = CSVTools.class.getClassLoader().getResourceAsStream(resource);
        Scanner myReader = new Scanner(in);
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            String[] s = data.split(",");
            if (s.length == 2) {
                map.put(s[0] + ":", IRITools.removeQuotationMarks(s[1]));
            }
        }
        myReader.close();
        return map;
    }

}