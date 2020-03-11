package com.sri.jfreecell;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Initialize deal to one of the pre-determined 32,000 deals in MS FreeCell.
 * 
 * @author Sateesh Gampala
 */
public class Deal {

    private static final ClassLoader CLSLDR = Deal.class.getClassLoader();
    private static final String ranks = ".A23456789TJQK";
    private static final String suits = "CDHS";
    private static InputStream stream;

    private static CardPile initialize(int[] deals) {
        // Gives cards AC - KC, AD - KD, AH - KH, AS - KS
        short[] cards = new short[52];
        for (short i = 0; i < 52; i++) {
            cards[i] = (short) (1 + i);
        }

        // now process the deal number
        short[][] dealt = new short[9][9];
        for (short i = 0; i < 9; i++) {
            for (short j = 0; j < 9; j++) {
                dealt[i][j] = 0;
            }
        }

        int wLeft = 52;
        for (int i = 0; i < deals.length; i++) {
            int j = deals[i];
            dealt[(i % 8) + 1][i / 8] = cards[j];
            cards[j] = cards[--wLeft];
        }

        CardPile cardList = new CardPile();
        for (int r = 0; r <= 6; r++) {
            for (int c = 1; c <= 8; c++) {
                int card = dealt[c][r];
                if (card <= 52 && card > 0) {
                    cardList.push(cardInfo(card));
                }
            }
        }

        return cardList;
    }

    public static final CardPile initialize(int dealNumber) throws IOException {
        stream = CLSLDR.getResourceAsStream("32000.txt");
        Scanner sc = new Scanner(stream);
        try {
            for (int i = 0; i <= 32000; i++) {
                String line = sc.nextLine();
                if (i < dealNumber) {
                    continue;
                }

                StringTokenizer st = new StringTokenizer(line, "., ");
                // get task no.
                int val = Integer.valueOf(st.nextToken()); // bypass number
                assert (val == i);

                // construct deal "shuffle" sequence
                int[] deals = new int[52];
                int idx = 0;
                while (st.hasMoreTokens()) {
                    deals[idx++] = Integer.valueOf(st.nextToken());
                }

                // prepare the initial board.
                return initialize(deals);
            }

            // not found!
            return null;
        } finally {
            sc.close();
            stream.close();
        }
    }

    private static Card cardInfo(int card) {
        int rs = 1 + ((card - 1) >> 2);
        int ss = ((card - 1) % 4);

        try {
            Face f = Face.fromCode(ranks.charAt(rs));
            Suit s = Suit.fromCode(suits.charAt(ss));
            Card c = new Card(f, s);
            c.turnFaceUp();
            return c;
        } catch (Exception e) {
            System.out.println("BAD");
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }
}
