/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.matthiasmann.twlthemeeditor.datamodel;

import de.matthiasmann.twlthemeeditor.VirtualFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jdom.Document;
import org.jdom.output.XMLOutputter;

/**
 *
 * @author MannMat
 */
public class VirtualXMLFile implements VirtualFile {

    private final Document document;

    public VirtualXMLFile(Document document) {
        this.document = document;
    }

    public InputStream openStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new XMLOutputter().output(document, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

}
