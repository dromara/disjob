/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.util;

import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
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

    public static final int EOF             = -1; // end of file read

    public static final int BUFF_SIZE       = 8192; // file buffer size

    public static final String TOP_PATH     = "..";

    public static final String CURRENT_PATH = ".";

    // ------------------------------------------------------------charset encoding

    public static final Charset DEFAULT_CHARSET = Charset.defaultCharset(); // default charset

    public static final String DEFAULT_CHARSET_NAME = DEFAULT_CHARSET.name(); // default charset name

    public static final String UTF_8 = "UTF-8"; // UTF-8 encoding

    // ------------------------------------------------------------file separator

    public static final String WINDOWS_FOLDER_SEPARATOR = "\\"; // Windows file separator

    public static final String UNIX_FOLDER_SEPARATOR = "/"; // Unix file separator

    public static final String SYSTEM_FOLDER_SEPARATOR = File.separator; // system file separator

    // ------------------------------------------------------------line separator

    public static final String UNIX_LINE_SEPARATOR = "\n"; // Unix file line separator spec \n  LF

    public static final String WINDOWS_LINE_SEPARATOR = "\r\n"; // Windows file line separator spec \r\n  CRLF

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

    public static String tmpDir() {
        String tmpDir = SystemUtils.JAVA_IO_TMPDIR;
        if (!tmpDir.endsWith(File.separator)) {
            tmpDir += File.separator;
        }
        return tmpDir;
    }

    /**
     * Creates directory if not exists
     *
     * @param path the path
     * @return directory
     * @throws IOException if create failed
     */
    public static File mkdirIfNotExists(String path) throws IOException {
        File file = new File(path);
        mkdirIfNotExists(file);
        return file;
    }

    /**
     * Creates directory if not exists
     *
     * @param path the path
     * @throws IOException if create failed
     */
    public static void mkdirIfNotExists(File path) throws IOException {
        if (path.exists()) {
            if (path.isDirectory()) {
                return;
            }
            throw new IOException("Exists mkdir path is not a directory: " + path.getAbsolutePath());
        }
        if (!path.mkdirs()) {
            throw new IOException("Create directory failed: " + path.toPath());
        }
    }

    /**
     * Clear or make directory
     *
     * @param path the path
     * @return file
     * @throws IOException if clean or make failed
     */
    public static File cleanOrMakeDir(String path) throws IOException {
        File file = new File(path);
        cleanOrMakeDir(file);
        return file;
    }

    /**
     * Clear or make directory
     *
     * @param path the path
     * @throws IOException if clean or make failed
     */
    public static void cleanOrMakeDir(File path) throws IOException {
        if (path.exists()) {
            if (path.isDirectory()) {
                PathUtils.deleteDirectory(path.toPath());
            } else {
                throw new IOException("Exists clean path is not a directory: " + path.getAbsolutePath());
            }
        }
        if (!path.mkdirs()) {
            throw new IOException("Clean directory failed: " + path.toPath());
        }
    }

    /**
     * Creates file if not exists
     *
     * @param path the path
     * @return file
     * @throws IOException if create failed
     */
    public static File touchIfNotExists(String path) throws IOException {
        File file = new File(path);
        touchIfNotExists(file);
        return file;
    }

    /**
     * Creates file if not exists
     *
     * @param path the path
     * @throws IOException if create failed
     */
    public static void touchIfNotExists(File path) throws IOException {
        if (path.exists()) {
            if (path.isFile()) {
                return;
            }
            throw new IOException("Exists touch path is not a file: " + path.getAbsolutePath());
        }
        File parentFile = path.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IOException("Create parent file failed: " + parentFile.getAbsolutePath());
        }
        if (!path.createNewFile()) {
            throw new IOException("Create file failed: " + path.getAbsolutePath());
        }
    }

    // --------------------------------------------------------------------------file to string

    public static String toString(String file) {
        return toString(new File(file), DEFAULT_CHARSET_NAME);
    }

    public static String toString(File file) {
        return toString(file, DEFAULT_CHARSET_NAME);
    }

    public static String toString(File file, String charsetName) {
        return toString(file, Charset.forName(charsetName));
    }

    public static String toString(File file, Charset charset) {
        try (FileInputStream input = new FileInputStream(file);
             FileChannel channel = input.getChannel()) {
            //FileLock lock = channel.lock();
            //lock.release();
            ByteBuffer buffer = channel.map(MapMode.READ_ONLY, 0, channel.size());
            return charset.decode(buffer).toString();
        } catch (IOException e) {
            return ExceptionUtils.rethrow(e);
        }
    }

    /**
     * Reads file to byte array
     *
     * @param file the file
     * @return file byte array
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
            return ExceptionUtils.rethrow(e);
        }
    }

    // ---------------------------------------------------------------read line

    public static List<String> readLines(File file, String charset) throws FileNotFoundException {
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
     * @param input    the input stream
     * @param charset  the charset
     * @param consumer the consumer
     */
    public static void readLines(InputStream input, String charset, Consumer<String> consumer) {
        try (Scanner scanner = (charset == null) ? new Scanner(input) : new Scanner(input, charset)) {
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
            if (prefix.contains(UNIX_FOLDER_SEPARATOR)) {
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

    public static Charset charset(String charsetName) {
        return charsetName == null ? DEFAULT_CHARSET : Charset.forName(charsetName);
    }

    public static File toFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (Exception e) {
            // return new File(url.getPath());
            return ExceptionUtils.rethrow(e);
        }
    }

    public static Document parseToXmlDocument(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setExpandEntityReferences(false);
        } catch (Exception ignored) {
            // ignored
        }
        return factory.newDocumentBuilder().parse(inputStream);
    }

}
