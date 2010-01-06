/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;

/**
 *
 * @author MannMat
 */
public class TestEnv extends URLStreamHandler {

    private final HashMap<String, VirtualFile> files;

    public TestEnv() {
        this.files = new HashMap<String, VirtualFile>();
    }

    public void registerFile(String name, VirtualFile file) {
        if(name.length() == 0 || name.charAt(0) != '/') {
            name = "/".concat(name);
        }
        files.put(name, file);
    }

    public void registerFile(String name, final URL url) {
        registerFile(name, new VirtualFile() {
            public InputStream openStream() throws IOException {
                return url.openStream();
            }
        });
    }

    @Override
    protected URLConnection openConnection(final URL url) throws IOException {
        final String fileName = url.getFile();
        final VirtualFile file = files.get(fileName);
        return new URLConnection(url) {
            @Override
            public void connect() throws IOException {
                if(file == null) {
                    throw new FileNotFoundException(fileName);
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                if(file == null) {
                    throw new FileNotFoundException(fileName);
                }
                return file.openStream();
            }
        };
    }

    public URL getURL(String file) throws MalformedURLException {
        return new URL("testenv", "local", 80, file, this);
    }
}
