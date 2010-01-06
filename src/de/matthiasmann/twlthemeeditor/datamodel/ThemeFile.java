/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twl.utils.CallbackSupport;
import de.matthiasmann.twlthemeeditor.VirtualFile;
import de.matthiasmann.twlthemeeditor.XMLWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author MannMat
 */
public class ThemeFile {

    private final Document document;

    private Runnable[] callbacks;

    public ThemeFile(URL url) throws IOException {
        try {
            SAXBuilder saxb = new SAXBuilder(false);
            saxb.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return new InputSource(new StringReader(""));
                }
            });
            document = saxb.build(url);
        } catch(IOException ex) {
            throw ex;
        } catch(Exception ex) {
            throw new IOException(ex);
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        Writer w = new XMLWriter(new OutputStreamWriter(out, "UTF8"));
        new XMLOutputter().output(document, w);
        w.flush();
    }

    public void addCallback(Runnable cb) {
        callbacks = CallbackSupport.addCallbackToList(callbacks, cb, Runnable.class);
    }

    public void removeCallbacks(Runnable cb) {
        callbacks = CallbackSupport.removeCallbackFromList(callbacks, cb);
    }
    
    public Textures[] findTextures() {
        ArrayList<Textures> result = new ArrayList<Textures>();
        for(Object node : getRoot().getChildren()) {
            if(node instanceof Element) {
                Element element = (Element)node;
                String tagName = element.getName();
                
                if("textures".equals(tagName)) {
                    result.add(new Textures(this, element));
                }
            }
        }
        return result.toArray(new Textures[result.size()]);
    }

    public VirtualFile createVirtualFile() {
        return new VirtualXMLFile(document);
    }
    
    private Element getRoot() {
        return document.getRootElement();
    }

    void fireCallbacks() {
        CallbackSupport.fireCallbacks(callbacks);
    }
}
