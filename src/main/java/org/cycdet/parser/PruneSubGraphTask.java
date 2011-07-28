package org.cycdet.parser;

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

    public Boolean call() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
                inputFileName));
        map = (Map<Integer, LinkedList<Integer>>) ois.readObject();
        ois.close();
        logger.debug("Map read from {}", inputFileName);
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

        // Write map
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
                outputFileName, false));// Overwrite
        oos.writeObject(map);
        oos.close();
        logger.debug("Map written to {}", outputFileName);
        return true;
    }

}
