package org.example;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.en.*;
import org.apache.lucene.analysis.miscellaneous.CapitalizationFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class customAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String s) {
        List<String> stopWords = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(Constants.STOPWORD_LIST_PATH));
            String currentLine = reader.readLine();
            while(currentLine!=null){
                stopWords.add(currentLine);
                currentLine=reader.readLine();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream tokenStream = new LowerCaseFilter(tokenizer);
        tokenStream = new StopFilter(tokenStream, StopFilter.makeStopSet(stopWords));
        tokenStream = new EnglishPossessiveFilter(tokenStream);
        tokenStream = new PorterStemFilter(tokenStream);
        tokenStream = new CapitalizationFilter(tokenStream);
        tokenStream = new EnglishMinimalStemFilter(tokenStream);
        tokenStream = new KStemFilter(tokenStream);
        return new TokenStreamComponents(tokenizer,tokenStream);
    }
}
