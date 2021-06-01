package tools;

public class BeliefsSyntaxTools {
    public static String addPrefixAbox(String predicateName){
        return "a_"+predicateName;
    }

    public static String addPrefixTbox(String predicateName){
        return "t_"+predicateName;
    }

    public static String removePrefix(String predicateName){
        return predicateName.substring(2,predicateName.length());
    }
}
