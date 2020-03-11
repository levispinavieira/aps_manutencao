package com.sri.jfreecell;

/**
 * @author Sateesh Gampala
 *
 */
public class CardPileBulk extends CardPile {
    
    private static final long serialVersionUID = -2102907183340211155L;
    public CardPile pile;
    public int count;
    
    public CardPileBulk(CardPile pile, int count) {
        this.pile = pile;
        this.count = count;
    }
}
