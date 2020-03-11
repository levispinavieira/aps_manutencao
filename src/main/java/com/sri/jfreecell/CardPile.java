package com.sri.jfreecell;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.TimelineCallbackAdapter;
import org.pushingpixels.trident.interpolator.KeyFrames;
import org.pushingpixels.trident.interpolator.KeyTimes;
import org.pushingpixels.trident.interpolator.KeyValues;

/**
 * A pile of cards (can be used for a hand, deck, discard pile...) 
 * Subclasses: Deck (a CardPile of 52 Cards)
 * 
 * @author Sateesh Gampala
 *
 */
public class CardPile implements Iterable<Card>, Serializable {

    private static final long serialVersionUID = 3039382357994794527L;
    public static final int TABLEAU_INCR_Y = 18;

    protected ArrayList<Card> cards = new ArrayList<Card>(); // All the cards.
    protected Rectangle loc = null;

    private transient boolean highlight = false;
    private Color backgroundColor;
    private float opacity = 0.7f;
    private transient Timeline blinkTimeline;

    public CardPile() {
        initTimeLines();
    }

    private void initTimeLines() {
        blinkTimeline = new Timeline(this);
        blinkTimeline.setDuration(2000);
        blinkTimeline.addCallback(new TimelineCallbackAdapter() {
            @Override
            public void onTimelineStateChanged(TimelineState oldState, TimelineState newState, float duration,
                float timelinePos) {
                if (newState == TimelineState.DONE) {
                    backgroundColor = Color.yellow;
                    highlight = false;
                }
            }
        });
    }

    public void pushIgnoreRules(Card newCard) {
	cards.add(newCard);
	if (loc != null)
	    newCard.setPosition(loc.x, loc.y);
    }

    public Card popIgnoreRules() {
	int lastIndex = size() - 1;
	Card crd = cards.get(lastIndex);
	cards.remove(lastIndex);
	return crd;
    }

    public boolean push(Card newCard) {
	if (isAllowedtoAdd(newCard)) {
	    cards.add(newCard);
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Subclasses can override this to enforce their rules for adding.
     * 
     * @param card
     * @return
     */
    public boolean isAllowedtoAdd(Card card) {
	return true;
    }

    /**
     * Gets no of card in the Pile
     * 
     * @return no of card in the Pile
     */
    public int size() {
	return cards.size();
    }

    /**
     * Remove top card if it is valid.
     * 
     * @return removed card.
     */
    public Card pop() {
	if (!isRemovable()) {
	    throw new UnsupportedOperationException("Illegal attempt to remove.");
	}
	return popIgnoreRules();
    }

    /**
     * Shuffles the cards
     */
    public void shuffle() {
	Collections.shuffle(cards);
    }

    /**
     * gets the top card of the pile
     * 
     * @return card
     */
    public Card peekTop() {
	return cards.get(cards.size() - 1);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Card> iterator() {
	return cards.iterator();
    }

    /**
     * Returns reverse Iterator.
     * 
     * @return
     */
    public ListIterator<Card> reverseIterator() {
	return cards.listIterator(cards.size());
    }

    /**
     * Clear all card from the pile.
     */
    public void clear() {
	cards.clear();
    }

    /**
     * Gets position position of the pile.
     * 
     * @return
     */
    public Rectangle getPosition() {
	return this.loc;
    }

    public void setPosition(Rectangle loc) {
	this.loc = loc;
	int i = 0;
	for (Card card : cards) {
	    card.setPosition(loc.x, loc.y + (TABLEAU_INCR_Y * i++));
	}
    }

    public void resetCardsPos() {
	int i = 0;
	for (Card card : cards) {
	    card.setPosition(this.loc.x, this.loc.y + (TABLEAU_INCR_Y * i++));
	}
    }

    /**
     * Subclasses can override this to enforce their rules for removal.
     * 
     * @return
     */
    public boolean isRemovable() {
	return true;
    }

    public boolean isMovable(Card card) {
	return true;
    }

    public void setPosition(Card card, int newX, int newY) {
	for (int i = cards.indexOf(card); i >= 0 && i < cards.size(); i++) {
	    cards.get(i).setPosition(newX, newY);
	    newY += TABLEAU_INCR_Y;
	}
    }

    public void drawDragged(Graphics g, Card card) {
	for (int i = cards.indexOf(card); i >= 0 && i < cards.size(); i++) {
	    cards.get(i).draw(g);
	}
    }

    public ArrayList<Card> getCardListFrom(Card card) {
	ArrayList<Card> cardsList = new ArrayList<Card>();
	for (int i = cards.indexOf(card); i >= 0 && i < cards.size(); i++) {
	    cardsList.add(cards.get(i));
	}
	return cardsList;
    }

    public void draw(Graphics g) {
	Graphics2D g2 = (Graphics2D) g.create();
	if (highlight) {
	    g2.setStroke(new BasicStroke(3));
	    g2.setColor(this.backgroundColor);
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.setComposite(AlphaComposite.SrcOver.derive(opacity));
	}
	RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(loc.x, loc.y, loc.width, loc.height, 10, 10);
	g2.draw(roundedRectangle);
	g2.dispose();
    }

    public void blink(int delay) {
	highlight = true;
	backgroundColor = Color.red;
	KeyValues<Float> xValues = KeyValues.create(1f, 0.75f, 0.5f, 0.75f, 1f, 0.75f, 0.5f, 0.75f, 1f);
	KeyTimes alphaTimes = new KeyTimes(0f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 1f);
	try {
	    blinkTimeline.setInitialDelay(delay);
	} catch (Exception ex) {
	}
	blinkTimeline.addPropertyToInterpolate("opacity", new KeyFrames<Float>(xValues, alphaTimes));
	this.blinkTimeline.replay();
    }

    public void stopBlink() {
	blinkTimeline.end();
    }

    public Color getBackgroundColor() {
	return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
	this.backgroundColor = backgroundColor;
    }

    public float getOpacity() {
	return opacity;
    }

    public void setOpacity(float opacity) {
	this.opacity = opacity;
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        initTimeLines();
    }
}
