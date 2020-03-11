package com.sri.jfreecell.event;

import static com.sri.jfreecell.event.MenuActionListener.MenuAction.ABOUT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.EXIT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.HINT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.SELECT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.NEW;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.RESTART;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.UNDO;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.sri.jfreecell.UIFreeCell;

/**
 * Listener class for Menu actions.
 * 
 * @author Sateesh Gampala
 *
 */
public class MenuActionListener implements ActionListener {

    private UIFreeCell frame;
    
    public static class MenuAction {
        public static final String NEW = "New Game";
        public static final String SELECT = "Select Game";
        public static final String RESTART = "Restart Game";
        public static final String UNDO = "Undo Step";
        public static final String HINT = "Hint";
        public static final String STATISTICS = "Statistics";
        public static final String OPTIONS = "Options";
        public static final String HELP = "JFree Cell Help";
        public static final String ABOUT = "About FreeCell";
        public static final String EXIT = "Exit";
   } 

    public MenuActionListener(UIFreeCell frame) {
	this.frame = frame;
    }

    public void actionPerformed(ActionEvent evt) {
	if (evt.getActionCommand().equals(NEW)) {
	    frame.loadRandGame();
	} else if (evt.getActionCommand().equals(SELECT)) {
            frame.selectGame();
        } else if (evt.getActionCommand().equals(RESTART)) {
            frame.model.restartGame();
        } else if (evt.getActionCommand().equals(UNDO)) {
	    frame.model.undoMove();
	} else if (evt.getActionCommand().equals(HINT)) {
	    frame.model.findHint();
	} else if (evt.getActionCommand().equals(EXIT)) {
	    frame.exit(frame.model.getState().equals(GameEvents.COMPLETE));
	} else if (evt.getActionCommand().equals(ABOUT)) {
	    frame.showAbout();
	}
    }

}