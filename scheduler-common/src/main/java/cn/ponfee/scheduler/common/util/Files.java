package cn.ponfee.scheduler.common.util;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

/**
 * File utilities.
 *
 * @author Ponfee
 */
public final class Files {

    public static final int EOF = -1; // end of file read

    public static final int BUFF_SIZE = 8192; // file buffer size

    public static final String TOP_PATH                 = "..";

    public static final String CURRENT_PATH             = ".";

    // ------------------------------------------------------------charset encoding
    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset(); // default charset

    public static final String DEFAULT_CHARSET_NAME = DEFAULT_CHARSET.name(); // default charset name

    public static final String UTF_8 = "UTF-8"; // UTF-8 encoding

    // ------------------------------------------------------------file separator
    public static final String WINDOWS_FOLDER_SEPARATOR = "\\"; // windows file separator

    public static final String UNIX_FOLDER_SEPARATOR = "/"; // unix file separator

    public static final String SYSTEM_FOLDER_SEPARATOR = File.separator; // system file separator

    // ------------------------------------------------------------line separator
    public static final String UNIX_LINE_SEPARATOR = "\n"; // unix file line separator spec \n  LF

    public static final String WINDOWS_LINE_SEPARATOR = "\r\n"; // windows file line separator spec \r\n  CRLF

    public static final String MAC_LINE_SEPARATOR = "\r"; // mac file line separator spec \r  CR

    public static final String SYSTEM_LINE_SEPARATOR; // system file line separator

    static {
        /*
        String separator = java.security.AccessController.doPrivileged(
            new sun.security.action.GetPropertyAction("line.separator")
        );
        if (separator == null || separator.length() == 0) {
            separator = System.getProperty("line.separator", "\n");
        }
        SYSTEM_LINE_SEPARATOR = separator;
        */
        StringBuilderWriter buffer = new StringBuilderWriter(4);
        PrintWriter out = new PrintWriter(buffer);
        out.println();
        SYSTEM_LINE_SEPARATOR = buffer.toString();
        out.close();
    }

    /**
     * 创建目录
     *
     * @param path
     * @return
     */
    public static File mkdir(String path) {
        File file = new File(path);
        mkdir(file);
        return file;
    }

    /**
     * 创建目录
     *
     * @param file
     * @return
     */
    public static void mkdir(File file) {
        if (file.isFile()) {
            throw new IllegalStateException(file.getAbsolutePath() + " is a directory.");
        }

        if (file.exists()) {
            return;
        }

        if (file.mkdirs()) {
            file.setLastModified(System.currentTimeMillis());
        }
    }

    /**
     * 创建文件
     *
     * @param path
     * @return
     */
    public static File touch(String path) {
        File file = new File(path);
        touch(file);
        return file;
    }

    /**
     * 创建文件
     *
     * @param file
     * @return
     */
    public static void touch(File file) {
        if (file.isDirectory()) {
            throw new IllegalStateException(file.getAbsolutePath() + " is a directory.");
        }

        if (file.exists()) {
            return;
        }

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            if (file.createNewFile()) {
                file.setLastModified(System.currentTimeMillis());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void deleteQuietly(File file) {
        if (file == null) {
            return;
        }

        try {
            //org.apache.commons.io.FileUtils.deleteQuietly(file);
            java.nio.file.Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }
    }

    // --------------------------------------------------------------------------file to string
    public static String toString(String file) throws IOException {
        return toString(new File(file), DEFAULT_CHARSET_NAME);
    }

    public static String toString(File file) throws IOException {
        return toString(file, DEFAULT_CHARSET_NAME);
    }

    public static String toString(File file, String charsetName) {
        return toString(file, Charset.forName(charsetName));
    }

    public static String toString(File file, Charset charset) {
        try (FileInputStream input = new FileInputStream(file);
             FileChannel channel = input.getChannel()
        ) {
            //FileLock lock = channel.lock();
            //lock.release();
            ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
            return charset.decode(buffer).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads file to byte array
     *
     * @param file
     * @return
     */
    public static byte[] toByteArray(File file) {
        try (FileInputStream in = new FileInputStream(file);
             FileChannel channel = in.getChannel()
        ) {
            ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes, 0, bytes.length);
            return bytes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------read line
    public static List<String> readLines(File file, String charset)
            throws FileNotFoundException {
        return readLines(new FileInputStream(file), charset);
    }

    public static List<String> readLines(InputStream input, String charset) {
        List<String> list = new LinkedList<>();
        readLines(input, charset, list::add);
        return list;
    }

    /**
     * Read input-stream as text line
     *
     * @param input
     * @param charset
     * @param consumer
     */
    public static void readLines(InputStream input, String charset,
                                 Consumer<String> consumer) {
        try (Scanner scanner = (charset == null)
                ? new Scanner(input)
                : new Scanner(input, charset)) {
            while (scanner.hasNextLine()) {
                consumer.accept(scanner.nextLine());
            }
        }
    }

    // -----------------------------------------------------------------readByteArray
    public static byte[] readByteArray(InputStream input, int count) throws IOException {
        byte[] bytes = new byte[count];
        int n, index = 0;
        while (index < count && (n = input.read(bytes, index, count - index)) != EOF) {
            index += n;
        }

        return (index == count) ? bytes : Arrays.copyOf(bytes, index);
    }

    public static byte[] readByteArray(File file, int count) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return readByteArray(input, count);
        }
    }

    public static byte[] readByteArray(String filePath, int count) throws IOException {
        return readByteArray(new File(filePath), count);
    }

    /**
     * 文件路径规范化，如“path/..”内部的点号
     * 注意：windows的文件分隔符“\”会替换为“/”
     *
     * @param path 文件路径
     * @return 规范的文件路径
     */
    public static String cleanPath(String path) {
        if (path == null) {
            return null;
        }

        String pathToUse = StringUtils.replace(path, WINDOWS_FOLDER_SEPARATOR, UNIX_FOLDER_SEPARATOR);

        // Strip prefix from path to analyze, to not treat it as part of the
        // first path element. This is necessary to correctly parse paths like
        // "file:core/../core/io/Resource.class", where the ".." should just
        // strip the first "core" directory while keeping the "file:" prefix.
        int prefixIndex = pathToUse.indexOf(":");
        String prefix = "";
        if (prefixIndex != -1) {
            prefix = pathToUse.substring(0, prefixIndex + 1);
            if (prefix.contains("/")) {
                prefix = "";
            } else {
                pathToUse = pathToUse.substring(prefixIndex + 1);
            }
        }
        if (pathToUse.startsWith(UNIX_FOLDER_SEPARATOR)) {
            prefix = prefix + UNIX_FOLDER_SEPARATOR;
            pathToUse = pathToUse.substring(1);
        }

        String[] pathArray = StringUtils.split(pathToUse, UNIX_FOLDER_SEPARATOR);
        List<String> pathElements = new LinkedList<>();
        int tops = 0;

        for (int i = pathArray.length - 1; i >= 0; i--) {
            String element = pathArray[i];
            if (CURRENT_PATH.equals(element)) {
                // Points to current directory - drop it.
            } else if (TOP_PATH.equals(element)) {
                // Registering top path found.
                tops++;
            } else {
                if (tops > 0) {
                    // Merging path element with element corresponding to top path.
                    tops--;
                } else {
                    // Normal path element found.
                    pathElements.add(0, element);
                }
            }
        }

        // Remaining top paths need to be retained.
        for (int i = 0; i < tops; i++) {
            pathElements.add(0, TOP_PATH);
        }

        return prefix + String.join(UNIX_FOLDER_SEPARATOR, pathElements);
    }
}
