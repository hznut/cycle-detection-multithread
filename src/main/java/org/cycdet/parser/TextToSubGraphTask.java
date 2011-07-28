package org.cycdet.parser;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextToSubGraphTask implements Callable<Boolean> {
    private final int STARTING_LINE_NO;
    private LineNumberReader reader;
    private final int LINES_TO_READ;
    private final String outputFileName;
    private final Set<Integer> prunedNodes;
    private final String inputFileName;
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSubGraphTask.class);

    public TextToSubGraphTask(String fileName, int startingLineNo, int linesToRead,
            String outputFileName, Set<Integer> prunedNodes)
            throws FileNotFoundException, IOException, EOFException {
        this.STARTING_LINE_NO = startingLineNo;
        this.LINES_TO_READ = linesToRead;
        this.outputFileName = outputFileName;
        this.prunedNodes = prunedNodes;
        this.inputFileName = fileName;
    }

    public Boolean call() throws Exception {
        Map<Integer, LinkedList<Integer>> map =
                new HashMap<Integer, LinkedList<Integer>>();
        reader = new LineNumberReader(new FileReader(inputFileName));
        reader.setLineNumber(STARTING_LINE_NO);
        logger.debug("Opened {}", inputFileName);
        for (int i = 0; i <= LINES_TO_READ; i++) {
            String line = reader.readLine();
            StringTokenizer tokenizer1 = new StringTokenizer(line, "\t");
            int node = Integer.parseInt(tokenizer1.nextToken());
            StringTokenizer tokenizer2 = new StringTokenizer(
                    tokenizer1.nextToken(), ",");
            LinkedList<Integer> neighbors = new LinkedList<Integer>();
            while (tokenizer2.hasMoreTokens()) {
                int neighbor = Integer.parseInt(tokenizer2.nextToken());
                if (!prunedNodes.contains(neighbor)) {
                    neighbors.add(neighbor);
                }
            }
            if (neighbors.size() <= 1) {
                prunedNodes.add(node);
                logger.debug("Pruned node {}", node);
            } else {
                map.put(node, neighbors);
            }
        }
        reader.close();

        // Write map
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                outputFileName));
        oos.writeObject(map);
        oos.close();
        logger.debug("Wrote map to file {}.", outputFileName);
        return true;
    }

}
