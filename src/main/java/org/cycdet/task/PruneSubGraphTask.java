package org.cycdet.task;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PruneSubGraphTask implements Callable<Boolean> {
    private final String outputFileName;

    // Threadsafe shared datastructure
    private final Set<Integer> prunedNodes;

    private Map<Integer, LinkedList<Integer>> map;
    private final String inputFileName;
    private static final Logger logger = LoggerFactory
            .getLogger(TextToSubGraphTask.class);

    public PruneSubGraphTask(String fileName, String outputFileName,
            Set<Integer> prunedNodes) throws FileNotFoundException,
            IOException, ClassNotFoundException {
        this.outputFileName = outputFileName;
        this.prunedNodes = prunedNodes;
        this.inputFileName = fileName;
    }

    /**
     * This call method does following:
     * <p>
     * Reads map representing the subgraph from file. Prunes nodes (if any
     * eligible) from subgraph. Writes the map representing the subgraph back to
     * file.
     * </p>
     */
    public Boolean call() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                inputFileName));
        map = (Map<Integer, LinkedList<Integer>>) ois.readObject();
        ois.close();
        logger.debug("Map read from {}", inputFileName);

        // Prune the subgraph
        map = pruneSubgraph(map);

        // Write map
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                outputFileName, false));// Overwrite
        oos.writeObject(map);
        oos.close();
        logger.debug("Map written to {}", outputFileName);
        return true;
    }

    /**
     * Pruning logic:
     * <p>
     * For each key(node) go through list of neighbor ids. Any neighbor node
     * that is detected as a pruned node is removed from list.
     * </p>
     * <p>
     * Any node that has one or less neighbors is pruned i.e. that key and it's
     * value are removed from the map.
     * </p>
     * 
     * @param map
     * @return
     */
    private Map<Integer, LinkedList<Integer>> pruneSubgraph(
            Map<Integer, LinkedList<Integer>> map) {
        for (Integer node : map.keySet()) {
            LinkedList<Integer> neighbors = map.get(node);
            for (Integer neighbor : neighbors) {
                if (prunedNodes.contains(neighbor)) {
                    neighbors.remove(neighbor);
                }
            }
            
            if (neighbors.size() <= 1) {
                prunedNodes.add(node);
                map.remove(node);
                logger.debug("Node {} pruned", node);
            }
        }
        return map;
    }

}
