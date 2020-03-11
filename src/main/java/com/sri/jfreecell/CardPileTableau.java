package com.sri.jfreecell;

import java.util.ArrayList;

/**
 * Card pile with the initial cards. Only need to specify rules for adding cards. Default rules apply to remove them.
 * 
 * @author Sateesh Gampala
 *
 */
public class CardPileTableau extends CardPile {

    private static final long serialVersionUID = -3457695861280593662L;
    private ArrayList<Card> cascadedCards = new ArrayList<Card>();

    @Override
    public void pushIgnoreRules(Card newCard) {
	super.pushIgnoreRules(newCard);
	if (loc != null) {
	    newCard.setPosition(loc.x, loc.y + (TABLEAU_INCR_Y * (cards.size() - 1)));
	}
    }

    @Override
    public boolean push(Card newCard) {
	if (super.push(newCard)) {
	    addToCascade(newCard);
	    if (loc != null) {
		newCard.setPosition(loc.x, loc.y + (TABLEAU_INCR_Y * (cards.size() - 1)));
	    }
	    return true;
	}
	return false;
    }

    @Override
    public Card pop() {
	Card crd = super.pop();
	deleteFromCascade(crd);
	return crd;
    }

    @Override
    public boolean isAllowedtoAdd(Card card) {
	return (this.size() == 0 || validStack(this.peekTop(), card));
    }

    @Override
    public boolean isMovable(Card tcard) {
	return cascadedCards.contains(tcard);
    }

    /**
     * Checks if two provided card are valid stack.
     * 
     * @param topCard
     * @param btmCard
     * @return true, if face value is one lower and it's the opposite color else false
     */
    public static boolean validStack(Card topCard, Card btmCard) {
	return topCard != null && btmCard != null && topCard.getFace().ordinal() - 1 == btmCard.getFace().ordinal()
		&& topCard.getSuit().getColor() != btmCard.getSuit().getColor();
    }

    public ArrayList<Card> getCascades(int maxCards) {
	int size = cascadedCards.size();
	int stIdx = (size > maxCards ? size - maxCards : 0);
	return new ArrayList<Card>(cascadedCards.subList(stIdx, size));
    }

    public void addToCascade(Card card) {
	synchronized (cascadedCards) {
	    cascadedCards.add(card);
	}
    }

    public void deleteFromCascade(Card card) {
	synchronized (cascadedCards) {
	    int idx = cascadedCards.indexOf(card);
	    if (idx != -1) {
		cascadedCards.subList(idx, cascadedCards.size()).clear();
		if (cascadedCards.size() == 0 && size() > 0) {
		    updateCascades();
		}
	    }
	}
    }

    public void updateCascades() {
	synchronized (cascadedCards) {
	    for (int i = 0; i < cards.size() - 1; i++) {
		if (validStack(cards.get(i), cards.get(i + 1))) {
		    cascadedCards.add(cards.get(i));
		} else {
		    cascadedCards.clear();
		}
	    }
	    if (size() > 0) {
		cascadedCards.add(peekTop());
	    }
	}
    }
}
