package com.sri.jfreecell;

import static com.sri.jfreecell.event.MenuActionListener.MenuAction.ABOUT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.EXIT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.HELP;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.HINT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.NEW;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.OPTIONS;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.RESTART;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.SELECT;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.STATISTICS;
import static com.sri.jfreecell.event.MenuActionListener.MenuAction.UNDO;
import static com.sri.jfreecell.util.FileUtil.STATE_FILE;
import static com.sri.jfreecell.util.FileUtil.*;
import static com.sri.jfreecell.util.FileUtil.saveObjecttoFile;
import static java.awt.event.ActionEvent.ALT_MASK;
import static java.awt.event.ActionEvent.CTRL_MASK;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_F1;
import static java.awt.event.KeyEvent.VK_F2;
import static java.awt.event.KeyEvent.VK_F3;
import static java.awt.event.KeyEvent.VK_F4;
import static java.awt.event.KeyEvent.VK_F5;
import static java.awt.event.KeyEvent.VK_G;
import static java.awt.event.KeyEvent.VK_H;
import static java.awt.event.KeyEvent.VK_J;
import static java.awt.event.KeyEvent.VK_N;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_R;
import static java.awt.event.KeyEvent.VK_S;
import static java.awt.event.KeyEvent.VK_T;
import static java.awt.event.KeyEvent.VK_U;
import static java.awt.event.KeyEvent.VK_X;
import static java.awt.event.KeyEvent.VK_Z;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.sri.jfreecell.event.GameEvents;
import com.sri.jfreecell.event.GameListenerImpl;
import com.sri.jfreecell.event.MenuActionListener;
import com.sri.jfreecell.util.ImageUtil;

/**
 * Main class for FreeCell. Free Cell solitaire program. Main program / JFrame.
 * Adds a few components and the main graphics area, UICardPanel, that handles
 * the mouse and painting.
 * 
 * @author Sateesh Gampala
 * @version 5.0.1
 */
public class UIFreeCell extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final ClassLoader CLSLDR = UIFreeCell.class.getClassLoader();
    private static final ImageIcon icon = new ImageIcon(CLSLDR.getResource("cardimages/icon.png"));

    public GameModel model;

    public static final String version = "5.2.10";

    private UICardPanel boardDisplay;
    private JLabel cardCount;

    private static final int PORT = 6789;
    private static ServerSocket socket;

    public static void main(String[] args) {
        checkIfRunning();
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new UIFreeCell();
            }
        });
    }

    public UIFreeCell() {
        checkAndLoadGame();
        boardDisplay = new UICardPanel(model);
        model.addGameListener(new GameListenerImpl(this));

        cardCount = new JLabel("52 ", SwingConstants.RIGHT);
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controlPanel.add(new JLabel("Cards Left:"));
        controlPanel.add(cardCount);

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(controlPanel, BorderLayout.SOUTH);
        content.add(boardDisplay, BorderLayout.CENTER);

        setContentPane(content);
        setJMenuBar(createMenu());
        setTitle("FreeCell #" + model.gameNo);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit(model.getState().equals(GameEvents.COMPLETE));
                e.getWindow().dispose();
            }
        });
        setIconImage(icon.getImage());
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
        this.model.notifyChanges();
    }

    private JMenuBar createMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Game");
        menu.setMnemonic(VK_G);
        menuBar.add(menu);

        createMenuItem(menu, NEW, VK_N, VK_F2, 0);
        createMenuItem(menu, SELECT, VK_S, VK_F3, 0);
        createMenuItem(menu, RESTART, VK_R, VK_R, CTRL_MASK);
        menu.addSeparator();
        createMenuItem(menu, UNDO, VK_U, VK_Z, CTRL_MASK);
        createMenuItem(menu, HINT, VK_H, VK_H, 0);
        menu.addSeparator();
        createMenuItem(menu, STATISTICS, VK_T, VK_F4, 0).setEnabled(false);
        createMenuItem(menu, OPTIONS, VK_O, VK_F5, 0).setEnabled(false);
        menu.addSeparator();
        createMenuItem(menu, EXIT, VK_X, VK_X, ALT_MASK);

        menu = new JMenu("Help");
        menu.setMnemonic(VK_H);
        menuBar.add(menu);

        createMenuItem(menu, HELP, VK_J, VK_F1, 0);
        createMenuItem(menu, ABOUT, VK_A, 0, 0);
        menuBar.add(menu);
        
        return menuBar;
    }

    private JMenuItem createMenuItem(JMenu parent, String menuName, int mnemonic, int keyCode, int modifiers) {
        JMenuItem menuItem = new JMenuItem(menuName, mnemonic);
        if (keyCode > 0)
            menuItem.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        menuItem.addActionListener(new MenuActionListener(this));
        parent.add(menuItem);
        return menuItem;
    }

    public void updateCardCount(int count) {
        cardCount.setText(count + " ");
    }

    public void loadRandGame() {
        model.loadRandGame();
        setTitle("FreeCell #" + model.gameNo);
    }

    /**
     * Shows window to select game.
     */
    public void selectGame() {
        int gameNo = 1;
        JLabel bLabel = new JLabel("Select a game number from 1 to 32000");
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(bLabel);
        String userInput = showInputDialog(this, panel, "Game Number", PLAIN_MESSAGE);
        if (userInput == null)
            return;
        try {
            gameNo = Integer.parseInt(userInput);
            if (gameNo < 1 || gameNo > 32000)
                throw new Exception();
        } catch (Exception e) {
            showMessageDialog(this, "Invalid game number! Try again.");
            return;
        }
        setTitle("FreeCell #" + gameNo);
        model.loadGame(gameNo);
    }

    /**
     * Create and show About window.
     */
    public void showAbout() {
        icon.setImage(ImageUtil.getScaledImage(icon.getImage(), 40, 40));
        JLabel aLabel = new JLabel("<html>FreeCell<br> v" + version + "</html>", icon, JLabel.LEFT);
        JLabel bLabel = new JLabel("<html>\u00a9 2016-17 Sateesh Chandra G<br>All rights reserved.</html>");
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(aLabel);
        panel.add(bLabel);
        showMessageDialog(this, panel, "About FreeCell", PLAIN_MESSAGE);
    }

    /**
     * Checks if any other instance is already running.
     */
    private static void checkIfRunning() {
        try {
            socket = new ServerSocket(PORT, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
        } catch (BindException e) {
            System.err.println("Found another JFreeCell instance is running.");
            showMessageDialog(null, "Looks like another instance of Freecell is already running.", "Alert", ERROR_MESSAGE);
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Unexpected error.");
            e.printStackTrace();
            System.exit(2);
        }
    }

    private void checkAndLoadGame() {
        GameModel model = (GameModel) getObjectfromFile(STATE_FILE);
        if (model != null) {
            this.model = model;
            deleteFile(STATE_FILE);
        } else {
            this.model = new GameModel();
        }
    }

    public void exit(boolean isGameComplete) {
        if(!isGameComplete) {
            saveObjecttoFile(model, STATE_FILE);
        }
        System.exit(0);
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
