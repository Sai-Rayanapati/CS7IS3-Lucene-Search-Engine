package org.example;

import java.io.*;

import java.nio.file.Paths;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class CreateIndex {
    public static void main(String[] args) throws IOException, ParseException {
        String analyzerString = null;
        String similarityString;
        Analyzer analyzer = null;
        if(args.length>=2) {
            if(args[0].equalsIgnoreCase(Constants.ENGLISH)||args[0].equalsIgnoreCase(Constants.STANDARD)||args[0].equalsIgnoreCase(Constants.CUSTOM)){
                analyzerString = args[0];
                System.out.println("Indexing using: "+args[0]+" Analyzer");
            }
            else {
                analyzerString = Constants.ENGLISH;
                System.out.println("Indexing using: "+Constants.ENGLISH+" Analyzer");
            }
            if(args[1].equalsIgnoreCase(Constants.VSM)||args[1].equalsIgnoreCase(Constants.BM25)){
                similarityString = args[1];
                System.out.println("Searching using: "+args[1]+" Similarity");
            }
            else {
                similarityString = Constants.BM25;
                System.out.println("Searching using: "+Constants.BM25+" Similarity");
            }
        }
        else{
            analyzerString = Constants.ENGLISH;
            System.out.println("Indexing using: "+Constants.ENGLISH+" Analyzer");
            similarityString = Constants.BM25;
            System.out.println("Searching using: "+Constants.BM25+" Similarity");
        }

        if(analyzerString.equalsIgnoreCase(Constants.ENGLISH)) {
            analyzer = new EnglishAnalyzer();
        }
        else if(analyzerString.equalsIgnoreCase(Constants.STANDARD)){
            analyzer = new StandardAnalyzer();
        }
        else if(analyzerString.equalsIgnoreCase(Constants.CUSTOM)){
            analyzer = new customAnalyzer();
        }

        // Index Cranfield Collection and add documents
        Directory directory = FSDirectory.open(Paths.get(Constants.INDEX_DIRECTORY));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        List<Document> cranfieldDocuments = indexCranfiedCollection();
        iwriter.addDocuments(cranfieldDocuments);
        iwriter.close();
        directory.close();

        //Query Cranfield collection
        searcher(analyzer,similarityString);
    }

    private static void searcher (Analyzer analyzer, String similarity) throws IOException, ParseException {
        IndexReader ireader = DirectoryReader.open(FSDirectory.open(Paths.get(Constants.INDEX_DIRECTORY)));
        IndexSearcher isearcher = new IndexSearcher(ireader);
        if(similarity.equalsIgnoreCase(Constants.VSM)) {
            isearcher.setSimilarity(new ClassicSimilarity());
        }
        else if(similarity.equalsIgnoreCase(Constants.BM25)){
            isearcher.setSimilarity(new BM25Similarity());
        }
        String[] fieldsToSearch = {Constants.TITLE, Constants.WORDS};
        HashMap<String, Float> weights = new HashMap<>();
        weights.put(Constants.TITLE, 0.4F);
        weights.put(Constants.WORDS, 0.6F);
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fieldsToSearch, analyzer, weights);
        List<String> queries = readQueryList(Constants.CRAN_QUERY_PATH);
        BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.RESULTS_PATH));
        int i=1;
        for(String stringQuery : queries){
            String queryString = QueryParser.escape(stringQuery);
            Query query = parser.parse(queryString);
            TopDocs topDocs = isearcher.search(query,100);
            //System.out.println("Q"+i+" -"+stringQuery);
            int j=1;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs){
                Document document = isearcher.doc(scoreDoc.doc);
                writer.write(i + " 0"+ document.get(Constants.ID) + j+ " " +scoreDoc.score + " STANDARD");
                writer.newLine();
                j++;
                //System.out.println("DocID: " + document.get(Constants.ID) + ", Score: " + scoreDoc.score);
            }
            i++;
        }
        writer.close();
    }
    private static List<Document> indexCranfiedCollection() throws IOException {
        List<Document> cranfieldDocuments= new ArrayList<>();
        String cranString = fileToString(Constants.CRAN_PATH);
        String cranStringSplit[] = cranString.split(Constants.ID);
        for (int i=1;i<cranStringSplit.length;i++){
            Document cranFieldDoc = createIndexDocuments(cranStringSplit[i]);
            cranfieldDocuments.add(cranFieldDoc);
        }
        return cranfieldDocuments;
    }
    private static List<String> readQueryList(String path) throws IOException {
        List<String> queryList = new ArrayList<>();
        String queryFileString = fileToString(path);
        String[] queries = queryFileString.split(Constants.ID);
        for(int i=0;i< queries.length;i++){
            // i=0 has a blank line
            if(i!=0){
                queryList.add(queries[i].split(Constants.WORDS)[1]);
            }
        }
        return queryList;
    }
    private static Document createIndexDocuments(String docContent){
        Document document =new Document();
        String parts[] = docContent.split(Constants.TITLE);
        document.add(new StringField(Constants.ID,parts[0],Field.Store.YES));

        parts = parts[1].split(Constants.AUTHOR);
        document.add(new TextField(Constants.TITLE,parts[0],Field.Store.YES));

        parts = parts[1].split(Constants.BIBLIOGRAPHY);
        document.add(new TextField(Constants.AUTHOR,parts[0],Field.Store.YES));

        parts = parts[1].split(Constants.WORDS);
        document.add(new TextField(Constants.BIBLIOGRAPHY,parts[0],Field.Store.YES));

        document.add(new TextField(Constants.WORDS,parts[1],Field.Store.YES));

        return document;

    }
    private static String fileToString(String path) throws IOException {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            StringBuilder builder = new StringBuilder();
            String currentLine = br.readLine();
            while (currentLine!=null){
                builder.append(currentLine+" ");
                currentLine=br.readLine();
            }
            String fileString = builder.toString();
            return fileString;
        } catch (FileNotFoundException e) {
            return null;
        }
    }
}

