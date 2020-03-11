package com.sri.jfreecell;

import java.awt.Color;

/**
 * A card suit type
 * 
 * @author Sateesh Gampala
 *
 */
enum Suit {
    CLUBS('C', Color.BLACK), DIAMONDS('D', Color.RED), HEARTS('H', Color.RED), SPADES('S', Color.BLACK);

    private final Color color;
    private final char code;

    Suit(char code, Color color) {
        this.code = code;
        this.color = color;
    }

    public Color getColor() {
        return this.color;
    }

    public char getCode() {
        return this.code;
    }

    public static Suit fromCode(char code) {
        for (Suit suit : Suit.values()) {
            if (suit.code == code) {
                return suit;
            }
        }
        return null;
    }
}
