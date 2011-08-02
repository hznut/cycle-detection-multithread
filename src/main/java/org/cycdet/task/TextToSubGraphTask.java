package org.cycdet.task;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.cycdet.parser.FileBlockReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextToSubGraphTask implements Callable<Boolean> {
    private final FileBlockReader reader;
    private final String outputFileName;

    // Threadsafe shared datastructure
    private final Set<Integer> prunedNodes;

    private static final Logger logger = LoggerFactory
            .getLogger(TextToSubGraphTask.class);

    public TextToSubGraphTask(FileBlockReader reader, String outputFileName,
            Set<Integer> prunedNodes) {
        this.outputFileName = outputFileName;
        this.prunedNodes = prunedNodes;
        this.reader = reader;
    }

    /**
     * This call method does following:
     * <p>
     * Reads each line of the block from text file.
     * </p>
     * <p>
     * Extracts node id and list of neighbor ids. Any neighbor node that is
     * detected as a pruned node is not included.
     * </p>
     * <p>
     * Puts the node as key and list of neighbors as value in map. Any node that
     * has one or less neighbors is pruned i.e. not put in the map.
     * </p>
     * <p>
     * Writes the map representing the subgraph to output file.
     * </p>
     */
    public Boolean call() throws Exception {
        logger.debug("Opened block {}", reader.getBlockId());
        // Read Block
        String str = reader.readBlock();
        reader.close();

        Map<Integer, LinkedList<Integer>> map = createPrunedSubGraph(str);

        // Write map
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                outputFileName));
        oos.writeObject(map);
        oos.close();
        logger.debug("Wrote map to file {}.", outputFileName);
        return true;
    }

    /**
     * <p>
     * Reads each line.
     * </p>
     * <p>
     * Extracts node id and list of neighbor ids. Any neighbor node that is
     * detected as a pruned node is not included.
     * </p>
     * <p>
     * Puts the node as key and list of neighbors as value in map. Any node that
     * has one or less neighbors is pruned i.e. not put in the map.
     * </p>
     * 
     * @param str
     * @return
     */
    private Map<Integer, LinkedList<Integer>> createPrunedSubGraph(String str) {
        Map<Integer, LinkedList<Integer>> map = 
                new HashMap<Integer, LinkedList<Integer>>();
        if (str != null) {
            str = str.trim();
            if (!str.trim().isEmpty()) {
                for (String line : str.split("\n")) {// Read each line in block
                    LinkedList<Integer> neighbors = new LinkedList<Integer>();
                    String[] arr = line.split("\t");
                    int node = Integer.parseInt(arr[0]);
                    for (String num : arr[1].split(",")) {
                        int neighbor = Integer.parseInt(num);
                        if (!prunedNodes.contains(neighbor)) {
                            neighbors.add(neighbor);
                        }
                    }
                    if (neighbors.size() <= 1) {
                        prunedNodes.add(node);
                        logger.debug("Pruned node {}", node);
                    } else {
                        map.put(node, neighbors);// populate map
                    }
                }
            }// if
        }
        return map;
    }

}
