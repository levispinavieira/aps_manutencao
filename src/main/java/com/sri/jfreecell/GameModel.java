package com.sri.jfreecell;

import static com.sri.jfreecell.event.GameEvents.COMPLETE;
import static com.sri.jfreecell.event.GameEvents.INPROGRESS;
import static com.sri.jfreecell.event.GameEvents.MOVE;
import static com.sri.jfreecell.event.GameEvents.NEW;
import static com.sri.jfreecell.event.GameEvents.NOMOVESLEFT;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.concurrent.ThreadLocalRandom;

import com.sri.jfreecell.event.GameEvent;
import com.sri.jfreecell.event.GameEvents;
import com.sri.jfreecell.event.GameListener;

/**
 * Model for the Game.
 * 
 * @author Sateesh Gampala
 *
 */
public class GameModel implements Iterable<CardPile>, Serializable {

    private static final long serialVersionUID = -6674914038491480772L;

    private CardPile[] freeCells;
    private CardPile[] tableau;
    private CardPile[] tableauBkp;
    private CardPile[] foundation;

    private ArrayList<CardPile> allPiles;
    private transient ArrayList<GameListener> gameListeners;
    private transient ArrayDeque<CardPile> undoStack;

    private int completedCards = 0;
    private int noOfAutoMoves = 0;

    private boolean autoComplete = true;
    private int moveHelpidx = 0;
    private Object prevBlinkFrom;
    private Object prevBlinkTo;

    private GameEvents state;
    public boolean showBlackScreen = false;
    public int gameNo;

    public GameModel() {
        gameListeners = new ArrayList<GameListener>();
        allPiles = new ArrayList<CardPile>();
        undoStack = new ArrayDeque<CardPile>();

        freeCells = new CardPile[4];
        tableau = new CardPileTableau[8];
        tableauBkp = new CardPileTableau[8];
        foundation = new CardPile[4];

        // Create empty piles to hold "foundation"
        for (int pile = 0; pile < foundation.length; pile++) {
            foundation[pile] = new CardPileFoundation();
            allPiles.add(foundation[pile]);
        }
        // Create empty piles of Free Cells.
        for (int pile = 0; pile < freeCells.length; pile++) {
            freeCells[pile] = new CardPileFreeCell();
            allPiles.add(freeCells[pile]);
        }
        // Arrange the cards into piles.
        for (int pile = 0; pile < tableau.length; pile++) {
            tableau[pile] = new CardPileTableau();
            tableauBkp[pile] = new CardPileTableau();
            allPiles.add(tableau[pile]);
        }

        gameNo = getRandGameNo();
        startNewGame();
    }

    private int getRandGameNo() {
        return ThreadLocalRandom.current().nextInt(1, 32000 + 1);
    }

    public void loadGame(int game) {
        gameNo = game;
        startNewGame();
    }

    public void loadRandGame() {
        gameNo = getRandGameNo();
        startNewGame();
    }

    public void startNewGame() {
        CardPile pile;
        Deck deck;
        try {
            pile = Deal.initialize(gameNo);
            deck = new Deck(pile);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Game not Found: " + gameNo);
            deck = new Deck();
        }

        // ... Empty all the piles.
        for (CardPile p : allPiles) {
            p.clear();
        }

        for (int i = 0; i < tableauBkp.length; i++) {
            tableauBkp[i].clear();
        }

        // ... Deal the cards into the piles.
        int whichPile = 0;
        for (Card crd : deck) {
            tableau[whichPile].pushIgnoreRules(crd);
            tableauBkp[whichPile].pushIgnoreRules(crd);
            whichPile = (whichPile + 1) % tableau.length;
        }

        for (int i = 0; i < tableau.length; i++) {
            ((CardPileTableau) tableau[i]).updateCascades();
        }

        state = NEW;
        completedCards = 0;
        undoStack.clear();
        notifyChanges();
    }

    public void restartGame() {
        for (CardPile p : allPiles) {
            p.clear();
        }

        for (int i = 0; i < tableauBkp.length; i++) {
            for (Card card : tableauBkp[i]) {
                tableau[i].pushIgnoreRules(card);
            }
            ((CardPileTableau) tableau[i]).updateCascades();
        }

        state = NEW;
        completedCards = 0;
        undoStack.clear();
        notifyChanges();
    }

    public boolean moveFromPileToPile(Card card, CardPile source, CardPile target) {
        if (card.equals(source.peekTop())) {
            return moveCard(source, target);
        } else {
            if (target.isAllowedtoAdd(card)) {
                ArrayList<Card> cards = source.getCardListFrom(card);
                int moves = getMaxNoOfCardsMovable(target.size() == 0);
                if (cards.size() <= moves) {
                    moveCards(cards, source, target);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If auto complete is enable, it checks and move the eligible cards from
     * Tableau and FreeCell Piles to Foundation Pile.
     */
    public void checkForAutoMoves() {
        noOfAutoMoves = 0;
        if (!autoComplete)
            return;

        boolean moveHappened;
        int ord;
        int[] minval = getMinEligibleOrdinals();
        Card c;
        do {
            moveHappened = false;
            for (CardPile pile : getTableauPiles()) {
                if (pile.size() == 0)
                    continue;
                c = pile.peekTop();
                ord = c.getFace().ordinal();
                if (c.getSuit().getColor().equals(Color.RED)) {
                    if (ord > minval[0] && ord > 1)
                        continue;
                } else {
                    if (ord > minval[1] && ord > 1)
                        continue;
                }
                if (moveToFoundationPile(pile)) {
                    minval = getMinEligibleOrdinals();
                    moveHappened = true;
                    noOfAutoMoves++;
                }
            }

            for (CardPile pile : getFreeCellPiles()) {
                if (pile.size() == 0)
                    continue;
                c = pile.peekTop();
                ord = c.getFace().ordinal();
                if (c.getSuit().getColor().equals(Color.RED)) {
                    if (ord > minval[0] && ord > 1)
                        continue;
                } else {
                    if (ord > minval[1] && ord > 1)
                        continue;
                }
                if (moveToFoundationPile(pile)) {
                    minval = getMinEligibleOrdinals();
                    moveHappened = true;
                    noOfAutoMoves++;
                }
            }
        } while (moveHappened);
        if (noOfAutoMoves > 0) {
            notifyChanges();
            validate();
        }
        return;
    }

    /**
     * Returns minimum valued card (Black & Red) that can be moved automatically
     * to Foundation pile. E.g., If we need to move 6 of Spade (Black) then
     * minimum both 5 Diamond and 5 Heart (Red) should be on Foundation Pile.
     * 
     * @return Array of Black and Red ordinal
     */
    private int[] getMinEligibleOrdinals() {
        int blk = -1;
        int red = -1;
        int ord;
        for (CardPile pile : getFoundationPiles()) {
            if (pile.size() == 0)
                continue;
            Card c = pile.peekTop();
            ord = c.getFace().ordinal();
            if (c.getSuit().getColor().equals(Color.BLACK)) {
                if (ord < blk || blk == -1) {
                    blk = ord;
                }
            } else {
                if (ord < red || red == -1) {
                    red = ord;
                }
            }
        }

        int min[] = { blk + 1, red + 1 };
        return min;
    }

    public boolean moveToFoundationPile(CardPile source) {
        if (source.size() == 0)
            return false;
        CardPile target = findEligibleCellInFoundationPile(source.peekTop());
        if (target == null)
            return false;
        return moveCard(source, target);
    }

    private CardPile findEligibleCellInFoundationPile(Card card) {
        for (CardPile pile : getFoundationPiles()) {
            if (pile.isAllowedtoAdd(card)) {
                return pile;
            }
        }
        return null;
    }

    public boolean moveToFreeCellPile(CardPile source) {
        CardPile target = findEmptyCellInFreeCellPile();
        if (target == null)
            return false;
        return moveCard(source, target);
    }

    private CardPile findEmptyCellInFreeCellPile() {
        for (CardPile pile : getFreeCellPiles()) {
            if (pile.size() == 0)
                return pile;
        }
        return null;
    }

    private boolean moveCard(CardPile source, CardPile target) {
        if (source.size() == 0)
            return false;
        Card crd = source.peekTop();
        if (target.isAllowedtoAdd(crd)) {
            Rectangle pos = null;
            int x = crd.getX(), y = crd.getY();
            if (!(target instanceof CardPileTableau)) {
                pos = target.getPosition();
            }
            target.push(crd);
            source.pop();
            recordStep(source, target);
            if (pos != null) {
                crd.moveTo(pos.x, pos.y);
            } else {
                crd.moveFrom(x, y);
            }
            if(state.equals(NEW)) {
                state = INPROGRESS;
            }
            return true;
        }
        return false;
    }

    private void recordStep(CardPile source, CardPile target) {
        if (target instanceof CardPileFoundation) {
            completedCards++;
        } else if (source instanceof CardPileFoundation) {
            completedCards--;
        }
        moveHelpidx = 0;
        undoStack.push(source);
        undoStack.push(target);
    }

    private boolean moveCards(ArrayList<Card> cards, CardPile source, CardPile target) {
        if (source.size() == 0)
            return false;
        for (Card card2 : cards) {
            target.push(card2);
            source.pop();
        }
        recordBulkStep(source, target, cards.size());
        return true;
    }

    private void recordBulkStep(CardPile source, CardPile target, int size) {
        if (target instanceof CardPileFoundation) {
            completedCards += size;
        } else if (source instanceof CardPileFoundation) {
            completedCards -= size;
        }
        moveHelpidx = 0;
        undoStack.push(source);
        undoStack.push(new CardPileBulk(target, size));
    }

    public void undoLastStep() {
        if (noOfAutoMoves > 0) {
            for (int i = 0; i < noOfAutoMoves; i++) {
                undoMove();
                noOfAutoMoves--;
            }
        }
        undoMove();
    }

    public void undoMove() {
        if (!undoStack.isEmpty()) {
            CardPile target = undoStack.pop();
            CardPile source = undoStack.pop();
            if (target instanceof CardPileBulk) {
                CardPileBulk bulk = (CardPileBulk) target;
                ArrayList<Card> cards = new ArrayList<Card>();
                for (int i = 0; i < bulk.count; i++) {
                    cards.add(bulk.pile.pop());
                }
                Collections.reverse(cards);
                for (Card card : cards) {
                    source.pushIgnoreRules(card);
                }
                if (source instanceof CardPileTableau) {
                    ((CardPileTableau) source).updateCascades();
                }
            } else {
                source.pushIgnoreRules(target.popIgnoreRules());
                if (source instanceof CardPileTableau) {
                    ((CardPileTableau) source).updateCascades();
                }
                if (target instanceof CardPileTableau) {
                    ((CardPileTableau) target).deleteFromCascade(source.peekTop());
                }
                if (target instanceof CardPileFoundation) {
                    completedCards--;
                }
            }
            moveHelpidx = 0;
            notifyChanges();
        }
    }

    public void validate() {
        if (completedCards == 52) {
            completedCards = 0;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    state = COMPLETE;
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    CardPile[] cardPile1 = getFoundationPiles();
                    Card card = null;
                    ListIterator<Card> li0 = cardPile1[0].reverseIterator();
                    ListIterator<Card> li1 = cardPile1[1].reverseIterator();
                    ListIterator<Card> li2 = cardPile1[2].reverseIterator();
                    ListIterator<Card> li3 = cardPile1[3].reverseIterator();

                    int targetY = UICardPanel.DISPLAY_HEIGHT + Card.CARD_HEIGHT;

                    for (int i = 0; i < 52; i++) {
                        synchronized (li0) {
                            if (li0.hasPrevious()) {
                                card = li0.previous();
                                card.turnFaceDown();
                                card.bounce(200, targetY, (i * 250));
                            }
                        }
                        synchronized (li1) {
                            if (li1.hasPrevious()) {
                                card = li1.previous();
                                card.turnFaceDown();
                                card.bounce(300, targetY, (i * 250));
                            }
                        }
                        synchronized (li2) {
                            if (li2.hasPrevious()) {
                                card = li2.previous();
                                card.turnFaceDown();
                                card.bounce(400, targetY, (i * 250));
                            }
                        }
                        synchronized (li3) {
                            if (li3.hasPrevious()) {
                                card = li3.previous();
                                card.turnFaceDown();
                                card.bounce(500, targetY, (i * 250));
                            }
                        }
                    }
                    try {
                        Thread.sleep(6000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    showBlackScreen = true;
                    notifyChanges();
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    fireGameEvent(COMPLETE);
                    showBlackScreen = false;
                }
            }).start();
        }
        if (!hasMoves(false)) {
            fireGameEvent(NOMOVESLEFT);
        }
    }

    private boolean hasMoves(boolean showHelp) {
        CardPile[] tPiles = getTableauPiles();
        CardPile[] fcPiles = getFreeCellPiles();
        CardPile[] fPiles = getFoundationPiles();

        for (int j = 0; j < fcPiles.length; j++) {
            if (fcPiles[j].size() == 0) {
                return true;
            } else {
                CardPile cardPile2 = fcPiles[j];
                for (int i = 0; i < tPiles.length; i++) {
                    CardPile cardPile1 = tPiles[i];
                    if (cardPile1.size() > 0) {
                        if (cardPile1.isAllowedtoAdd(cardPile2.peekTop())) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
                for (int i = 0; i < fPiles.length; i++) {
                    if (fPiles[i].isAllowedtoAdd(cardPile2.peekTop())) {
                        return true;
                    }
                }
            }
        }

        for (int i = 0; i < tPiles.length; i++) {
            CardPile cardPile1 = tPiles[i];
            if (cardPile1.size() == 0) {
                return true;
            } else {
                for (int j = i + 1; j < tPiles.length; j++) {
                    CardPile cardPile2 = tPiles[j];
                    boolean canMove = cardPile2.size() == 0 || cardPile2.isAllowedtoAdd(cardPile1.peekTop())
                        || cardPile1.isAllowedtoAdd(cardPile2.peekTop());
                    if (canMove)
                        return true;
                }
                for (int j = 0; j < fPiles.length; j++) {
                    if (fPiles[j].isAllowedtoAdd(cardPile1.peekTop()))
                        return true;
                }
            }
        }
        return false;
    }

    public void findHint() {
        CardPileTableau[] tPiles = (CardPileTableau[]) getTableauPiles();
        CardPile[] fcPiles = getFreeCellPiles();
        CardPile[] fPiles = getFoundationPiles();
        int idx = 0;
        int maxMoves = getMaxNoOfCardsMovable(false);

        for (int i = 0; i < tPiles.length; i++) {
            CardPileTableau cardPile1 = tPiles[i];
            if (cardPile1.size() > 0) {
                ArrayList<Card> cards = cardPile1.getCascades(maxMoves);
                for (int j = 0; j < tPiles.length; j++) {
                    if (j == i)
                        continue;
                    CardPileTableau cardPile2 = tPiles[j];
                    if (cardPile2.size() == 0) {
                        int maxMovesToBlnk = getMaxNoOfCardsMovable(true);
                        int cSize = cards.size();
                        int bIdx = maxMovesToBlnk > cSize ? cSize : cSize - maxMovesToBlnk;
                        if (showHint(idx, cards.get(bIdx), cardPile2)) {
                            return;
                        }
                        idx++;
                    } else {
                        for (Card card : cards) {
                            if (cardPile2.isAllowedtoAdd(card)) {
                                if (showHint(idx, card, cardPile2)) {
                                    return;
                                }
                                idx++;
                            }
                        }
                    }
                }
            }
        }

        for (int j = 0; j < fcPiles.length; j++) {
            if (fcPiles[j].size() > 0) {
                CardPile cardPile2 = fcPiles[j];
                for (int i = 0; i < tPiles.length; i++) {
                    CardPile cardPile1 = tPiles[i];
                    if (cardPile1.size() > 0) {
                        if (cardPile1.isAllowedtoAdd(cardPile2.peekTop())) {
                            if (showHint(idx, cardPile2.peekTop(), cardPile1)) {
                                return;
                            }
                            idx++;
                        }
                    }
                }
                for (int i = 0; i < fPiles.length; i++) {
                    if (fPiles[i].isAllowedtoAdd(cardPile2.peekTop())) {
                        if (showHint(idx, cardPile2.peekTop(), fPiles[i])) {
                            return;
                        }
                        idx++;
                    }
                }
            }
        }

        for (int j = 0; j < fcPiles.length; j++) {
            if (fcPiles[j].size() == 0) {
                for (int i = 0; i < tPiles.length; i++) {
                    if (showHint(idx, tPiles[i].peekTop(), fcPiles[j])) {
                        return;
                    }
                    idx++;
                }
                break;
            }
        }

        if (idx > 0 && moveHelpidx == idx) {
            moveHelpidx = 0;
            findHint();
        } else {
            moveHelpidx = 0;
        }
    }

    private boolean showHint(int idx, Card card, CardPile cardPile2) {
        if (idx == moveHelpidx) {
            clearHint();

            card.blink(0);
            prevBlinkFrom = card;

            if (cardPile2.size() > 0) {
                cardPile2.peekTop().blink(1000);
                prevBlinkTo = cardPile2.peekTop();
            } else {
                cardPile2.blink(1000);
                prevBlinkTo = cardPile2;
            }
            moveHelpidx++;
            return true;
        }
        return false;
    }

    private void clearHint() {
        if (prevBlinkFrom != null) {
            ((Card) prevBlinkFrom).stopBlink();
        }
        if (prevBlinkTo != null) {
            if (prevBlinkTo instanceof Card) {
                ((Card) prevBlinkTo).stopBlink();
            } else {
                ((CardPile) prevBlinkTo).stopBlink();
            }
        }
    }

    private int getMaxNoOfCardsMovable(boolean toEmpty) {
        int tCount = 0, fCount = 0;
        CardPile[] tPiles = getTableauPiles();
        CardPile[] fcPiles = getFreeCellPiles();

        for (int j = 0; j < fcPiles.length; j++) {
            if (fcPiles[j].size() == 0) {
                fCount++;
            }
        }

        for (int j = 0; j < tPiles.length; j++) {
            if (tPiles[j].size() == 0) {
                tCount++;
            }
        }
        if (tCount > 0) {
            if (fCount > 0) {
                fCount--;
                tCount++;
            }
            if (toEmpty) {
                tCount--;
            }
            tCount = ((tCount * (tCount + 1)) / 2);
        }

        return tCount + fCount + 1;
    }

    public void notifyChanges() {
        fireGameEvent(MOVE, 52 - completedCards);
    }

    public synchronized void addGameListener(GameListener l) {
        gameListeners.add(l);
    }

    public synchronized void removeGameListener(GameListener l) {
        gameListeners.remove(l);
    }

    private synchronized void fireGameEvent(GameEvents eve, Object value) {
        GameEvent event = new GameEvent(this, eve, value);
        for (GameListener listener : gameListeners) {
            listener.onEvent(event);
        }
    }

    private void fireGameEvent(GameEvents eve) {
        fireGameEvent(eve, null);
    }

    public Iterator<CardPile> iterator() {
        return allPiles.iterator();
    }

    public CardPile getTableauPile(int i) {
        return tableau[i];
    }

    public CardPile[] getTableauPiles() {
        return tableau;
    }

    public CardPile[] getFreeCellPiles() {
        return freeCells;
    }

    public CardPile getFreeCellPile(int cellNum) {
        return freeCells[cellNum];
    }

    public CardPile[] getFoundationPiles() {
        return foundation;
    }

    public CardPile getFoundationPile(int cellNum) {
        return foundation[cellNum];
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }
    
    public GameEvents getState() {
        return state;
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        gameListeners = new ArrayList<GameListener>();
        undoStack = new ArrayDeque<CardPile>();
    }
}
