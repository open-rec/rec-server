package com.openrec.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class FileUtil {

    public static String read(String fileName) {
        InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(fileName);
        InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        StringBuffer sb = new StringBuffer();
        String line;
        do {
            try {
                line = bufferedReader.readLine();
                if (line != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } while (line != null);
        return sb.toString();
    }
}
