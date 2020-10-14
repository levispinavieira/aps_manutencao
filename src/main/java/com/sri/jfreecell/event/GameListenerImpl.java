package com.sri.jfreecell.event;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;

import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.sri.jfreecell.UIFreeCell;

/**
 * Game event listener implementation class
 * 
 * @author Sateesh Gampala
 *
 */
public class GameListenerImpl implements GameListener {

    private UIFreeCell frame;

    public GameListenerImpl(UIFreeCell frame) {
        this.frame = frame;
    }

    @Override
    public void onEvent(GameEvent ge) {
        switch (ge.getEvent()) {
            case MOVE:
                onMove(ge);
                break;
            case COMPLETE:
                gameComplete();
                break;
            case NOMOVESLEFT:
                noMovesLeft();
                break;
            default:
                break;
        }
    }

    private void onMove(GameEvent ge) {
        frame.updateCardCount((int) ge.getValue());
        frame.updateNumeroJogadas(ge.getNumeroJogadas());
        frame.repaint();
    }

    private void gameComplete() {
        JLabel aLabel = new JLabel("Parabéns, você venceu");
        JLabel bLabel = new JLabel("Deseja jogar novamente?");
        JCheckBox cCheck = new JCheckBox("Selecione um jogo");
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.add(aLabel);
        panel.add(bLabel);
        panel.add(cCheck);

        int input = showOptionDialog(frame, panel, "Fim de Jogo", YES_NO_OPTION, INFORMATION_MESSAGE, null, null, null);
        if (input == 0) {
            if (cCheck.isSelected()) {
                frame.selectGame();
            } else {
                frame.loadRandGame();
            }
        } else {
           frame.exit(true);
        }
    }

    private void noMovesLeft() {
    	String[] buttons = { "Restart", "Novo Jogo", "Voltar um movimento" };
        int input = showOptionDialog(frame, "Sem mais movimentos !!!", "Sem Movimentos", YES_NO_OPTION, ERROR_MESSAGE, null, buttons,
            buttons[0]);

        switch (input) {
            case 0:
                frame.model.restartGame();
                break;
            case 1:
                frame.loadRandGame();
                break;
            case 2:
                frame.model.undoLastStep();
                break;
            default:
                frame.loadRandGame();
                break;
        }
    }

}
