package com.sri.jfreecell;

/**
 * A Deck is a particular kind of CardPile with 52 Cards in it
 * 
 * @author Sateesh Gampala
 *
 */
public class Deck extends CardPile {

    private static final long serialVersionUID = -6039578474112103082L;
    
    /**
     * Creates a new instance of Deck
     */
    public Deck() {
        for (Suit s : Suit.values()) {
            for (Face f : Face.values()) {
                Card c = new Card(f, s);
                c.turnFaceUp();
                this.push(c);
            }
        }
        shuffle();
    }

    public Deck(CardPile cp) {
        for (Card c : cp) {
            this.push(c);
        }
    }
}
