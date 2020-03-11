package com.sri.jfreecell;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.swing.JComponent;

import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.RepeatBehavior;
import org.pushingpixels.trident.TimelineScenario;
import org.pushingpixels.trident.callback.TimelineScenarioCallback;
import org.pushingpixels.trident.swing.SwingRepaintTimeline;

import com.sri.jfreecell.firework.VolleyExplosion;

/**
 * JPanel that displays cards, and manages the mouse.
         Cards are in three groups:
         * Tableau. The initial cards are in a "tableau" consisting of
           8 piles, with 7 cards in the first four, and 6 in the second four.
           Cards can be removed from here.  Cards from other tableau piles
           or from free cells can be played on either an empty tableau pile,
           or on a card with a one higher face value and of the opposite color.
         * Free cells.  There are four "free cells" where single cards can
           be temporarily stored.
         * Foundation.  Card suits are built up here.  Only Aces can be
           placed on empty cells and successive cards must be one higher
           of the same suit.  No cards can be removed from the foundation.

         Communication with the model:
         * The mouse can drag cards around. When a dragged card is
           dropped on a pile, the mouseReleased listener calls on the
           model to move the card from one pile to another.
           The "rules" implemented by the piles may prevent this, but
           that's not a problem, because it simply won't be moved, and
           when redrawn, will show up where it originally was.
         * The other interaction between the model and this "mod" of the
           model is that this class implements the ChangeListener interface,
           and registers itself with the model so that it's called whenever
           the model changes.  The stateChanged method that is called when
           this happens only does a repaint and clear of the dragged card info.
           
 * @author Sateesh Gampala
 *
 */
public class UICardPanel extends JComponent implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = -8396218648106782099L;

    // constants
    private static final int NUMBER_OF_PILES = 8;

    // ... Constants specifying position of display elements
    private static final int GAP = 10;
    private static final int FOUNDATION_TOP = GAP;
    private static final int FOUNDATION_BOTTOM = FOUNDATION_TOP + Card.CARD_HEIGHT;

    private static final int FREE_CELL_TOP = GAP;
    private static final int FREE_CELL_BOTTOM = FREE_CELL_TOP + Card.CARD_HEIGHT;

    private static final int TABLEAU_TOP = 2 * GAP + Math.max(FOUNDATION_BOTTOM, FREE_CELL_BOTTOM);
    private static final int TABLEAU_START_X = GAP;
    private static final int TABLEAU_INCR_X = Card.CARD_WIDTH + GAP;

    public static final int DISPLAY_WIDTH = (NUMBER_OF_PILES + 1) * TABLEAU_INCR_X;
    public static final int DISPLAY_HEIGHT = TABLEAU_TOP + 3 * Card.CARD_HEIGHT + GAP;

    private static final Color BACKGROUND_COLOR = new Color(0, 110, 135);

    // fields

    /** Position in image of mouse press to make dragging look better. */
    private int dragFromX = 0; // Displacement inside image of mouse press
    private int dragFromY = 0;

    // ... Selected card and its pile for dragging purposes.
    private Card draggedCard = null; // Current draggable card
    private CardPile draggedFromPile = null; // Which pile it came from

    private Card highlightedCard = null;
    private CardPile highlightedPile = null;

    private GameModel model;
    private Set<VolleyExplosion> volleys;
    private Map<VolleyExplosion, TimelineScenario> volleyScenarios;

    /** Constructor sets size, colors, and adds mouse listeners. */
    public UICardPanel(GameModel mdl) {
        model = mdl;
        setPreferredSize(new Dimension(DISPLAY_WIDTH, DISPLAY_HEIGHT));
        setBackground(Color.green);

        // ... Add mouse listeners.
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        // ... Set location of all piles in model
        int x = TABLEAU_START_X; // Initial x position.
        for (int pileNum = 0; pileNum < NUMBER_OF_PILES; pileNum++) {
            CardPile p;
            if (pileNum < 4) {
                p = model.getFreeCellPile(pileNum);
                p.setPosition(new Rectangle(x, FREE_CELL_TOP, Card.CARD_WIDTH, Card.CARD_HEIGHT));
                p = model.getFoundationPile(pileNum);
                p.setPosition(new Rectangle(x + (TABLEAU_INCR_X * 5), FOUNDATION_TOP, Card.CARD_WIDTH, Card.CARD_HEIGHT));
            } else {
            }

            p = model.getTableauPile(pileNum);
            p.setPosition(new Rectangle(x + (TABLEAU_INCR_X / 2), TABLEAU_TOP, Card.CARD_WIDTH, 3 * Card.CARD_HEIGHT));

            x += TABLEAU_INCR_X;
        }

        Timeline repaint = new SwingRepaintTimeline(this);
        repaint.playLoop(RepeatBehavior.LOOP);
    }

    /** Draw the cards. */
    @Override
    public void paintComponent(Graphics g) {
        // ... Paint background.
        int width = getWidth();
        int height = getHeight();
        if (model.showBlackScreen) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(BACKGROUND_COLOR);
        }
        g.fillRect(0, 0, width, height);// , because of the override
        g.setColor(Color.BLACK); // Restore pen color.

        if (!model.showBlackScreen) {
            // ... Display each pile.
            for (CardPile pile : model.getTableauPiles()) {
                _drawPile(g, pile, false);
            }
            for (CardPile pile : model.getFreeCellPiles()) {
                _drawPile(g, pile, true);
            }
            for (CardPile pile : model.getFoundationPiles()) {
                _drawPile(g, pile, false);
            }
        }
        if (model.showBlackScreen) {
            if (volleys == null) {
                prepareFireWork();
            } else {
                synchronized (volleys) {
                    for (TimelineScenario scenario : volleyScenarios.values())
                        scenario.resume();
                }
            }
            
            synchronized (volleys) {
                for (VolleyExplosion exp : volleys)
                    exp.paint(g);
            }
        } else {
            if (volleys != null) {
                synchronized (volleys) {
                    for (TimelineScenario scenario : volleyScenarios.values())
                        scenario.suspend();
                }
            }
        }

        // ... Draw the dragged card, if any. Drawing at end So that it will on
        // top of all cards.
        if (draggedCard != null) {
            draggedFromPile.drawDragged(g, draggedCard);
        }
    }

    private void _drawPile(Graphics g, CardPile pile, boolean topOnly) {
        pile.draw(g);
        if (pile.size() > 0) {
            if (topOnly) {
                Card card = pile.peekTop();
                if (card != draggedCard) {
                    // ... Draw only non-dragged card.
                    card.draw(g);
                }
            } else {
                // ... Draw all cards except dragged card.
                for (Card card : pile) {
                    if (card == draggedCard) {
                        break;
                    }
                    card.draw(g);
                }
            }
        }
    }

    public void mouseMoved(MouseEvent e) {
        _findFocusCard(e);
    }

    public void mousePressed(MouseEvent e) {
        _clearDrag();
        if (!_findFocusCard(e)) {
            return;
        }
        if (highlightedPile.isMovable(highlightedCard)) {
            draggedCard = highlightedCard;
            draggedFromPile = highlightedPile;
            dragFromX = e.getX() - highlightedCard.getX();
            dragFromY = e.getY() - highlightedCard.getY();
        }
    }

    public void mouseDragged(MouseEvent e) {
        if (draggedCard == null) {
            return;
        }
        int newX = e.getX() - dragFromX;
        int newY = e.getY() - dragFromY;

        // ... Don't move the image off the screen sides
        newX = Math.max(newX, 0);
        newX = Math.min(newX, getWidth() - Card.CARD_WIDTH);

        // ... Don't move the image off top or bottom
        newY = Math.max(newY, 0);
        newY = Math.min(newY, getHeight() - Card.CARD_HEIGHT);

        draggedFromPile.setPosition(draggedCard, newX, newY);
    }

    public void mouseReleased(MouseEvent e) {
        if (draggedCard != null && draggedFromPile != null) {
            boolean isCardMoved = false;
            CardPile targetPile = _findPileAt(e.getX(), e.getY());
            if (targetPile != null) {
                isCardMoved = model.moveFromPileToPile(draggedCard, draggedFromPile, targetPile);
                if (isCardMoved) {
                    model.notifyChanges();
                    model.validate();
                }
                model.checkForAutoMoves();
            }
            if (!isCardMoved) {
                draggedFromPile.resetCardsPos();
            }
            _clearDrag();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (!_findFocusCard(e)) {
            return;
        }
        if (e.getClickCount() % 2 == 0) {
            if (highlightedCard != null && highlightedPile != null) {
                boolean isCardMoved = model.moveToFoundationPile(highlightedPile);
                if (!isCardMoved && !(highlightedPile instanceof CardPileFreeCell)) {
                    isCardMoved = model.moveToFreeCellPile(highlightedPile);
                }
                if (isCardMoved) {
                    model.notifyChanges();
                    model.validate();
                    _clearHighlight();
                }
                model.checkForAutoMoves();
            }
        }
    }

    private boolean _findFocusCard(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        if (highlightedCard != null && highlightedPile != null && highlightedPile.size() > 0) {
            boolean isTopCard = highlightedCard.equals(highlightedPile.peekTop());
            // Check if focus is in same card
            if (highlightedCard.isVisibleInside(x, y, isTopCard)) {
                highlightedCard.highlight(true);
                return true; // Same card.
            } else {
                highlightedCard.highlight(false);
                // Check if focus is in same pile
                int x1 = highlightedCard.getX();
                Rectangle loc = highlightedPile.getPosition();
                if ((x >= x1 && x < x1 + Card.CARD_WIDTH) && (y >= loc.y && y < loc.height)) {
                    if (_findInPile(highlightedPile, x, y))
                        return true;
                }
            }
        }
        _clearHighlight();
        // Check if focus is on any card.
        for (CardPile pile : model) {
            if (pile.isRemovable() && pile.size() > 0) {
                if (_findInPile(pile, x, y))
                    return true;
            }
        }
        return false;
    }

    private boolean _findInPile(CardPile pile, int x, int y) {
        Card topCard = pile.peekTop();
        for (Card card : pile) {
            if (card.isVisibleInside(x, y, card.equals(topCard))) {
                highlightedPile = pile;
                highlightedCard = card;
                card.highlight(true);
                return true;
            }
        }
        return false;
    }

    private void _clearHighlight() {
        if (highlightedCard != null) {
            highlightedCard.highlight(false);
        }
        highlightedCard = null;
        highlightedPile = null;
    }

    private void _clearDrag() {
        draggedCard = null;
        draggedFromPile = null;
    }

    private CardPile _findPileAt(int x, int y) {
        for (CardPile pile : model) {
            if (pile.getPosition().contains(x, y)) {
                return pile;
            }
        }
        return null;
    }

    // Ignore other mouse events.
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }



    private void prepareFireWork() {
        this.volleys = new HashSet<VolleyExplosion>();
        this.volleyScenarios = new HashMap<VolleyExplosion, TimelineScenario>();

        Timeline repaint = new SwingRepaintTimeline(this);
        repaint.playLoop(RepeatBehavior.LOOP);

        new Thread() {
            @Override
            public void run() {
                while (true) {
                    addExplosions(10);
                }
            }
        }.start();
    }

    private void addExplosions(int count) {
        final CountDownLatch latch = new CountDownLatch(count);

        Random randomizer = new Random();
        for (int i = 0; i < count; i++) {
            int r = randomizer.nextInt(255);
            int g = 100 + randomizer.nextInt(155);
            int b = 50 + randomizer.nextInt(205);
            Color color = new Color(r, g, b);

            int x = 60 + randomizer.nextInt(getWidth() - 120);
            int y = 60 + randomizer.nextInt(getHeight() - 120);
            final VolleyExplosion exp = new VolleyExplosion(x, y, color);

            synchronized (volleys) {
                volleys.add(exp);
                TimelineScenario scenario = exp.getExplosionScenario();
                scenario.addCallback(new TimelineScenarioCallback() {
                    @Override
                    public void onTimelineScenarioDone() {
                        synchronized (volleys) {
                            volleys.remove(exp);
                            volleyScenarios.remove(exp);
                            latch.countDown();
                        }
                    }
                });
                volleyScenarios.put(exp, scenario);
                scenario.play();
            }
        }

        try {
            latch.await();
        } catch (Exception exc) {
        }
    }
}
