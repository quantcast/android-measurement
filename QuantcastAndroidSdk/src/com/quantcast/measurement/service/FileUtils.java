/**
 * Copyright 2012 Quantcast Corp.
 *
 * This software is licensed under the Quantcast Mobile App Measurement Terms of Service
 * https://www.quantcast.com/learning-center/quantcast-terms/mobile-app-measurement-tos
 * (the “License”). You may not use this file unless (1) you sign up for an account at
 * https://www.quantcast.com and click your agreement to the License and (2) are in
 *  compliance with the License. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.quantcast.measurement.service;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.content.Context;

class FileUtils {
    
    private static final String BASE_DIR = "com.quantcast";
    
    private static final String EMPTY_STRING = "";
    
    public static File getBaseDirectory(Context context) {
        return context.getDir(BASE_DIR, Context.MODE_PRIVATE);
    }
    
    public static File getFile(Context context, String filename) {
        return new File(getBaseDirectory(context), filename);
    }
    
    /**
     * See {@link FileUtils#openOutputStream(File)} for known compatibility.
     * 
     * @param file  This should not be <code>null</code>.
     * @param string
     * @throws IOException
     */
    public static void writeStringToFile(File file, String string) throws IOException {
        if (string == null) {
            string = EMPTY_STRING;
        }
        OutputStream out = null;
        try {
            out = openOutputStream(file);
            out.write(string.getBytes());
        } finally {
            closeQuietly(out);
        }
    }
    
    /**
     * This method has not yet been proven compatible for files outside an app's own context.
     * 
     * @param file  This should not be <code>null</code>.
     * @return
     * @throws IOException
     */
    public static FileOutputStream openOutputStream(File file) throws IOException {
        checkForDirectoryFile(file);
        return new FileOutputStream(file);
    }

    /**
     * See {@link FileUtils#openInputStream(File)} for compatibility.
     * 
     * @param file  This should not be <code>null</code>.
     * @return
     * @throws IOException
     */
    public static String readFileAsString(File file) throws IOException {
        if (!file.exists()) {
            return EMPTY_STRING;
        }
        
        InputStream in = null;
        try {
            in = openInputStream(file);
            return extractStringFromInputStream(in);
        } finally {
            closeQuietly(in);
        }
    }
    
    /**
     * This method has not yet been proven compatible for files outside an app's own context.
     * 
     * @param file  This should not be <code>null</code> and should exist.
     * @return
     * @throws IOException
     */
    public static FileInputStream openInputStream(File file) throws IOException {
        checkForDirectoryFile(file);
        return new FileInputStream(file);
    }
    
    public static String extractStringFromInputStream(InputStream input) throws IOException {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        String line = null;

        try {
            br = new BufferedReader( new InputStreamReader( input ) );

            while ( ( line = br.readLine() ) != null) {
                sb.append( line );
            }

        } catch ( final IOException e ) {
            // TODO: Handle exception
        } finally {
            closeQuietly( br );
        }

        return sb.toString();
    }
    
    public static void checkForDirectoryFile(File file) throws IOException {
        if (file.isDirectory()) {
            throw new IOException("File " + file.getAbsolutePath() + " is a directory.");
        }
    }
    
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (IOException e) {
                // do nothing
            }
        }
    }
}
