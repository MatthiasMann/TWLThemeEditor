/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.matthiasmann.twlthemeeditor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author MannMat
 */
public class XMLWriter extends BufferedWriter {

    public XMLWriter(Writer out) {
        super(out);
    }

    @Override
    public void write(String str) throws IOException {
        super.write(" />".equals(str) ? "/>" : str);
    }

}
