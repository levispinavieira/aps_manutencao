package com.sri.jfreecell;

/**
 * Defines a card face value.
 * 
 * @author Sateesh Gampala
 *
 */
enum Face {
    ACE('A'), DEUCE('2'), THREE('3'), FOUR('4'), FIVE('5'), SIX('6'), SEVEN('7'), EIGHT('8'), NINE('9'), TEN('T'), JACK('J'), QUEEN('Q'), KING('K');
    
    private final char code;
    
    Face (char code) {
        this.code = code;
    }
    
    public char getCode() {
        return this.code;
    }
    
    public static Face fromCode(char code) {
        for (Face face : Face.values()) {
            if (face.code == code) {
                return face;
            }
        }
        return null;
    }
}
