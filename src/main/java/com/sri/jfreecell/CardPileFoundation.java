// Purpose: Represents a Foundation card pile.

package com.sri.jfreecell;

import java.awt.Rectangle;

/**
 * Represents a Foundation card pile. This is a non removable pile. Card will be collected as per deck order.
 * 
 * @author Sateesh Gampala
 *
 */
public class CardPileFoundation extends CardPile {

    private static final long serialVersionUID = -5609208267476759214L;

    @Override
    public boolean isAllowedtoAdd(Card card) {
	// Accept Ace card if pile is empty.
	if ((this.size() == 0) && (card.getFace() == Face.ACE)) {
	    return true;
	}
	// Accept if face value is one higher and it's the same color.
	if (size() > 0) {
	    Card top = peekTop();
	    if ((top.getSuit() == card.getSuit() && (top.getFace().ordinal() + 1 == card.getFace().ordinal()))) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public boolean isRemovable() {
	return false;
    }

    @Override
    public boolean isMovable(Card card) {
	return false;
    }
    
    @Override
    public void setPosition(Rectangle loc) {
        this.loc = loc;
        for (Card card : cards) {
            card.setPosition(loc.x, loc.y);
        }
    }

    @Override
    public void resetCardsPos() {
        for (Card card : cards) {
            card.setPosition(this.loc.x, this.loc.y);
        }
    }
}
