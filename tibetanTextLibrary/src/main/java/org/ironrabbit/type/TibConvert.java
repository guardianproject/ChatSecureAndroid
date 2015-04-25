package org.ironrabbit.type;

import java.util.HashMap;
import java.util.LinkedList;
/**
 * @author Tom Meyer
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Java port of Andrew West's code sample "Tibetan.cpp", used in BabelPad.
 * 
 * Based on the Unicode <-> GB/T20524-2006 mappings described in the tables at 
 *    http://sites.google.com/site/chrisfynn2/precomposedtibetan-parta
 *    
 * Jan 16 2010, Initial port from C++
 * Jan 18 2010, Turned fixed-length C++-style arrays into variable Java ones,
 *              and probably shrunk the code size in half by doing that.
 *              Added tableIndex to speed up Unicode->Precomposed lookup (it was
 *              traversing the entire big mapping table for each character).
 *              
 * TODO: proper documentation and unit tests
 *       make sure Precomposed->Unicode mapping works (untested)
 *       further speed-up by only traversing the table once per character
 *
 */
import java.util.ListIterator;

public class TibConvert {
        protected TibConvert() {}
        
        private static HashMap<Character, LinkedList<Integer>> tableIndex = null;
        
        // The lookup table to handle mappings between Tibetan Unicode and GB/T20524-2006
        private static final char TableAMapping[][] =
        {
                { 0xF300, 0x0F68, 0x0F72 },             // 0F68 0F72
                { 0xF301, 0x0F68, 0x0F80 },             // 0F68 0F80
                { 0xF302, 0x0F68, 0x0F74 },             // 0F68 0F74
                { 0xF303, 0x0F68, 0x0F7A },             // 0F68 0F7A
                { 0xF304, 0x0F68, 0x0F7C },             // 0F68 0F7C
                { 0xF305, 0x0F40, 0x0F71 },             // 0F40 0F71
                { 0xF306, 0x0F40, 0x0F72 },             // 0F40 0F72
                { 0xF307, 0x0F40, 0x0F80 },             // 0F40 0F80
                { 0xF308, 0x0F40, 0x0F74 },             // 0F40 0F74
                { 0xF309, 0x0F40, 0x0F7A },             // 0F40 0F7A
                { 0xF30A, 0x0F40, 0x0F7C },             // 0F40 0F7C
                { 0xF30B, 0x0F40, 0x0FB1 },             // 0F40 0FB1
                { 0xF30C, 0x0F40, 0x0FB1, 0x0F72 },             // 0F40 0FB1 0F72
                { 0xF30D, 0x0F40, 0x0FB1, 0x0F80 },             // 0F40 0FB1 0F80
                { 0xF30E, 0x0F40, 0x0FB1, 0x0F74 },             // 0F40 0FB1 0F74
                { 0xF30F, 0x0F40, 0x0FB1, 0x0F7A },             // 0F40 0FB1 0F7A
                { 0xF310, 0x0F40, 0x0FB1, 0x0F7C },             // 0F40 0FB1 0F7C
                { 0xF311, 0x0F40, 0x0FB2 },             // 0F40 0FB2
                { 0xF312, 0x0F40, 0x0FB2, 0x0F72 },             // 0F40 0FB2 0F72
                { 0xF313, 0x0F40, 0x0FB2, 0x0F80 },             // 0F40 0FB2 0F80
                { 0xF314, 0x0F40, 0x0FB2, 0x0F74 },             // 0F40 0FB2 0F74
                { 0xF315, 0x0F40, 0x0FB2, 0x0F7A },             // 0F40 0FB2 0F7A
                { 0xF316, 0x0F40, 0x0FB2, 0x0F7C },             // 0F40 0FB2 0F7C
                { 0xF317, 0x0F40, 0x0FB3 },             // 0F40 0FB3
                { 0xF318, 0x0F40, 0x0FB3, 0x0F72 },             // 0F40 0FB3 0F72
                { 0xF319, 0x0F40, 0x0FB3, 0x0F80 },             // 0F40 0FB3 0F80
                { 0xF31A, 0x0F40, 0x0FB3, 0x0F74 },             // 0F40 0FB3 0F74
                { 0xF31B, 0x0F40, 0x0FB3, 0x0F7A },             // 0F40 0FB3 0F7A
                { 0xF31C, 0x0F40, 0x0FB3, 0x0F7C },             // 0F40 0FB3 0F7C
                { 0xF31D, 0x0F40, 0x0FAD },             // 0F40 0FAD
                { 0xF31E, 0x0F62, 0x0F90 },             // 0F62 0F90
                { 0xF31F, 0x0F62, 0x0F90, 0x0F72 },             // 0F62 0F90 0F72
                { 0xF320, 0x0F62, 0x0F90, 0x0F80 },             // 0F62 0F90 0F80
                { 0xF321, 0x0F62, 0x0F90, 0x0F74 },             // 0F62 0F90 0F74
                { 0xF322, 0x0F62, 0x0F90, 0x0F7A },             // 0F62 0F90 0F7A
                { 0xF323, 0x0F62, 0x0F90, 0x0F7C },             // 0F62 0F90 0F7C
                { 0xF324, 0x0F62, 0x0F90, 0x0FB1 },             // 0F62 0F90 0FB1
                { 0xF325, 0x0F62, 0x0F90, 0x0FB1, 0x0F72 },             // 0F62 0F90 0FB1 0F72
                { 0xF326, 0x0F62, 0x0F90, 0x0FB1, 0x0F80 },             // 0F62 0F90 0FB1 0F80
                { 0xF327, 0x0F62, 0x0F90, 0x0FB1, 0x0F74 },             // 0F62 0F90 0FB1 0F74
                { 0xF328, 0x0F62, 0x0F90, 0x0FB1, 0x0F7A },             // 0F62 0F90 0FB1 0F7A
                { 0xF329, 0x0F62, 0x0F90, 0x0FB1, 0x0F7C },             // 0F62 0F90 0FB1 0F7C
                { 0xF32A, 0x0F63, 0x0F90 },             // 0F63 0F90
                { 0xF32B, 0x0F63, 0x0F90, 0x0F72 },             // 0F63 0F90 0F72
                { 0xF32C, 0x0F63, 0x0F90, 0x0F80 },             // 0F63 0F90 0F80
                { 0xF32D, 0x0F63, 0x0F90, 0x0F74 },             // 0F63 0F90 0F74
                { 0xF32E, 0x0F63, 0x0F90, 0x0F7A },             // 0F63 0F90 0F7A
                { 0xF32F, 0x0F63, 0x0F90, 0x0F7C },             // 0F63 0F90 0F7C
                { 0xF330, 0x0F66, 0x0F90 },             // 0F66 0F90
                { 0xF331, 0x0F66, 0x0F90, 0x0F72 },             // 0F66 0F90 0F72
                { 0xF332, 0x0F66, 0x0F90, 0x0F80 },             // 0F66 0F90 0F80
                { 0xF333, 0x0F66, 0x0F90, 0x0F74 },             // 0F66 0F90 0F74
                { 0xF334, 0x0F66, 0x0F90, 0x0F7A },             // 0F66 0F90 0F7A
                { 0xF335, 0x0F66, 0x0F90, 0x0F7C },             // 0F66 0F90 0F7C
                { 0xF336, 0x0F66, 0x0F90, 0x0FB1 },             // 0F66 0F90 0FB1
                { 0xF337, 0x0F66, 0x0F90, 0x0FB1, 0x0F72 },             // 0F66 0F90 0FB1 0F72
                { 0xF338, 0x0F66, 0x0F90, 0x0FB1, 0x0F80 },             // 0F66 0F90 0FB1 0F80
                { 0xF339, 0x0F66, 0x0F90, 0x0FB1, 0x0F74 },             // 0F66 0F90 0FB1 0F74
                { 0xF33A, 0x0F66, 0x0F90, 0x0FB1, 0x0F7A },             // 0F66 0F90 0FB1 0F7A
                { 0xF33B, 0x0F66, 0x0F90, 0x0FB1, 0x0F7C },             // 0F66 0F90 0FB1 0F7C
                { 0xF33C, 0x0F66, 0x0F90, 0x0FB2 },             // 0F66 0F90 0FB2
                { 0xF33D, 0x0F66, 0x0F90, 0x0FB2, 0x0F72 },             // 0F66 0F90 0FB2 0F72
                { 0xF33E, 0x0F66, 0x0F90, 0x0FB2, 0x0F80 },             // 0F66 0F90 0FB2 0F80
                { 0xF33F, 0x0F66, 0x0F90, 0x0FB2, 0x0F74 },             // 0F66 0F90 0FB2 0F74
                { 0xF340, 0x0F66, 0x0F90, 0x0FB2, 0x0F7A },             // 0F66 0F90 0FB2 0F7A
                { 0xF341, 0x0F66, 0x0F90, 0x0FB2, 0x0F7C },             // 0F66 0F90 0FB2 0F7C
                { 0xF342, 0x0F41, 0x0F71 },             // 0F41 0F71
                { 0xF343, 0x0F41, 0x0F72 },             // 0F41 0F72
                { 0xF344, 0x0F41, 0x0F80 },             // 0F41 0F80
                { 0xF345, 0x0F41, 0x0F74 },             // 0F41 0F74
                { 0xF346, 0x0F41, 0x0F7A },             // 0F41 0F7A
                { 0xF347, 0x0F41, 0x0F7C },             // 0F41 0F7C
                { 0xF348, 0x0F41, 0x0FB1 },             // 0F41 0FB1
                { 0xF349, 0x0F41, 0x0FB1, 0x0F72 },             // 0F41 0FB1 0F72
                { 0xF34A, 0x0F41, 0x0FB1, 0x0F80 },             // 0F41 0FB1 0F80
                { 0xF34B, 0x0F41, 0x0FB1, 0x0F74 },             // 0F41 0FB1 0F74
                { 0xF34C, 0x0F41, 0x0FB1, 0x0F7A },             // 0F41 0FB1 0F7A
                { 0xF34D, 0x0F41, 0x0FB1, 0x0F7C },             // 0F41 0FB1 0F7C
                { 0xF34E, 0x0F41, 0x0FB2 },             // 0F41 0FB2
                { 0xF34F, 0x0F41, 0x0FB2, 0x0F72 },             // 0F41 0FB2 0F72
                { 0xF350, 0x0F41, 0x0FB2, 0x0F80 },             // 0F41 0FB2 0F80
                { 0xF351, 0x0F41, 0x0FB2, 0x0F74 },             // 0F41 0FB2 0F74
                { 0xF352, 0x0F41, 0x0FB2, 0x0F7A },             // 0F41 0FB2 0F7A
                { 0xF353, 0x0F41, 0x0FB2, 0x0F7C },             // 0F41 0FB2 0F7C
                { 0xF354, 0x0F41, 0x0FAD },             // 0F41 0FAD
                { 0xF355, 0x0F41, 0x0FAD, 0x0F72 },             // 0F41 0FAD 0F72
                { 0xF356, 0x0F41, 0x0FAD, 0x0F80 },             // 0F41 0FAD 0F80
                { 0xF357, 0x0F41, 0x0FAD, 0x0F74 },             // 0F41 0FAD 0F74
                { 0xF358, 0x0F41, 0x0FAD, 0x0F7A },             // 0F41 0FAD 0F7A
                { 0xF359, 0x0F41, 0x0FAD, 0x0F7C },             // 0F41 0FAD 0F7C
                { 0xF35A, 0x0F42, 0x0F71 },             // 0F42 0F71
                { 0xF35B, 0x0F42, 0x0F72 },             // 0F42 0F72
                { 0xF35C, 0x0F42, 0x0F80 },             // 0F42 0F80
                { 0xF35D, 0x0F42, 0x0F74 },             // 0F42 0F74
                { 0xF35E, 0x0F42, 0x0F7A },             // 0F42 0F7A
                { 0xF35F, 0x0F42, 0x0F7C },             // 0F42 0F7C
                { 0xF360, 0x0F42, 0x0FB1 },             // 0F42 0FB1
                { 0xF361, 0x0F42, 0x0FB1, 0x0F72 },             // 0F42 0FB1 0F72
                { 0xF362, 0x0F42, 0x0FB1, 0x0F80 },             // 0F42 0FB1 0F80
                { 0xF363, 0x0F42, 0x0FB1, 0x0F74 },             // 0F42 0FB1 0F74
                { 0xF364, 0x0F42, 0x0FB1, 0x0F7A },             // 0F42 0FB1 0F7A
                { 0xF365, 0x0F42, 0x0FB1, 0x0F7C },             // 0F42 0FB1 0F7C
                { 0xF366, 0x0F42, 0x0FB2 },             // 0F42 0FB2
                { 0xF367, 0x0F42, 0x0FB2, 0x0F72 },             // 0F42 0FB2 0F72
                { 0xF368, 0x0F42, 0x0FB2, 0x0F80 },             // 0F42 0FB2 0F80
                { 0xF369, 0x0F42, 0x0FB2, 0x0F74 },             // 0F42 0FB2 0F74
                { 0xF36A, 0x0F42, 0x0FB2, 0x0F7A },             // 0F42 0FB2 0F7A
                { 0xF36B, 0x0F42, 0x0FB2, 0x0F7C },             // 0F42 0FB2 0F7C
                { 0xF36C, 0x0F42, 0x0FB2, 0x0FAD },             // 0F42 0FB2 0FAD
                { 0xF36D, 0x0F42, 0x0FB3 },             // 0F42 0FB3
                { 0xF36E, 0x0F42, 0x0FB3, 0x0F72 },             // 0F42 0FB3 0F72
                { 0xF36F, 0x0F42, 0x0FB3, 0x0F80 },             // 0F42 0FB3 0F80
                { 0xF370, 0x0F42, 0x0FB3, 0x0F74 },             // 0F42 0FB3 0F74
                { 0xF371, 0x0F42, 0x0FB3, 0x0F7A },             // 0F42 0FB3 0F7A
                { 0xF372, 0x0F42, 0x0FB3, 0x0F7C },             // 0F42 0FB3 0F7C
                { 0xF373, 0x0F42, 0x0FAD },             // 0F42 0FAD
                { 0xF374, 0x0F62, 0x0F92 },             // 0F62 0F92
                { 0xF375, 0x0F62, 0x0F92, 0x0F72 },             // 0F62 0F92 0F72
                { 0xF376, 0x0F62, 0x0F92, 0x0F80 },             // 0F62 0F92 0F80
                { 0xF377, 0x0F62, 0x0F92, 0x0F74 },             // 0F62 0F92 0F74
                { 0xF378, 0x0F62, 0x0F92, 0x0F7A },             // 0F62 0F92 0F7A
                { 0xF379, 0x0F62, 0x0F92, 0x0F7C },             // 0F62 0F92 0F7C
                { 0xF37A, 0x0F62, 0x0F92, 0x0FB1 },             // 0F62 0F92 0FB1
                { 0xF37B, 0x0F62, 0x0F92, 0x0FB1, 0x0F72 },             // 0F62 0F92 0FB1 0F72
                { 0xF37C, 0x0F62, 0x0F92, 0x0FB1, 0x0F80 },             // 0F62 0F92 0FB1 0F80
                { 0xF37D, 0x0F62, 0x0F92, 0x0FB1, 0x0F74 },             // 0F62 0F92 0FB1 0F74
                { 0xF37E, 0x0F62, 0x0F92, 0x0FB1, 0x0F7A },             // 0F62 0F92 0FB1 0F7A
                { 0xF37F, 0x0F62, 0x0F92, 0x0FB1, 0x0F7C },             // 0F62 0F92 0FB1 0F7C
                { 0xF380, 0x0F63, 0x0F92 },             // 0F63 0F92
                { 0xF381, 0x0F63, 0x0F92, 0x0F72 },             // 0F63 0F92 0F72
                { 0xF382, 0x0F63, 0x0F92, 0x0F80 },             // 0F63 0F92 0F80
                { 0xF383, 0x0F63, 0x0F92, 0x0F74 },             // 0F63 0F92 0F74
                { 0xF384, 0x0F63, 0x0F92, 0x0F7A },             // 0F63 0F92 0F7A
                { 0xF385, 0x0F63, 0x0F92, 0x0F7C },             // 0F63 0F92 0F7C
                { 0xF386, 0x0F66, 0x0F92 },             // 0F66 0F92
                { 0xF387, 0x0F66, 0x0F92, 0x0F72 },             // 0F66 0F92 0F72
                { 0xF388, 0x0F66, 0x0F92, 0x0F80 },             // 0F66 0F92 0F80
                { 0xF389, 0x0F66, 0x0F92, 0x0F74 },             // 0F66 0F92 0F74
                { 0xF38A, 0x0F66, 0x0F92, 0x0F7A },             // 0F66 0F92 0F7A
                { 0xF38B, 0x0F66, 0x0F92, 0x0F7C },             // 0F66 0F92 0F7C
                { 0xF38C, 0x0F66, 0x0F92, 0x0FB1 },             // 0F66 0F92 0FB1
                { 0xF38D, 0x0F66, 0x0F92, 0x0FB1, 0x0F72 },             // 0F66 0F92 0FB1 0F72
                { 0xF38E, 0x0F66, 0x0F92, 0x0FB1, 0x0F80 },             // 0F66 0F92 0FB1 0F80
                { 0xF38F, 0x0F66, 0x0F92, 0x0FB1, 0x0F74 },             // 0F66 0F92 0FB1 0F74
                { 0xF390, 0x0F66, 0x0F92, 0x0FB1, 0x0F7A },             // 0F66 0F92 0FB1 0F7A
                { 0xF391, 0x0F66, 0x0F92, 0x0FB1, 0x0F7C },             // 0F66 0F92 0FB1 0F7C
                { 0xF392, 0x0F66, 0x0F92, 0x0FB2 },             // 0F66 0F92 0FB2
                { 0xF393, 0x0F66, 0x0F92, 0x0FB2, 0x0F72 },             // 0F66 0F92 0FB2 0F72
                { 0xF394, 0x0F66, 0x0F92, 0x0FB2, 0x0F80 },             // 0F66 0F92 0FB2 0F80
                { 0xF395, 0x0F66, 0x0F92, 0x0FB2, 0x0F74 },             // 0F66 0F92 0FB2 0F74
                { 0xF396, 0x0F66, 0x0F92, 0x0FB2, 0x0F7A },             // 0F66 0F92 0FB2 0F7A
                { 0xF397, 0x0F66, 0x0F92, 0x0FB2, 0x0F7C },             // 0F66 0F92 0FB2 0F7C
                { 0xF398, 0x0F42, 0x0FB7, 0x0F71 },             // 0F42 0FB7 0F71
                { 0xF399, 0x0F42, 0x0FB7, 0x0F72 },             // 0F42 0FB7 0F72
                { 0xF39A, 0x0F42, 0x0FB7, 0x0F80 },             // 0F42 0FB7 0F80
                { 0xF39B, 0x0F42, 0x0FB7, 0x0F74 },             // 0F42 0FB7 0F74
                { 0xF39C, 0x0F42, 0x0FB7, 0x0F7A },             // 0F42 0FB7 0F7A
                { 0xF39D, 0x0F42, 0x0FB7, 0x0F7C },             // 0F42 0FB7 0F7C
                { 0xF39E, 0x0F44, 0x0F71 },             // 0F44 0F71
                { 0xF39F, 0x0F44, 0x0F72 },             // 0F44 0F72
                { 0xF3A0, 0x0F44, 0x0F80 },             // 0F44 0F80
                { 0xF3A1, 0x0F44, 0x0F74 },             // 0F44 0F74
                { 0xF3A2, 0x0F44, 0x0F7A },             // 0F44 0F7A
                { 0xF3A3, 0x0F44, 0x0F7C },             // 0F44 0F7C
                { 0xF3A4, 0x0F62, 0x0F94 },             // 0F62 0F94
                { 0xF3A5, 0x0F62, 0x0F94, 0x0F72 },             // 0F62 0F94 0F72
                { 0xF3A6, 0x0F62, 0x0F94, 0x0F80 },             // 0F62 0F94 0F80
                { 0xF3A7, 0x0F62, 0x0F94, 0x0F74 },             // 0F62 0F94 0F74
                { 0xF3A8, 0x0F62, 0x0F94, 0x0F7A },             // 0F62 0F94 0F7A
                { 0xF3A9, 0x0F62, 0x0F94, 0x0F7C },             // 0F62 0F94 0F7C
                { 0xF3AA, 0x0F63, 0x0F94 },             // 0F63 0F94
                { 0xF3AB, 0x0F63, 0x0F94, 0x0F72 },             // 0F63 0F94 0F72
                { 0xF3AC, 0x0F63, 0x0F94, 0x0F80 },             // 0F63 0F94 0F80
                { 0xF3AD, 0x0F63, 0x0F94, 0x0F74 },             // 0F63 0F94 0F74
                { 0xF3AE, 0x0F63, 0x0F94, 0x0F7A },             // 0F63 0F94 0F7A
                { 0xF3AF, 0x0F63, 0x0F94, 0x0F7C },             // 0F63 0F94 0F7C
                { 0xF3B0, 0x0F66, 0x0F94 },             // 0F66 0F94
                { 0xF3B1, 0x0F66, 0x0F94, 0x0F72 },             // 0F66 0F94 0F72
                { 0xF3B2, 0x0F66, 0x0F94, 0x0F80 },             // 0F66 0F94 0F80
                { 0xF3B3, 0x0F66, 0x0F94, 0x0F74 },             // 0F66 0F94 0F74
                { 0xF3B4, 0x0F66, 0x0F94, 0x0F7A },             // 0F66 0F94 0F7A
                { 0xF3B5, 0x0F66, 0x0F94, 0x0F7C },             // 0F66 0F94 0F7C
                { 0xF3B6, 0x0F45, 0x0F71 },             // 0F45 0F71
                { 0xF3B7, 0x0F45, 0x0F72 },             // 0F45 0F72
                { 0xF3B8, 0x0F45, 0x0F80 },             // 0F45 0F80
                { 0xF3B9, 0x0F45, 0x0F74 },             // 0F45 0F74
                { 0xF3BA, 0x0F45, 0x0F71, 0x0F74 },             // 0F45 0F71 0F74
                { 0xF3BB, 0x0F45, 0x0F7A },             // 0F45 0F7A
                { 0xF3BC, 0x0F45, 0x0F7C },             // 0F45 0F7C
                { 0xF3BD, 0x0F45, 0x0F7E },             // 0F45 0F7E
                { 0xF3BE, 0x0F63, 0x0F95 },             // 0F63 0F95
                { 0xF3BF, 0x0F63, 0x0F95, 0x0F72 },             // 0F63 0F95 0F72
                { 0xF3C0, 0x0F63, 0x0F95, 0x0F80 },             // 0F63 0F95 0F80
                { 0xF3C1, 0x0F63, 0x0F95, 0x0F74 },             // 0F63 0F95 0F74
                { 0xF3C2, 0x0F63, 0x0F95, 0x0F7A },             // 0F63 0F95 0F7A
                { 0xF3C3, 0x0F63, 0x0F95, 0x0F7C },             // 0F63 0F95 0F7C
                { 0xF3C4, 0x0F45, 0x0F92, 0x0F72 },             // 0F45 0F92 0F72
                { 0xF3C5, 0x0F45, 0x0F94, 0x0F7C },             // 0F45 0F94 0F7C
                { 0xF3C6, 0x0F45, 0x0FB1, 0x0F7C },             // 0F45 0FB1 0F7C
                { 0xF3C7, 0x0F45, 0x0FAD, 0x0F72 },             // 0F45 0FAD 0F72
                { 0xF3C8, 0x0F45, 0x0FAD, 0x0F7C },             // 0F45 0FAD 0F7C
                { 0xF3C9, 0x0F46, 0x0F71 },             // 0F46 0F71
                { 0xF3CA, 0x0F46, 0x0F72 },             // 0F46 0F72
                { 0xF3CB, 0x0F46, 0x0F71, 0x0F72 },             // 0F46 0F71 0F72
                { 0xF3CC, 0x0F46, 0x0F80 },             // 0F46 0F80
                { 0xF3CD, 0x0F46, 0x0F74 },             // 0F46 0F74
                { 0xF3CE, 0x0F46, 0x0F7A },             // 0F46 0F7A
                { 0xF3CF, 0x0F46, 0x0F7C },             // 0F46 0F7C
                { 0xF3D0, 0x0F46, 0x0F71, 0x0F7C },             // 0F46 0F71 0F7C
                { 0xF3D1, 0x0F46, 0x0F7E },             // 0F46 0F7E
                { 0xF3D2, 0x0F46, 0x0F92, 0x0F72 },             // 0F46 0F92 0F72
                { 0xF3D3, 0x0F46, 0x0F92, 0x0F80 },             // 0F46 0F92 0F80
                { 0xF3D4, 0x0F46, 0x0FB6, 0x0F80 },             // 0F46 0FB6 0F80
                { 0xF3D5, 0x0F46, 0x0FB6, 0x0F7C },             // 0F46 0FB6 0F7C
                { 0xF3D6, 0x0F62, 0x0F96, 0x0F74 },             // 0F62 0F96 0F74
                { 0xF3D7, 0x0F63, 0x0F96, 0x0F80 },             // 0F63 0F96 0F80
                { 0xF3D8, 0x0F63, 0x0F96, 0x0F7C },             // 0F63 0F96 0F7C
                { 0xF3D9, 0x0F47, 0x0F71 },             // 0F47 0F71
                { 0xF3DA, 0x0F47, 0x0F72 },             // 0F47 0F72
                { 0xF3DB, 0x0F47, 0x0F80 },             // 0F47 0F80
                { 0xF3DC, 0x0F47, 0x0F74 },             // 0F47 0F74
                { 0xF3DD, 0x0F47, 0x0F7A },             // 0F47 0F7A
                { 0xF3DE, 0x0F47, 0x0F7C },             // 0F47 0F7C
                { 0xF3DF, 0x0F47, 0x0F7E },             // 0F47 0F7E
                { 0xF3E0, 0x0F62, 0x0F97 },             // 0F62 0F97
                { 0xF3E1, 0x0F62, 0x0F97, 0x0F72 },             // 0F62 0F97 0F72
                { 0xF3E2, 0x0F62, 0x0F97, 0x0F80 },             // 0F62 0F97 0F80
                { 0xF3E3, 0x0F62, 0x0F97, 0x0F74 },             // 0F62 0F97 0F74
                { 0xF3E4, 0x0F62, 0x0F97, 0x0F7A },             // 0F62 0F97 0F7A
                { 0xF3E5, 0x0F62, 0x0F97, 0x0F7C },             // 0F62 0F97 0F7C
                { 0xF3E6, 0x0F63, 0x0F97 },             // 0F63 0F97
                { 0xF3E7, 0x0F63, 0x0F97, 0x0F72 },             // 0F63 0F97 0F72
                { 0xF3E8, 0x0F63, 0x0F97, 0x0F80 },             // 0F63 0F97 0F80
                { 0xF3E9, 0x0F63, 0x0F97, 0x0F74 },             // 0F63 0F97 0F74
                { 0xF3EA, 0x0F63, 0x0F97, 0x0F7A },             // 0F63 0F97 0F7A
                { 0xF3EB, 0x0F63, 0x0F97, 0x0F7C },             // 0F63 0F97 0F7C
                { 0xF3EC, 0x0F49, 0x0F71 },             // 0F49 0F71
                { 0xF3ED, 0x0F49, 0x0F72 },             // 0F49 0F72
                { 0xF3EE, 0x0F49, 0x0F80 },             // 0F49 0F80
                { 0xF3EF, 0x0F49, 0x0F74 },             // 0F49 0F74
                { 0xF3F0, 0x0F49, 0x0F7A },             // 0F49 0F7A
                { 0xF3F1, 0x0F49, 0x0F7C },             // 0F49 0F7C
                { 0xF3F2, 0x0F49, 0x0FAD },             // 0F49 0FAD
                { 0xF3F3, 0x0F62, 0x0F99 },             // 0F62 0F99
                { 0xF3F4, 0x0F62, 0x0F99, 0x0F72 },             // 0F62 0F99 0F72
                { 0xF3F5, 0x0F62, 0x0F99, 0x0F80 },             // 0F62 0F99 0F80
                { 0xF3F6, 0x0F62, 0x0F99, 0x0F74 },             // 0F62 0F99 0F74
                { 0xF3F7, 0x0F62, 0x0F99, 0x0F7A },             // 0F62 0F99 0F7A
                { 0xF3F8, 0x0F62, 0x0F99, 0x0F7C },             // 0F62 0F99 0F7C
                { 0xF3F9, 0x0F66, 0x0F99 },             // 0F66 0F99
                { 0xF3FA, 0x0F66, 0x0F99, 0x0F72 },             // 0F66 0F99 0F72
                { 0xF3FB, 0x0F66, 0x0F99, 0x0F80 },             // 0F66 0F99 0F80
                { 0xF3FC, 0x0F66, 0x0F99, 0x0F74 },             // 0F66 0F99 0F74
                { 0xF3FD, 0x0F66, 0x0F99, 0x0F7A },             // 0F66 0F99 0F7A
                { 0xF3FE, 0x0F66, 0x0F99, 0x0F7C },             // 0F66 0F99 0F7C
                { 0xF3FF, 0x0F4A, 0x0F71 },             // 0F4A 0F71
                { 0xF400, 0x0F4A, 0x0F72 },             // 0F4A 0F72
                { 0xF401, 0x0F4A, 0x0F80 },             // 0F4A 0F80
                { 0xF402, 0x0F4A, 0x0F74 },             // 0F4A 0F74
                { 0xF403, 0x0F4A, 0x0F7A },             // 0F4A 0F7A
                { 0xF404, 0x0F4A, 0x0F7C },             // 0F4A 0F7C
                { 0xF405, 0x0F4B, 0x0F71 },             // 0F4B 0F71
                { 0xF406, 0x0F4B, 0x0F72 },             // 0F4B 0F72
                { 0xF407, 0x0F4B, 0x0F80 },             // 0F4B 0F80
                { 0xF408, 0x0F4B, 0x0F74 },             // 0F4B 0F74
                { 0xF409, 0x0F4B, 0x0F7A },             // 0F4B 0F7A
                { 0xF40A, 0x0F4B, 0x0F7C },             // 0F4B 0F7C
                { 0xF40B, 0x0F4C, 0x0F71 },             // 0F4C 0F71
                { 0xF40C, 0x0F4C, 0x0F72 },             // 0F4C 0F72
                { 0xF40D, 0x0F4C, 0x0F80 },             // 0F4C 0F80
                { 0xF40E, 0x0F4C, 0x0F74 },             // 0F4C 0F74
                { 0xF40F, 0x0F4C, 0x0F7A },             // 0F4C 0F7A
                { 0xF410, 0x0F4C, 0x0F7C },             // 0F4C 0F7C
                { 0xF411, 0x0F4C, 0x0FB7, 0x0F71 },             // 0F4C 0FB7 0F71
                { 0xF412, 0x0F4C, 0x0FB7, 0x0F72 },             // 0F4C 0FB7 0F72
                { 0xF413, 0x0F4C, 0x0FB7, 0x0F80 },             // 0F4C 0FB7 0F80
                { 0xF414, 0x0F4C, 0x0FB7, 0x0F74 },             // 0F4C 0FB7 0F74
                { 0xF415, 0x0F4C, 0x0FB7, 0x0F7A },             // 0F4C 0FB7 0F7A
                { 0xF416, 0x0F4C, 0x0FB7, 0x0F7C },             // 0F4C 0FB7 0F7C
                { 0xF417, 0x0F4E, 0x0F71 },             // 0F4E 0F71
                { 0xF418, 0x0F4E, 0x0F72 },             // 0F4E 0F72
                { 0xF419, 0x0F4E, 0x0F80 },             // 0F4E 0F80
                { 0xF41A, 0x0F4E, 0x0F74 },             // 0F4E 0F74
                { 0xF41B, 0x0F4E, 0x0F7A },             // 0F4E 0F7A
                { 0xF41C, 0x0F4E, 0x0F7C },             // 0F4E 0F7C
                { 0xF41D, 0x0F4F, 0x0F71 },             // 0F4F 0F71
                { 0xF41E, 0x0F4F, 0x0F72 },             // 0F4F 0F72
                { 0xF41F, 0x0F4F, 0x0F80 },             // 0F4F 0F80
                { 0xF420, 0x0F4F, 0x0F74 },             // 0F4F 0F74
                { 0xF421, 0x0F4F, 0x0F7A },             // 0F4F 0F7A
                { 0xF422, 0x0F4F, 0x0F7C },             // 0F4F 0F7C
                { 0xF423, 0x0F4F, 0x0FB2 },             // 0F4F 0FB2
                { 0xF424, 0x0F4F, 0x0FB2, 0x0F72 },             // 0F4F 0FB2 0F72
                { 0xF425, 0x0F4F, 0x0FB2, 0x0F80 },             // 0F4F 0FB2 0F80
                { 0xF426, 0x0F4F, 0x0FB2, 0x0F74 },             // 0F4F 0FB2 0F74
                { 0xF427, 0x0F4F, 0x0FB2, 0x0F7A },             // 0F4F 0FB2 0F7A
                { 0xF428, 0x0F4F, 0x0FB2, 0x0F7C },             // 0F4F 0FB2 0F7C
                { 0xF429, 0x0F62, 0x0F9F },             // 0F62 0F9F
                { 0xF42A, 0x0F62, 0x0F9F, 0x0F72 },             // 0F62 0F9F 0F72
                { 0xF42B, 0x0F62, 0x0F9F, 0x0F80 },             // 0F62 0F9F 0F80
                { 0xF42C, 0x0F62, 0x0F9F, 0x0F74 },             // 0F62 0F9F 0F74
                { 0xF42D, 0x0F62, 0x0F9F, 0x0F7A },             // 0F62 0F9F 0F7A
                { 0xF42E, 0x0F62, 0x0F9F, 0x0F7C },             // 0F62 0F9F 0F7C
                { 0xF42F, 0x0F63, 0x0F9F },             // 0F63 0F9F
                { 0xF430, 0x0F63, 0x0F9F, 0x0F72 },             // 0F63 0F9F 0F72
                { 0xF431, 0x0F63, 0x0F9F, 0x0F80 },             // 0F63 0F9F 0F80
                { 0xF432, 0x0F63, 0x0F9F, 0x0F74 },             // 0F63 0F9F 0F74
                { 0xF433, 0x0F63, 0x0F9F, 0x0F7A },             // 0F63 0F9F 0F7A
                { 0xF434, 0x0F63, 0x0F9F, 0x0F7C },             // 0F63 0F9F 0F7C
                { 0xF435, 0x0F66, 0x0F9F },             // 0F66 0F9F
                { 0xF436, 0x0F66, 0x0F9F, 0x0F72 },             // 0F66 0F9F 0F72
                { 0xF437, 0x0F66, 0x0F9F, 0x0F80 },             // 0F66 0F9F 0F80
                { 0xF438, 0x0F66, 0x0F9F, 0x0F74 },             // 0F66 0F9F 0F74
                { 0xF439, 0x0F66, 0x0F9F, 0x0F7A },             // 0F66 0F9F 0F7A
                { 0xF43A, 0x0F66, 0x0F9F, 0x0F7C },             // 0F66 0F9F 0F7C
                { 0xF43B, 0x0F50, 0x0F71 },             // 0F50 0F71
                { 0xF43C, 0x0F50, 0x0F72 },             // 0F50 0F72
                { 0xF43D, 0x0F50, 0x0F80 },             // 0F50 0F80
                { 0xF43E, 0x0F50, 0x0F74 },             // 0F50 0F74
                { 0xF43F, 0x0F50, 0x0F7A },             // 0F50 0F7A
                { 0xF440, 0x0F50, 0x0F7C },             // 0F50 0F7C
                { 0xF441, 0x0F50, 0x0FB2 },             // 0F50 0FB2
                { 0xF442, 0x0F50, 0x0FB2, 0x0F72 },             // 0F50 0FB2 0F72
                { 0xF443, 0x0F50, 0x0FB2, 0x0F80 },             // 0F50 0FB2 0F80
                { 0xF444, 0x0F50, 0x0FB2, 0x0F74 },             // 0F50 0FB2 0F74
                { 0xF445, 0x0F50, 0x0FB2, 0x0F7A },             // 0F50 0FB2 0F7A
                { 0xF446, 0x0F50, 0x0FB2, 0x0F7C },             // 0F50 0FB2 0F7C
                { 0xF447, 0x0F51, 0x0F71 },             // 0F51 0F71
                { 0xF448, 0x0F51, 0x0F72 },             // 0F51 0F72
                { 0xF449, 0x0F51, 0x0F80 },             // 0F51 0F80
                { 0xF44A, 0x0F51, 0x0F74 },             // 0F51 0F74
                { 0xF44B, 0x0F51, 0x0F7A },             // 0F51 0F7A
                { 0xF44C, 0x0F51, 0x0F7C },             // 0F51 0F7C
                { 0xF44D, 0x0F51, 0x0FB2 },             // 0F51 0FB2
                { 0xF44E, 0x0F51, 0x0FB2, 0x0F72 },             // 0F51 0FB2 0F72
                { 0xF44F, 0x0F51, 0x0FB2, 0x0F80 },             // 0F51 0FB2 0F80
                { 0xF450, 0x0F51, 0x0FB2, 0x0F74 },             // 0F51 0FB2 0F74
                { 0xF451, 0x0F51, 0x0FB2, 0x0F7A },             // 0F51 0FB2 0F7A
                { 0xF452, 0x0F51, 0x0FB2, 0x0F7C },             // 0F51 0FB2 0F7C
                { 0xF453, 0x0F51, 0x0FB2, 0x0FAD },             // 0F51 0FB2 0FAD
                { 0xF454, 0x0F51, 0x0FAD },             // 0F51 0FAD
                { 0xF455, 0x0F51, 0x0FAD, 0x0F72 },             // 0F51 0FAD 0F72
                { 0xF456, 0x0F51, 0x0FAD, 0x0F80 },             // 0F51 0FAD 0F80
                { 0xF457, 0x0F51, 0x0FAD, 0x0F74 },             // 0F51 0FAD 0F74
                { 0xF458, 0x0F51, 0x0FAD, 0x0F7A },             // 0F51 0FAD 0F7A
                { 0xF459, 0x0F51, 0x0FAD, 0x0F7C },             // 0F51 0FAD 0F7C
                { 0xF45A, 0x0F62, 0x0FA1 },             // 0F62 0FA1
                { 0xF45B, 0x0F62, 0x0FA1, 0x0F72 },             // 0F62 0FA1 0F72
                { 0xF45C, 0x0F62, 0x0FA1, 0x0F80 },             // 0F62 0FA1 0F80
                { 0xF45D, 0x0F62, 0x0FA1, 0x0F74 },             // 0F62 0FA1 0F74
                { 0xF45E, 0x0F62, 0x0FA1, 0x0F7A },             // 0F62 0FA1 0F7A
                { 0xF45F, 0x0F62, 0x0FA1, 0x0F7C },             // 0F62 0FA1 0F7C
                { 0xF460, 0x0F63, 0x0FA1 },             // 0F63 0FA1
                { 0xF461, 0x0F63, 0x0FA1, 0x0F72 },             // 0F63 0FA1 0F72
                { 0xF462, 0x0F63, 0x0FA1, 0x0F80 },             // 0F63 0FA1 0F80
                { 0xF463, 0x0F63, 0x0FA1, 0x0F74 },             // 0F63 0FA1 0F74
                { 0xF464, 0x0F63, 0x0FA1, 0x0F7A },             // 0F63 0FA1 0F7A
                { 0xF465, 0x0F63, 0x0FA1, 0x0F7C },             // 0F63 0FA1 0F7C
                { 0xF466, 0x0F66, 0x0FA1 },             // 0F66 0FA1
                { 0xF467, 0x0F66, 0x0FA1, 0x0F72 },             // 0F66 0FA1 0F72
                { 0xF468, 0x0F66, 0x0FA1, 0x0F80 },             // 0F66 0FA1 0F80
                { 0xF469, 0x0F66, 0x0FA1, 0x0F74 },             // 0F66 0FA1 0F74
                { 0xF46A, 0x0F66, 0x0FA1, 0x0F7A },             // 0F66 0FA1 0F7A
                { 0xF46B, 0x0F66, 0x0FA1, 0x0F7C },             // 0F66 0FA1 0F7C
                { 0xF46C, 0x0F51, 0x0FB7, 0x0F71 },             // 0F51 0FB7 0F71
                { 0xF46D, 0x0F51, 0x0FB7, 0x0F72 },             // 0F51 0FB7 0F72
                { 0xF46E, 0x0F51, 0x0FB7, 0x0F80 },             // 0F51 0FB7 0F80
                { 0xF46F, 0x0F51, 0x0FB7, 0x0F74 },             // 0F51 0FB7 0F74
                { 0xF470, 0x0F51, 0x0FB7, 0x0F7A },             // 0F51 0FB7 0F7A
                { 0xF471, 0x0F51, 0x0FB7, 0x0F7C },             // 0F51 0FB7 0F7C
                { 0xF472, 0x0F53, 0x0F71 },             // 0F53 0F71
                { 0xF473, 0x0F53, 0x0F72 },             // 0F53 0F72
                { 0xF474, 0x0F53, 0x0F80 },             // 0F53 0F80
                { 0xF475, 0x0F53, 0x0F74 },             // 0F53 0F74
                { 0xF476, 0x0F53, 0x0F7A },             // 0F53 0F7A
                { 0xF477, 0x0F53, 0x0F7C },             // 0F53 0F7C
                { 0xF478, 0x0F62, 0x0FA3 },             // 0F62 0FA3
                { 0xF479, 0x0F62, 0x0FA3, 0x0F72 },             // 0F62 0FA3 0F72
                { 0xF47A, 0x0F62, 0x0FA3, 0x0F80 },             // 0F62 0FA3 0F80
                { 0xF47B, 0x0F62, 0x0FA3, 0x0F74 },             // 0F62 0FA3 0F74
                { 0xF47C, 0x0F62, 0x0FA3, 0x0F7A },             // 0F62 0FA3 0F7A
                { 0xF47D, 0x0F62, 0x0FA3, 0x0F7C },             // 0F62 0FA3 0F7C
                { 0xF47E, 0x0F66, 0x0FA3 },             // 0F66 0FA3
                { 0xF47F, 0x0F66, 0x0FA3, 0x0F72 },             // 0F66 0FA3 0F72
                { 0xF480, 0x0F66, 0x0FA3, 0x0F80 },             // 0F66 0FA3 0F80
                { 0xF481, 0x0F66, 0x0FA3, 0x0F74 },             // 0F66 0FA3 0F74
                { 0xF482, 0x0F66, 0x0FA3, 0x0F7A },             // 0F66 0FA3 0F7A
                { 0xF483, 0x0F66, 0x0FA3, 0x0F7C },             // 0F66 0FA3 0F7C
                { 0xF484, 0x0F66, 0x0FA3, 0x0FB2 },             // 0F66 0FA3 0FB2
                { 0xF485, 0x0F66, 0x0FA3, 0x0FB2, 0x0F72 },             // 0F66 0FA3 0FB2 0F72
                { 0xF486, 0x0F66, 0x0FA3, 0x0FB2, 0x0F80 },             // 0F66 0FA3 0FB2 0F80
                { 0xF487, 0x0F66, 0x0FA3, 0x0FB2, 0x0F74 },             // 0F66 0FA3 0FB2 0F74
                { 0xF488, 0x0F66, 0x0FA3, 0x0FB2, 0x0F7A },             // 0F66 0FA3 0FB2 0F7A
                { 0xF489, 0x0F66, 0x0FA3, 0x0FB2, 0x0F7C },             // 0F66 0FA3 0FB2 0F7C
                { 0xF48A, 0x0F54, 0x0F71 },             // 0F54 0F71
                { 0xF48B, 0x0F54, 0x0F72 },             // 0F54 0F72
                { 0xF48C, 0x0F54, 0x0F80 },             // 0F54 0F80
                { 0xF48D, 0x0F54, 0x0F74 },             // 0F54 0F74
                { 0xF48E, 0x0F54, 0x0F7A },             // 0F54 0F7A
                { 0xF48F, 0x0F54, 0x0F7C },             // 0F54 0F7C
                { 0xF490, 0x0F54, 0x0FB1 },             // 0F54 0FB1
                { 0xF491, 0x0F54, 0x0FB1, 0x0F72 },             // 0F54 0FB1 0F72
                { 0xF492, 0x0F54, 0x0FB1, 0x0F80 },             // 0F54 0FB1 0F80
                { 0xF493, 0x0F54, 0x0FB1, 0x0F74 },             // 0F54 0FB1 0F74
                { 0xF494, 0x0F54, 0x0FB1, 0x0F7A },             // 0F54 0FB1 0F7A
                { 0xF495, 0x0F54, 0x0FB1, 0x0F7C },             // 0F54 0FB1 0F7C
                { 0xF496, 0x0F54, 0x0FB2 },             // 0F54 0FB2
                { 0xF497, 0x0F54, 0x0FB2, 0x0F72 },             // 0F54 0FB2 0F72
                { 0xF498, 0x0F54, 0x0FB2, 0x0F80 },             // 0F54 0FB2 0F80
                { 0xF499, 0x0F54, 0x0FB2, 0x0F74 },             // 0F54 0FB2 0F74
                { 0xF49A, 0x0F54, 0x0FB2, 0x0F7A },             // 0F54 0FB2 0F7A
                { 0xF49B, 0x0F54, 0x0FB2, 0x0F7C },             // 0F54 0FB2 0F7C
                { 0xF49C, 0x0F63, 0x0FA4 },             // 0F63 0FA4
                { 0xF49D, 0x0F63, 0x0FA4, 0x0F72 },             // 0F63 0FA4 0F72
                { 0xF49E, 0x0F63, 0x0FA4, 0x0F80 },             // 0F63 0FA4 0F80
                { 0xF49F, 0x0F63, 0x0FA4, 0x0F74 },             // 0F63 0FA4 0F74
                { 0xF4A0, 0x0F63, 0x0FA4, 0x0F7A },             // 0F63 0FA4 0F7A
                { 0xF4A1, 0x0F63, 0x0FA4, 0x0F7C },             // 0F63 0FA4 0F7C
                { 0xF4A2, 0x0F66, 0x0FA4 },             // 0F66 0FA4
                { 0xF4A3, 0x0F66, 0x0FA4, 0x0F72 },             // 0F66 0FA4 0F72
                { 0xF4A4, 0x0F66, 0x0FA4, 0x0F80 },             // 0F66 0FA4 0F80
                { 0xF4A5, 0x0F66, 0x0FA4, 0x0F74 },             // 0F66 0FA4 0F74
                { 0xF4A6, 0x0F66, 0x0FA4, 0x0F7A },             // 0F66 0FA4 0F7A
                { 0xF4A7, 0x0F66, 0x0FA4, 0x0F7C },             // 0F66 0FA4 0F7C
                { 0xF4A8, 0x0F66, 0x0FA4, 0x0FB1 },             // 0F66 0FA4 0FB1
                { 0xF4A9, 0x0F66, 0x0FA4, 0x0FB1, 0x0F72 },             // 0F66 0FA4 0FB1 0F72
                { 0xF4AA, 0x0F66, 0x0FA4, 0x0FB1, 0x0F80 },             // 0F66 0FA4 0FB1 0F80
                { 0xF4AB, 0x0F66, 0x0FA4, 0x0FB1, 0x0F74 },             // 0F66 0FA4 0FB1 0F74
                { 0xF4AC, 0x0F66, 0x0FA4, 0x0FB1, 0x0F7A },             // 0F66 0FA4 0FB1 0F7A
                { 0xF4AD, 0x0F66, 0x0FA4, 0x0FB1, 0x0F7C },             // 0F66 0FA4 0FB1 0F7C
                { 0xF4AE, 0x0F66, 0x0FA4, 0x0FB2 },             // 0F66 0FA4 0FB2
                { 0xF4AF, 0x0F66, 0x0FA4, 0x0FB2, 0x0F72 },             // 0F66 0FA4 0FB2 0F72
                { 0xF4B0, 0x0F66, 0x0FA4, 0x0FB2, 0x0F80 },             // 0F66 0FA4 0FB2 0F80
                { 0xF4B1, 0x0F66, 0x0FA4, 0x0FB2, 0x0F74 },             // 0F66 0FA4 0FB2 0F74
                { 0xF4B2, 0x0F66, 0x0FA4, 0x0FB2, 0x0F7A },             // 0F66 0FA4 0FB2 0F7A
                { 0xF4B3, 0x0F66, 0x0FA4, 0x0FB2, 0x0F7C },             // 0F66 0FA4 0FB2 0F7C
                { 0xF4B4, 0x0F55, 0x0F71 },             // 0F55 0F71
                { 0xF4B5, 0x0F55, 0x0F72 },             // 0F55 0F72
                { 0xF4B6, 0x0F55, 0x0F80 },             // 0F55 0F80
                { 0xF4B7, 0x0F55, 0x0F74 },             // 0F55 0F74
                { 0xF4B8, 0x0F55, 0x0F7A },             // 0F55 0F7A
                { 0xF4B9, 0x0F55, 0x0F7C },             // 0F55 0F7C
                { 0xF4BA, 0x0F55, 0x0FB1 },             // 0F55 0FB1
                { 0xF4BB, 0x0F55, 0x0FB1, 0x0F72 },             // 0F55 0FB1 0F72
                { 0xF4BC, 0x0F55, 0x0FB1, 0x0F80 },             // 0F55 0FB1 0F80
                { 0xF4BD, 0x0F55, 0x0FB1, 0x0F74 },             // 0F55 0FB1 0F74
                { 0xF4BE, 0x0F55, 0x0FB1, 0x0F7A },             // 0F55 0FB1 0F7A
                { 0xF4BF, 0x0F55, 0x0FB1, 0x0F7C },             // 0F55 0FB1 0F7C
                { 0xF4C0, 0x0F55, 0x0FB1, 0x0FAD },             // 0F55 0FB1 0FAD
                { 0xF4C1, 0x0F55, 0x0FB2 },             // 0F55 0FB2
                { 0xF4C2, 0x0F55, 0x0FB2, 0x0F72 },             // 0F55 0FB2 0F72
                { 0xF4C3, 0x0F55, 0x0FB2, 0x0F80 },             // 0F55 0FB2 0F80
                { 0xF4C4, 0x0F55, 0x0FB2, 0x0F74 },             // 0F55 0FB2 0F74
                { 0xF4C5, 0x0F55, 0x0FB2, 0x0F7A },             // 0F55 0FB2 0F7A
                { 0xF4C6, 0x0F55, 0x0FB2, 0x0F7C },             // 0F55 0FB2 0F7C
                { 0xF4C7, 0x0F67, 0x0FA5 },             // 0F67 0FA5
                { 0xF4C8, 0x0F67, 0x0FA5, 0x0F71 },             // 0F67 0FA5 0F71
                { 0xF4C9, 0x0F67, 0x0FA5, 0x0F72 },             // 0F67 0FA5 0F72
                { 0xF4CA, 0x0F67, 0x0FA5, 0x0F74 },             // 0F67 0FA5 0F74
                { 0xF4CB, 0x0F67, 0x0FA5, 0x0F7A },             // 0F67 0FA5 0F7A
                { 0xF4CC, 0x0F67, 0x0FA5, 0x0F7C },             // 0F67 0FA5 0F7C
                { 0xF4CD, 0x0F56, 0x0F71 },             // 0F56 0F71
                { 0xF4CE, 0x0F56, 0x0F72 },             // 0F56 0F72
                { 0xF4CF, 0x0F56, 0x0F80 },             // 0F56 0F80
                { 0xF4D0, 0x0F56, 0x0F74 },             // 0F56 0F74
                { 0xF4D1, 0x0F56, 0x0F7A },             // 0F56 0F7A
                { 0xF4D2, 0x0F56, 0x0F7C },             // 0F56 0F7C
                { 0xF4D3, 0x0F56, 0x0FB1 },             // 0F56 0FB1
                { 0xF4D4, 0x0F56, 0x0FB1, 0x0F72 },             // 0F56 0FB1 0F72
                { 0xF4D5, 0x0F56, 0x0FB1, 0x0F80 },             // 0F56 0FB1 0F80
                { 0xF4D6, 0x0F56, 0x0FB1, 0x0F74 },             // 0F56 0FB1 0F74
                { 0xF4D7, 0x0F56, 0x0FB1, 0x0F7A },             // 0F56 0FB1 0F7A
                { 0xF4D8, 0x0F56, 0x0FB1, 0x0F7C },             // 0F56 0FB1 0F7C
                { 0xF4D9, 0x0F56, 0x0FB2 },             // 0F56 0FB2
                { 0xF4DA, 0x0F56, 0x0FB2, 0x0F72 },             // 0F56 0FB2 0F72
                { 0xF4DB, 0x0F56, 0x0FB2, 0x0F80 },             // 0F56 0FB2 0F80
                { 0xF4DC, 0x0F56, 0x0FB2, 0x0F74 },             // 0F56 0FB2 0F74
                { 0xF4DD, 0x0F56, 0x0FB2, 0x0F7A },             // 0F56 0FB2 0F7A
                { 0xF4DE, 0x0F56, 0x0FB2, 0x0F7C },             // 0F56 0FB2 0F7C
                { 0xF4DF, 0x0F56, 0x0FB3 },             // 0F56 0FB3
                { 0xF4E0, 0x0F56, 0x0FB3, 0x0F72 },             // 0F56 0FB3 0F72
                { 0xF4E1, 0x0F56, 0x0FB3, 0x0F80 },             // 0F56 0FB3 0F80
                { 0xF4E2, 0x0F56, 0x0FB3, 0x0F74 },             // 0F56 0FB3 0F74
                { 0xF4E3, 0x0F56, 0x0FB3, 0x0F7A },             // 0F56 0FB3 0F7A
                { 0xF4E4, 0x0F56, 0x0FB3, 0x0F7C },             // 0F56 0FB3 0F7C
                { 0xF4E5, 0x0F62, 0x0FA6 },             // 0F62 0FA6
                { 0xF4E6, 0x0F62, 0x0FA6, 0x0F72 },             // 0F62 0FA6 0F72
                { 0xF4E7, 0x0F62, 0x0FA6, 0x0F80 },             // 0F62 0FA6 0F80
                { 0xF4E8, 0x0F62, 0x0FA6, 0x0F74 },             // 0F62 0FA6 0F74
                { 0xF4E9, 0x0F62, 0x0FA6, 0x0F7A },             // 0F62 0FA6 0F7A
                { 0xF4EA, 0x0F62, 0x0FA6, 0x0F7C },             // 0F62 0FA6 0F7C
                { 0xF4EB, 0x0F63, 0x0FA6 },             // 0F63 0FA6
                { 0xF4EC, 0x0F63, 0x0FA6, 0x0F72 },             // 0F63 0FA6 0F72
                { 0xF4ED, 0x0F63, 0x0FA6, 0x0F80 },             // 0F63 0FA6 0F80
                { 0xF4EE, 0x0F63, 0x0FA6, 0x0F74 },             // 0F63 0FA6 0F74
                { 0xF4EF, 0x0F63, 0x0FA6, 0x0F7A },             // 0F63 0FA6 0F7A
                { 0xF4F0, 0x0F63, 0x0FA6, 0x0F7C },             // 0F63 0FA6 0F7C
                { 0xF4F1, 0x0F66, 0x0FA6 },             // 0F66 0FA6
                { 0xF4F2, 0x0F66, 0x0FA6, 0x0F72 },             // 0F66 0FA6 0F72
                { 0xF4F3, 0x0F66, 0x0FA6, 0x0F80 },             // 0F66 0FA6 0F80
                { 0xF4F4, 0x0F66, 0x0FA6, 0x0F74 },             // 0F66 0FA6 0F74
                { 0xF4F5, 0x0F66, 0x0FA6, 0x0F7A },             // 0F66 0FA6 0F7A
                { 0xF4F6, 0x0F66, 0x0FA6, 0x0F7C },             // 0F66 0FA6 0F7C
                { 0xF4F7, 0x0F66, 0x0FA6, 0x0FB1 },             // 0F66 0FA6 0FB1
                { 0xF4F8, 0x0F66, 0x0FA6, 0x0FB1, 0x0F72 },             // 0F66 0FA6 0FB1 0F72
                { 0xF4F9, 0x0F66, 0x0FA6, 0x0FB1, 0x0F80 },             // 0F66 0FA6 0FB1 0F80
                { 0xF4FA, 0x0F66, 0x0FA6, 0x0FB1, 0x0F74 },             // 0F66 0FA6 0FB1 0F74
                { 0xF4FB, 0x0F66, 0x0FA6, 0x0FB1, 0x0F7A },             // 0F66 0FA6 0FB1 0F7A
                { 0xF4FC, 0x0F66, 0x0FA6, 0x0FB1, 0x0F7C },             // 0F66 0FA6 0FB1 0F7C
                { 0xF4FD, 0x0F66, 0x0FA6, 0x0FB2 },             // 0F66 0FA6 0FB2
                { 0xF4FE, 0x0F66, 0x0FA6, 0x0FB2, 0x0F72 },             // 0F66 0FA6 0FB2 0F72
                { 0xF4FF, 0x0F66, 0x0FA6, 0x0FB2, 0x0F80 },             // 0F66 0FA6 0FB2 0F80
                { 0xF500, 0x0F66, 0x0FA6, 0x0FB2, 0x0F74 },             // 0F66 0FA6 0FB2 0F74
                { 0xF501, 0x0F66, 0x0FA6, 0x0FB2, 0x0F7A },             // 0F66 0FA6 0FB2 0F7A
                { 0xF502, 0x0F66, 0x0FA6, 0x0FB2, 0x0F7C },             // 0F66 0FA6 0FB2 0F7C
                { 0xF503, 0x0F56, 0x0FB7, 0x0F71 },             // 0F56 0FB7 0F71
                { 0xF504, 0x0F56, 0x0FB7, 0x0F72 },             // 0F56 0FB7 0F72
                { 0xF505, 0x0F56, 0x0FB7, 0x0F80 },             // 0F56 0FB7 0F80
                { 0xF506, 0x0F56, 0x0FB7, 0x0F74 },             // 0F56 0FB7 0F74
                { 0xF507, 0x0F56, 0x0FB7, 0x0F7A },             // 0F56 0FB7 0F7A
                { 0xF508, 0x0F56, 0x0FB7, 0x0F7C },             // 0F56 0FB7 0F7C
                { 0xF509, 0x0F58, 0x0F71 },             // 0F58 0F71
                { 0xF50A, 0x0F58, 0x0F72 },             // 0F58 0F72
                { 0xF50B, 0x0F58, 0x0F80 },             // 0F58 0F80
                { 0xF50C, 0x0F58, 0x0F74 },             // 0F58 0F74
                { 0xF50D, 0x0F58, 0x0F7A },             // 0F58 0F7A
                { 0xF50E, 0x0F58, 0x0F7C },             // 0F58 0F7C
                { 0xF50F, 0x0F58, 0x0FB1 },             // 0F58 0FB1
                { 0xF510, 0x0F58, 0x0FB1, 0x0F72 },             // 0F58 0FB1 0F72
                { 0xF511, 0x0F58, 0x0FB1, 0x0F80 },             // 0F58 0FB1 0F80
                { 0xF512, 0x0F58, 0x0FB1, 0x0F74 },             // 0F58 0FB1 0F74
                { 0xF513, 0x0F58, 0x0FB1, 0x0F7A },             // 0F58 0FB1 0F7A
                { 0xF514, 0x0F58, 0x0FB1, 0x0F7C },             // 0F58 0FB1 0F7C
                { 0xF515, 0x0F58, 0x0FB2 },             // 0F58 0FB2
                { 0xF516, 0x0F58, 0x0FB2, 0x0F72 },             // 0F58 0FB2 0F72
                { 0xF517, 0x0F58, 0x0FB2, 0x0F80 },             // 0F58 0FB2 0F80
                { 0xF518, 0x0F58, 0x0FB2, 0x0F74 },             // 0F58 0FB2 0F74
                { 0xF519, 0x0F58, 0x0FB2, 0x0F7A },             // 0F58 0FB2 0F7A
                { 0xF51A, 0x0F58, 0x0FB2, 0x0F7C },             // 0F58 0FB2 0F7C
                { 0xF51B, 0x0F62, 0x0FA8 },             // 0F62 0FA8
                { 0xF51C, 0x0F62, 0x0FA8, 0x0F72 },             // 0F62 0FA8 0F72
                { 0xF51D, 0x0F62, 0x0FA8, 0x0F80 },             // 0F62 0FA8 0F80
                { 0xF51E, 0x0F62, 0x0FA8, 0x0F74 },             // 0F62 0FA8 0F74
                { 0xF51F, 0x0F62, 0x0FA8, 0x0F7A },             // 0F62 0FA8 0F7A
                { 0xF520, 0x0F62, 0x0FA8, 0x0F7C },             // 0F62 0FA8 0F7C
                { 0xF521, 0x0F62, 0x0FA8, 0x0FB1 },             // 0F62 0FA8 0FB1
                { 0xF522, 0x0F62, 0x0FA8, 0x0FB1, 0x0F72 },             // 0F62 0FA8 0FB1 0F72
                { 0xF523, 0x0F62, 0x0FA8, 0x0FB1, 0x0F80 },             // 0F62 0FA8 0FB1 0F80
                { 0xF524, 0x0F62, 0x0FA8, 0x0FB1, 0x0F74 },             // 0F62 0FA8 0FB1 0F74
                { 0xF525, 0x0F62, 0x0FA8, 0x0FB1, 0x0F7A },             // 0F62 0FA8 0FB1 0F7A
                { 0xF526, 0x0F62, 0x0FA8, 0x0FB1, 0x0F7C },             // 0F62 0FA8 0FB1 0F7C
                { 0xF527, 0x0F66, 0x0FA8 },             // 0F66 0FA8
                { 0xF528, 0x0F66, 0x0FA8, 0x0F72 },             // 0F66 0FA8 0F72
                { 0xF529, 0x0F66, 0x0FA8, 0x0F80 },             // 0F66 0FA8 0F80
                { 0xF52A, 0x0F66, 0x0FA8, 0x0F74 },             // 0F66 0FA8 0F74
                { 0xF52B, 0x0F66, 0x0FA8, 0x0F7A },             // 0F66 0FA8 0F7A
                { 0xF52C, 0x0F66, 0x0FA8, 0x0F7C },             // 0F66 0FA8 0F7C
                { 0xF52D, 0x0F66, 0x0FA8, 0x0FB1 },             // 0F66 0FA8 0FB1
                { 0xF52E, 0x0F66, 0x0FA8, 0x0FB1, 0x0F72 },             // 0F66 0FA8 0FB1 0F72
                { 0xF52F, 0x0F66, 0x0FA8, 0x0FB1, 0x0F80 },             // 0F66 0FA8 0FB1 0F80
                { 0xF530, 0x0F66, 0x0FA8, 0x0FB1, 0x0F74 },             // 0F66 0FA8 0FB1 0F74
                { 0xF531, 0x0F66, 0x0FA8, 0x0FB1, 0x0F7A },             // 0F66 0FA8 0FB1 0F7A
                { 0xF532, 0x0F66, 0x0FA8, 0x0FB1, 0x0F7C },             // 0F66 0FA8 0FB1 0F7C
                { 0xF533, 0x0F66, 0x0FA8, 0x0FB2 },             // 0F66 0FA8 0FB2
                { 0xF534, 0x0F66, 0x0FA8, 0x0FB2, 0x0F72 },             // 0F66 0FA8 0FB2 0F72
                { 0xF535, 0x0F66, 0x0FA8, 0x0FB2, 0x0F80 },             // 0F66 0FA8 0FB2 0F80
                { 0xF536, 0x0F66, 0x0FA8, 0x0FB2, 0x0F74 },             // 0F66 0FA8 0FB2 0F74
                { 0xF537, 0x0F66, 0x0FA8, 0x0FB2, 0x0F7A },             // 0F66 0FA8 0FB2 0F7A
                { 0xF538, 0x0F66, 0x0FA8, 0x0FB2, 0x0F7C },             // 0F66 0FA8 0FB2 0F7C
                { 0xF539, 0x0F59, 0x0F71 },             // 0F59 0F71
                { 0xF53A, 0x0F59, 0x0F72 },             // 0F59 0F72
                { 0xF53B, 0x0F59, 0x0F80 },             // 0F59 0F80
                { 0xF53C, 0x0F59, 0x0F74 },             // 0F59 0F74
                { 0xF53D, 0x0F59, 0x0F7A },             // 0F59 0F7A
                { 0xF53E, 0x0F59, 0x0F7C },             // 0F59 0F7C
                { 0xF53F, 0x0F62, 0x0FA9 },             // 0F62 0FA9
                { 0xF540, 0x0F62, 0x0FA9, 0x0F72 },             // 0F62 0FA9 0F72
                { 0xF541, 0x0F62, 0x0FA9, 0x0F80 },             // 0F62 0FA9 0F80
                { 0xF542, 0x0F62, 0x0FA9, 0x0F74 },             // 0F62 0FA9 0F74
                { 0xF543, 0x0F62, 0x0FA9, 0x0F7A },             // 0F62 0FA9 0F7A
                { 0xF544, 0x0F62, 0x0FA9, 0x0F7C },             // 0F62 0FA9 0F7C
                { 0xF545, 0x0F62, 0x0FA9, 0x0FAD },             // 0F62 0FA9 0FAD
                { 0xF546, 0x0F66, 0x0FA9 },             // 0F66 0FA9
                { 0xF547, 0x0F66, 0x0FA9, 0x0F72 },             // 0F66 0FA9 0F72
                { 0xF548, 0x0F66, 0x0FA9, 0x0F80 },             // 0F66 0FA9 0F80
                { 0xF549, 0x0F66, 0x0FA9, 0x0F74 },             // 0F66 0FA9 0F74
                { 0xF54A, 0x0F66, 0x0FA9, 0x0F7A },             // 0F66 0FA9 0F7A
                { 0xF54B, 0x0F66, 0x0FA9, 0x0F7C },             // 0F66 0FA9 0F7C
                { 0xF54C, 0x0F5A, 0x0F71 },             // 0F5A 0F71
                { 0xF54D, 0x0F5A, 0x0F72 },             // 0F5A 0F72
                { 0xF54E, 0x0F5A, 0x0F80 },             // 0F5A 0F80
                { 0xF54F, 0x0F5A, 0x0F74 },             // 0F5A 0F74
                { 0xF550, 0x0F5A, 0x0F7A },             // 0F5A 0F7A
                { 0xF551, 0x0F5A, 0x0F7C },             // 0F5A 0F7C
                { 0xF552, 0x0F5A, 0x0FAD },             // 0F5A 0FAD
                { 0xF553, 0x0F5B, 0x0F71 },             // 0F5B 0F71
                { 0xF554, 0x0F5B, 0x0F72 },             // 0F5B 0F72
                { 0xF555, 0x0F5B, 0x0F80 },             // 0F5B 0F80
                { 0xF556, 0x0F5B, 0x0F74 },             // 0F5B 0F74
                { 0xF557, 0x0F5B, 0x0F7A },             // 0F5B 0F7A
                { 0xF558, 0x0F5B, 0x0F7C },             // 0F5B 0F7C
                { 0xF559, 0x0F62, 0x0FAB },             // 0F62 0FAB
                { 0xF55A, 0x0F62, 0x0FAB, 0x0F72 },             // 0F62 0FAB 0F72
                { 0xF55B, 0x0F62, 0x0FAB, 0x0F80 },             // 0F62 0FAB 0F80
                { 0xF55C, 0x0F62, 0x0FAB, 0x0F74 },             // 0F62 0FAB 0F74
                { 0xF55D, 0x0F62, 0x0FAB, 0x0F7A },             // 0F62 0FAB 0F7A
                { 0xF55E, 0x0F62, 0x0FAB, 0x0F7C },             // 0F62 0FAB 0F7C
                { 0xF55F, 0x0F5B, 0x0FB7, 0x0F71 },             // 0F5B 0FB7 0F71
                { 0xF560, 0x0F5B, 0x0FB7, 0x0F72 },             // 0F5B 0FB7 0F72
                { 0xF561, 0x0F5B, 0x0FB7, 0x0F80 },             // 0F5B 0FB7 0F80
                { 0xF562, 0x0F5B, 0x0FB7, 0x0F74 },             // 0F5B 0FB7 0F74
                { 0xF563, 0x0F5B, 0x0FB7, 0x0F7A },             // 0F5B 0FB7 0F7A
                { 0xF564, 0x0F5B, 0x0FB7, 0x0F7C },             // 0F5B 0FB7 0F7C
                { 0xF565, 0x0F5D, 0x0F71 },             // 0F5D 0F71
                { 0xF566, 0x0F5D, 0x0F72 },             // 0F5D 0F72
                { 0xF567, 0x0F5D, 0x0F80 },             // 0F5D 0F80
                { 0xF568, 0x0F5D, 0x0F74 },             // 0F5D 0F74
                { 0xF569, 0x0F5D, 0x0F7A },             // 0F5D 0F7A
                { 0xF56A, 0x0F5D, 0x0F7C },             // 0F5D 0F7C
                { 0xF56B, 0x0F5E, 0x0F39 },             // 0F5E 0F39
                { 0xF56C, 0x0F5E, 0x0F71 },             // 0F5E 0F71
                { 0xF56D, 0x0F5E, 0x0F72 },             // 0F5E 0F72
                { 0xF56E, 0x0F5E, 0x0F71, 0x0F72 },             // 0F5E 0F71 0F72
                { 0xF56F, 0x0F5E, 0x0F80 },             // 0F5E 0F80
                { 0xF570, 0x0F5E, 0x0F74 },             // 0F5E 0F74
                { 0xF571, 0x0F5E, 0x0F7A },             // 0F5E 0F7A
                { 0xF572, 0x0F5E, 0x0F7C },             // 0F5E 0F7C
                { 0xF573, 0x0F5E, 0x0F7E },             // 0F5E 0F7E
                { 0xF574, 0x0F5E, 0x0FB2, 0x0F7A },             // 0F5E 0FB2 0F7A
                { 0xF575, 0x0F5E, 0x0FB2, 0x0FAD },             // 0F5E 0FB2 0FAD
                { 0xF576, 0x0F5E, 0x0FAD },             // 0F5E 0FAD
                { 0xF577, 0x0F5E, 0x0F92 },             // 0F5E 0F92
                { 0xF578, 0x0F5E, 0x0F94 },             // 0F5E 0F94
                { 0xF579, 0x0F5E, 0x0F94, 0x0F80 },             // 0F5E 0F94 0F80
                { 0xF57A, 0x0F5E, 0x0FA3 },             // 0F5E 0FA3
                { 0xF57B, 0x0F5E, 0x0FA3, 0x0F72 },             // 0F5E 0FA3 0F72
                { 0xF57C, 0x0F5F, 0x0F39 },             // 0F5F 0F39
                { 0xF57D, 0x0F5F, 0x0F71 },             // 0F5F 0F71
                { 0xF57E, 0x0F5F, 0x0F72 },             // 0F5F 0F72
                { 0xF57F, 0x0F5F, 0x0F80 },             // 0F5F 0F80
                { 0xF580, 0x0F5F, 0x0F74 },             // 0F5F 0F74
                { 0xF581, 0x0F5F, 0x0F7A },             // 0F5F 0F7A
                { 0xF582, 0x0F5F, 0x0F7C },             // 0F5F 0F7C
                { 0xF583, 0x0F5F, 0x0FB2, 0x0F72 },             // 0F5F 0FB2 0F72
                { 0xF584, 0x0F5F, 0x0FB2, 0x0F80 },             // 0F5F 0FB2 0F80
                { 0xF585, 0x0F5F, 0x0FB3 },             // 0F5F 0FB3
                { 0xF586, 0x0F5F, 0x0FB3, 0x0F72 },             // 0F5F 0FB3 0F72
                { 0xF587, 0x0F5F, 0x0FB3, 0x0F80 },             // 0F5F 0FB3 0F80
                { 0xF588, 0x0F5F, 0x0FB3, 0x0F74 },             // 0F5F 0FB3 0F74
                { 0xF589, 0x0F5F, 0x0FB3, 0x0F7A },             // 0F5F 0FB3 0F7A
                { 0xF58A, 0x0F5F, 0x0FB3, 0x0F7C },             // 0F5F 0FB3 0F7C
                { 0xF58B, 0x0F5F, 0x0FAD },             // 0F5F 0FAD
                { 0xF58C, 0x0F5F, 0x0FAD, 0x0F74 },             // 0F5F 0FAD 0F74
                { 0xF58D, 0x0F5F, 0x0FAD, 0x0F7A },             // 0F5F 0FAD 0F7A
                { 0xF58E, 0x0F5F, 0x0FAD, 0x0F7C },             // 0F5F 0FAD 0F7C
                { 0xF58F, 0x0F5F, 0x0F94 },             // 0F5F 0F94
                { 0xF590, 0x0F60, 0x0F39 },             // 0F60 0F39
                { 0xF591, 0x0F60, 0x0F72 },             // 0F60 0F72
                { 0xF592, 0x0F60, 0x0F39, 0x0F72 },             // 0F60 0F39 0F72 = NFD 0F60 0F72 0F39
                { 0xF593, 0x0F60, 0x0F80 },             // 0F60 0F80
                { 0xF594, 0x0F60, 0x0F74 },             // 0F60 0F74
                { 0xF595, 0x0F60, 0x0F39, 0x0F74 },             // 0F60 0F39 0F74 = NFD 0F60 0F74 0F39
                { 0xF596, 0x0F60, 0x0F7A },             // 0F60 0F7A
                { 0xF597, 0x0F60, 0x0F7C },             // 0F60 0F7C
                { 0xF598, 0x0F60, 0x0F71, 0x0F7C },             // 0F60 0F71 0F7C
                { 0xF599, 0x0F60, 0x0F7E },             // 0F60 0F7E
                { 0xF59A, 0x0F60, 0x0F74, 0x0F7E },             // 0F60 0F74 0F7E
                { 0xF59B, 0x0F60, 0x0F94 },             // 0F60 0F94
                { 0xF59C, 0x0F60, 0x0F39, 0x0F94 },             // 0F60 0F39 0F94
                { 0xF59D, 0x0F60, 0x0FB1, 0x0FAD },             // 0F60 0FB1 0FAD
                { 0xF59E, 0x0F60, 0x0FAD },             // 0F60 0FAD
                { 0xF59F, 0x0F60, 0x0F39, 0x0FAD, 0x0F72 },             // 0F60 0F39 0FAD 0F72
                { 0xF5A0, 0x0F60, 0x0F39, 0x0FAD, 0x0F80 },             // 0F60 0F39 0FAD 0F80
                { 0xF5A1, 0x0F60, 0x0FAD, 0x0F7A },             // 0F60 0FAD 0F7A
                { 0xF5A2, 0x0F60, 0x0FAD, 0x0F7C },             // 0F60 0FAD 0F7C
                { 0xF5A3, 0x0F61, 0x0F71 },             // 0F61 0F71
                { 0xF5A4, 0x0F61, 0x0F72 },             // 0F61 0F72
                { 0xF5A5, 0x0F61, 0x0F80 },             // 0F61 0F80
                { 0xF5A6, 0x0F61, 0x0F74 },             // 0F61 0F74
                { 0xF5A7, 0x0F61, 0x0F7A },             // 0F61 0F7A
                { 0xF5A8, 0x0F61, 0x0F7C },             // 0F61 0F7C
                { 0xF5A9, 0x0F62, 0x0F71 },             // 0F62 0F71
                { 0xF5AA, 0x0F62, 0x0F72 },             // 0F62 0F72
                { 0xF5AB, 0x0F62, 0x0F74 },             // 0F62 0F74
                { 0xF5AC, 0x0F62, 0x0F7A },             // 0F62 0F7A
                { 0xF5AD, 0x0F62, 0x0F7C },             // 0F62 0F7C
                { 0xF5AE, 0x0F62, 0x0FB3 },             // 0F62 0FB3
                { 0xF5AF, 0x0F62, 0x0FB3, 0x0F72 },             // 0F62 0FB3 0F72
                { 0xF5B0, 0x0F62, 0x0FB3, 0x0F80 },             // 0F62 0FB3 0F80
                { 0xF5B1, 0x0F62, 0x0FB3, 0x0F74 },             // 0F62 0FB3 0F74
                { 0xF5B2, 0x0F62, 0x0FB3, 0x0F7A },             // 0F62 0FB3 0F7A
                { 0xF5B3, 0x0F62, 0x0FB3, 0x0F7C },             // 0F62 0FB3 0F7C
                { 0xF5B4, 0x0F62, 0x0FAD },             // 0F62 0FAD
                { 0xF5B5, 0x0F63, 0x0F71 },             // 0F63 0F71
                { 0xF5B6, 0x0F63, 0x0F72 },             // 0F63 0F72
                { 0xF5B7, 0x0F63, 0x0F74 },             // 0F63 0F74
                { 0xF5B8, 0x0F63, 0x0F7A },             // 0F63 0F7A
                { 0xF5B9, 0x0F63, 0x0F7C },             // 0F63 0F7C
                { 0xF5BA, 0x0F63, 0x0FAD },             // 0F63 0FAD
                { 0xF5BB, 0x0F64, 0x0F71 },             // 0F64 0F71
                { 0xF5BC, 0x0F64, 0x0F72 },             // 0F64 0F72
                { 0xF5BD, 0x0F64, 0x0F80 },             // 0F64 0F80
                { 0xF5BE, 0x0F64, 0x0F74 },             // 0F64 0F74
                { 0xF5BF, 0x0F64, 0x0F7A },             // 0F64 0F7A
                { 0xF5C0, 0x0F64, 0x0F7C },             // 0F64 0F7C
                { 0xF5C1, 0x0F64, 0x0FAD },             // 0F64 0FAD
                { 0xF5C2, 0x0F65, 0x0F71 },             // 0F65 0F71
                { 0xF5C3, 0x0F65, 0x0F72 },             // 0F65 0F72
                { 0xF5C4, 0x0F65, 0x0F80 },             // 0F65 0F80
                { 0xF5C5, 0x0F65, 0x0F74 },             // 0F65 0F74
                { 0xF5C6, 0x0F65, 0x0F7A },             // 0F65 0F7A
                { 0xF5C7, 0x0F65, 0x0F7C },             // 0F65 0F7C
                { 0xF5C8, 0x0F66, 0x0F71 },             // 0F66 0F71
                { 0xF5C9, 0x0F66, 0x0F72 },             // 0F66 0F72
                { 0xF5CA, 0x0F66, 0x0F80 },             // 0F66 0F80
                { 0xF5CB, 0x0F66, 0x0F74 },             // 0F66 0F74
                { 0xF5CC, 0x0F66, 0x0F7A },             // 0F66 0F7A
                { 0xF5CD, 0x0F66, 0x0F7C },             // 0F66 0F7C
                { 0xF5CE, 0x0F66, 0x0FB2 },             // 0F66 0FB2
                { 0xF5CF, 0x0F66, 0x0FB2, 0x0F72 },             // 0F66 0FB2 0F72
                { 0xF5D0, 0x0F66, 0x0FB2, 0x0F80 },             // 0F66 0FB2 0F80
                { 0xF5D1, 0x0F66, 0x0FB2, 0x0F74 },             // 0F66 0FB2 0F74
                { 0xF5D2, 0x0F66, 0x0FB2, 0x0F7A },             // 0F66 0FB2 0F7A
                { 0xF5D3, 0x0F66, 0x0FB2, 0x0F7C },             // 0F66 0FB2 0F7C
                { 0xF5D4, 0x0F66, 0x0FB3 },             // 0F66 0FB3
                { 0xF5D5, 0x0F66, 0x0FB3, 0x0F72 },             // 0F66 0FB3 0F72
                { 0xF5D6, 0x0F66, 0x0FB3, 0x0F80 },             // 0F66 0FB3 0F80
                { 0xF5D7, 0x0F66, 0x0FB3, 0x0F74 },             // 0F66 0FB3 0F74
                { 0xF5D8, 0x0F66, 0x0FB3, 0x0F7A },             // 0F66 0FB3 0F7A
                { 0xF5D9, 0x0F66, 0x0FB3, 0x0F7C },             // 0F66 0FB3 0F7C
                { 0xF5DA, 0x0F66, 0x0FAD },             // 0F66 0FAD
                { 0xF5DB, 0x0F66, 0x0FAD, 0x0F72 },             // 0F66 0FAD 0F72
                { 0xF5DC, 0x0F66, 0x0FAD, 0x0F80 },             // 0F66 0FAD 0F80
                { 0xF5DD, 0x0F66, 0x0FAD, 0x0F74 },             // 0F66 0FAD 0F74
                { 0xF5DE, 0x0F66, 0x0FAD, 0x0F7A },             // 0F66 0FAD 0F7A
                { 0xF5DF, 0x0F66, 0x0FAD, 0x0F7C },             // 0F66 0FAD 0F7C
                { 0xF5E0, 0x0F67, 0x0F71 },             // 0F67 0F71
                { 0xF5E1, 0x0F67, 0x0F72 },             // 0F67 0F72
                { 0xF5E2, 0x0F67, 0x0F80 },             // 0F67 0F80
                { 0xF5E3, 0x0F67, 0x0F74 },             // 0F67 0F74
                { 0xF5E4, 0x0F67, 0x0F7A },             // 0F67 0F7A
                { 0xF5E5, 0x0F67, 0x0F7C },             // 0F67 0F7C
                { 0xF5E6, 0x0F67, 0x0FB2 },             // 0F67 0FB2
                { 0xF5E7, 0x0F67, 0x0FB2, 0x0F72 },             // 0F67 0FB2 0F72
                { 0xF5E8, 0x0F67, 0x0FB2, 0x0F80 },             // 0F67 0FB2 0F80
                { 0xF5E9, 0x0F67, 0x0FB2, 0x0F74 },             // 0F67 0FB2 0F74
                { 0xF5EA, 0x0F67, 0x0FB2, 0x0F7A },             // 0F67 0FB2 0F7A
                { 0xF5EB, 0x0F67, 0x0FB2, 0x0F7C },             // 0F67 0FB2 0F7C
                { 0xF5EC, 0x0F67, 0x0FAD },             // 0F67 0FAD
                { 0xF5ED, 0x0F63, 0x0FB7 },             // 0F63 0FB7
                { 0xF5EE, 0x0F63, 0x0FB7, 0x0F72 },             // 0F63 0FB7 0F72
                { 0xF5EF, 0x0F63, 0x0FB7, 0x0F80 },             // 0F63 0FB7 0F80
                { 0xF5F0, 0x0F63, 0x0FB7, 0x0F74 },             // 0F63 0FB7 0F74
                { 0xF5F1, 0x0F63, 0x0FB7, 0x0F7A },             // 0F63 0FB7 0F7A
                { 0xF5F2, 0x0F63, 0x0FB7, 0x0F7C },             // 0F63 0FB7 0F7C
                { 0xF5F3, 0x0F40, 0x0FB5, 0x0F71 },             // 0F40 0FB5 0F71
                { 0xF5F4, 0x0F40, 0x0FB5, 0x0F72 },             // 0F40 0FB5 0F72
                { 0xF5F5, 0x0F40, 0x0FB5, 0x0F80 },             // 0F40 0FB5 0F80
                { 0xF5F6, 0x0F40, 0x0FB5, 0x0F74 },             // 0F40 0FB5 0F74
                { 0xF5F7, 0x0F40, 0x0FB5, 0x0F7A },             // 0F40 0FB5 0F7A
                { 0xF5F8, 0x0F40, 0x0FB5, 0x0F7C },             // 0F40 0FB5 0F7C
                { 0xF5F9, 0x0F68, 0x0F71 },             // 0F68 0F71
                { 0xF5FA, 0x0F68, 0x0F71, 0x0F72 },             // 0F68 0F71 0F72
                { 0xF5FB, 0x0F68, 0x0F71, 0x0F74 },             // 0F68 0F71 0F74
                { 0xF5FC, 0x0F62, 0x0F80 },             // 0F62 0F80
                { 0xF5FD, 0x0F62, 0x0F71, 0x0F80 },             // 0F62 0F71 0F80
                { 0xF5FE, 0x0F63, 0x0F80 },             // 0F63 0F80
                { 0xF5FF, 0x0F63, 0x0F71, 0x0F80 },             // 0F63 0F71 0F80
                { 0xF600, 0x0F68, 0x0F7B },             // 0F68 0F7B
                { 0xF601, 0x0F68, 0x0F71, 0x0F7C },             // 0F68 0F71 0F7C
                { 0xF602, 0x0F68, 0x0F7D },             // 0F68 0F7D
                { 0xF603, 0x0F68, 0x0F7E },             // 0F68 0F7E
                { 0xF604, 0x0F68, 0x0F83 },             // 0F68 0F83
                { 0xF605, 0x0F68, 0x0F71, 0x0F7E },             // 0F68 0F71 0F7E
                { 0xF606, 0x0F68, 0x0F72, 0x0F7E },             // 0F68 0F72 0F7E
                { 0xF607, 0x0F68, 0x0F71, 0x0F72, 0x0F7E },             // 0F68 0F71 0F72 0F7E
                { 0xF608, 0x0F68, 0x0F74, 0x0F7E },             // 0F68 0F74 0F7E
                { 0xF609, 0x0F68, 0x0F71, 0x0F74, 0x0F7E },             // 0F68 0F71 0F74 0F7E
                { 0xF60A, 0x0F68, 0x0F71, 0x0F74, 0x0F83 },             // 0F68 0F71 0F74 0F83
                { 0xF60B, 0x0F62, 0x0F80, 0x0F7E },             // 0F62 0F80 0F7E
                { 0xF60C, 0x0F63, 0x0F80, 0x0F7E },             // 0F63 0F80 0F7E
                { 0xF60D, 0x0F68, 0x0F71, 0x0F7C, 0x0F7E },             // 0F68 0F71 0F7C 0F7E
                { 0xF60E, 0x0F68, 0x0F71, 0x0F7C, 0x0F83 },             // 0F68 0F71 0F7C 0F83
                { 0xF60F, 0x0F68, 0x0F7D, 0x0F7E },             // 0F68 0F7D 0F7E
                { 0xF610, 0x0F68, 0x0FB1, 0x0F7C },             // 0F68 0FB1 0F7C
                { 0xF611, 0x0F68, 0x0FB1, 0x0F7D },             // 0F68 0FB1 0F7D
                { 0xF612, 0x0F40, 0x0F71, 0x0F72 },             // 0F40 0F71 0F72
                { 0xF613, 0x0F40, 0x0F71, 0x0F74 },             // 0F40 0F71 0F74
                { 0xF614, 0x0F40, 0x0FB2, 0x0F71, 0x0F80 },             // 0F40 0FB2 0F71 0F80
                { 0xF615, 0x0F40, 0x0FB3, 0x0F71, 0x0F80 },             // 0F40 0FB3 0F71 0F80
                { 0xF616, 0x0F40, 0x0F71, 0x0F7A },             // 0F40 0F71 0F7A
                { 0xF617, 0x0F40, 0x0F7B },             // 0F40 0F7B
                { 0xF618, 0x0F40, 0x0F7D },             // 0F40 0F7D
                { 0xF619, 0x0F40, 0x0F7E },             // 0F40 0F7E
                { 0xF61A, 0x0F40, 0x0F71, 0x0F7E },             // 0F40 0F71 0F7E
                { 0xF61B, 0x0F88, 0x0F90 },             // 0F88 0F90
                { 0xF61C, 0x0F40, 0x0F90 },             // 0F40 0F90
                { 0xF61D, 0x0F40, 0x0F90, 0x0F71 },             // 0F40 0F90 0F71
                { 0xF61E, 0x0F40, 0x0F90, 0x0F72 },             // 0F40 0F90 0F72
                { 0xF61F, 0x0F40, 0x0F90, 0x0F74 },             // 0F40 0F90 0F74
                { 0xF620, 0x0F40, 0x0F94 },             // 0F40 0F94
                { 0xF621, 0x0F40, 0x0F94, 0x0F74 },             // 0F40 0F94 0F74
                { 0xF622, 0x0F40, 0x0F94, 0x0F7B },             // 0F40 0F94 0F7B
                { 0xF623, 0x0F40, 0x0F94, 0x0F7C },             // 0F40 0F94 0F7C
                { 0xF624, 0x0F40, 0x0FA9, 0x0F7A },             // 0F40 0FA9 0F7A
                { 0xF625, 0x0F40, 0x0FAA, 0x0F72 },             // 0F40 0FAA 0F72
                { 0xF626, 0x0F40, 0x0FAA, 0x0F74 },             // 0F40 0FAA 0F74
                { 0xF627, 0x0F40, 0x0F9A },             // 0F40 0F9A
                { 0xF628, 0x0F40, 0x0F9A, 0x0F72 },             // 0F40 0F9A 0F72
                { 0xF629, 0x0F40, 0x0F9A, 0x0F7C },             // 0F40 0F9A 0F7C
                { 0xF62A, 0x0F40, 0x0F9E, 0x0F72 },             // 0F40 0F9E 0F72
                { 0xF62B, 0x0F40, 0x0F9E, 0x0F7A },             // 0F40 0F9E 0F7A
                { 0xF62C, 0x0F40, 0x0F9E, 0x0F7C },             // 0F40 0F9E 0F7C
                { 0xF62D, 0x0F40, 0x0F9F },             // 0F40 0F9F
                { 0xF62E, 0x0F40, 0x0F9F, 0x0F71 },             // 0F40 0F9F 0F71
                { 0xF62F, 0x0F40, 0x0F9F, 0x0F72 },             // 0F40 0F9F 0F72
                { 0xF630, 0x0F40, 0x0F9F, 0x0F71, 0x0F72 },             // 0F40 0F9F 0F71 0F72
                { 0xF631, 0x0F40, 0x0F9F, 0x0F7A },             // 0F40 0F9F 0F7A
                { 0xF632, 0x0F40, 0x0F9F, 0x0F7C },             // 0F40 0F9F 0F7C
                { 0xF633, 0x0F40, 0x0F9F, 0x0FB2, 0x0F72 },             // 0F40 0F9F 0FB2 0F72
                { 0xF634, 0x0F40, 0x0FA0, 0x0F7C },             // 0F40 0FA0 0F7C
                { 0xF635, 0x0F40, 0x0FA0, 0x0F7E },             // 0F40 0FA0 0F7E
                { 0xF636, 0x0F40, 0x0FA8 },             // 0F40 0FA8
                { 0xF637, 0x0F40, 0x0FB1, 0x0F71 },             // 0F40 0FB1 0F71
                { 0xF638, 0x0F40, 0x0FB1, 0x0F7B },             // 0F40 0FB1 0F7B
                { 0xF639, 0x0F40, 0x0FB2, 0x0F71, 0x0F72 },             // 0F40 0FB2 0F71 0F72
                { 0xF63A, 0x0F40, 0x0FB2, 0x0F71, 0x0F74 },             // 0F40 0FB2 0F71 0F74
                { 0xF63B, 0x0F40, 0x0FB2, 0x0F72, 0x0F7E },             // 0F40 0FB2 0F72 0F7E
                { 0xF63C, 0x0F40, 0x0FB2, 0x0F71, 0x0F74, 0x0F7E },             // 0F40 0FB2 0F71 0F74 0F7E
                { 0xF63D, 0x0F40, 0x0FB3, 0x0F7B },             // 0F40 0FB3 0F7B
                { 0xF63E, 0x0F40, 0x0FAD, 0x0F72 },             // 0F40 0FAD 0F72
                { 0xF63F, 0x0F40, 0x0FAD, 0x0F7A },             // 0F40 0FAD 0F7A
                { 0xF640, 0x0F40, 0x0FAD, 0x0F7E },             // 0F40 0FAD 0F7E
                { 0xF641, 0x0F40, 0x0FB4, 0x0F72 },             // 0F40 0FB4 0F72
                { 0xF642, 0x0F40, 0x0FB4, 0x0FB2, 0x0F74, 0x0F83 },             // 0F40 0FB4 0FB2 0F74 0F83
                { 0xF643, 0x0F40, 0x0FB5, 0x0F71, 0x0F72 },             // 0F40 0FB5 0F71 0F72
                { 0xF644, 0x0F40, 0x0FB5, 0x0F71, 0x0F74 },             // 0F40 0FB5 0F71 0F74
                { 0xF645, 0x0F40, 0x0FB5, 0x0FB2, 0x0F80 },             // 0F40 0FB5 0FB2 0F80
                { 0xF646, 0x0F40, 0x0FB5, 0x0FB2, 0x0F71, 0x0F80 },             // 0F40 0FB5 0FB2 0F71 0F80
                { 0xF647, 0x0F40, 0x0FB5, 0x0FB3, 0x0F80 },             // 0F40 0FB5 0FB3 0F80
                { 0xF648, 0x0F40, 0x0FB5, 0x0FB3, 0x0F71, 0x0F80 },             // 0F40 0FB5 0FB3 0F71 0F80
                { 0xF649, 0x0F40, 0x0FB5, 0x0F7B },             // 0F40 0FB5 0F7B
                { 0xF64A, 0x0F40, 0x0FB5, 0x0F7D },             // 0F40 0FB5 0F7D
                { 0xF64B, 0x0F40, 0x0FB5, 0x0F7E },             // 0F40 0FB5 0F7E
                { 0xF64C, 0x0F40, 0x0FB5, 0x0F71, 0x0F7E },             // 0F40 0FB5 0F71 0F7E
                { 0xF64D, 0x0F40, 0x0FB5, 0x0F9E },             // 0F40 0FB5 0F9E
                { 0xF64E, 0x0F40, 0x0FB5, 0x0FA8 },             // 0F40 0FB5 0FA8
                { 0xF64F, 0x0F40, 0x0FB5, 0x0FA8, 0x0F71 },             // 0F40 0FB5 0FA8 0F71
                { 0xF650, 0x0F40, 0x0FB5, 0x0FA8, 0x0F72 },             // 0F40 0FB5 0FA8 0F72
                { 0xF651, 0x0F40, 0x0FB5, 0x0FA8, 0x0F7A },             // 0F40 0FB5 0FA8 0F7A
                { 0xF652, 0x0F40, 0x0FB5, 0x0FB1 },             // 0F40 0FB5 0FB1
                { 0xF653, 0x0F40, 0x0FB5, 0x0FB2 },             // 0F40 0FB5 0FB2
                { 0xF654, 0x0F40, 0x0FB6, 0x0F7E },             // 0F40 0FB6 0F7E
                { 0xF655, 0x0F41, 0x0F71, 0x0F72 },             // 0F41 0F71 0F72
                { 0xF656, 0x0F41, 0x0FB2, 0x0F71, 0x0F80 },             // 0F41 0FB2 0F71 0F80
                { 0xF657, 0x0F41, 0x0F7B },             // 0F41 0F7B
                { 0xF658, 0x0F41, 0x0F7D },             // 0F41 0F7D
                { 0xF659, 0x0F41, 0x0F7E },             // 0F41 0F7E
                { 0xF65A, 0x0F41, 0x0F72, 0x0F7E },             // 0F41 0F72 0F7E
                { 0xF65B, 0x0F41, 0x0F74, 0x0F7E },             // 0F41 0F74 0F7E
                { 0xF65C, 0x0F88, 0x0F91 },             // 0F88 0F91
                { 0xF65D, 0x0F41, 0x0F9A },             // 0F41 0F9A
                { 0xF65E, 0x0F41, 0x0FB1, 0x0F71 },             // 0F41 0FB1 0F71
                { 0xF65F, 0x0F41, 0x0FB1, 0x0F7B },             // 0F41 0FB1 0F7B
                { 0xF660, 0x0F41, 0x0FB1, 0x0F7B, 0x0F7A },             // 0F41 0FB1 0F7B 0F7A
                { 0xF661, 0x0F41, 0x0FB2, 0x0F71 },             // 0F41 0FB2 0F71
                { 0xF662, 0x0F41, 0x0FB2, 0x0F7E },             // 0F41 0FB2 0F7E
                { 0xF663, 0x0F41, 0x0FB2, 0x0F74, 0x0F7E },             // 0F41 0FB2 0F74 0F7E
                { 0xF664, 0x0F41, 0x0FB3 },             // 0F41 0FB3
                { 0xF665, 0x0F41, 0x0FAD, 0x0F71, 0x0F74 },             // 0F41 0FAD 0F71 0F74
                { 0xF666, 0x0F42, 0x0F71, 0x0F72 },             // 0F42 0F71 0F72
                { 0xF667, 0x0F42, 0x0F71, 0x0F80 },             // 0F42 0F71 0F80
                { 0xF668, 0x0F42, 0x0FB2, 0x0F71, 0x0F80 },             // 0F42 0FB2 0F71 0F80
                { 0xF669, 0x0F42, 0x0F7B },             // 0F42 0F7B
                { 0xF66A, 0x0F42, 0x0F71, 0x0F7C },             // 0F42 0F71 0F7C
                { 0xF66B, 0x0F42, 0x0F7D },             // 0F42 0F7D
                { 0xF66C, 0x0F42, 0x0F7E },             // 0F42 0F7E
                { 0xF66D, 0x0F42, 0x0F74, 0x0F7E },             // 0F42 0F74 0F7E
                { 0xF66E, 0x0F42, 0x0F92, 0x0F71 },             // 0F42 0F92 0F71
                { 0xF66F, 0x0F42, 0x0F92, 0x0F72 },             // 0F42 0F92 0F72
                { 0xF670, 0x0F42, 0x0F92, 0x0F74 },             // 0F42 0F92 0F74
                { 0xF671, 0x0F42, 0x0F94, 0x0F7A },             // 0F42 0F94 0F7A
                { 0xF672, 0x0F42, 0x0F99 },             // 0F42 0F99
                { 0xF673, 0x0F42, 0x0F9E, 0x0F72 },             // 0F42 0F9E 0F72
                { 0xF674, 0x0F42, 0x0F9F },             // 0F42 0F9F
                { 0xF675, 0x0F42, 0x0F9F, 0x0F74 },             // 0F42 0F9F 0F74
                { 0xF676, 0x0F42, 0x0F9F, 0x0F7C },             // 0F42 0F9F 0F7C
                { 0xF677, 0x0F42, 0x0FA1, 0x0FB7, 0x0F72 },             // 0F42 0FA1 0FB7 0F72
                { 0xF678, 0x0F42, 0x0FA3 },             // 0F42 0FA3
                { 0xF679, 0x0F42, 0x0FA3, 0x0F72 },             // 0F42 0FA3 0F72
                { 0xF67A, 0x0F42, 0x0FA3, 0x0F71, 0x0F72 },             // 0F42 0FA3 0F71 0F72
                { 0xF67B, 0x0F42, 0x0FA3, 0x0F7A },             // 0F42 0FA3 0F7A
                { 0xF67C, 0x0F42, 0x0FA8 },             // 0F42 0FA8
                { 0xF67D, 0x0F42, 0x0FB1, 0x0F71 },             // 0F42 0FB1 0F71
                { 0xF67E, 0x0F42, 0x0FB1, 0x0FAD, 0x0F72 },             // 0F42 0FB1 0FAD 0F72
                { 0xF67F, 0x0F42, 0x0FB2, 0x0F71 },             // 0F42 0FB2 0F71
                { 0xF680, 0x0F42, 0x0FAD, 0x0F72 },             // 0F42 0FAD 0F72
                { 0xF681, 0x0F42, 0x0FB6 },             // 0F42 0FB6
                { 0xF682, 0x0F42, 0x0FB6, 0x0F72 },             // 0F42 0FB6 0F72
                { 0xF683, 0x0F42, 0x0FB7, 0x0F71, 0x0F74 },             // 0F42 0FB7 0F71 0F74
                { 0xF684, 0x0F42, 0x0FB7, 0x0FB2 },             // 0F42 0FB7 0FB2
                { 0xF685, 0x0F42, 0x0FB7, 0x0FB2, 0x0F71 },             // 0F42 0FB7 0FB2 0F71
                { 0xF686, 0x0F42, 0x0FB7, 0x0FB2, 0x0F72 },             // 0F42 0FB7 0FB2 0F72
                { 0xF687, 0x0F42, 0x0FB7, 0x0FB2, 0x0F71, 0x0F72 },             // 0F42 0FB7 0FB2 0F71 0F72
                { 0xF688, 0x0F42, 0x0FB7, 0x0FAD },             // 0F42 0FB7 0FAD
                { 0xF689, 0x0F42, 0x0FB7, 0x0FB3 },             // 0F42 0FB7 0FB3
                { 0xF68A, 0x0F44, 0x0F71, 0x0F74 },             // 0F44 0F71 0F74
                { 0xF68B, 0x0F44, 0x0F71, 0x0F7C },             // 0F44 0F71 0F7C
                { 0xF68C, 0x0F44, 0x0F90 },             // 0F44 0F90
                { 0xF68D, 0x0F44, 0x0F90, 0x0F71 },             // 0F44 0F90 0F71
                { 0xF68E, 0x0F44, 0x0F90, 0x0F74 },             // 0F44 0F90 0F74
                { 0xF68F, 0x0F44, 0x0F90, 0x0FB2, 0x0F80 },             // 0F44 0F90 0FB2 0F80
                { 0xF690, 0x0F44, 0x0F90, 0x0F7A },             // 0F44 0F90 0F7A
                { 0xF691, 0x0F44, 0x0F91 },             // 0F44 0F91
                { 0xF692, 0x0F44, 0x0F92 },             // 0F44 0F92
                { 0xF693, 0x0F44, 0x0F92, 0x0F71 },             // 0F44 0F92 0F71
                { 0xF694, 0x0F44, 0x0F92, 0x0F72 },             // 0F44 0F92 0F72
                { 0xF695, 0x0F44, 0x0F92, 0x0F71, 0x0F72 },             // 0F44 0F92 0F71 0F72
                { 0xF696, 0x0F44, 0x0F92, 0x0F74 },             // 0F44 0F92 0F74
                { 0xF697, 0x0F44, 0x0F92, 0x0F7A },             // 0F44 0F92 0F7A
                { 0xF698, 0x0F44, 0x0F94 },             // 0F44 0F94
                { 0xF699, 0x0F44, 0x0FB3 },             // 0F44 0FB3
                { 0xF69A, 0x0F44, 0x0FB4 },             // 0F44 0FB4
                { 0xF69B, 0x0F44, 0x0FB4, 0x0F72 },             // 0F44 0FB4 0F72
                { 0xF69C, 0x0F44, 0x0FB4, 0x0F74 },             // 0F44 0FB4 0F74
                { 0xF69D, 0x0F59, 0x0F84 },             // 0F59 0F84
                { 0xF69E, 0x0F59, 0x0F71, 0x0F72 },             // 0F59 0F71 0F72
                { 0xF69F, 0x0F59, 0x0F71, 0x0F80 },             // 0F59 0F71 0F80
                { 0xF6A0, 0x0F59, 0x0F71, 0x0F74 },             // 0F59 0F71 0F74
                { 0xF6A1, 0x0F59, 0x0FB2, 0x0F80 },             // 0F59 0FB2 0F80
                { 0xF6A2, 0x0F59, 0x0FB2, 0x0F71, 0x0F80 },             // 0F59 0FB2 0F71 0F80
                { 0xF6A3, 0x0F59, 0x0F71, 0x0F7A },             // 0F59 0F71 0F7A
                { 0xF6A4, 0x0F59, 0x0F7B },             // 0F59 0F7B
                { 0xF6A5, 0x0F59, 0x0F71, 0x0F7C },             // 0F59 0F71 0F7C
                { 0xF6A6, 0x0F59, 0x0F7D },             // 0F59 0F7D
                { 0xF6A7, 0x0F59, 0x0F7E },             // 0F59 0F7E
                { 0xF6A8, 0x0F59, 0x0FA9 },             // 0F59 0FA9
                { 0xF6A9, 0x0F59, 0x0FA9, 0x0F71 },             // 0F59 0FA9 0F71
                { 0xF6AA, 0x0F59, 0x0FAA },             // 0F59 0FAA
                { 0xF6AB, 0x0F59, 0x0FAA, 0x0F71 },             // 0F59 0FAA 0F71
                { 0xF6AC, 0x0F59, 0x0FAA, 0x0F72 },             // 0F59 0FAA 0F72
                { 0xF6AD, 0x0F59, 0x0FAA, 0x0F7A },             // 0F59 0FAA 0F7A
                { 0xF6AE, 0x0F59, 0x0FB1, 0x0F7B },             // 0F59 0FB1 0F7B
                { 0xF6AF, 0x0F59, 0x0FB3 },             // 0F59 0FB3
                { 0xF6B0, 0x0F59, 0x0FAD },             // 0F59 0FAD
                { 0xF6B1, 0x0F59, 0x0FAD, 0x0F71 },             // 0F59 0FAD 0F71
                { 0xF6B2, 0x0F5A, 0x0F71, 0x0F72 },             // 0F5A 0F71 0F72
                { 0xF6B3, 0x0F5A, 0x0F71, 0x0F74 },             // 0F5A 0F71 0F74
                { 0xF6B4, 0x0F5A, 0x0FA9 },             // 0F5A 0FA9
                { 0xF6B5, 0x0F5A, 0x0FAA, 0x0F71 },             // 0F5A 0FAA 0F71
                { 0xF6B6, 0x0F5A, 0x0FB1 },             // 0F5A 0FB1
                { 0xF6B7, 0x0F5A, 0x0FAD, 0x0F71 },             // 0F5A 0FAD 0F71
                { 0xF6B8, 0x0F5A, 0x0FAD, 0x0F7C },             // 0F5A 0FAD 0F7C
                { 0xF6B9, 0x0F5B, 0x0F71, 0x0F72 },             // 0F5B 0F71 0F72
                { 0xF6BA, 0x0F5B, 0x0F71, 0x0F74 },             // 0F5B 0F71 0F74
                { 0xF6BB, 0x0F5B, 0x0FB2, 0x0F80 },             // 0F5B 0FB2 0F80
                { 0xF6BC, 0x0F5B, 0x0F71, 0x0F7A },             // 0F5B 0F71 0F7A
                { 0xF6BD, 0x0F5B, 0x0FAB, 0x0F74 },             // 0F5B 0FAB 0F74
                { 0xF6BE, 0x0F5B, 0x0F99 },             // 0F5B 0F99
                { 0xF6BF, 0x0F5B, 0x0F99, 0x0F71 },             // 0F5B 0F99 0F71
                { 0xF6C0, 0x0F5B, 0x0F99, 0x0F72 },             // 0F5B 0F99 0F72
                { 0xF6C1, 0x0F5B, 0x0F99, 0x0F71, 0x0F72 },             // 0F5B 0F99 0F71 0F72
                { 0xF6C2, 0x0F5B, 0x0FA1 },             // 0F5B 0FA1
                { 0xF6C3, 0x0F5B, 0x0FB1 },             // 0F5B 0FB1
                { 0xF6C4, 0x0F5B, 0x0FB1, 0x0F7C },             // 0F5B 0FB1 0F7C
                { 0xF6C5, 0x0F5B, 0x0FB2 },             // 0F5B 0FB2
                { 0xF6C6, 0x0F5B, 0x0FB2, 0x0F71 },             // 0F5B 0FB2 0F71
                { 0xF6C7, 0x0F5B, 0x0FB2, 0x0F72 },             // 0F5B 0FB2 0F72
                { 0xF6C8, 0x0F5B, 0x0FB2, 0x0F7A },             // 0F5B 0FB2 0F7A
                { 0xF6C9, 0x0F5B, 0x0FAD },             // 0F5B 0FAD
                { 0xF6CA, 0x0F5B, 0x0FAD, 0x0F71, 0x0F72 },             // 0F5B 0FAD 0F71 0F72
                { 0xF6CB, 0x0F5B, 0x0FB7, 0x0F7B },             // 0F5B 0FB7 0F7B
                { 0xF6CC, 0x0F49, 0x0F71, 0x0F72 },             // 0F49 0F71 0F72
                { 0xF6CD, 0x0F49, 0x0F71, 0x0F80 },             // 0F49 0F71 0F80
                { 0xF6CE, 0x0F49, 0x0FA9 },             // 0F49 0FA9
                { 0xF6CF, 0x0F49, 0x0FA9, 0x0F71 },             // 0F49 0FA9 0F71
                { 0xF6D0, 0x0F49, 0x0FA9, 0x0F72 },             // 0F49 0FA9 0F72
                { 0xF6D1, 0x0F49, 0x0FA9, 0x0F74 },             // 0F49 0FA9 0F74
                { 0xF6D2, 0x0F49, 0x0FA9, 0x0F7C },             // 0F49 0FA9 0F7C
                { 0xF6D3, 0x0F49, 0x0FAA },             // 0F49 0FAA
                { 0xF6D4, 0x0F49, 0x0FAA, 0x0F72 },             // 0F49 0FAA 0F72
                { 0xF6D5, 0x0F49, 0x0FAB },             // 0F49 0FAB
                { 0xF6D6, 0x0F49, 0x0FAB, 0x0F71 },             // 0F49 0FAB 0F71
                { 0xF6D7, 0x0F49, 0x0FAB, 0x0F72 },             // 0F49 0FAB 0F72
                { 0xF6D8, 0x0F49, 0x0FAB, 0x0F74 },             // 0F49 0FAB 0F74
                { 0xF6D9, 0x0F49, 0x0FAB, 0x0FB7 },             // 0F49 0FAB 0FB7
                { 0xF6DA, 0x0F49, 0x0F99 },             // 0F49 0F99
                { 0xF6DB, 0x0F49, 0x0FB3, 0x0F72 },             // 0F49 0FB3 0F72
                { 0xF6DC, 0x0F4A, 0x0F71, 0x0F72 },             // 0F4A 0F71 0F72
                { 0xF6DD, 0x0F4A, 0x0F71, 0x0F74 },             // 0F4A 0F71 0F74
                { 0xF6DE, 0x0F4A, 0x0F7E },             // 0F4A 0F7E
                { 0xF6DF, 0x0F4A, 0x0F90 },             // 0F4A 0F90
                { 0xF6E0, 0x0F4A, 0x0F90, 0x0F7E },             // 0F4A 0F90 0F7E
                { 0xF6E1, 0x0F4A, 0x0F9A },             // 0F4A 0F9A
                { 0xF6E2, 0x0F4A, 0x0F9A, 0x0F72 },             // 0F4A 0F9A 0F72
                { 0xF6E3, 0x0F4A, 0x0FBB },             // 0F4A 0FBB
                { 0xF6E4, 0x0F4A, 0x0FBB, 0x0F7B },             // 0F4A 0FBB 0F7B
                { 0xF6E5, 0x0F4A, 0x0FAD, 0x0F71, 0x0F7E },             // 0F4A 0FAD 0F71 0F7E
                { 0xF6E6, 0x0F4B, 0x0FB2, 0x0F80 },             // 0F4B 0FB2 0F80
                { 0xF6E7, 0x0F4B, 0x0FB1 },             // 0F4B 0FB1
                { 0xF6E8, 0x0F4C, 0x0F71, 0x0F72 },             // 0F4C 0F71 0F72
                { 0xF6E9, 0x0F4C, 0x0F71, 0x0F74 },             // 0F4C 0F71 0F74
                { 0xF6EA, 0x0F4C, 0x0FB3, 0x0F80 },             // 0F4C 0FB3 0F80
                { 0xF6EB, 0x0F4C, 0x0F7E },             // 0F4C 0F7E
                { 0xF6EC, 0x0F4C, 0x0F7C, 0x0F7E },             // 0F4C 0F7C 0F7E
                { 0xF6ED, 0x0F4C, 0x0F92 },             // 0F4C 0F92
                { 0xF6EE, 0x0F4C, 0x0F92, 0x0FB2 },             // 0F4C 0F92 0FB2
                { 0xF6EF, 0x0F4C, 0x0F9C },             // 0F4C 0F9C
                { 0xF6F0, 0x0F4C, 0x0F9C, 0x0FB7 },             // 0F4C 0F9C 0FB7
                { 0xF6F1, 0x0F4C, 0x0F9E, 0x0F71 },             // 0F4C 0F9E 0F71
                { 0xF6F2, 0x0F4C, 0x0FBB },             // 0F4C 0FBB
                { 0xF6F3, 0x0F4C, 0x0FB7, 0x0F71, 0x0F74 },             // 0F4C 0FB7 0F71 0F74
                { 0xF6F4, 0x0F4C, 0x0FB7, 0x0FB2 },             // 0F4C 0FB7 0FB2
                { 0xF6F5, 0x0F4E, 0x0F71, 0x0F72 },             // 0F4E 0F71 0F72
                { 0xF6F6, 0x0F4E, 0x0F71, 0x0F74 },             // 0F4E 0F71 0F74
                { 0xF6F7, 0x0F4E, 0x0F71, 0x0F7E },             // 0F4E 0F71 0F7E
                { 0xF6F8, 0x0F4E, 0x0F9A },             // 0F4E 0F9A
                { 0xF6F9, 0x0F4E, 0x0F9A, 0x0F72 },             // 0F4E 0F9A 0F72
                { 0xF6FA, 0x0F4E, 0x0F9A, 0x0F7A },             // 0F4E 0F9A 0F7A
                { 0xF6FB, 0x0F4E, 0x0F9C },             // 0F4E 0F9C
                { 0xF6FC, 0x0F4E, 0x0F9C, 0x0F72 },             // 0F4E 0F9C 0F72
                { 0xF6FD, 0x0F4E, 0x0F9C, 0x0F7A },             // 0F4E 0F9C 0F7A
                { 0xF6FE, 0x0F4E, 0x0F9C, 0x0F7C },             // 0F4E 0F9C 0F7C
                { 0xF6FF, 0x0F4E, 0x0F9C, 0x0FB7 },             // 0F4E 0F9C 0FB7
                { 0xF700, 0x0F4E, 0x0F9E },             // 0F4E 0F9E
                { 0xF701, 0x0F4E, 0x0F9E, 0x0F72 },             // 0F4E 0F9E 0F72
                { 0xF702, 0x0F4E, 0x0F9E, 0x0F7A },             // 0F4E 0F9E 0F7A
                { 0xF703, 0x0F4E, 0x0FB1 },             // 0F4E 0FB1
                { 0xF704, 0x0F4E, 0x0FB1, 0x0F71 },             // 0F4E 0FB1 0F71
                { 0xF705, 0x0F4E, 0x0FB1, 0x0F7A },             // 0F4E 0FB1 0F7A
                { 0xF706, 0x0F4E, 0x0FB1, 0x0F7E },             // 0F4E 0FB1 0F7E
                { 0xF707, 0x0F4F, 0x0F71, 0x0F72 },             // 0F4F 0F71 0F72
                { 0xF708, 0x0F4F, 0x0FB2, 0x0F71, 0x0F80 },             // 0F4F 0FB2 0F71 0F80
                { 0xF709, 0x0F4F, 0x0F71, 0x0F7A },             // 0F4F 0F71 0F7A
                { 0xF70A, 0x0F4F, 0x0F7B },             // 0F4F 0F7B
                { 0xF70B, 0x0F4F, 0x0F71, 0x0F7C },             // 0F4F 0F71 0F7C
                { 0xF70C, 0x0F4F, 0x0F7D },             // 0F4F 0F7D
                { 0xF70D, 0x0F4F, 0x0F7E },             // 0F4F 0F7E
                { 0xF70E, 0x0F4F, 0x0F83 },             // 0F4F 0F83
                { 0xF70F, 0x0F4F, 0x0F71, 0x0F7E },             // 0F4F 0F71 0F7E
                { 0xF710, 0x0F4F, 0x0F72, 0x0F7E },             // 0F4F 0F72 0F7E
                { 0xF711, 0x0F4F, 0x0F71, 0x0F72, 0x0F7E },             // 0F4F 0F71 0F72 0F7E
                { 0xF712, 0x0F4F, 0x0F90 },             // 0F4F 0F90
                { 0xF713, 0x0F4F, 0x0F90, 0x0F71 },             // 0F4F 0F90 0F71
                { 0xF714, 0x0F4F, 0x0F90, 0x0F74 },             // 0F4F 0F90 0F74
                { 0xF715, 0x0F4F, 0x0F9F },             // 0F4F 0F9F
                { 0xF716, 0x0F4F, 0x0F9F, 0x0F71 },             // 0F4F 0F9F 0F71
                { 0xF717, 0x0F4F, 0x0F9F, 0x0F72 },             // 0F4F 0F9F 0F72
                { 0xF718, 0x0F4F, 0x0F9F, 0x0F74 },             // 0F4F 0F9F 0F74
                { 0xF719, 0x0F4F, 0x0F9F, 0x0F83 },             // 0F4F 0F9F 0F83
                { 0xF71A, 0x0F4F, 0x0F9F, 0x0F71, 0x0F7E },             // 0F4F 0F9F 0F71 0F7E
                { 0xF71B, 0x0F4F, 0x0F9F, 0x0FB2 },             // 0F4F 0F9F 0FB2
                { 0xF71C, 0x0F4F, 0x0FA0, 0x0F71 },             // 0F4F 0FA0 0F71
                { 0xF71D, 0x0F4F, 0x0FA3 },             // 0F4F 0FA3
                { 0xF71E, 0x0F4F, 0x0FA3, 0x0F71 },             // 0F4F 0FA3 0F71
                { 0xF71F, 0x0F4F, 0x0FA3, 0x0F7A },             // 0F4F 0FA3 0F7A
                { 0xF720, 0x0F4F, 0x0FA4 },             // 0F4F 0FA4
                { 0xF721, 0x0F4F, 0x0FA4, 0x0F74 },             // 0F4F 0FA4 0F74
                { 0xF722, 0x0F4F, 0x0FA4, 0x0F71, 0x0F74 },             // 0F4F 0FA4 0F71 0F74
                { 0xF723, 0x0F4F, 0x0FA8 },             // 0F4F 0FA8
                { 0xF724, 0x0F4F, 0x0FA8, 0x0F71 },             // 0F4F 0FA8 0F71
                { 0xF725, 0x0F4F, 0x0FA8, 0x0FB1 },             // 0F4F 0FA8 0FB1
                { 0xF726, 0x0F4F, 0x0FB1 },             // 0F4F 0FB1
                { 0xF727, 0x0F4F, 0x0FB1, 0x0F71 },             // 0F4F 0FB1 0F71
                { 0xF728, 0x0F4F, 0x0FB1, 0x0F74 },             // 0F4F 0FB1 0F74
                { 0xF729, 0x0F4F, 0x0FB1, 0x0F7A },             // 0F4F 0FB1 0F7A
                { 0xF72A, 0x0F4F, 0x0FB1, 0x0F7E },             // 0F4F 0FB1 0F7E
                { 0xF72B, 0x0F4F, 0x0FB2, 0x0F71 },             // 0F4F 0FB2 0F71
                { 0xF72C, 0x0F4F, 0x0FB2, 0x0F71, 0x0F72 },             // 0F4F 0FB2 0F71 0F72
                { 0xF72D, 0x0F4F, 0x0FB2, 0x0F71, 0x0F74 },             // 0F4F 0FB2 0F71 0F74
                { 0xF72E, 0x0F4F, 0x0FB2, 0x0F7B },             // 0F4F 0FB2 0F7B
                { 0xF72F, 0x0F4F, 0x0FB2, 0x0F7D },             // 0F4F 0FB2 0F7D
                { 0xF730, 0x0F4F, 0x0FB2, 0x0F7E },             // 0F4F 0FB2 0F7E
                { 0xF731, 0x0F4F, 0x0FB2, 0x0F71, 0x0F7E },             // 0F4F 0FB2 0F71 0F7E
                { 0xF732, 0x0F4F, 0x0FB2, 0x0F71, 0x0F83 },             // 0F4F 0FB2 0F71 0F83
                { 0xF733, 0x0F4F, 0x0FB2, 0x0F72, 0x0F7E },             // 0F4F 0FB2 0F72 0F7E
                { 0xF734, 0x0F4F, 0x0FB2, 0x0F71, 0x0F72, 0x0F7E },             // 0F4F 0FB2 0F71 0F72 0F7E
                { 0xF735, 0x0F4F, 0x0FBC, 0x0FB1 },             // 0F4F 0FBC 0FB1
                { 0xF736, 0x0F4F, 0x0FBC, 0x0FB1, 0x0F7E },             // 0F4F 0FBC 0FB1 0F7E
                { 0xF737, 0x0F4F, 0x0FAD },             // 0F4F 0FAD
                { 0xF738, 0x0F4F, 0x0FAD, 0x0F71 },             // 0F4F 0FAD 0F71
                { 0xF739, 0x0F4F, 0x0FAD, 0x0F72 },             // 0F4F 0FAD 0F72
                { 0xF73A, 0x0F4F, 0x0FAD, 0x0F7A },             // 0F4F 0FAD 0F7A
                { 0xF73B, 0x0F4F, 0x0FAD, 0x0F7E },             // 0F4F 0FAD 0F7E
                { 0xF73C, 0x0F4F, 0x0FAD, 0x0F71, 0x0F7E },             // 0F4F 0FAD 0F71 0F7E
                { 0xF73D, 0x0F4F, 0x0FB6, 0x0F7E },             // 0F4F 0FB6 0F7E
                { 0xF73E, 0x0F50, 0x0F71, 0x0F72 },             // 0F50 0F71 0F72
                { 0xF73F, 0x0F50, 0x0F71, 0x0F74 },             // 0F50 0F71 0F74
                { 0xF740, 0x0F50, 0x0FB2, 0x0F71, 0x0F80 },             // 0F50 0FB2 0F71 0F80
                { 0xF741, 0x0F50, 0x0F71, 0x0F7A },             // 0F50 0F71 0F7A
                { 0xF742, 0x0F50, 0x0F71, 0x0F7C },             // 0F50 0F71 0F7C
                { 0xF743, 0x0F50, 0x0FB1 },             // 0F50 0FB1
                { 0xF744, 0x0F50, 0x0FB3, 0x0FB7, 0x0F71, 0x0F72, 0x0F7E },             // 0F50 0FB3 0FB7 0F71 0F72 0F7E
                { 0xF745, 0x0F50, 0x0FAD },             // 0F50 0FAD
                { 0xF746, 0x0F50, 0x0FB7, 0x0FB2, 0x0F72 },             // 0F50 0FB7 0FB2 0F72
                { 0xF747, 0x0F51, 0x0F71, 0x0F72 },             // 0F51 0F71 0F72
                { 0xF748, 0x0F51, 0x0F71, 0x0F74 },             // 0F51 0F71 0F74
                { 0xF749, 0x0F51, 0x0F7B },             // 0F51 0F7B
                { 0xF74A, 0x0F51, 0x0F71, 0x0F7C },             // 0F51 0F71 0F7C
                { 0xF74B, 0x0F51, 0x0F7D },             // 0F51 0F7D
                { 0xF74C, 0x0F51, 0x0F7E },             // 0F51 0F7E
                { 0xF74D, 0x0F51, 0x0F74, 0x0F7E },             // 0F51 0F74 0F7E
                { 0xF74E, 0x0F51, 0x0F92 },             // 0F51 0F92
                { 0xF74F, 0x0F51, 0x0F92, 0x0F71 },             // 0F51 0F92 0F71
                { 0xF750, 0x0F51, 0x0F92, 0x0FB7 },             // 0F51 0F92 0FB7
                { 0xF751, 0x0F51, 0x0F92, 0x0FB7, 0x0F71 },             // 0F51 0F92 0FB7 0F71
                { 0xF752, 0x0F51, 0x0F94 },             // 0F51 0F94
                { 0xF753, 0x0F51, 0x0FA1 },             // 0F51 0FA1
                { 0xF754, 0x0F51, 0x0FA1, 0x0F71 },             // 0F51 0FA1 0F71
                { 0xF755, 0x0F51, 0x0FA1, 0x0FB7 },             // 0F51 0FA1 0FB7
                { 0xF756, 0x0F51, 0x0FA1, 0x0FB7, 0x0F71 },             // 0F51 0FA1 0FB7 0F71
                { 0xF757, 0x0F51, 0x0FA1, 0x0FB7, 0x0F72 },             // 0F51 0FA1 0FB7 0F72
                { 0xF758, 0x0F51, 0x0FA1, 0x0FB7, 0x0F71, 0x0F72 },             // 0F51 0FA1 0FB7 0F71 0F72
                { 0xF759, 0x0F51, 0x0FA1, 0x0FB7, 0x0F74 },             // 0F51 0FA1 0FB7 0F74
                { 0xF75A, 0x0F51, 0x0FA1, 0x0FB7, 0x0F7A },             // 0F51 0FA1 0FB7 0F7A
                { 0xF75B, 0x0F51, 0x0FA1, 0x0FB7, 0x0F7C },             // 0F51 0FA1 0FB7 0F7C
                { 0xF75C, 0x0F51, 0x0FA1, 0x0FB7, 0x0FB1 },             // 0F51 0FA1 0FB7 0FB1
                { 0xF75D, 0x0F51, 0x0FA1, 0x0FB7, 0x0FB2 },             // 0F51 0FA1 0FB7 0FB2
                { 0xF75E, 0x0F51, 0x0FA3 },             // 0F51 0FA3
                { 0xF75F, 0x0F51, 0x0FA6, 0x0FB7 },             // 0F51 0FA6 0FB7
                { 0xF760, 0x0F51, 0x0FA6, 0x0FB7, 0x0F74 },             // 0F51 0FA6 0FB7 0F74
                { 0xF761, 0x0F51, 0x0FA6, 0x0FB7, 0x0F71, 0x0F74 },             // 0F51 0FA6 0FB7 0F71 0F74
                { 0xF762, 0x0F51, 0x0FA8 },             // 0F51 0FA8
                { 0xF763, 0x0F51, 0x0FA8, 0x0F71 },             // 0F51 0FA8 0F71
                { 0xF764, 0x0F51, 0x0FA8, 0x0F72 },             // 0F51 0FA8 0F72
                { 0xF765, 0x0F51, 0x0FA8, 0x0F74 },             // 0F51 0FA8 0F74
                { 0xF766, 0x0F51, 0x0FA8, 0x0F7A },             // 0F51 0FA8 0F7A
                { 0xF767, 0x0F51, 0x0FA8, 0x0F7C },             // 0F51 0FA8 0F7C
                { 0xF768, 0x0F51, 0x0FBB },             // 0F51 0FBB
                { 0xF769, 0x0F51, 0x0FBB, 0x0F71 },             // 0F51 0FBB 0F71
                { 0xF76A, 0x0F51, 0x0FBB, 0x0F7A },             // 0F51 0FBB 0F7A
                { 0xF76B, 0x0F51, 0x0FBB, 0x0F7C },             // 0F51 0FBB 0F7C
                { 0xF76C, 0x0F51, 0x0FB2, 0x0F71 },             // 0F51 0FB2 0F71
                { 0xF76D, 0x0F51, 0x0FB2, 0x0F71, 0x0F72 },             // 0F51 0FB2 0F71 0F72
                { 0xF76E, 0x0F51, 0x0FB2, 0x0F71, 0x0F74 },             // 0F51 0FB2 0F71 0F74
                { 0xF76F, 0x0F51, 0x0FB2, 0x0F7B },             // 0F51 0FB2 0F7B
                { 0xF770, 0x0F51, 0x0FB2, 0x0F7E },             // 0F51 0FB2 0F7E
                { 0xF771, 0x0F51, 0x0FB2, 0x0F71, 0x0F7E },             // 0F51 0FB2 0F71 0F7E
                { 0xF772, 0x0F51, 0x0FB2, 0x0F71, 0x0F74, 0x0F83 },             // 0F51 0FB2 0F71 0F74 0F83
                { 0xF773, 0x0F51, 0x0FAD, 0x0F71 },             // 0F51 0FAD 0F71
                { 0xF774, 0x0F51, 0x0FAD, 0x0F71, 0x0F72 },             // 0F51 0FAD 0F71 0F72
                { 0xF775, 0x0F51, 0x0FAD, 0x0F7E },             // 0F51 0FAD 0F7E
                { 0xF776, 0x0F51, 0x0FB7, 0x0F71, 0x0F72 },             // 0F51 0FB7 0F71 0F72
                { 0xF777, 0x0F51, 0x0FB7, 0x0F71, 0x0F74 },             // 0F51 0FB7 0F71 0F74
                { 0xF778, 0x0F51, 0x0FB7, 0x0FB2, 0x0F80 },             // 0F51 0FB7 0FB2 0F80
                { 0xF779, 0x0F51, 0x0FB7, 0x0FB1 },             // 0F51 0FB7 0FB1
                { 0xF77A, 0x0F51, 0x0FB7, 0x0FB1, 0x0F71 },             // 0F51 0FB7 0FB1 0F71
                { 0xF77B, 0x0F51, 0x0FB7, 0x0FB1, 0x0F7A },             // 0F51 0FB7 0FB1 0F7A
                { 0xF77C, 0x0F51, 0x0FB7, 0x0FB2, 0x0F74 },             // 0F51 0FB7 0FB2 0F74
                { 0xF77D, 0x0F51, 0x0FB7, 0x0FAD },             // 0F51 0FB7 0FAD
                { 0xF77E, 0x0F51, 0x0FB7, 0x0FAD, 0x0F71 },             // 0F51 0FB7 0FAD 0F71
                { 0xF77F, 0x0F51, 0x0FB7, 0x0FAD, 0x0F7E },             // 0F51 0FB7 0FAD 0F7E
                { 0xF780, 0x0F53, 0x0F71, 0x0F72 },             // 0F53 0F71 0F72
                { 0xF781, 0x0F53, 0x0F71, 0x0F74 },             // 0F53 0F71 0F74
                { 0xF782, 0x0F53, 0x0FB2, 0x0F80 },             // 0F53 0FB2 0F80
                { 0xF783, 0x0F53, 0x0F71, 0x0F7A },             // 0F53 0F71 0F7A
                { 0xF784, 0x0F53, 0x0F7B },             // 0F53 0F7B
                { 0xF785, 0x0F53, 0x0F71, 0x0F7C },             // 0F53 0F71 0F7C
                { 0xF786, 0x0F53, 0x0F7D },             // 0F53 0F7D
                { 0xF787, 0x0F53, 0x0F7E },             // 0F53 0F7E
                { 0xF788, 0x0F53, 0x0F90 },             // 0F53 0F90
                { 0xF789, 0x0F53, 0x0FAB },             // 0F53 0FAB
                { 0xF78A, 0x0F53, 0x0F9F },             // 0F53 0F9F
                { 0xF78B, 0x0F53, 0x0F9F, 0x0F71 },             // 0F53 0F9F 0F71
                { 0xF78C, 0x0F53, 0x0F9F, 0x0F72 },             // 0F53 0F9F 0F72
                { 0xF78D, 0x0F53, 0x0F9F, 0x0F71, 0x0F72 },             // 0F53 0F9F 0F71 0F72
                { 0xF78E, 0x0F53, 0x0F9F, 0x0F74 },             // 0F53 0F9F 0F74
                { 0xF78F, 0x0F53, 0x0F9F, 0x0F71, 0x0F74 },             // 0F53 0F9F 0F71 0F74
                { 0xF790, 0x0F53, 0x0F9F, 0x0F7A },             // 0F53 0F9F 0F7A
                { 0xF791, 0x0F53, 0x0F9F, 0x0F7E },             // 0F53 0F9F 0F7E
                { 0xF792, 0x0F53, 0x0F9F, 0x0FB2 },             // 0F53 0F9F 0FB2
                { 0xF793, 0x0F53, 0x0F9F, 0x0FB2, 0x0F71 },             // 0F53 0F9F 0FB2 0F71
                { 0xF794, 0x0F53, 0x0F9F, 0x0FB2, 0x0F72 },             // 0F53 0F9F 0FB2 0F72
                { 0xF795, 0x0F53, 0x0FA0 },             // 0F53 0FA0
                { 0xF796, 0x0F53, 0x0FA0, 0x0F71 },             // 0F53 0FA0 0F71
                { 0xF797, 0x0F53, 0x0FA1 },             // 0F53 0FA1
                { 0xF798, 0x0F53, 0x0FA1, 0x0F72 },             // 0F53 0FA1 0F72
                { 0xF799, 0x0F53, 0x0FA1, 0x0F74 },             // 0F53 0FA1 0F74
                { 0xF79A, 0x0F53, 0x0FA1, 0x0F7A },             // 0F53 0FA1 0F7A
                { 0xF79B, 0x0F53, 0x0FA1, 0x0FB2 },             // 0F53 0FA1 0FB2
                { 0xF79C, 0x0F53, 0x0FA1, 0x0FB2, 0x0F71 },             // 0F53 0FA1 0FB2 0F71
                { 0xF79D, 0x0F53, 0x0FA1, 0x0FB2, 0x0F72 },             // 0F53 0FA1 0FB2 0F72
                { 0xF79E, 0x0F53, 0x0FA1, 0x0FB2, 0x0F71, 0x0F72 },             // 0F53 0FA1 0FB2 0F71 0F72
                { 0xF79F, 0x0F53, 0x0FA1, 0x0FB7 },             // 0F53 0FA1 0FB7
                { 0xF7A0, 0x0F53, 0x0FA1, 0x0FB7, 0x0F72 },             // 0F53 0FA1 0FB7 0F72
                { 0xF7A1, 0x0F53, 0x0FA1, 0x0FB7, 0x0F74 },             // 0F53 0FA1 0FB7 0F74
                { 0xF7A2, 0x0F53, 0x0FA1, 0x0FB7, 0x0F7A },             // 0F53 0FA1 0FB7 0F7A
                { 0xF7A3, 0x0F53, 0x0FA1, 0x0FB7, 0x0F7C },             // 0F53 0FA1 0FB7 0F7C
                { 0xF7A4, 0x0F53, 0x0FA3 },             // 0F53 0FA3
                { 0xF7A5, 0x0F53, 0x0FA3, 0x0F71 },             // 0F53 0FA3 0F71
                { 0xF7A6, 0x0F53, 0x0FA3, 0x0F72 },             // 0F53 0FA3 0F72
                { 0xF7A7, 0x0F53, 0x0FA3, 0x0F74 },             // 0F53 0FA3 0F74
                { 0xF7A8, 0x0F53, 0x0FA3, 0x0F7A },             // 0F53 0FA3 0F7A
                { 0xF7A9, 0x0F53, 0x0FA4, 0x0F71 },             // 0F53 0FA4 0F71
                { 0xF7AA, 0x0F53, 0x0FA6, 0x0FB2, 0x0F72 },             // 0F53 0FA6 0FB2 0F72
                { 0xF7AB, 0x0F53, 0x0FA6, 0x0FB7 },             // 0F53 0FA6 0FB7
                { 0xF7AC, 0x0F53, 0x0FA8 },             // 0F53 0FA8
                { 0xF7AD, 0x0F53, 0x0FA8, 0x0F71 },             // 0F53 0FA8 0F71
                { 0xF7AE, 0x0F53, 0x0FA8, 0x0F72 },             // 0F53 0FA8 0F72
                { 0xF7AF, 0x0F53, 0x0FB1 },             // 0F53 0FB1
                { 0xF7B0, 0x0F53, 0x0FB1, 0x0F71 },             // 0F53 0FB1 0F71
                { 0xF7B1, 0x0F53, 0x0FB1, 0x0F7C },             // 0F53 0FB1 0F7C
                { 0xF7B2, 0x0F53, 0x0FB1, 0x0F7E },             // 0F53 0FB1 0F7E
                { 0xF7B3, 0x0F53, 0x0FB1, 0x0F71, 0x0F7E },             // 0F53 0FB1 0F71 0F7E
                { 0xF7B4, 0x0F53, 0x0FB2 },             // 0F53 0FB2
                { 0xF7B5, 0x0F53, 0x0FB2, 0x0F71 },             // 0F53 0FB2 0F71
                { 0xF7B6, 0x0F53, 0x0FB2, 0x0F7A },             // 0F53 0FB2 0F7A
                { 0xF7B7, 0x0F53, 0x0FB3 },             // 0F53 0FB3
                { 0xF7B8, 0x0F53, 0x0FAD },             // 0F53 0FAD
                { 0xF7B9, 0x0F53, 0x0FAD, 0x0F71 },             // 0F53 0FAD 0F71
                { 0xF7BA, 0x0F54, 0x0F71, 0x0F72 },             // 0F54 0F71 0F72
                { 0xF7BB, 0x0F54, 0x0F71, 0x0F74 },             // 0F54 0F71 0F74
                { 0xF7BC, 0x0F54, 0x0F71, 0x0F7A },             // 0F54 0F71 0F7A
                { 0xF7BD, 0x0F54, 0x0F7B },             // 0F54 0F7B
                { 0xF7BE, 0x0F54, 0x0F71, 0x0F7C },             // 0F54 0F71 0F7C
                { 0xF7BF, 0x0F54, 0x0F7D },             // 0F54 0F7D
                { 0xF7C0, 0x0F54, 0x0F7E },             // 0F54 0F7E
                { 0xF7C1, 0x0F54, 0x0F74, 0x0F7E },             // 0F54 0F74 0F7E
                { 0xF7C2, 0x0F89, 0x0FA4 },             // 0F89 0FA4
                { 0xF7C3, 0x0F54, 0x0F9F },             // 0F54 0F9F
                { 0xF7C4, 0x0F54, 0x0F9F, 0x0F71 },             // 0F54 0F9F 0F71
                { 0xF7C5, 0x0F54, 0x0F9F, 0x0F72 },             // 0F54 0F9F 0F72
                { 0xF7C6, 0x0F54, 0x0F9F, 0x0F7A },             // 0F54 0F9F 0F7A
                { 0xF7C7, 0x0F54, 0x0F9F, 0x0F7C },             // 0F54 0F9F 0F7C
                { 0xF7C8, 0x0F54, 0x0FA1 },             // 0F54 0FA1
                { 0xF7C9, 0x0F54, 0x0FB1, 0x0FAD },             // 0F54 0FB1 0FAD
                { 0xF7CA, 0x0F54, 0x0FB2, 0x0F71 },             // 0F54 0FB2 0F71
                { 0xF7CB, 0x0F54, 0x0FB2, 0x0F71, 0x0F72 },             // 0F54 0FB2 0F71 0F72
                { 0xF7CC, 0x0F54, 0x0FB2, 0x0F71, 0x0F74 },             // 0F54 0FB2 0F71 0F74
                { 0xF7CD, 0x0F54, 0x0FA5 },             // 0F54 0FA5
                { 0xF7CE, 0x0F54, 0x0FB3, 0x0F74 },             // 0F54 0FB3 0F74
                { 0xF7CF, 0x0F55, 0x0F71, 0x0F72 },             // 0F55 0F71 0F72
                { 0xF7D0, 0x0F55, 0x0F71, 0x0F74 },             // 0F55 0F71 0F74
                { 0xF7D1, 0x0F55, 0x0F71, 0x0F7C },             // 0F55 0F71 0F7C
                { 0xF7D2, 0x0F55, 0x0F7A, 0x0F7E },             // 0F55 0F7A 0F7E
                { 0xF7D3, 0x0F89, 0x0FA5 },             // 0F89 0FA5
                { 0xF7D4, 0x0F55, 0x0FA5 },             // 0F55 0FA5
                { 0xF7D5, 0x0F55, 0x0FB1, 0x0F71 },             // 0F55 0FB1 0F71
                { 0xF7D6, 0x0F55, 0x0FB2, 0x0F71, 0x0F72 },             // 0F55 0FB2 0F71 0F72
                { 0xF7D7, 0x0F55, 0x0FB2, 0x0F74, 0x0F7E },             // 0F55 0FB2 0F74 0F7E
                { 0xF7D8, 0x0F55, 0x0FB2, 0x0FAD },             // 0F55 0FB2 0FAD
                { 0xF7D9, 0x0F56, 0x0F71, 0x0F72 },             // 0F56 0F71 0F72
                { 0xF7DA, 0x0F56, 0x0F71, 0x0F74 },             // 0F56 0F71 0F74
                { 0xF7DB, 0x0F56, 0x0F71, 0x0F7A },             // 0F56 0F71 0F7A
                { 0xF7DC, 0x0F56, 0x0F7B },             // 0F56 0F7B
                { 0xF7DD, 0x0F56, 0x0F71, 0x0F7C },             // 0F56 0F71 0F7C
                { 0xF7DE, 0x0F56, 0x0F7E },             // 0F56 0F7E
                { 0xF7DF, 0x0F56, 0x0F90 },             // 0F56 0F90
                { 0xF7E0, 0x0F56, 0x0F94 },             // 0F56 0F94
                { 0xF7E1, 0x0F56, 0x0FAB },             // 0F56 0FAB
                { 0xF7E2, 0x0F56, 0x0F9F },             // 0F56 0F9F
                { 0xF7E3, 0x0F56, 0x0FA1 },             // 0F56 0FA1
                { 0xF7E4, 0x0F56, 0x0FA1, 0x0FB7, 0x0F72 },             // 0F56 0FA1 0FB7 0F72
                { 0xF7E5, 0x0F56, 0x0FA6, 0x0F72 },             // 0F56 0FA6 0F72
                { 0xF7E6, 0x0F56, 0x0FB1, 0x0F71 },             // 0F56 0FB1 0F71
                { 0xF7E7, 0x0F56, 0x0FB1, 0x0F71, 0x0F74 },             // 0F56 0FB1 0F71 0F74
                { 0xF7E8, 0x0F56, 0x0FB2, 0x0F71 },             // 0F56 0FB2 0F71
                { 0xF7E9, 0x0F56, 0x0FB2, 0x0F71, 0x0F72 },             // 0F56 0FB2 0F71 0F72
                { 0xF7EA, 0x0F56, 0x0FB2, 0x0F71, 0x0F74 },             // 0F56 0FB2 0F71 0F74
                { 0xF7EB, 0x0F56, 0x0FB2, 0x0F71, 0x0F74, 0x0F7E },             // 0F56 0FB2 0F71 0F74 0F7E
                { 0xF7EC, 0x0F56, 0x0FB6 },             // 0F56 0FB6
                { 0xF7ED, 0x0F56, 0x0FB7, 0x0F71, 0x0F72 },             // 0F56 0FB7 0F71 0F72
                { 0xF7EE, 0x0F56, 0x0FB7, 0x0F71, 0x0F74 },             // 0F56 0FB7 0F71 0F74
                { 0xF7EF, 0x0F56, 0x0FB7, 0x0FB2, 0x0F80 },             // 0F56 0FB7 0FB2 0F80
                { 0xF7F0, 0x0F56, 0x0FB7, 0x0FB2, 0x0F71, 0x0F80 },             // 0F56 0FB7 0FB2 0F71 0F80
                { 0xF7F1, 0x0F56, 0x0FB7, 0x0F7B },             // 0F56 0FB7 0F7B
                { 0xF7F2, 0x0F56, 0x0FB7, 0x0F7E },             // 0F56 0FB7 0F7E
                { 0xF7F3, 0x0F56, 0x0FB7, 0x0FB1 },             // 0F56 0FB7 0FB1
                { 0xF7F4, 0x0F56, 0x0FB7, 0x0FB1, 0x0F71 },             // 0F56 0FB7 0FB1 0F71
                { 0xF7F5, 0x0F56, 0x0FB7, 0x0FB1, 0x0F7A },             // 0F56 0FB7 0FB1 0F7A
                { 0xF7F6, 0x0F56, 0x0FB7, 0x0FB1, 0x0F7B },             // 0F56 0FB7 0FB1 0F7B
                { 0xF7F7, 0x0F56, 0x0FB7, 0x0FB1, 0x0F7C },             // 0F56 0FB7 0FB1 0F7C
                { 0xF7F8, 0x0F56, 0x0FB7, 0x0FB1, 0x0F7E },             // 0F56 0FB7 0FB1 0F7E
                { 0xF7F9, 0x0F56, 0x0FB7, 0x0FB1, 0x0F71, 0x0F7E },             // 0F56 0FB7 0FB1 0F71 0F7E
                { 0xF7FA, 0x0F56, 0x0FB7, 0x0FB2 },             // 0F56 0FB7 0FB2
                { 0xF7FB, 0x0F56, 0x0FB7, 0x0FB2, 0x0F71 },             // 0F56 0FB7 0FB2 0F71
                { 0xF7FC, 0x0F56, 0x0FB7, 0x0FB2, 0x0F72 },             // 0F56 0FB7 0FB2 0F72
                { 0xF7FD, 0x0F56, 0x0FB7, 0x0FB2, 0x0F74 },             // 0F56 0FB7 0FB2 0F74
                { 0xF7FE, 0x0F56, 0x0FB7, 0x0FB2, 0x0F7E },             // 0F56 0FB7 0FB2 0F7E
                { 0xF7FF, 0x0F56, 0x0FB7, 0x0FB2, 0x0F71, 0x0F7E },             // 0F56 0FB7 0FB2 0F71 0F7E
                { 0xF800, 0x0F56, 0x0FB7, 0x0FB2, 0x0F74, 0x0F7E },             // 0F56 0FB7 0FB2 0F74 0F7E
                { 0xF801, 0x0F56, 0x0FB7, 0x0FB2, 0x0F74, 0x0F83 },             // 0F56 0FB7 0FB2 0F74 0F83
                { 0xF802, 0x0F56, 0x0FB7, 0x0FB2, 0x0F71, 0x0F74, 0x0F7E },             // 0F56 0FB7 0FB2 0F71 0F74 0F7E
                { 0xF803, 0x0F56, 0x0FB7, 0x0FB2, 0x0F71, 0x0F74, 0x0F83 },             // 0F56 0FB7 0FB2 0F71 0F74 0F83
                { 0xF804, 0x0F56, 0x0FB7, 0x0FB3 },             // 0F56 0FB7 0FB3
                { 0xF805, 0x0F56, 0x0FB7, 0x0FAD, 0x0F71 },             // 0F56 0FB7 0FAD 0F71
                { 0xF806, 0x0F56, 0x0FB7, 0x0FAD },             // 0F56 0FB7 0FAD
                { 0xF807, 0x0F58, 0x0F71, 0x0F72 },             // 0F58 0F71 0F72
                { 0xF808, 0x0F58, 0x0F71, 0x0F74 },             // 0F58 0F71 0F74
                { 0xF809, 0x0F58, 0x0F71, 0x0F7A },             // 0F58 0F71 0F7A
                { 0xF80A, 0x0F58, 0x0F7B },             // 0F58 0F7B
                { 0xF80B, 0x0F58, 0x0F71, 0x0F7C },             // 0F58 0F71 0F7C
                { 0xF80C, 0x0F58, 0x0F7D },             // 0F58 0F7D
                { 0xF80D, 0x0F58, 0x0F7E },             // 0F58 0F7E
                { 0xF80E, 0x0F58, 0x0F71, 0x0F7E },             // 0F58 0F71 0F7E
                { 0xF80F, 0x0F58, 0x0F7D, 0x0F7E },             // 0F58 0F7D 0F7E
                { 0xF810, 0x0F58, 0x0F92 },             // 0F58 0F92
                { 0xF811, 0x0F58, 0x0F94 },             // 0F58 0F94
                { 0xF812, 0x0F58, 0x0FA4 },             // 0F58 0FA4
                { 0xF813, 0x0F58, 0x0FA4, 0x0F72 },             // 0F58 0FA4 0F72
                { 0xF814, 0x0F58, 0x0FA5 },             // 0F58 0FA5
                { 0xF815, 0x0F58, 0x0FA6 },             // 0F58 0FA6
                { 0xF816, 0x0F58, 0x0FA6, 0x0F72 },             // 0F58 0FA6 0F72
                { 0xF817, 0x0F58, 0x0FA6, 0x0F74 },             // 0F58 0FA6 0F74
                { 0xF818, 0x0F58, 0x0FA6, 0x0F7C },             // 0F58 0FA6 0F7C
                { 0xF819, 0x0F58, 0x0FA6, 0x0FB7 },             // 0F58 0FA6 0FB7
                { 0xF81A, 0x0F58, 0x0FA6, 0x0FB7, 0x0F72 },             // 0F58 0FA6 0FB7 0F72
                { 0xF81B, 0x0F58, 0x0FA6, 0x0FB7, 0x0F74 },             // 0F58 0FA6 0FB7 0F74
                { 0xF81C, 0x0F58, 0x0FA6, 0x0FB7, 0x0F7A },             // 0F58 0FA6 0FB7 0F7A
                { 0xF81D, 0x0F58, 0x0FA6, 0x0FB7, 0x0F7C },             // 0F58 0FA6 0FB7 0F7C
                { 0xF81E, 0x0F58, 0x0FA6, 0x0FB7, 0x0FB2 },             // 0F58 0FA6 0FB7 0FB2
                { 0xF81F, 0x0F58, 0x0FA8 },             // 0F58 0FA8
                { 0xF820, 0x0F58, 0x0FA8, 0x0F71 },             // 0F58 0FA8 0F71
                { 0xF821, 0x0F58, 0x0FA8, 0x0F72 },             // 0F58 0FA8 0F72
                { 0xF822, 0x0F58, 0x0FB1, 0x0FAD },             // 0F58 0FB1 0FAD
                { 0xF823, 0x0F58, 0x0FB2, 0x0F71 },             // 0F58 0FB2 0F71
                { 0xF824, 0x0F58, 0x0FB3 },             // 0F58 0FB3
                { 0xF825, 0x0F58, 0x0FAD },             // 0F58 0FAD
                { 0xF826, 0x0F58, 0x0FB6 },             // 0F58 0FB6
                { 0xF827, 0x0F61, 0x0F71, 0x0F72 },             // 0F61 0F71 0F72
                { 0xF828, 0x0F61, 0x0F71, 0x0F74 },             // 0F61 0F71 0F74
                { 0xF829, 0x0F61, 0x0F71, 0x0F7C },             // 0F61 0F71 0F7C
                { 0xF82A, 0x0F61, 0x0F71, 0x0F7E },             // 0F61 0F71 0F7E
                { 0xF82B, 0x0F61, 0x0F94 },             // 0F61 0F94
                { 0xF82C, 0x0F61, 0x0F9F, 0x0F7C },             // 0F61 0F9F 0F7C
                { 0xF82D, 0x0F61, 0x0FA3 },             // 0F61 0FA3
                { 0xF82E, 0x0F61, 0x0FA3, 0x0F72 },             // 0F61 0FA3 0F72
                { 0xF82F, 0x0F61, 0x0FA6 },             // 0F61 0FA6
                { 0xF830, 0x0F61, 0x0FA8 },             // 0F61 0FA8
                { 0xF831, 0x0F61, 0x0FB1 },             // 0F61 0FB1
                { 0xF832, 0x0F61, 0x0FB2 },             // 0F61 0FB2
                { 0xF833, 0x0F61, 0x0FAD },             // 0F61 0FAD
                { 0xF834, 0x0F62, 0x0F71, 0x0F72 },             // 0F62 0F71 0F72
                { 0xF835, 0x0F62, 0x0F71, 0x0F74 },             // 0F62 0F71 0F74
                { 0xF836, 0x0F62, 0x0F7B },             // 0F62 0F7B
                { 0xF837, 0x0F62, 0x0F71, 0x0F7C },             // 0F62 0F71 0F7C
                { 0xF838, 0x0F62, 0x0F7D },             // 0F62 0F7D
                { 0xF839, 0x0F62, 0x0F7E },             // 0F62 0F7E
                { 0xF83A, 0x0F62, 0x0F92, 0x0FB7 },             // 0F62 0F92 0FB7
                { 0xF83B, 0x0F62, 0x0F92, 0x0FB7, 0x0F7E },             // 0F62 0F92 0FB7 0F7E
                { 0xF83C, 0x0F62, 0x0FAA, 0x0F80 },             // 0F62 0FAA 0F80
                { 0xF83D, 0x0F62, 0x0FAB, 0x0FAB },             // 0F62 0FAB 0FAB
                { 0xF83E, 0x0F62, 0x0FAB, 0x0F99, 0x0F71 },             // 0F62 0FAB 0F99 0F71
                { 0xF83F, 0x0F62, 0x0F9A },             // 0F62 0F9A
                { 0xF840, 0x0F62, 0x0F9E },             // 0F62 0F9E
                { 0xF841, 0x0F62, 0x0F9E, 0x0F71 },             // 0F62 0F9E 0F71
                { 0xF842, 0x0F62, 0x0F9E, 0x0F72 },             // 0F62 0F9E 0F72
                { 0xF843, 0x0F62, 0x0F9E, 0x0F9E },             // 0F62 0F9E 0F9E
                { 0xF844, 0x0F62, 0x0F9F, 0x0F71 },             // 0F62 0F9F 0F71
                { 0xF845, 0x0F62, 0x0F9F, 0x0F71, 0x0F72 },             // 0F62 0F9F 0F71 0F72
                { 0xF846, 0x0F62, 0x0F9F, 0x0F9F },             // 0F62 0F9F 0F9F
                { 0xF847, 0x0F62, 0x0F9F, 0x0F9F, 0x0F72 },             // 0F62 0F9F 0F9F 0F72
                { 0xF848, 0x0F62, 0x0F9F, 0x0FA8 },             // 0F62 0F9F 0FA8
                { 0xF849, 0x0F6A, 0x0FA0 },             // 0F6A 0FA0
                { 0xF84A, 0x0F6A, 0x0FA0, 0x0F72 },             // 0F6A 0FA0 0F72
                { 0xF84B, 0x0F62, 0x0FA1, 0x0F71 },             // 0F62 0FA1 0F71
                { 0xF84C, 0x0F62, 0x0FA1, 0x0FB2, 0x0F71 },             // 0F62 0FA1 0FB2 0F71
                { 0xF84D, 0x0F62, 0x0FA1, 0x0FB2, 0x0F72 },             // 0F62 0FA1 0FB2 0F72
                { 0xF84E, 0x0F62, 0x0FA1, 0x0FB7 },             // 0F62 0FA1 0FB7
                { 0xF84F, 0x0F62, 0x0FA1, 0x0FB7, 0x0F72 },             // 0F62 0FA1 0FB7 0F72
                { 0xF850, 0x0F62, 0x0FA4 },             // 0F62 0FA4
                { 0xF851, 0x0F62, 0x0FA6, 0x0F71 },             // 0F62 0FA6 0F71
                { 0xF852, 0x0F62, 0x0FA6, 0x0F7E },             // 0F62 0FA6 0F7E
                { 0xF853, 0x0F62, 0x0FA6, 0x0FA6 },             // 0F62 0FA6 0FA6
                { 0xF854, 0x0F62, 0x0FA6, 0x0FA6, 0x0F72 },             // 0F62 0FA6 0FA6 0F72
                { 0xF855, 0x0F62, 0x0FA6, 0x0FB7 },             // 0F62 0FA6 0FB7
                { 0xF856, 0x0F62, 0x0FA6, 0x0FB7, 0x0F72 },             // 0F62 0FA6 0FB7 0F72
                { 0xF857, 0x0F62, 0x0FA6, 0x0FB7, 0x0F7A },             // 0F62 0FA6 0FB7 0F7A
                { 0xF858, 0x0F62, 0x0FA8, 0x0F71 },             // 0F62 0FA8 0F71
                { 0xF859, 0x0F62, 0x0FA8, 0x0FA8 },             // 0F62 0FA8 0FA8
                { 0xF85A, 0x0F62, 0x0FA8, 0x0FA8, 0x0F71 },             // 0F62 0FA8 0FA8 0F71
                { 0xF85B, 0x0F62, 0x0FB1 },             // 0F62 0FB1
                { 0xF85C, 0x0F62, 0x0FB1, 0x0F71 },             // 0F62 0FB1 0F71
                { 0xF85D, 0x0F62, 0x0FB1, 0x0F7C },             // 0F62 0FB1 0F7C
                { 0xF85E, 0x0F62, 0x0FB1, 0x0F7E },             // 0F62 0FB1 0F7E
                { 0xF85F, 0x0F62, 0x0FBB, 0x0FB1 },             // 0F62 0FBB 0FB1
                { 0xF860, 0x0F62, 0x0FB3, 0x0FB7 },             // 0F62 0FB3 0FB7
                { 0xF861, 0x0F62, 0x0FB3, 0x0FB7, 0x0F7A },             // 0F62 0FB3 0FB7 0F7A
                { 0xF862, 0x0F62, 0x0FAD, 0x0F72 },             // 0F62 0FAD 0F72
                { 0xF863, 0x0F62, 0x0FAD, 0x0F7C },             // 0F62 0FAD 0F7C
                { 0xF864, 0x0F62, 0x0FB4 },             // 0F62 0FB4
                { 0xF865, 0x0F62, 0x0FB4, 0x0F72 },             // 0F62 0FB4 0F72
                { 0xF866, 0x0F62, 0x0FB4, 0x0F74 },             // 0F62 0FB4 0F74
                { 0xF867, 0x0F62, 0x0FB5 },             // 0F62 0FB5
                { 0xF868, 0x0F62, 0x0FB7 },             // 0F62 0FB7
                { 0xF869, 0x0F62, 0x0FB7, 0x0FB1 },             // 0F62 0FB7 0FB1
                { 0xF86A, 0x0F63, 0x0F71, 0x0F72 },             // 0F63 0F71 0F72
                { 0xF86B, 0x0F63, 0x0F71, 0x0F74 },             // 0F63 0F71 0F74
                { 0xF86C, 0x0F63, 0x0F71, 0x0F7A },             // 0F63 0F71 0F7A
                { 0xF86D, 0x0F63, 0x0F71, 0x0F7C },             // 0F63 0F71 0F7C
                { 0xF86E, 0x0F63, 0x0F7D },             // 0F63 0F7D
                { 0xF86F, 0x0F63, 0x0F7E },             // 0F63 0F7E
                { 0xF870, 0x0F63, 0x0F71, 0x0F83 },             // 0F63 0F71 0F83
                { 0xF871, 0x0F63, 0x0F92, 0x0FB1 },             // 0F63 0F92 0FB1
                { 0xF872, 0x0F63, 0x0F92, 0x0FB1, 0x0F74 },             // 0F63 0F92 0FB1 0F74
                { 0xF873, 0x0F63, 0x0F92, 0x0FB1, 0x0F7C },             // 0F63 0F92 0FB1 0F7C
                { 0xF874, 0x0F63, 0x0F92, 0x0FB7 },             // 0F63 0F92 0FB7
                { 0xF875, 0x0F63, 0x0FA0 },             // 0F63 0FA0
                { 0xF876, 0x0F63, 0x0FA4, 0x0F71 },             // 0F63 0FA4 0F71
                { 0xF877, 0x0F63, 0x0FA6, 0x0F71 },             // 0F63 0FA6 0F71
                { 0xF878, 0x0F63, 0x0FA8 },             // 0F63 0FA8
                { 0xF879, 0x0F63, 0x0FA8, 0x0F72 },             // 0F63 0FA8 0F72
                { 0xF87A, 0x0F63, 0x0FB1 },             // 0F63 0FB1
                { 0xF87B, 0x0F63, 0x0FB1, 0x0F71 },             // 0F63 0FB1 0F71
                { 0xF87C, 0x0F63, 0x0FB2, 0x0F7D },             // 0F63 0FB2 0F7D
                { 0xF87D, 0x0F63, 0x0FB3 },             // 0F63 0FB3
                { 0xF87E, 0x0F63, 0x0FB3, 0x0F71 },             // 0F63 0FB3 0F71
                { 0xF87F, 0x0F63, 0x0FB3, 0x0F72 },             // 0F63 0FB3 0F72
                { 0xF880, 0x0F63, 0x0FB3, 0x0F71, 0x0F72 },             // 0F63 0FB3 0F71 0F72
                { 0xF881, 0x0F63, 0x0FB3, 0x0F74 },             // 0F63 0FB3 0F74
                { 0xF882, 0x0F63, 0x0FB3, 0x0F71, 0x0F74 },             // 0F63 0FB3 0F71 0F74
                { 0xF883, 0x0F63, 0x0FB3, 0x0F7A },             // 0F63 0FB3 0F7A
                { 0xF884, 0x0F63, 0x0FB3, 0x0F7E },             // 0F63 0FB3 0F7E
                { 0xF885, 0x0F63, 0x0FAD, 0x0F71 },             // 0F63 0FAD 0F71
                { 0xF886, 0x0F5D, 0x0F71, 0x0F72 },             // 0F5D 0F71 0F72
                { 0xF887, 0x0F5D, 0x0F7E },             // 0F5D 0F7E
                { 0xF888, 0x0F5D, 0x0F83 },             // 0F5D 0F83
                { 0xF889, 0x0F64, 0x0F71, 0x0F72 },             // 0F64 0F71 0F72
                { 0xF88A, 0x0F64, 0x0F71, 0x0F80 },             // 0F64 0F71 0F80
                { 0xF88B, 0x0F64, 0x0F71, 0x0F74 },             // 0F64 0F71 0F74
                { 0xF88C, 0x0F64, 0x0FB2, 0x0F80 },             // 0F64 0FB2 0F80
                { 0xF88D, 0x0F64, 0x0FB2, 0x0F71, 0x0F80 },             // 0F64 0FB2 0F71 0F80
                { 0xF88E, 0x0F64, 0x0F7D },             // 0F64 0F7D
                { 0xF88F, 0x0F64, 0x0F7E },             // 0F64 0F7E
                { 0xF890, 0x0F64, 0x0F94, 0x0F80 },             // 0F64 0F94 0F80
                { 0xF891, 0x0F64, 0x0FA9 },             // 0F64 0FA9
                { 0xF892, 0x0F64, 0x0FA9, 0x0F72 },             // 0F64 0FA9 0F72
                { 0xF893, 0x0F64, 0x0FA9, 0x0FAA },             // 0F64 0FA9 0FAA
                { 0xF894, 0x0F64, 0x0FAA },             // 0F64 0FAA
                { 0xF895, 0x0F64, 0x0F9F, 0x0F72 },             // 0F64 0F9F 0F72
                { 0xF896, 0x0F64, 0x0FA3, 0x0F71 },             // 0F64 0FA3 0F71
                { 0xF897, 0x0F64, 0x0FA8 },             // 0F64 0FA8
                { 0xF898, 0x0F64, 0x0FB1 },             // 0F64 0FB1
                { 0xF899, 0x0F64, 0x0FB1, 0x0F71 },             // 0F64 0FB1 0F71
                { 0xF89A, 0x0F64, 0x0FB1, 0x0F72 },             // 0F64 0FB1 0F72
                { 0xF89B, 0x0F64, 0x0FB2 },             // 0F64 0FB2
                { 0xF89C, 0x0F64, 0x0FB2, 0x0F71 },             // 0F64 0FB2 0F71
                { 0xF89D, 0x0F64, 0x0FB2, 0x0F72 },             // 0F64 0FB2 0F72
                { 0xF89E, 0x0F64, 0x0FB2, 0x0F71, 0x0F72 },             // 0F64 0FB2 0F71 0F72
                { 0xF89F, 0x0F64, 0x0FB3, 0x0F7C },             // 0F64 0FB3 0F7C
                { 0xF8A0, 0x0F64, 0x0FB3, 0x0F7D },             // 0F64 0FB3 0F7D
                { 0xF8A1, 0x0F64, 0x0FB3, 0x0F7E },             // 0F64 0FB3 0F7E
                { 0xF8A2, 0x0F64, 0x0FAD, 0x0F71 },             // 0F64 0FAD 0F71
                { 0xF8A3, 0x0F64, 0x0FAD, 0x0F72 },             // 0F64 0FAD 0F72
                { 0xF8A4, 0x0F64, 0x0FAD, 0x0F7C },             // 0F64 0FAD 0F7C
                { 0xF8A5, 0x0F64, 0x0FB4 },             // 0F64 0FB4
                { 0xF8A6, 0x0F65, 0x0F90 },             // 0F65 0F90
                { 0xF8A7, 0x0F65, 0x0F90, 0x0F7B },             // 0F65 0F90 0F7B
                { 0xF8A8, 0x0F65, 0x0F9A },             // 0F65 0F9A
                { 0xF8A9, 0x0F65, 0x0F9A, 0x0F71 },             // 0F65 0F9A 0F71
                { 0xF8AA, 0x0F65, 0x0F9A, 0x0F72 },             // 0F65 0F9A 0F72
                { 0xF8AB, 0x0F65, 0x0F9B },             // 0F65 0F9B
                { 0xF8AC, 0x0F65, 0x0F9B, 0x0F71 },             // 0F65 0F9B 0F71
                { 0xF8AD, 0x0F65, 0x0F9B, 0x0F72 },             // 0F65 0F9B 0F72
                { 0xF8AE, 0x0F65, 0x0F9E },             // 0F65 0F9E
                { 0xF8AF, 0x0F65, 0x0F9E, 0x0F72 },             // 0F65 0F9E 0F72
                { 0xF8B0, 0x0F65, 0x0F9E, 0x0F71, 0x0F72 },             // 0F65 0F9E 0F71 0F72
                { 0xF8B1, 0x0F65, 0x0F9E, 0x0F74 },             // 0F65 0F9E 0F74
                { 0xF8B2, 0x0F65, 0x0F9E, 0x0F71, 0x0F74 },             // 0F65 0F9E 0F71 0F74
                { 0xF8B3, 0x0F65, 0x0FA4 },             // 0F65 0FA4
                { 0xF8B4, 0x0F65, 0x0FA4, 0x0F7A },             // 0F65 0FA4 0F7A
                { 0xF8B5, 0x0F65, 0x0FB1, 0x0F7C },             // 0F65 0FB1 0F7C
                { 0xF8B6, 0x0F65, 0x0FB3 },             // 0F65 0FB3
                { 0xF8B7, 0x0F66, 0x0F71, 0x0F72 },             // 0F66 0F71 0F72
                { 0xF8B8, 0x0F66, 0x0F71, 0x0F80 },             // 0F66 0F71 0F80
                { 0xF8B9, 0x0F66, 0x0F71, 0x0F74 },             // 0F66 0F71 0F74
                { 0xF8BA, 0x0F66, 0x0F7B },             // 0F66 0F7B
                { 0xF8BB, 0x0F66, 0x0F71, 0x0F7C },             // 0F66 0F71 0F7C
                { 0xF8BC, 0x0F66, 0x0F7D },             // 0F66 0F7D
                { 0xF8BD, 0x0F66, 0x0F7E },             // 0F66 0F7E
                { 0xF8BE, 0x0F66, 0x0F83 },             // 0F66 0F83
                { 0xF8BF, 0x0F66, 0x0F72, 0x0F7E },             // 0F66 0F72 0F7E
                { 0xF8C0, 0x0F66, 0x0F74, 0x0F7E },             // 0F66 0F74 0F7E
                { 0xF8C1, 0x0F66, 0x0F7A, 0x0F7E },             // 0F66 0F7A 0F7E
                { 0xF8C2, 0x0F66, 0x0F7B, 0x0F7E },             // 0F66 0F7B 0F7E
                { 0xF8C3, 0x0F66, 0x0F90, 0x0F71 },             // 0F66 0F90 0F71
                { 0xF8C4, 0x0F66, 0x0F91 },             // 0F66 0F91
                { 0xF8C5, 0x0F66, 0x0FA9, 0x0FB3 },             // 0F66 0FA9 0FB3
                { 0xF8C6, 0x0F66, 0x0FAB },             // 0F66 0FAB
                { 0xF8C7, 0x0F66, 0x0FAB, 0x0FB7 },             // 0F66 0FAB 0FB7
                { 0xF8C8, 0x0F66, 0x0F9F, 0x0F71 },             // 0F66 0F9F 0F71
                { 0xF8C9, 0x0F66, 0x0F9F, 0x0FB1 },             // 0F66 0F9F 0FB1
                { 0xF8CA, 0x0F66, 0x0F9F, 0x0FB2 },             // 0F66 0F9F 0FB2
                { 0xF8CB, 0x0F66, 0x0F9F, 0x0FB2, 0x0F71 },             // 0F66 0F9F 0FB2 0F71
                { 0xF8CC, 0x0F66, 0x0F9F, 0x0FB2, 0x0F72 },             // 0F66 0F9F 0FB2 0F72
                { 0xF8CD, 0x0F66, 0x0F9F, 0x0FB2, 0x0F71, 0x0F72 },             // 0F66 0F9F 0FB2 0F71 0F72
                { 0xF8CE, 0x0F66, 0x0F9F, 0x0FB2, 0x0F7B },             // 0F66 0F9F 0FB2 0F7B
                { 0xF8CF, 0x0F66, 0x0F9F, 0x0FB2, 0x0F7C },             // 0F66 0F9F 0FB2 0F7C
                { 0xF8D0, 0x0F66, 0x0F9F, 0x0FB2, 0x0F7E },             // 0F66 0F9F 0FB2 0F7E
                { 0xF8D1, 0x0F66, 0x0F9F, 0x0FB2, 0x0F71, 0x0F7E },             // 0F66 0F9F 0FB2 0F71 0F7E
                { 0xF8D2, 0x0F66, 0x0F9F, 0x0FB2, 0x0F72, 0x0F7E },             // 0F66 0F9F 0FB2 0F72 0F7E
                { 0xF8D3, 0x0F66, 0x0F9F, 0x0FB2, 0x0F71, 0x0F72, 0x0F7E },             // 0F66 0F9F 0FB2 0F71 0F72 0F7E
                { 0xF8D4, 0x0F66, 0x0F9F, 0x0FAD, 0x0F7C },             // 0F66 0F9F 0FAD 0F7C
                { 0xF8D5, 0x0F66, 0x0F9F, 0x0FAD, 0x0F7E },             // 0F66 0F9F 0FAD 0F7E
                { 0xF8D6, 0x0F66, 0x0FA0, 0x0F71 },             // 0F66 0FA0 0F71
                { 0xF8D7, 0x0F66, 0x0FA0, 0x0F72 },             // 0F66 0FA0 0F72
                { 0xF8D8, 0x0F66, 0x0FA0, 0x0F71, 0x0F74 },             // 0F66 0FA0 0F71 0F74
                { 0xF8D9, 0x0F66, 0x0FA5 },             // 0F66 0FA5
                { 0xF8DA, 0x0F66, 0x0FA5, 0x0F71 },             // 0F66 0FA5 0F71
                { 0xF8DB, 0x0F66, 0x0FA5, 0x0F74 },             // 0F66 0FA5 0F74
                { 0xF8DC, 0x0F66, 0x0FA6, 0x0FB7 },             // 0F66 0FA6 0FB7
                { 0xF8DD, 0x0F66, 0x0FA6, 0x0FB7, 0x0F7C },             // 0F66 0FA6 0FB7 0F7C
                { 0xF8DE, 0x0F66, 0x0FA8, 0x0F71 },             // 0F66 0FA8 0F71
                { 0xF8DF, 0x0F66, 0x0FB1 },             // 0F66 0FB1
                { 0xF8E0, 0x0F66, 0x0FB1, 0x0F7B },             // 0F66 0FB1 0F7B
                { 0xF8E1, 0x0F66, 0x0FB2, 0x0F71 },             // 0F66 0FB2 0F71
                { 0xF8E2, 0x0F66, 0x0FAD, 0x0F71 },             // 0F66 0FAD 0F71
                { 0xF8E3, 0x0F66, 0x0FB4, 0x0F7A },             // 0F66 0FB4 0F7A
                { 0xF8E4, 0x0F66, 0x0FB6 },             // 0F66 0FB6
                { 0xF8E5, 0x0F66, 0x0FB7 },             // 0F66 0FB7
                { 0xF8E6, 0x0F66, 0x0FB7, 0x0F7C },             // 0F66 0FB7 0F7C
                { 0xF8E7, 0x0F67, 0x0F71, 0x0F72 },             // 0F67 0F71 0F72
                { 0xF8E8, 0x0F67, 0x0F71, 0x0F74 },             // 0F67 0F71 0F74
                { 0xF8E9, 0x0F67, 0x0FB2, 0x0F71, 0x0F80 },             // 0F67 0FB2 0F71 0F80
                { 0xF8EA, 0x0F67, 0x0F71, 0x0F7C },             // 0F67 0F71 0F7C
                { 0xF8EB, 0x0F67, 0x0F7E },             // 0F67 0F7E
                { 0xF8EC, 0x0F67, 0x0F83 },             // 0F67 0F83
                { 0xF8ED, 0x0F67, 0x0F74, 0x0F7E },             // 0F67 0F74 0F7E
                { 0xF8EE, 0x0F67, 0x0F71, 0x0F74, 0x0F7E },             // 0F67 0F71 0F74 0F7E
                { 0xF8EF, 0x0F67, 0x0F71, 0x0F74, 0x0F83 },             // 0F67 0F71 0F74 0F83
                { 0xF8F0, 0x0F67, 0x0F9C },             // 0F67 0F9C
                { 0xF8F1, 0x0F67, 0x0F9E },             // 0F67 0F9E
                { 0xF8F2, 0x0F67, 0x0FA3 },             // 0F67 0FA3
                { 0xF8F3, 0x0F67, 0x0FA3, 0x0F72 },             // 0F67 0FA3 0F72
                { 0xF8F4, 0x0F67, 0x0FA8 },             // 0F67 0FA8
                { 0xF8F5, 0x0F67, 0x0FA8, 0x0F71 },             // 0F67 0FA8 0F71
                { 0xF8F6, 0x0F67, 0x0FB1 },             // 0F67 0FB1
                { 0xF8F7, 0x0F67, 0x0FB1, 0x0F74 },             // 0F67 0FB1 0F74
                { 0xF8F8, 0x0F67, 0x0FB1, 0x0F7A },             // 0F67 0FB1 0F7A
                { 0xF8F9, 0x0F67, 0x0FB1, 0x0FAD },             // 0F67 0FB1 0FAD
                { 0xF8FA, 0x0F67, 0x0FB1, 0x0FAD, 0x0F7A },             // 0F67 0FB1 0FAD 0F7A
                { 0xF8FB, 0x0F67, 0x0FB2, 0x0F71, 0x0F72 },             // 0F67 0FB2 0F71 0F72
                { 0xF8FC, 0x0F67, 0x0FB3 },             // 0F67 0FB3
                { 0xF8FD, 0x0F67, 0x0FAD, 0x0F71 },             // 0F67 0FAD 0F71
                { 0xF8FE, 0x0F67, 0x0FAD, 0x0F71, 0x0F74 },             // 0F67 0FAD 0F71 0F74
                { 0xF8FF, 0x0F67, 0x0FAD, 0x0F7C },             // 0F67 0FAD 0F7C
                { 0x0F00, 0x0F68, 0x0F7C, 0x0F7E },             // 0F68 0F7C 0F7E = U+0F00
                { 0xF592, 0x0F60, 0x0F72, 0x0F39 },             // 0F60 0F39 0F72 = NFD 0F60 0F72 0F39
                { 0xF595, 0x0F60, 0x0F74, 0x0F39 }              // 0F60 0F39 0F74 = NFD 0F60 0F74 0F39
        };

        private synchronized static void createTableIndex() {
                if (tableIndex == null) {
                        // This is our very first time running this, so build an index into the table
                        tableIndex = new HashMap<Character, LinkedList<Integer>>();
                        char prevInitialChar = 0x0000;
                        for (int i=0; i < TableAMapping.length; i++) {
                                if (TableAMapping[i][1] != prevInitialChar) {
                                        char currInitialChar = TableAMapping[i][1];
                                        Character currInitialCharacter = new Character(currInitialChar);
                                        // We have a new run of a character to index
                                        LinkedList<Integer> currCharList;
                                        if (tableIndex.containsKey(currInitialCharacter)){ 
                                                // Add it to an existing index
                                                currCharList = tableIndex.get(currInitialCharacter);
                                                currCharList.add(new Integer(i));
                                        } else {
                                                // Create a new list
                                                currCharList = new LinkedList<Integer>();
                                                currCharList.add(new Integer(i));
                                                tableIndex.put(currInitialCharacter, currCharList);
                                        }
                                        prevInitialChar = currInitialChar;
                                }
                        }
                }
                
        }
        
        public static String convertUnicodeToPrecomposedTibetan( String pStrIn) {
                return convertUnicodeToPrecomposedTibetan(pStrIn, 0, pStrIn.length());
        }
        
        public static String convertUnicodeToPrecomposedTibetan( String pStrIn, int nStart, int nEnd )
        {
                // If this is our first time, we may need to create an index
                createTableIndex();
                
                StringBuffer bStrOut = new StringBuffer(nEnd - nStart);
                
                // The lookahead of characters we're currently considering
                char chInput[] = { 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000 };

                // The current best match
                char chPrecomposedTibetan = 0x0000;
                char chPending[] = { 0xFFFF, 0xFFFF, 0xFFFF };

                int nCol = 0;

                for ( int i = nStart; i < nEnd; i++ )
                {
                        nCol++;

                        if ( chPending[0] == 0xFFFF )
                        {
                                // First time through, so initialize things
                                chInput[nCol] = pStrIn.charAt( i );
                                char[] chTib = {chInput[nCol], chPending[0], chPending[1]};
                                decomposeTibetan(chTib);
                                chInput[nCol] = chTib[0];
                                chPending[0] = chTib[1];
                                chPending[1] = chTib[2];
                        }
                        else
                        {
                                // Look at the next character
                                chInput[nCol] = chPending[0];
                                chPending[0] = chPending[1];
                                chPending[1] = chPending[2];
                                chPending[2] = 0xFFFF;
                                i--;
                        }

                        boolean bPotentialConjunct = false; // Do we have a possible match

                        switch ( nCol )
                        {
                        case ( 1 ) :
                                if ( ( ( chInput[nCol] >= 0x0F40 ) && ( chInput[nCol] <= 0x0F6A ) ) || ( ( chInput[nCol] >= 0x0F88 ) && ( chInput[nCol] <= 0x0F89 ) ) )
                                {
                                        bPotentialConjunct = true;
                                }
                                break;

                        case ( 2 ) :
                                if ( ( chInput[nCol] == 0x0F39 ) || ( ( chInput[nCol] >= 0x0F71 ) && ( chInput[nCol] <= 0x0FBC ) ) )
                                {
                                        bPotentialConjunct = true;
                                }
                                break;

                        case ( 3 ) :            // 0x0F39 only in NFD
                                if ( ( chInput[nCol] == 0x0F39 ) || ( ( chInput[nCol] >= 0x0F71 ) && ( chInput[nCol] <= 0x0FB7 ) ) )
                                {
                                        bPotentialConjunct = true;
                                }
                                break;

                        case ( 4 ) :
                                if ( ( chInput[nCol] >= 0x0F71 ) && ( chInput[nCol] <= 0x0FB2 ) )
                                {
                                        bPotentialConjunct = true;
                                }
                                break;

                        case ( 5 ) :
                                if ( ( chInput[nCol] >= 0x0F72 ) && ( chInput[nCol] <= 0x0F83 ) )
                                {
                                        bPotentialConjunct = true;
                                }
                                break;

                        case ( 6 ) :
                                if ( ( chInput[nCol] >= 0x0F72 ) && ( chInput[nCol] <= 0x0F83 ) )
                                {
                                        bPotentialConjunct = true;
                                }
                                break;
                        }

                        if ( ( bPotentialConjunct ) && ( nCol > 1 ) )
                        {
                                bPotentialConjunct = false;
                                
                                // Look for the starting point for that character in the table
                                LinkedList<Integer> startLocs = tableIndex.get(chInput[1]);
                                if (startLocs != null) {
                                        for (ListIterator<Integer> li = startLocs.listIterator(); li.hasNext();) {
                                                int startLoc = li.next().intValue();
                                                for ( int j = startLoc; 
                                                          j < TableAMapping.length && TableAMapping[j][1] == chInput[1]; j++ )
                                                {
                                                        boolean bMatch = true;
                                                        for ( int k = 1; k <= nCol && k < TableAMapping[j].length; k++ )
                                                        {
                                                                if ( TableAMapping[j][k] != chInput[k] )
                                                                {
                                                                        bMatch = false;
                                                                        break;
                                                                }
                                                        }
                                                        if ( bMatch == true )
                                                        {
                                                                bPotentialConjunct = true;
                                                                if ( nCol+1 == TableAMapping[j].length)
                                                                {
                                                                        chPrecomposedTibetan = TableAMapping[j][0];
                                                                        break;
                                                                }
                                                        }
                                                }
                                        }
                                }
/*                              for ( int j = 0; j < TableAMapping.length; j++ )
                                {
                                        if ( TableAMapping[j][1] == chInput[1] )
                                        {
                                                boolean bMatch = true;
                                                for ( int k = 1; k <= nCol && k < TableAMapping[j].length; k++ )
                                                {
                                                        if ( TableAMapping[j][k] != chInput[k] )
                                                        {
                                                                bMatch = false;
                                                                break;
                                                        }
                                                }
                                                if ( bMatch == true )
                                                {
                                                        bPotentialConjunct = true;
                                                        if ( nCol+1 == TableAMapping[j].length)
                                                        {
                                                                chPrecomposedTibetan = TableAMapping[j][0];
                                                                break;
                                                        }
                                                }
                                        }
                                } */
                        }

                        if ( ( i + 1 ) == nEnd )
                        {
                                if ( bPotentialConjunct && ( chPrecomposedTibetan == 0x0000 ) )
                                {
                                        bPotentialConjunct = false;
                                }
                        }

                        if ( ! bPotentialConjunct )
                        {
                                if ( nCol == 1 )
                                {
                                        bStrOut.append(chInput[nCol]);
                                        chInput[nCol] = 0x0000;
                                }
                                else
                                {
                                        chPrecomposedTibetan = outputPrecomposedTibetan( bStrOut, chPrecomposedTibetan, chInput, nCol );

                                        if ( chInput[nCol] == 0xFFFF )
                                        {
                                                bStrOut.append(chInput[nCol]);
                                        }
                                        else
                                        {
                                                chPending[2] = chPending[1];
                                                chPending[1] = chPending[0];
                                                chPending[0] = chInput[nCol];
                                        }

                                        for ( int j = 1; j <= nCol; j++ )
                                        {
                                                chInput[j] = 0x0000;
                                        }
                                }
                                nCol = 0;
                        }
                }

                for ( int i = 0; i < 3; i++ )
                {
                        if ( chPending[i] != 0xFFFF )
                        {
                                bStrOut.append(chInput[nCol]);
                        }
                }

                return bStrOut.toString();
        }

        private static char outputPrecomposedTibetan(StringBuffer pStrOut, char chPrecomposedTibetan, char[] chInput, int nCol )
        {
                if ( chPrecomposedTibetan == 0x0000 )
                {
                        int nStartCol = 1;
                        // Convert GHA, DDHA, DHA, BHA, DZAHA and KSSA to precomposed characters
                        if ( chInput[2] == 0x0FB7 )
                        {
                                switch ( chInput[1] )
                                {
                                case ( 0x0F42 ) :
                                case ( 0x0F4C ) :
                                case ( 0x0F51 ) :
                                case ( 0x0F56 ) :
                                case ( 0x0F5B ) :
                                        chInput[2] = (char)( (int)chInput[1] + 0x0001 );
                                        nStartCol = 2;
                                        break;
                                }
                        }
                        else if ( ( chInput[1] == 0x0F40 ) && ( chInput[2] == 0x0FB5 ) )
                        {
                                chInput[2] = 0x0F69;
                                nStartCol = 2;
                        }
                        for ( int j = nStartCol; j < nCol; j++ )
                        {
                                pStrOut.append(chInput[j] );
                        }
                        return 0x0000;
                }
                else
                {
                        pStrOut.append(chPrecomposedTibetan);
                        return 0x0000; // chPrecomposedTibetan = 0x0000;
                }
        }


        public static String convertPrecomposedTibetanToUnicode( String pStrIn, int nStart, int nEnd )
        {
                StringBuffer bStringOut = new StringBuffer(nEnd - nStart);

                char chInput[] = {0x0000, 0x0000, 0x0000};

                for ( int i = nStart; i < nEnd; i++ )
                {
                        chInput[0] = pStrIn.charAt( i );

                        if ( ( chInput[0] >= 0xF300 ) && ( chInput[0] <= 0xF8FF ) )
                        {
                                int nIndex = ( chInput[0] - 0xF300 );
                                for ( int j = 1; j < TableAMapping[nIndex].length; j++ )
                                {
                                        bStringOut.append(TableAMapping[nIndex][j]);
                                }
                        }
                        else
                        {
                                decomposeTibetan( chInput);

                                bStringOut.append(chInput[0]);

                                if ( chInput[1] != 0xFFFF )
                                {
                                        bStringOut.append(chInput[1]);
                                }

                                if ( chInput[2] != 0xFFFF )
                                {
                                        bStringOut.append(chInput[2]);
                                }
                        }
                }

                return bStringOut.toString();
        }

        private static int decomposeTibetan(char[] chTib)
        {
                int nDecompose = 1;

                chTib[1] = 0xFFFF;
                chTib[2] = 0xFFFF;

                switch ( chTib[0] )
                {
                case ( 0x0F00 ) :               // TIBETAN SYLLABLE OM
                        chTib[0] = 0x0F68;
                        chTib[1] = 0x0F7C;
                        chTib[2] = 0x0F7E;
                        nDecompose = 3;
                        break;

                case ( 0x0F43 ) :               // TIBETAN LETTER GHA
                        chTib[0] = 0x0F42;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0F4D ) :               // TIBETAN LETTER DDHA
                        chTib[0] = 0x0F4C;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0F52 ) :               // TIBETAN LETTER DHA
                        chTib[0] = 0x0F51;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0F57 ) :               // TIBETAN LETTER BHA
                        chTib[0] = 0x0F56;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0F5C ) :               // TIBETAN LETTER DZHA
                        chTib[0] = 0x0F5B;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0F69 ) :               // TIBETAN LETTER KSSA
                        chTib[0] = 0x0F40;
                        chTib[1] = 0x0FB5;
                        nDecompose = 2;
                        break;

                case ( 0x0F93 ) :               // TIBETAN SUBJOINED LETTER GHA
                        chTib[0] = 0x0F92;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0F9D ) :               // TIBETAN SUBJOINED LETTER DDHA
                        chTib[0] = 0x0F9C;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0FA2 ) :               // TIBETAN SUBJOINED LETTER DHA
                        chTib[0] = 0x0FA1;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0FA7 ) :               // TIBETAN SUBJOINED LETTER BHA
                        chTib[0] = 0x0FA6;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0FAC ) :               // TIBETAN SUBJOINED LETTER DZHA
                        chTib[0] = 0x0FAB;
                        chTib[1] = 0x0FB7;
                        nDecompose = 2;
                        break;

                case ( 0x0FB9 ) :               // TIBETAN SUBJOINED LETTER KSSA
                        chTib[0] = 0x0F90;
                        chTib[1] = 0x0FB5;
                        nDecompose = 2;
                        break;

                case ( 0x0F73 ) :               // TIBETAN VOWEL SIGN II
                        chTib[0] = 0x0F71;
                        chTib[1] = 0x0F72;
                        nDecompose = 2;
                        break;

                case ( 0x0F75 ) :               // TIBETAN VOWEL SIGN UU
                        chTib[0] = 0x0F71;
                        chTib[1] = 0x0F74;
                        nDecompose = 2;
                        break;

                case ( 0x0F81 ) :               // TIBETAN VOWEL SIGN REVERSED II
                        chTib[0] = 0x0F71;
                        chTib[1] = 0x0F80;
                        nDecompose = 2;
                        break;

                case ( 0x0F76) :                // TIBETAN VOWEL SIGN VOCALIC R
                        chTib[0] = 0x0FB2;
                        chTib[1] = 0x0F80;
                        nDecompose = 2;
                        break;

                case ( 0x0F77) :                // TIBETAN VOWEL SIGN VOCALIC RR
                        chTib[0] = 0x0FB2;
                        chTib[1] = 0x0F71;
                        chTib[2] = 0x0F80;
                        nDecompose = 3;
                        break;

                case ( 0x0F78) :                // TIBETAN VOWEL SIGN VOCALIC L
                        chTib[0] = 0x0FB3;
                        chTib[1] = 0x0F80;
                        nDecompose = 2;
                        break;

                case ( 0x0F79) :                // TIBETAN VOWEL SIGN VOCALIC LL
                        chTib[0] = 0x0FB3;
                        chTib[1] = 0x0F71;
                        chTib[2] = 0x0F80;
                        nDecompose = 3;
                        break;
                }

                return nDecompose;
        }
}
