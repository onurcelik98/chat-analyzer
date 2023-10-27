import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishTokenizer;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Onur Çelik (with love ♥️)
 */
public class ChatAnalysisApp {
    private static final String INPUT_FILE_PATH = "src/main/resources/chat.txt";
    private static final String OUTPUT_FILE_PATH = "src/main/resources/output.txt";
    private static final String FINAL_OUTPUT_FILE_PATH = "src/main/resources/final_output.txt";
    private static final String LOOKUP_DIRECTORY_PATH = "src/main/resources/data/normalization";
    private static final String LANGUAGE_MODEL_FILE_PATH = "src/main/resources/data/lm/lm.2gram.slm";
    private static final int N = 500;

    public static void main(String[] args) throws IOException {
        analyzeInputAndWriteDownMaxOccurrencesUpToN(INPUT_FILE_PATH, OUTPUT_FILE_PATH, N);
        prepareResultForWordCloud(OUTPUT_FILE_PATH, FINAL_OUTPUT_FILE_PATH);
    }

    private static void analyzeInputAndWriteDownMaxOccurrencesUpToN(String inputFilePath, String outputFilePath, int N) throws IOException {
        Path lookupRoot = Paths.get(LOOKUP_DIRECTORY_PATH);
        Path lmFile = Paths.get(LANGUAGE_MODEL_FILE_PATH);
        TurkishMorphology morphology = TurkishMorphology.builder()
                .setLexicon(RootLexicon.getDefault())
                .useInformalAnalysis()
                .build();
        TurkishSentenceNormalizer normalizer = new
                TurkishSentenceNormalizer(morphology, lookupRoot, lmFile);
        Pattern emojiPattern = Pattern.compile("([\\p{InEmoticons}]|♥️|[^\\p{InEmoticons}]+)");

        Map<String, Integer> wordOccurrences = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                String sentence = line;

                // remove the timestamp and the sender name
                String[] parts = line.split(": ", 2);
                if (parts.length > 1) {
                    sentence = parts[1];
                }

                if (sentence.equals("<Medya dahil edilmedi>")) {
                    sentence = "\uD83D\uDCF7";
                }

                if (sentence.startsWith("http") || sentence.startsWith("www")) {
                    sentence = "\uD83D\uDD17";
                }


                String normalizedSentence = normalizer.normalize(sentence);

                TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
                List<Token> tokens = tokenizer.tokenize(normalizedSentence);
                List<String> words = new ArrayList<>();

                for (Token t : tokens) {
                    if (t.getType() == Token.Type.Punctuation) {
                        continue;
                    }
                    String text = t.getText();
                    Matcher matcher = emojiPattern.matcher(text);
                    while (matcher.find()) {
                        String element = matcher.group();
                        element = removeMultipleOccurrencesOfLastCharacter(element);
                        words.add(element.trim());
                    }
                }

                for (String w : words) {
                    if (wordOccurrences.containsKey(w)) {
                        wordOccurrences.put(w, wordOccurrences.get(w) + 1);
                    } else {
                        wordOccurrences.put(w, 1);
                    }
                }
            }
        }

        PriorityQueue<Map.Entry<String, Integer>> maxNEntries = new PriorityQueue<>(
                N,
                Comparator.comparingInt(Map.Entry::getValue)
        );

        for (Map.Entry<String, Integer> entry : wordOccurrences.entrySet()) {
            maxNEntries.add(entry);
            if (maxNEntries.size() > N) {
                maxNEntries.poll();
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            PriorityQueue<Map.Entry<String, Integer>> reversedPriorityQueue = new PriorityQueue<>(
                    N,
                    (e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())
            );
            reversedPriorityQueue.addAll(maxNEntries);

            while (!reversedPriorityQueue.isEmpty()) {
                writer.write(reversedPriorityQueue.poll().toString());
                writer.newLine();
            }
        }
    }

    private static String removeMultipleOccurrencesOfLastCharacter(String input) {
        if (input == null || input.length() <= 1) {
            return input;
        }

        StringBuilder builder = new StringBuilder();
        boolean jobDone = false;

        for (int i = input.length() - 1; i >= 0; i--) {
            if (!jobDone && i!=0 && input.charAt(i) == input.charAt(i-1)) {
                continue;
            } else {
                builder.append(input.charAt(i));
                jobDone = true;
            }
        }
        return builder.reverse().toString();
    }

    @Deprecated
    private static void countIncompliantSubstrings(String inputFilePath, String outputFilePath, String substring) throws IOException {
        int substringMatchesCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                substringMatchesCount += Pattern.compile(substring)
                        .matcher(line)
                        .results()
                        .count();
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            writer.write(substring + "=" + substringMatchesCount);
        }
    }

    @Deprecated
    private static String[] decomposeToRepeatedSubstrings(String string, String substring) {
        int len1 = string.length();
        int len2 = substring.length();

        if (len1 == 0 || len2 == 0 || len1 % len2 != 0) {
            return new String[]{string};
        }

        int repetitionFactor = len1 / len2;
        String repeated = substring.repeat(repetitionFactor);

        if (string.equals(repeated)) {
            String[] result = new String[repetitionFactor];

            for (int i = 0; i < repetitionFactor; i++) {
                result[i] = substring;
            }

            return result;
        }

        return new String[]{string};
    }

    private static void prepareResultForWordCloud(String resultFilePath, String finalOutputFileName) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(resultFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(finalOutputFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length <= 1) {
                    continue;
                } else {
                    int repeatFactor;
                    try {
                        repeatFactor = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        repeatFactor = 27;
                        System.out.println("REPEAT FACTOR CANNOT BE READ");
                    }
                    writer.write((parts[0] + " ").repeat(repeatFactor));
                }
            }
        }
    }
}