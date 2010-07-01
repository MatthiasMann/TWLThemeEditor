/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: TTFFile.java 463581 2006-10-13 07:45:19Z bdelacretaz $ */

package de.matthiasmann.twlthemeeditor.fontgen;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads a TrueType file or a TrueType Collection.
 * The TrueType spec can be found at the Microsoft.
 * Typography site: http://www.microsoft.com/truetype/
 */
public class TTFFile {
    /**
     * Table directory
     */
    private HashMap<String, TTFDirTabEntry> dirTabs;
    /** The table of kerning values */
    private IntMap<IntMap<Integer>> kerningTab;                          // for CIDs
    /** The unicode mapping */
    private BitSet unicodeDefined;

    /** The all important conversion between internal units and Em */
    private int upem;                                // unitsPerEm from "head" table

    /** The name of the font */
    private String fontName = "";
    /** The full name of the font */
    private String fullName = "";
    /** The notice stored in the font file */
    private String notice = "";
    /** The font family name */
    private String familyName = "";
    /** The sub family name */
    private String subFamilyName = "";

    /** The glyph to unicode mapping */
    private IntMap<Integer> glyphMap;
    
    /**
     * Position inputstream to position indicated
     * in the dirtab offset + offset
     * 
     * @param in The reader to read the tables from
     * @param name The table to search for 
     * @param offset The offset to start at
     * @return True if found the table
     * @throws IOException Indicatesa a failure to read
     */
    boolean seekTab(FontFileReader in, String name, long offset) throws IOException {
        TTFDirTabEntry dt = dirTabs.get(name);
        if (dt == null) {
            return false;
        } else {
            in.seekSet(dt.getOffset() + offset);
        }
        return true;
    }

    /**
     * Get whatever UPEM is
     * 
     * @return The UPEM
     */
    public int getUPEM() {
    	return upem;
    }
    
    /**
     * Read the cmap table,
     * return false if the table is not present or only unsupported
     * tables are present. Currently only unicode cmaps are supported.
     * Set the unicodeIndex in the TTFMtxEntries and fills in the
     * cmaps vector.
     * 
     * @param in The reader to get the table from
     * @return True if the table has been read
     * @throws IOException Indicates a failure to read the table
     */
    private boolean readCMAP(FontFileReader in) throws IOException {
        unicodeDefined = new BitSet();
        
        seekTab(in, "cmap", 2);
        int numCMap = in.readTTFUShort();    // Number of cmap subtables
        long cmapUniOffset = 0;

        //Read offset for all tables. We are only interested in the unicode table
        for (int i = 0; i < numCMap; i++) {
            int cmapPID = in.readTTFUShort();
            int cmapEID = in.readTTFUShort();
            long cmapOffset = in.readTTFULong();

            if (cmapPID == 3 && cmapEID == 1) {
                cmapUniOffset = cmapOffset;
            }
        }

        if (cmapUniOffset <= 0) {
            return false;
        }

        // Read unicode cmap
        if (!seekTab(in, "cmap", cmapUniOffset)) {
            return false;
        }
        int cmapFormat = in.readTTFUShort();
        if (cmapFormat != 4) {
            return false;
        }

        glyphMap = new IntMap<Integer>();
        
        in.skip(2 * 2);     // Skip length & language number
        int cmapSegCount = in.readTTFUShort() / 2;
        in.skip(3 * 2);     // Skip search parameters

        int[] cmapEndCounts = new int[cmapSegCount];
        int[] cmapStartCounts = new int[cmapSegCount];
        int[] cmapDeltas = new int[cmapSegCount];
        int[] cmapRangeOffsets = new int[cmapSegCount];

        for (int i=0 ; i<cmapSegCount ; i++) {
            cmapEndCounts[i] = in.readTTFUShort();
        }

        in.skip(2);    // Skip reservedPad

        for (int i=0 ; i<cmapSegCount ; i++) {
            cmapStartCounts[i] = in.readTTFUShort();
        }

        for (int i=0 ; i<cmapSegCount ; i++) {
            cmapDeltas[i] = in.readTTFShort();
        }

        int startRangeOffset = in.getCurrentPos();

        for (int i=0 ; i<cmapSegCount ; i++) {
            cmapRangeOffsets[i] = in.readTTFUShort();
        }

        // Insert the unicode id for the glyphs in mtxWxTab
        // and fill in the cmaps ArrayList

        for (int seg=0 ; seg<cmapSegCount ; seg++) {
            for (int unicode=cmapStartCounts[seg] ; unicode<=cmapEndCounts[seg] ; unicode++) {
                int glyphIdx;

                // the last character 65535 = .notdef
                // may have a range offset
                if (cmapRangeOffsets[seg] != 0 && unicode != 65535) {
                    int glyphOffset = startRangeOffset + cmapRangeOffsets[seg] + ((unicode - cmapStartCounts[seg]) + seg) * 2;
                    in.seekSet(glyphOffset);
                    glyphIdx = in.readTTFUShort();
                    if(glyphIdx != 0) {
                        glyphIdx = (glyphIdx + cmapDeltas[seg]) & 0xffff;
                    }
                } else {
                    glyphIdx = (unicode + cmapDeltas[seg]) & 0xffff;
                }

                glyphMap.put(glyphIdx, unicode);
                unicodeDefined.set(unicode);
            }
        }
        return true;
    }

    /**
     * Reads the font using a FontFileReader.
     *
     * @param in The FontFileReader to use
     * @return False if the font was invalid 
     * @throws IOException In case of an I/O problem
     */
    public boolean readFont(FontFileReader in) throws IOException {
        return readFont(in, (String)null);
    }

    /**
     * Read the font data.
     * If the fontfile is a TrueType Collection (.ttc file)
     * the name of the font to read data for must be supplied,
     * else the name is ignored.
     *
     * @param in The FontFileReader to use
     * @param name The name of the font
     * @return boolean Returns true if the font is valid
     * @throws IOException In case of an I/O problem
     */
    public boolean readFont(FontFileReader in, String name) throws IOException {

        /*
         * Check if TrueType collection, and that the name
         * exists in the collection
         */
        if (!checkTTC(in, name)) {
            if (name == null) {
                throw new IllegalArgumentException("For TrueType collection you must specify which font to select");
            } else {
                throw new IOException("Name does not exist in the TrueType collection: " + name);
            }
        }

        readDirTabs(in);
        readFontHeader(in);
        readName(in);
        // Read cmap table and fill in ansiwidths
        boolean valid = readCMAP(in);
        if (!valid) {
            return false;
        }
        readKerning(in);
        return true;
    }

    /**
     * Returns the PostScript name of the font.
     * @return String The PostScript name
     */
    public String getPostScriptName() {
        if ("Regular".equals(subFamilyName) || "Roman".equals(subFamilyName)) {
            return familyName;
        } else {
            return familyName + "," + subFamilyName;
        }
    }

    /**
     * Returns the font family name of the font.
     * @return String The family name
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Returns the kerning table.
     * @return Map The kerning table
     */
    public IntMap<IntMap<Integer>> getKerning() {
        return kerningTab;
    }

    public BitSet getDefinedUnicodePoints() {
        return unicodeDefined;
    }

    /**
     * Read Table Directory from the current position in the
     * FontFileReader and fill the global HashMap dirTabs
     * with the table name (String) as key and a TTFDirTabEntry
     * as value.
     * @param in FontFileReader to read the table directory from
     * @throws IOException in case of an I/O problem
     */
    protected void readDirTabs(FontFileReader in) throws IOException {
        in.skip(4);    // TTF_FIXED_SIZE
        int ntabs = in.readTTFUShort();
        in.skip(6);    // 3xTTF_USHORT_SIZE

        dirTabs = new HashMap<String, TTFDirTabEntry>();
        for (int i = 0; i < ntabs; i++) {
            TTFDirTabEntry pd = new TTFDirTabEntry();
            dirTabs.put(pd.read(in), pd);
        }
    }

    /**
     * Read the "head" table, this reads the bounding box and
     * sets the upem (unitsPerEM) variable
     * @param in FontFileReader to read the header from
     * @throws IOException in case of an I/O problem
     */
    protected void readFontHeader(FontFileReader in) throws IOException {
        seekTab(in, "head", 2 * 4 + 2 * 4 + 2);
        upem = in.readTTFUShort();
    }

    /**
     * Read the "name" table.
     * @param in FontFileReader to read from
     * @throws IOException In case of a I/O problem
     */
    private void readName(FontFileReader in) throws IOException {
        seekTab(in, "name", 2);
        int i = in.getCurrentPos();
        int n = in.readTTFUShort();
        int j = in.readTTFUShort() + i - 2;
        i += 2 * 2;

        while (n-- > 0) {
            // getLogger().debug("Iteration: " + n);
            in.seekSet(i);
            final int platformID = in.readTTFUShort();
            final int encodingID = in.readTTFUShort();
            final int languageID = in.readTTFUShort();

            int k = in.readTTFUShort();
            int l = in.readTTFUShort();

            if (((platformID == 1 || platformID == 3) 
                    && (encodingID == 0 || encodingID == 1))
                    && (k == 1 || k == 2 || k == 0 || k == 4 || k == 6)) {
                in.seekSet(j + in.readTTFUShort());
                String txt = in.readTTFString(l);
                
                switch (k) {
                case 0:
                    notice = txt;
                    break;
                case 1:
                    familyName = txt;
                    break;
                case 2:
                    subFamilyName = txt;
                    break;
                case 4:
                    fullName = txt;
                    break;
                case 6:
                    fontName = txt;
                    break;
                }
            }
            i += 6 * 2;
        }
    }

    /**
     * Read the kerning table, create a table for both CIDs and
     * winAnsiEncoding.
     * @param in FontFileReader to read from
     * @throws IOException In case of a I/O problem
     */
    private void readKerning(FontFileReader in) throws IOException {
        // Read kerning
        kerningTab = new IntMap<IntMap<Integer>>();
        if (seekTab(in, "kern", 0)) {
            int version = in.readTTFUShort();
            int nTables = in.readTTFUShort();
            //System.out.println("version="+version+" nTables="+nTables);

            long nextTablePos = in.getCurrentPos();
            for(int table=0 ; table<nTables ; table++) {
                in.seekSet(nextTablePos);
                nextTablePos += in.readTTFULong();
                int coverage = in.readTTFUShort();
                if ((coverage & 3) != 1) {  // only horizontal
                    continue;
                }

                int format = coverage >> 8;
                switch(format) {
                    case 0: {
                        int numPairs = in.readTTFUShort();;
                        in.skip(3 * 2);
                        for(int pair=0 ; pair<numPairs ; pair++) {
                            int i = in.readTTFUShort();
                            int j = in.readTTFUShort();
                            int kpx = in.readTTFShort();
                            if (kpx != 0) {
                                // CID kerning table entry, using unicode indexes
                                addKerning(i, j, kpx);
                            }
                        }
                        break;
                    }
                    default:
                        Logger.getLogger(TTFFile.class.getName()).log(Level.WARNING,
                                "Unsupported kerning subtable format: {0} (kern table version: {1})",
                                new Object[]{format, version});
                }
            }
        }
    }

    private void addKerning(int fromGlyph, int toGlyph, int kpx) {
        final Integer fromUnicode = glyphMap.get(fromGlyph);
        final Integer toUnicode = glyphMap.get(toGlyph);
        if (fromUnicode != null && toUnicode != null) {
            IntMap<Integer> adjTab = kerningTab.get(fromUnicode.intValue());
            if (adjTab == null) {
                adjTab = new IntMap<Integer>();
                kerningTab.put(fromUnicode.intValue(), adjTab);
            }
            adjTab.put(toUnicode.intValue(), kpx);
        }
    }

    /**
     * Check if this is a TrueType collection and that the given
     * name exists in the collection.
     * If it does, set offset in fontfile to the beginning of
     * the Table Directory for that font.
     * @param in FontFileReader to read from
     * @param name The name to check
     * @return True if not collection or font name present, false otherwise
     * @throws IOException In case of an I/O problem
     */
    protected final boolean checkTTC(FontFileReader in, String name) throws IOException {
        String tag = in.readTTFString(4);

        if ("ttcf".equals(tag)) {
            // This is a TrueType Collection
            in.skip(4);

            // Read directory offsets
            int numDirectories = (int)in.readTTFULong();
            // int numDirectories=in.readTTFUShort();
            long[] dirOffsets = new long[numDirectories];
            for (int i = 0; i < numDirectories; i++) {
                dirOffsets[i] = in.readTTFULong();
            }

            System.out.println("This is a TrueType collection file with " + numDirectories + " fonts");
            System.out.println("Containing the following fonts: ");

            // Read all the directories and name tables to check
            // If the font exists - this is a bit ugly, but...
            boolean found = false;

            // Iterate through all name tables even if font
            // Is found, just to show all the names
            long dirTabOffset = 0;
            for (int i = 0; (i < numDirectories); i++) {
                in.seekSet(dirOffsets[i]);
                readDirTabs(in);

                readName(in);

                if (fullName.equals(name)) {
                    found = true;
                    dirTabOffset = dirOffsets[i];
                    System.out.println(fullName + " <-- selected");
                } else {
                    System.out.println(fullName);
                }

                // Reset names
                notice = "";
                fullName = "";
                familyName = "";
                fontName = "";
                subFamilyName = "";
            }

            in.seekSet(dirTabOffset);
            return found;
        } else {
            in.seekSet(0);
            return true;
        }
    }
}