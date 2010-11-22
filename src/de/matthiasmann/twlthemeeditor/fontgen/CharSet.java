/*
 * Copyright (c) 2008-2010, Matthias Mann
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
package de.matthiasmann.twlthemeeditor.fontgen;

import java.lang.Character.UnicodeBlock;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author Matthias Mann
 */
public class CharSet {

    private static final String KEY_MANUAL = "ChatSetManual";
    
    private final HashMap<Character.UnicodeBlock, Boolean> blocks;
    private final BitSet manualCharactersBitSet;
    private String manualCharacters;

    public CharSet() {
        this.blocks = new HashMap<Character.UnicodeBlock, Boolean>();
        this.manualCharactersBitSet = new BitSet();
        this.manualCharacters = "";
    }

    public CharSet(CharSet charSet) {
        this();
        blocks.putAll(charSet.blocks);
        manualCharactersBitSet.or(charSet.manualCharactersBitSet);
        manualCharacters = charSet.manualCharacters;
    }

    public void setBlock(Character.UnicodeBlock block, boolean included) {
        blocks.put(block, included);
    }

    public boolean getBlockEnabled(Character.UnicodeBlock block) {
        return blocks.get(block) == Boolean.TRUE;
    }

    public String getManualCharacters() {
        return manualCharacters;
    }

    public void setManualCharacters(String characters) {
        manualCharacters = characters;
        manualCharactersBitSet.clear();
        for(int i=0,n=characters.length() ; i<n ; i++) {
            manualCharactersBitSet.set(characters.charAt(i));
        }
    }
    
    public boolean isIncluded(int ch) {
        return manualCharactersBitSet.get(ch) || getBlockEnabled(Character.UnicodeBlock.of(ch));
    }

    public void save(Properties prop) {
        for(Character.UnicodeBlock block : getSupportedBlocks()) {
            prop.setProperty(getKey(block), Boolean.toString(getBlockEnabled(block)));
        }
        prop.setProperty(KEY_MANUAL, getManualCharacters());
    }

    public void load(Properties prop) {
        blocks.clear();
        for(Character.UnicodeBlock block : getSupportedBlocks()) {
            if("true".equals(prop.getProperty(getKey(block)))) {
                setBlock(block, true);
            }
        }
        setManualCharacters(prop.getProperty(KEY_MANUAL, ""));
    }

    private static String getKey(Character.UnicodeBlock block) {
        return "CharSet.".concat(block.toString());
    }

    public static Character.UnicodeBlock[] getSupportedBlocks() {
        ArrayList<Character.UnicodeBlock> result = new ArrayList<Character.UnicodeBlock>();
        for(Field f : Character.UnicodeBlock.class.getFields()) {
            if(Modifier.isStatic(f.getModifiers()) && f.getType() == Character.UnicodeBlock.class) {
                try {
                    Character.UnicodeBlock block = (UnicodeBlock) f.get(null);
                    if(block != Character.UnicodeBlock.LOW_SURROGATES &&
                            block != Character.UnicodeBlock.HIGH_SURROGATES &&
                            block != Character.UnicodeBlock.SURROGATES_AREA &&
                            block != Character.UnicodeBlock.HIGH_PRIVATE_USE_SURROGATES) {
                        result.add(block);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return result.toArray(new Character.UnicodeBlock[result.size()]);
    }
}
