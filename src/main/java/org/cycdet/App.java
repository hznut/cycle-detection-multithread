package org.cycdet;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.cycdet.parser.FileBlockReader;
import org.cycdet.task.PruneSubGraphTask;
import org.cycdet.task.TextToSubGraphTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cycle Detection in undirected graph.
 * 
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException,
            InterruptedException, ClassNotFoundException {
        final String inputFileName = args[0];
        final String outputDirName = args[1];

        // Ideally set from property
        int desiredNumTasks = Integer.parseInt(args[2]);
        long totalNodes = getTotalLines(inputFileName);
        List<FileBlockReader> lstFileBlockReaders = FileBlockReader
                .generateBlockReaders(inputFileName, desiredNumTasks);
        int numTasks = lstFileBlockReaders.size();
        // Ideally set from property
        final int MIN_NUM_THREADS = (numTasks < 5) ? numTasks : 5;
        /*
         * corePoolSize = MIN_NUM_THREADS. Irrespective of number of nodes pruned (i.e.
         * even if only one node is pruned) we have to re-process all subgraphs.
         * maximumPoolSize = numTasks. Max new read tasks can only be these many.
         * keepAliveTime = 60 secs.
         * workQueue = LinkedBlockingQueue(2*numTasks)
         *  
         */
        ExecutorService executor = new ThreadPoolExecutor(MIN_NUM_THREADS,
                numTasks, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(2 * numTasks));
        Set<Integer> prunedNodes = Collections.synchronizedSet(
                new HashSet<Integer>());

        /*
         * The following also demonstrates parallel reading of different file
         * blocks.
         */
        Collection<TextToSubGraphTask> textToSubGraphTasks = new ArrayList<TextToSubGraphTask>(
                numTasks);
        for(FileBlockReader fileBlockReader : lstFileBlockReaders) {
            textToSubGraphTasks.add(new TextToSubGraphTask(fileBlockReader,
                    outputDirName + "map" + fileBlockReader.getBlockId(),
                    prunedNodes));
        }
        logger.debug("TextToSubGraphTask created.");
        executor.invokeAll(textToSubGraphTasks);
        logger.debug("TextToSubGraphTask completed.");

        int lastPrunedNodesCount = 0;
        if (prunedNodes.size() > lastPrunedNodesCount) {
            Collection<PruneSubGraphTask> pruneSubGraphTasks = new ArrayList<PruneSubGraphTask>(
                    numTasks);
            int iteration = 2;
            String fileName;
            do {
                pruneSubGraphTasks.clear();
                lastPrunedNodesCount = prunedNodes.size();
                for (int i = 0; i < numTasks; i++) {
                    fileName = outputDirName + "map" + i;
                    pruneSubGraphTasks.add(new PruneSubGraphTask(fileName,
                            fileName, prunedNodes));
                }
                logger.debug("PruneSubGraphTasks created for iteration {}",
                        iteration);
                executor.invokeAll(pruneSubGraphTasks);
                logger.debug("PruneSubGraphTasks completed for iteration {}",
                        iteration);
                iteration++;
            } while (prunedNodes.size() > lastPrunedNodesCount);
        }

        if (prunedNodes.size() == totalNodes) {// All nodes pruned => No Cycle
            logger.info("NO CYCLES detected.");
        } else {
            // Print remaining graph that has cycles and knots.
            // Write hashmaps as text
            String inFileName;
            String outFileName = outputDirName + "prunedgraph.dat";
            logger.info("One or more CYCLES detected. Check {}", outFileName);
            for (int i = 0; i < numTasks; i++) {
                inFileName = outputDirName + "map" + i;
                readMapWriteText(inFileName, outFileName);
            }
        }
    }

    private static void readMapWriteText(String inFileName,
            String outFileName) throws FileNotFoundException, IOException, 
            ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                inFileName));
        Map<Integer, LinkedList<Integer>> map = (Map<Integer, LinkedList<Integer>>) ois
                .readObject();
        ois.close();
        if (map.size() > 0) {
            PrintWriter out = new PrintWriter(new BufferedWriter(
                    new FileWriter(outFileName, true)));
            for (Map.Entry<Integer, LinkedList<Integer>> entry : map.entrySet()) {
                StringBuilder sb = new StringBuilder().append(entry.getKey())
                        .append("\t");
                for (Integer neighbor : entry.getValue()) {
                    sb.append(neighbor).append(',');
                }
                out.println(sb.toString());
            }
            out.flush();
            out.close();
        }
        logger.debug("Map read from {} and text written to {}", inFileName,
                outFileName);
    }

    private static long getTotalLines(String fileName) throws IOException {
        LineNumberReader reader = new LineNumberReader(new FileReader(fileName));
        reader.skip(Long.MAX_VALUE);
        long totalLines = reader.getLineNumber();
        reader.close();
        return totalLines;
    }
}
