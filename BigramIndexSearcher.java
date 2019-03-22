import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BigramIndexSearcher {
    private final String REMOVE_STOP_CHARS = "[\\s。、・゠＝．！？っッー゛゜「」『』（）〔〕［］｛｝｟｠〈〉《》【】〖〗〘〙〚〛〆〜…‥ヶ•◦﹅﹆※＊〽〓♪♫♬♩〇〒〶〠〄ⓍⓁⓎ\"～$@!%*?&#^-_. +']";

    private final String INDEXED_FILE = "output.idx";

    private String datasetFile;

    private String datasetEncoding;

    /**
     * Accepts the dataset file and encoding of the dataset.
     * @param datasetFile
     * @param datasetEncoding
     */
    public BigramIndexSearcher (String datasetFile, String datasetEncoding) {
        this.datasetFile = datasetFile;
        this.datasetEncoding = datasetEncoding;
    }


    /**
     * Performs indexing on the dataset file and generates output.idx file of indexes.
     */
    public void fileIndexing() {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            InputStream stream = BigramIndexSearcher.class.getResourceAsStream(datasetFile);
            InputStreamReader input = new InputStreamReader(stream, datasetEncoding);
            reader = new BufferedReader(input);

            int lineCounter = 1;
            SortedMap<Character, SortedSet<Integer>> unigramIndex = new TreeMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                // get columns and normalize them
                String[] columns = line.split("\\s*,\\s*");
                String normalizedCols = Stream.of(Arrays.copyOfRange(columns, 6, 9))
                        .map(col->col.replaceAll(REMOVE_STOP_CHARS, ""))
                        .collect(Collectors.joining());
                // get unigrams sets
                for (int i = 0; i < normalizedCols.length(); i++) {
                    char ch = normalizedCols.charAt(i);

                    SortedSet<Integer> indexSet;
                    if (unigramIndex.containsKey(ch)) {
                        indexSet = unigramIndex.get(ch);
                        unigramIndex.put(ch, indexSet);
                    } else {
                        indexSet = new TreeSet<>();
                        unigramIndex.put(ch, indexSet);
                    }
                    indexSet.add(lineCounter);
                }
                lineCounter++;
            }

            // save unigrams in as an indexed file
            OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream("./" + INDEXED_FILE), StandardCharsets.UTF_8);
            writer = new BufferedWriter(output);

            for (Map.Entry entry : unigramIndex.entrySet()) {
                writer.write(String.format("%s ", entry.getKey()));
                String indexes = ((SortedSet<?>) entry.getValue()).stream().map(String::valueOf).collect(Collectors.joining(" "));
                writer.write(indexes);
                writer.newLine();
            }
            writer.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bigramSearching();
    }

    /**
     * Function to search the input token and display results of bigrams occurrences.
     * It even displays entries that span through multiple rows.
     */
    public void bigramSearching() {
        BufferedReader reader = null;
        Scanner scanner = null;
        try {
            scanner = new Scanner(System.in);
            String again;

            int i = 1;
            do {
                InputStream stream = BigramIndexSearcher.class.getResourceAsStream(INDEXED_FILE);
                InputStreamReader input = new InputStreamReader(stream, StandardCharsets.UTF_8);
                reader = new BufferedReader(input);

                // accept input from the user
                System.out.printf("Input %d: ", i++);
                String token = scanner.nextLine();
                token = token.replaceAll("\\s", "");

                // create a key list from input token
                List<Character> keyList = new ArrayList<>();
                for (int k = 0; k < token.length(); k++) {
                    keyList.add(token.charAt(k));
                }

                // collect indexed unigrams from key list
                String line;
                Map<Character, List<Integer>> unigrams = new HashMap<>();
                while ((line = reader.readLine()) != null) {
                    char key = line.charAt(0);

                    if (keyList.contains(key)) {
                        line = line.substring(2);
                        List<Integer> indexList = Stream.of(line.split("\\s"))
                                .filter(index -> !index.isEmpty())
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        unigrams.put(key, indexList);
                    }
                }

                // find bigrams with their respective occurrences in the same rows
                List<Integer> previous = null;
                Set<Integer> occurrences = new HashSet<>();
                for (Character key : keyList) {
                    List<Integer> current = unigrams.get(key);
                    if (previous == null) {
                        previous = current;
                    } else {
                        List<Integer> common = previous.stream()
                                .filter(current::contains)
                                .collect(Collectors.toList());
                        previous = current;
                        occurrences.addAll(new HashSet<>(common));
                    }
                }
                stream.close();
                input.close();

                stream = BigramIndexSearcher.class.getResourceAsStream(datasetFile);
                input = new InputStreamReader(stream, datasetEncoding);
                reader = new BufferedReader(input);

                // Normalize columns and display output
                System.out.println("\nOutput:");
                int lineCounter = 1;
                String startRowField = "";
                while ((line = reader.readLine()) != null) {
                    String[] columns = line.split("\\s*,\\s*");
                    String rowField = columns[2];
                    String[] normalizedCols = Arrays.copyOfRange(columns, 6, 9);
                    String toPrint = rowField + "," + String.join(",", normalizedCols);

                    if (occurrences.contains(lineCounter)) {
                        startRowField = rowField;
                        System.out.println(toPrint);
                    } else if (startRowField.equals(rowField)) {
                        System.out.println(toPrint);
                    }
                    lineCounter++;
                }

                System.out.print("\ncontinue? (y/n): ");
                again = scanner.nextLine();
            } while (again.equals("y"));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (scanner != null) {
                    scanner.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Performs a test on the required dataset file "KEN_ALL.CSV"
     * @param args
     */
    public static void main(String...args) {
        BigramIndexSearcher searcher = new BigramIndexSearcher("KEN_ALL.CSV", "shift_jis");
        searcher.fileIndexing();
    }
}
