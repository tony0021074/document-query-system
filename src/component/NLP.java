package component;

/*
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
*/

public class NLP {

    public static String[] toTokens(String string) {
        String[] termList;
        termList = string.split("\\s+");
        for (int i=0; i<termList.length;i++) {
            termList[i] = termList[i].replaceAll("^\\W+|\\W+$", "");
        }
        return termList;
    }

    public static String toNormalized(String term) {
        term = term.replaceAll("\\W+","");
        return term.toLowerCase();
    }

    public static String toStemmed(String term) {
        Stemmer s =  new Stemmer();
        s.add(term.toCharArray(), term.length());
        s.stem();
        return s.toString();
    }

    //private static final String STOPPATH = "english.stop";

        /*public static Boolean isStopWord(String term) {
        try(BufferedReader br = new BufferedReader(new FileReader(STOPPATH))) {
            for(String line; (line = br.readLine()) != null; ) {
                if (term.equalsIgnoreCase(line)) return true;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }*/
}