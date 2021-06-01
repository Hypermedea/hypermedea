package tools;

public class IRITools {
    public static String getSuffixIri(String iri, boolean replaceToLowerCase)
    {
        String suffix = iri.replaceAll("<","").replaceAll(">","");
        if(suffix.contains("/")){
            suffix = suffix.substring(suffix.lastIndexOf("/")+1,suffix.length());
        }
        if(suffix.contains("#"))
        {
            suffix = suffix.substring(suffix.lastIndexOf("#")+1,suffix.length());
        }
        if(suffix.contains(":"))
        {
            suffix = suffix.substring(suffix.lastIndexOf(":")+1,suffix.length());
        }

        if (replaceToLowerCase){
            return firstCharToLowerCase(suffix);
        } else {
            return suffix;
        }
    }

    public static String[] cutSuffixIRI(String iri, boolean replaceToLowerCase)
    {
        String prefix;
        String suffix;
        int indexSeparation = 0;

        String s = iri.replaceAll("<","").replaceAll(">","");

        if(s.contains("/") & s.lastIndexOf("/") > indexSeparation){
            indexSeparation = s.lastIndexOf("/");
        }
        if(s.contains("#") & s.lastIndexOf("#") > indexSeparation){
            indexSeparation = s.lastIndexOf("#");
        }
        if(s.contains(":") & s.lastIndexOf(":") > indexSeparation){
            indexSeparation = s.lastIndexOf(":");
        }

        if (indexSeparation > 0) {
            prefix = s.substring(0,indexSeparation+1);
            suffix = s.substring(indexSeparation+1,s.length());
        } else {
            prefix = s.substring(0,indexSeparation);
            suffix = s.substring(indexSeparation,s.length());
        }

        if (replaceToLowerCase){
            return new String[]{prefix,firstCharToLowerCase(suffix)};
        } else {
            return new String[]{prefix,suffix};
        }
    }

    public static String firstCharToLowerCase(String str) {

        if(str == null || str.length() == 0)
            return "";

        if(str.length() == 1)
            return str.toLowerCase();

        char[] chArr = str.toCharArray();
        chArr[0] = Character.toLowerCase(chArr[0]);

        return new String(chArr);
    }

    //deprecated
    public static String getRemoveLanguageLabel(String s) {
        int indexSeparation = 0;
        if(s.contains("@en")){
            indexSeparation = s.indexOf("@en");
        } else if (s.contains("@")){
            indexSeparation = s.indexOf("@");
        } else {
            return s;
        }
        return s.substring(0,indexSeparation);
    }

    public static String addWrapperUri(String s){
        return "<" + s + ">";
    }

    public static String removeWrapperUri(String s){
        char[] c = s.toCharArray();
        if (c[0] == '<' & c[c.length-1] == '>'){
            s = s.substring(1,s.length()-1);
        }
        return s;
    }

    public static String getNameByMatchingPrefix(String namespace, String prefix){
        if (namespace.startsWith(prefix)){
            return namespace.substring(prefix.length(),namespace.length());
        } else {
            return "";
        }
    }
}
