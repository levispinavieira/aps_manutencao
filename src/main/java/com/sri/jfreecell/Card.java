package com.sri.jfreecell;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.URL;

import javax.swing.ImageIcon;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.TimelineCallbackAdapter;
import org.pushingpixels.trident.ease.Spline;
import org.pushingpixels.trident.interpolator.KeyFrames;
import org.pushingpixels.trident.interpolator.KeyTimes;
import org.pushingpixels.trident.interpolator.KeyValues;

/**
 * Represents a single Card.<br>
 * Issues:<br>
 * Fragile: This loads each card image from a file, and has the file-naming conventions for the card built into it <br>
 * To change to another set of card images, it's necessary to change the code
 * 
 * @author Sateesh Gampala
 *
 */
public class Card implements Serializable {
    private static final long serialVersionUID = -2942119585109171160L;
    public static final int CARD_WIDTH;
    public static final int CARD_HEIGHT;

    private static final String IMAGE_PATH = "cardimages/";
    private static final ImageIcon BACK_IMAGE; // Image of back of a card
    private static final ClassLoader CLSLDR;

    // just a static method that does some of the initialization
    static {
        // Get current classloader, and get the image resources
        CLSLDR = Card.class.getClassLoader();
        // using the .net.URL as a resource locator via the classloader
        URL imageURL = CLSLDR.getResource(IMAGE_PATH + "b.gif");
        BACK_IMAGE = new ImageIcon(imageURL);

	// These constants are assumed to work for all cards.
	CARD_WIDTH = BACK_IMAGE.getIconWidth();
	CARD_HEIGHT = BACK_IMAGE.getIconHeight();
    }

    private Face face;
    private Suit suit;
    private transient ImageIcon faceImage;
    private int x;
    private int y;
    private boolean faceUp = true;
    private boolean highlight = false;
    private Color backgroundColor;
    private float opacity = 0.7f;
    private transient Timeline sucessTimeline;
    private transient Timeline moveTimeline;
    private transient Timeline blinkTimeline;

    public Card(Face face, Suit suit) {
        // Set the face and suit values.
        this.face = face;
        this.suit = suit;
        // Assume card is at 0,0
        x = 0;
        y = 0;

        faceUp = false;
        backgroundColor = Color.yellow;
        loadImage();
        initTimeLines();
    }

    private void initTimeLines() {
        sucessTimeline = new Timeline(this);
        sucessTimeline.setDuration(3000);
        sucessTimeline.setEase(new Spline(0.8f));

        moveTimeline = new Timeline(this);
        moveTimeline.setDuration(250);

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

    private void loadImage() {
        char faceChar = "a23456789tjqk".charAt(this.face.ordinal());
        char suitChar = "cdhs".charAt(this.suit.ordinal());
        String cardFilename = "" + faceChar + suitChar + ".gif";
        faceImage = new ImageIcon(CLSLDR.getResource(IMAGE_PATH + cardFilename));
    }

    /**
     * Draws the card.
     * 
     * @param g
     */
    public void draw(Graphics g) {
	if (faceUp) {
	    faceImage.paintIcon(null, g, x, y);
	    if (highlight) {
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setStroke(new BasicStroke(3));
		g2.setColor(this.backgroundColor);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setComposite(AlphaComposite.SrcOver.derive(opacity));
		RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(x - 1, y - 1, CARD_WIDTH + 1,
			CARD_HEIGHT + 1, 10, 10);
		g2.draw(roundedRectangle);
		g2.dispose();
	    }
	} else {
	    BACK_IMAGE.paintIcon(null, g, x, y);
	}
    }

    /**
     * set x & y position of the card.
     * 
     * @param x
     * @param y
     */
    public void setPosition(int x, int y) {
	this.x = x;
	this.y = y;
    }

    /**
     * To bounce the card after game completion.
     * 
     * @param x
     * @param y
     * @param delay
     */
    public void bounce(int x, int y, int delay) {
	KeyValues<Integer> xValues = KeyValues.create(this.x, x + (this.x - x) / 4, x, x - (this.x - x) / 4, -CARD_WIDTH);
	KeyValues<Integer> yValues = KeyValues.create(this.y, this.y + (y - this.y) / 4, y, this.y + (y - this.y) / 4, 0);
	KeyTimes alphaTimes = new KeyTimes(0.0f, 0.25f, 0.5f, 0.75f, 1.0f);

	sucessTimeline.addPropertyToInterpolate("x", new KeyFrames<Integer>(xValues, alphaTimes));
	sucessTimeline.addPropertyToInterpolate("y", new KeyFrames<Integer>(yValues, alphaTimes));
	try {
	    sucessTimeline.setInitialDelay(delay);
	} catch (Exception ex) {
	}
	sucessTimeline.replay();
    }

    public void moveTo(int x, int y) {
	moveTimeline.addPropertyToInterpolate("x", this.x, x);
	moveTimeline.addPropertyToInterpolate("y", this.y, y);
	this.moveTimeline.replay();
    }

    public void moveFrom(int x, int y) {
	moveTimeline.addPropertyToInterpolate("x", x, this.x);
	moveTimeline.addPropertyToInterpolate("y", y, this.y);
	this.moveTimeline.replay();
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

    /**
     * Given a point, it tells whether this is inside card image.
     * 
     * @param x
     * @param y
     * @return true if given point is inside card image; false otherwise
     */
    public boolean isInside(int x, int y) {
	return (x >= this.x && x < this.x + CARD_WIDTH) && (y >= this.y && y < this.y + CARD_HEIGHT);
    }

    /**
     * Given a point, it tells whether this is inside visible card image.
     * 
     * @param x
     * @param y
     * @return true if given point is inside visible card image; false otherwise
     */
    public boolean isVisibleInside(int x, int y, boolean isTopCard) {
	return (x >= this.x && x < this.x + CARD_WIDTH) && (y >= this.y && (isTopCard ? y < this.y + CARD_HEIGHT : y < this.y + 15));
    }

    /**
     * Returns face value of card.
     * 
     * @return face
     */
    public Face getFace() {
	return face;
    }

    /**
     * Returns suit value of card.
     * 
     * @return suit
     */
    public Suit getSuit() {
	return suit;
    }

    public int getX() {
	return x;
    }

    public int getY() {
	return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void turnFaceUp() {
        this.faceUp = true;
    }

    public void turnFaceDown() {
        this.faceUp = false;
    }

    public void highlight(boolean highlight) {
	this.highlight = highlight;
	opacity = 0.7f;
    }

    public void setBackgroundColor(Color backgroundColor) {
	this.backgroundColor = backgroundColor;
    }

    public Color getBackgroundColor() {
	return backgroundColor;
    }

    public float getOpacity() {
	return opacity;
    }

    public void setOpacity(float opacity) {
	this.opacity = opacity;
    }

    @Override
    public String toString() {
	return "" + face + " of " + suit;
    }
    
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        initTimeLines();
        loadImage();
    }

}
