package org.cycdet.parser;

import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Reader for reading a logical file block marked by begin and end positions.
 * The file is assumed to be having characters.
 * 
 * @author hvijay
 * 
 */
public class FileBlockReader {
    private FileReader fRdr;
    private final int begin;
    private final int end;
    private final int blockId;
    private final String fileName;

    /**
     * Creates a new FileBlockReader given the parameters.
     * 
     * @param fileName
     * @param begin
     *            The character position in file from where to begin reading
     *            characters.
     * @param end
     *            The characters are read till end - 1 position in the file.
     * @param blockId
     * @throws IllegalArgumentException
     *             if: (a) fileName is blank/null (b) begin < 0 (c) end < 1 (d)
     *             end <= begin
     */
    FileBlockReader(String fileName, int begin, int end, int blockId) {
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("fileName is null/blank.");
        }
        this.fileName = fileName;
        if (end <= begin) {
            throw new IllegalArgumentException(
                    "Block end postion should be greater than begin position.");
        }
        if (begin < 0 || end < 1) {
            throw new IllegalArgumentException(
                    "Block begin and end postions should be positive.");
        }
        this.begin = begin;
        this.end = end;
        this.blockId = blockId;
    }

    /**
     * @return String of characters from begin to end-1 position.
     * @throws IOException
     */
    public String readBlock() throws IOException {
        if (fRdr == null) {
            fRdr = new FileReader(fileName);
        }
        
        fRdr.skip(begin);
        char[] cbuff = new char[end - begin + 1];
        fRdr.read(cbuff);
        return new String(cbuff);
    }

    public void close() throws IOException {
        if (fRdr != null) {
            fRdr.close();
        }
    }

    /**
     * Utility method for logically dividing the file represented by fileName
     * into blocks and generating FileBlockReaders for each of the logical
     * blocks. The number of FileReaders generated is less than or equal to the
     * desiredBlockCount.
     * <p>
     * There is high probability that blocks vary in the number of characters
     * they represent. This is because the logic tries to set block boundaries
     * such that:
     * </p>
     * <p>
     * The begin position is always after a '\n' character except for the first
     * block whose begin position is at first character in file.
     * </p>
     * <p>
     * The end position is always at '\n' character except for last block in
     * which case it is at last character in file.
     * </p>
     * 
     * @param fileName
     * @param desiredBlockCount
     * @return List of FileBlockReader instances for each of the logical blocks.
     * @throws IOException
     *             <p>
     *             If the named file does not exist, is a directory rather than
     *             a regular file, or for some other reason cannot be opened for
     *             reading.
     *             </p>
     *             <p>
     *             If an I/O error occurs
     *             </p>
     */
    public static List<FileBlockReader> generateBlockReaders(String fileName,
            int desiredBlockCount) throws IOException {
        if (fileName == null || fileName.length() == 0) {
            throw new IllegalArgumentException("fileName is null/blank.");
        }
        if (desiredBlockCount <= 0) {
            throw new IllegalArgumentException("desiredBlockCount is <= 0.");
        }
        FileReader fr = new FileReader(fileName);
        long l = 0;
        long totalChars = 0;
        do {
            l = fr.skip(Long.MAX_VALUE);
            totalChars += l;
        } while (l > 0);
        fr.close();

        fr = new FileReader(fileName);
        long blockSize = (long) Math.ceil((double) totalChars
                / desiredBlockCount);
        int begin, end;
        begin = end = 0;
        int actualBlockCount = 0;
        LinkedList<FileBlockReader> ll = new LinkedList<FileBlockReader>();
        while (end < totalChars) {
            begin = end;
            end += fr.skip(blockSize);
            if ((end - begin) == blockSize) {
                char c;
                do {
                    c = (char) fr.read();
                    end++;
                } while (c != '\n');
            }
            System.out.println(actualBlockCount + ") " + begin + "-" + end);
            actualBlockCount++;
            ll.add(new FileBlockReader(fileName, begin, end - 1,
                    actualBlockCount));
        }
        fr.close();
        return ll;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }

    public int getBlockId() {
        return blockId;
    }

    public String getFileName() {
        return fileName;
    }
}
