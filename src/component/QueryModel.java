package component;

import java.util.*;
import java.io.*;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class QueryModel {

    private Map<String, Map<String, List<Integer>>> invertedIndex; //HashMap<String,HashMap<String,ArrayList<Integer>>>
    private Map<String, Document> documentMap; //LinkedHashMap<docID, document>

    public QueryModel() {
        this.invertedIndex = new HashMap<>();
        this.documentMap = new LinkedHashMap<>();
    }

    public QueryModel(String savePath) {
        this();
        File saveFile = new File(savePath);
        if (saveFile.exists()&&saveFile.canRead()) {
            try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(saveFile)))) {
                String[] pathArray = (String[]) in.readObject();
                for(String path:pathArray) {
                    if (Files.exists(Paths.get(path))) {
                        addDocument(new Document(path));
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            saveToFile(savePath);
        }
    }

    public void saveToFile(String savePath) {
        String[] pathArray = this.documentMap.values().parallelStream().map(Document::getDocumentPath).toArray(String[]::new);
        try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(savePath)))) {
            out.writeObject(pathArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isQuery(String q) {
        String regex = "\\s*\"(\\s*\\w+\\s*)+\"\\s*(\\s+([aA][nN][dD]|[oO][rR])\\s+\"(\\s*\\w+\\s*)+\"\\s*)*";
        if (q.matches(regex)) return true;
        return false;
    }

    public Collection<Document> getDocuments() {
        return this.documentMap.values();
    }

    public Map<String, Map<String, List<Integer>>> getInvertedIndex() {
        return invertedIndex;
    }

    public void addDocument(Document document) {
        if (!containsDocument(document)) {
            String content = readDocumentContent(document);
            String[] indexTerms = extractTerms(content);
            String docID = getUnusedDocID();
            addToIndex(docID, indexTerms);
            this.documentMap.put(docID, document);
        }
    }

    public void removeDocument(Document document) {
        String docID = getIDByDocument(document);
        if (docID!=null) {
            this.invertedIndex.entrySet().removeIf(e->
                    e.getValue().containsKey(docID)&&e.getValue().size()==1);
            this.invertedIndex.replaceAll((k,v)-> {v.remove(docID);
            return v;});
            this.documentMap.remove(docID);
        }
    }

    public void removeAllDocuments() {
        this.invertedIndex.clear();
        this.documentMap.clear();
    }

    public void rebuildIndex() {
        this.invertedIndex.clear();
        for (Map.Entry<String, Document> e: this.documentMap.entrySet()) {
            Document document = e.getValue();
            String docID = e.getKey();
            String content = readDocumentContent(document);
            String[] indexTerms = extractTerms(content);
            addToIndex(docID, indexTerms);
        }
    }

    public Collection<Document> query(String query) {
        Set<Document> result = new LinkedHashSet<>();

        //interpret query string
        String regex = "\\s+\"|\"\\s+";
        String[] queryTokens = query.split(regex);
        for (int i=0; i<queryTokens.length;i++) {
            queryTokens[i] = queryTokens[i].replaceAll("^\\W+|\\W+$", "");
        }

        //execute the interpreted query
        result.addAll(booleanSearchDocuments(queryTokens[0]));
        for (int j=0;j<(queryTokens.length-2);j+=2) {
            if (queryTokens[j+1].equalsIgnoreCase("or")) {
                result.addAll(booleanSearchDocuments(queryTokens[j+2]));
            }
            if (queryTokens[j+1].equalsIgnoreCase("and")) {
                result.retainAll(booleanSearchDocuments(queryTokens[j+2]));
            }
        }

        return  result;
    }

    private String getUnusedDocID() {
        int i = 0;
        while(this.documentMap.containsKey(Integer.toString(i))) {
            i++;
        }
        return Integer.toString(i);
    }

    private Set<Document> booleanSearchDocuments(String phase) {
        Set<Document> result = new LinkedHashSet<>();
        String[] terms = extractTerms(phase);

        int i = 0;
        Map<String,List<Integer>> resultDocs;
        for (resultDocs = this.invertedIndex.getOrDefault(terms[i], new TreeMap<>()); resultDocs.size()>0&&i<(terms.length-1);) {
            i++;
            Map<String,List<Integer>> newResultDocs = new TreeMap<>();
            for (Map.Entry<String, List<Integer>> resultDoc : resultDocs.entrySet()) {
                Map<String,List<Integer>>nextDocs = this.invertedIndex.get(terms[i]);
                if(nextDocs!=null) {
                    String docID = resultDoc.getKey();
                    List<Integer>nextDocPosts = nextDocs.get(docID);
                    if (nextDocPosts!=null) {
                        List<Integer>resultDocPosts = resultDoc.getValue();
                        List<Integer> newResultDocPosts = intersectWithOffset(resultDocPosts,nextDocPosts,1);
                        if (newResultDocPosts.size()>0) {
                            newResultDocs.put(docID, newResultDocPosts);
                        }
                    }
                }
            }
            resultDocs = newResultDocs;
        }

        resultDocs.forEach((k,v)-> result.add(getDocumentByID(k)));

        return result;
    }

    private Boolean containsDocument(Document document) {
        return this.documentMap.entrySet().parallelStream().anyMatch(e->e.getValue().equals(document));
    }

    private Document getDocumentByID(String docID) {
        return this.documentMap.get(docID);
    }

    private String getIDByDocument(Document document) {
        return this.documentMap.entrySet().parallelStream().filter(e->e.getValue().equals(document)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    private List<Integer> intersectWithOffset(List<Integer> postingList1, List<Integer> postingList2, int offset) {
        return postingList2.parallelStream().filter(post-> postingList1.contains(post-offset)).collect(Collectors.toList());
    }

    private void addToIndex(String documentID, String[] terms) {
        for (int i=0; i<terms.length; i++) {
            Map<String, List<Integer>> docIDsAndPosts = this.invertedIndex.getOrDefault(terms[i],new LinkedHashMap<>());
            List<Integer> pos=docIDsAndPosts.getOrDefault(documentID, new ArrayList<>());
            pos.add(i);
            docIDsAndPosts.put(documentID, pos);
            this.invertedIndex.put(terms[i], docIDsAndPosts);
        }
    }


    private String[] extractTerms(String content) {
        //tokenization
        String[] terms = NLP.toTokens(content);

        for (int i=0; i<terms.length; i++) {
            //normalization
            terms[i] = NLP.toNormalized(terms[i]);

            //stemming
            terms[i] = NLP.toStemmed(terms[i]);
        }
        return terms;
    }

    private static String readDocumentContent(Document document) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(document.getDocumentPath()))){
            for(String line; (line = br.readLine()) != null; ) {
                    sb.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}