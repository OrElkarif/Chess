package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import logic.GameState;
import logic.SaveManager;

public class ModeSelectScreen extends JPanel {
    private JFrame parentFrame;

    public ModeSelectScreen(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(49, 46, 43));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(49, 46, 43));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(100, 0, 0, 0));

        JLabel titleLabel = new JLabel("SELECT MODE");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 55));
        titleLabel.setForeground(new Color(230, 192, 141));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("How many players?");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 22));
        subtitleLabel.setForeground(new Color(180, 160, 130));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- 1 PLAYER button ---
        JButton onePlayerButton = new JButton("1 PLAYER  (vs AI)");
        onePlayerButton.setFont(new Font("Arial", Font.BOLD, 28));
        onePlayerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        onePlayerButton.setMaximumSize(new Dimension(320, 65));
        onePlayerButton.setBackground(new Color(230, 192, 141));
        onePlayerButton.setForeground(new Color(49, 46, 43));
        onePlayerButton.setFocusPainted(false);
        onePlayerButton.setBorderPainted(false);

        // --- 2 PLAYERS button ---
        JButton twoPlayersButton = new JButton("2 PLAYERS");
        twoPlayersButton.setFont(new Font("Arial", Font.BOLD, 28));
        twoPlayersButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        twoPlayersButton.setMaximumSize(new Dimension(320, 65));
        twoPlayersButton.setBackground(new Color(230, 192, 141));
        twoPlayersButton.setForeground(new Color(49, 46, 43));
        twoPlayersButton.setFocusPainted(false);
        twoPlayersButton.setBorderPainted(false);

        // --- CONTINUE button (only if save file exists) ---
        JButton continueButton = new JButton("▶  CONTINUE SAVED GAME");
        continueButton.setFont(new Font("Arial", Font.BOLD, 22));
        continueButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueButton.setMaximumSize(new Dimension(320, 55));
        continueButton.setBackground(new Color(80, 130, 80));
        continueButton.setForeground(Color.WHITE);
        continueButton.setFocusPainted(false);
        continueButton.setBorderPainted(false);
        continueButton.setVisible(SaveManager.hasSave());

        // --- BACK button ---
        JButton backButton = new JButton("BACK");
        backButton.setFont(new Font("Arial", Font.BOLD, 20));
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(160, 50));
        backButton.setBackground(new Color(100, 90, 80));
        backButton.setForeground(new Color(230, 192, 141));
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);

        centerPanel.add(titleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        centerPanel.add(subtitleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 45)));
        centerPanel.add(onePlayerButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(twoPlayersButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(continueButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 35)));
        centerPanel.add(backButton);

        add(centerPanel, BorderLayout.CENTER);

        // --- Actions ---
        onePlayerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new BoardPanel(false));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });

        twoPlayersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new BoardPanel(true));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });

        continueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] saved = SaveManager.load();
                if (saved != null) {
                    GameState state = (GameState) saved[0];
                    boolean twoPlayer = (Boolean) saved[1];
                    parentFrame.getContentPane().removeAll();
                    parentFrame.add(new BoardPanel(twoPlayer, state));
                    parentFrame.revalidate();
                    parentFrame.repaint();
                } else {
                    JOptionPane.showMessageDialog(parentFrame,
                        "Could not load saved game.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                    continueButton.setVisible(false);
                }
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new StartScreen(parentFrame));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });
    }
}