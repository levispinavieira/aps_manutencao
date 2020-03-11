package com.sri.jfreecell;

/**
 * CardPile specialized to add only one card.
 * 
 * @author Sateesh Gampala
 *
 */
public class CardPileFreeCell extends CardPile {

    private static final long serialVersionUID = -6462488239384705042L;

    @Override
    public boolean isAllowedtoAdd(Card card) {
	// Accept only if the current pile is empty.
	return size() == 0;
    }

    @Override
    public boolean isMovable(Card card) {
	return true;
    }
}
