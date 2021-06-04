package tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

public class CSVTools {
    public static HashMap<String, String> readCSVToHashMap(String file){
        HashMap<String,String> map = new HashMap();
        try {
            File myObj = new File(file);
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                String[] s = data.split(",");
                if (s.length == 2){
                    //System.out.println(s[0]+":" + " ::: "+ IRITools.removeQuotationMarks(s[1]));
                    map.put(s[0]+":",IRITools.removeQuotationMarks(s[1]));
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            e.printStackTrace();
        }
        return map;
    }
}
