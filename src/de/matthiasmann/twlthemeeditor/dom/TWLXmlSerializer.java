/*
 * Copyright (c) 2008-2012, Matthias Mann
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.matthiasmann.twlthemeeditor.dom;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.xmlpull.v1.XmlSerializer;

/**
 *
 * @author Matthias Mann
 */
public class TWLXmlSerializer implements XmlSerializer {

    private final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
    private final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
    
    private Writer out;
    private int depth;
    private int nsCount;
    private String[] nsUri;
    private String[] nsPrefix;
    private String[] esName;
    private String[] esUri;
    private int[] esNSCount;
    private boolean inStartTag;

    public TWLXmlSerializer() {
        this.nsUri = new String[16];
        this.nsPrefix = new String[16];
        this.esName = new String[16];
        this.esUri = new String[16];
        this.esNSCount = new int[16];
        
        esNSCount[0] = 2;
        nsCount = 2;
        
        nsUri[0] = XMLNS_URI;
        nsPrefix[0] = "xmlns";
        
        nsUri[1] = XML_URI;
        nsPrefix[1] = "xml";
    }
    
    public boolean getFeature(String name) {
        return false;
    }

    public void setFeature(String name, boolean state) throws IllegalStateException {
        throw new IllegalStateException("unsupported");
    }

    public Object getProperty(String name) {
        return null;
    }

    public void setProperty(String name, Object value) throws IllegalStateException {
        throw new IllegalStateException("unsupported");
    }

    public void setOutput(Writer writer) throws IOException {
        this.out = writer;
    }

    public void setOutput(OutputStream os, String encoding) throws IOException {
        this.out = new OutputStreamWriter(os, encoding);
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void startDocument(String encoding, Boolean standalone) throws IOException {
        out.write("<?xml version=\"1.0\"");
        if(encoding != null) {
            out.write(" encoding=\"");
            out.write(encoding);
            out.write('"');
        }
        if(standalone != null) {
            out.write(" standalone=\"");
            out.write(standalone ? "yes\"" : "no\"");
        }
        out.write("?>");
    }

    public void docdecl(String text) throws IOException {
        out.write("<!DOCTYPE");
        out.write(text);
        out.write(">");
    }

    public void comment(String text) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        out.write("<!--");
        out.write(text);
        out.write("-->");
    }

    public void ignorableWhitespace(String text) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        out.write(text);
    }

    public void processingInstruction(String text) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        out.write("<?");
        out.write(text);
        out.write("?>");
    }

    public int getDepth() {
        return depth;
    }

    public String getName() {
        return esName[depth];
    }

    public String getNamespace() {
        return esUri[depth];
    }

    private boolean prefixUsed(String prefix) {
        for(int i=0,n=nsCount ; i<n ; i++) {
            if(nsPrefix[i].equals(prefix)) {
                return true;
            }
        }
        return false;
    }
    
    public String getPrefix(String namespace, boolean generatePrefix) {
        for(int i=0,n=nsCount ; i<n ; i++) {
            if(nsUri[i].equals(namespace)) {
                return nsPrefix[i];
            }
        }
        if(generatePrefix) {
            for(int n=1 ;; n++) {
                String prefix = "n" + n;
                if(!prefixUsed(prefix)) {
                    setPrefix(prefix, namespace);
                    return prefix;
                }
            }
        }
        return null;
    }

    public void setPrefix(String prefix, String namespace) throws IllegalStateException {
        if(inStartTag) {
            throw new IllegalStateException("in start tag");
        }
        if(prefixUsed(prefix)) {
            throw new IllegalStateException("duplicated prefix " + prefix);
        }
        if(nsCount == nsUri.length-1) {
            int size = nsCount * 2;
            nsUri    = grow(nsUri, size);
            nsPrefix = grow(nsPrefix, size);
        }
        nsUri[nsCount] = namespace;
        nsPrefix[nsCount] = prefix;
        nsCount++;
    }

    public XmlSerializer startTag(String namespace, String name) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        inStartTag = true;
        out.write('<');
        if(namespace != null && namespace.length() > 0) {
            String prefix = getPrefix(namespace, true);
            if(prefix.length() > 0) {
                out.write(prefix);
                out.write(':');
            }
        }
        if(depth == esName.length-1) {
            int size = depth * 2;
            esName = grow(esName, size);
            esUri  = grow(esUri, size);
        }
        esName[depth] = name;
        esUri[depth] = namespace;
        depth++;
        out.write(name);
        return this;
    }
    
    private void writeNamespaces() throws IOException {
        inStartTag = false;
        for(int i=esNSCount[depth-1] ; i<nsCount ; i++) {
            String prefix = nsPrefix[i];
            if(prefix.length() > 0) {
                out.write(" xmlns:");
                out.write(prefix);
                out.write("=\"");
            } else {
                out.write(" xmlns=\"");
            }
            escapeAttribute(nsUri[i]);
            out.write('\"');
        }
    }
    
    private void closeStartTag() throws IOException {
        writeNamespaces();
        out.write('>');
        esNSCount[depth] = nsCount;
    }

    public XmlSerializer attribute(String namespace, String name, String value) throws IOException, IllegalStateException {
        if(!inStartTag) {
            throw new IllegalStateException("not in start tag");
        }
        out.write(' ');
        if(namespace != null && namespace.length() > 0) {
            String prefix = getPrefix(namespace, true);
            if(prefix.length() > 0) {
                out.write(prefix);
                out.write(':');
            }
        }
        out.write(name);
        out.write("=\"");
        escapeAttribute(value);
        out.write('"');
        return this;
    }

    public XmlSerializer endTag(String namespace, String name) throws IOException, IllegalArgumentException, IllegalStateException {
        if(inStartTag) {
            writeNamespaces();
            out.write("/>");
            --depth;
            return this;
        }
        --depth;
        out.write("</");
        if(namespace != null && namespace.length() > 0) {
            String prefix = getPrefix(namespace, true);
            if(prefix.length() > 0) {
                out.write(prefix);
                out.write(':');
            }
        }
        out.write(name);
        out.write('>');
        return this;
    }

    public void cdsect(String text) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        out.write("<![CDATA[");
        out.write(text); //escape?
        out.write("]]>");
    }

    public void entityRef(String text) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        out.write('&');
        out.write(text); //escape?
        out.write(';');
    }

    public XmlSerializer text(String text) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        escapeText(text);
        return this;
    }

    public XmlSerializer text(char[] buf, int start, int len) throws IOException {
        if(inStartTag) {
            closeStartTag();
        }
        escapeText(buf, start, start+len);
        return this;
    }
    
    private void escapeAttribute(String str) throws IOException {
        int start = 0;
        int end = str.length();
        outer: for(int i=0 ; i<end ; i++) {
            char ch = str.charAt(i);
            String replace;
            switch (ch) {
                case '<': replace = "&lt;"; break;
                case '>': replace = "&gt;"; break;
                case '&': replace = "&amp;"; break;
                case '"': replace = "&quot;"; break;
                case 13: replace = "&#13;"; break;
                case 10: replace = "&#10;"; break;
                case 9: replace = "&#9;"; break;
                default:
                    if(ch < 32) {
                        throw new IllegalArgumentException("Character " + Integer.toString(ch) + " not allowed");
                    }
                    continue outer;
            }
            if(i > start) {
                out.write(str, start, i-start);
            }
            out.write(replace);
            start = i+1;
        }
        if(start < end) {
            out.write(str, start, end-start);
        }
    }
    
    private void escapeText(String str) throws IOException {
        int start = 0;
        int end = str.length();
        outer: for(int i=0 ; i<end ; i++) {
            char ch = str.charAt(i);
            String replace;
            switch (ch) {
                case '<': replace = "&lt;"; break;
                case '>': replace = "&gt;"; break;
                case '&': replace = "&amp;"; break;
                case 13:
                case 10:
                case 9:
                    continue outer;
                default:
                    if(ch < 32) {
                        throw new IllegalArgumentException("Character " + Integer.toString(ch) + " not allowed");
                    }
                    continue outer;
            }
            if(i > start) {
                out.write(str, start, i-start);
            }
            out.write(replace);
            start = i+1;
        }
        if(start < end) {
            out.write(str, start, end-start);
        }
    }
    
    private void escapeText(char[] buf, int start, int end) throws IOException {
        outer: for(int i=0 ; i<end ; i++) {
            char ch = buf[i];
            String replace;
            switch (ch) {
                case '<': replace = "&lt;"; break;
                case '>': replace = "&gt;"; break;
                case '&': replace = "&amp;"; break;
                case 13:
                case 10:
                case 9:
                    continue outer;
                default:
                    if(ch < 32) {
                        throw new IllegalArgumentException("Character " + Integer.toString(ch) + " not allowed");
                    }
                    continue outer;
            }
            if(i > start) {
                out.write(buf, start, i-start);
            }
            out.write(replace);
            start = i+1;
        }
        if(start < end) {
            out.write(buf, start, end-start);
        }
    }

    public void endDocument() throws IOException {
    }
    
    private static String[] grow(String[] src, int size) {
        String[] tmp = new String[size];
        System.arraycopy(src, 0, tmp, 0, src.length);
        return tmp;
    }
    private static int[] grow(int[] src, int size) {
        int[] tmp = new int[size];
        System.arraycopy(src, 0, tmp, 0, src.length);
        return tmp;
    }
}
