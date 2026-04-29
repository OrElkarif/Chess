package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WinScreen extends JPanel {
    private JFrame parentFrame;

    public WinScreen(JFrame frame, Color winner, String reason) {
        this.parentFrame = frame;
        setLayout(new BorderLayout());
        setBackground(new Color(49, 46, 43));

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(new Color(49, 46, 43));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(150, 0, 0, 0));

        String winnerText;
        if (winner == null) {
            winnerText = "DRAW!";
        } else if (winner == Color.WHITE || winner == Color.white) {
            winnerText = "WHITE WINS!";
        } else {
            winnerText = "BLACK WINS!";
        }

        JLabel titleLabel = new JLabel(winnerText);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 60));
        titleLabel.setForeground(new Color(230, 192, 141));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT); 

        JLabel reasonLabel = new JLabel(reason);
        reasonLabel.setFont(new Font("Arial", Font.PLAIN, 24));
        reasonLabel.setForeground(Color.LIGHT_GRAY);
        reasonLabel.setAlignmentX(Component.CENTER_ALIGNMENT); 

        JButton playAgainButton = new JButton("PLAY AGAIN");
        playAgainButton.setFont(new Font("Arial", Font.BOLD, 30));
        playAgainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAgainButton.setMaximumSize(new Dimension(250, 60));
        playAgainButton.setBackground(new Color(230, 192, 141));
        playAgainButton.setForeground(new Color(49, 46, 43));
        playAgainButton.setFocusPainted(false);
        playAgainButton.setBorderPainted(false);

        JButton mainMenuButton = new JButton("MAIN MENU");
        mainMenuButton.setFont(new Font("Arial", Font.BOLD, 30));
        mainMenuButton.setAlignmentX(Component.CENTER_ALIGNMENT); 
        mainMenuButton.setMaximumSize(new Dimension(250, 60));
        mainMenuButton.setBackground(new Color(230, 192, 141));
        mainMenuButton.setForeground(new Color(49, 46, 43));
        mainMenuButton.setFocusPainted(false);
        mainMenuButton.setBorderPainted(false);

        centerPanel.add(titleLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(reasonLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 50)));
        centerPanel.add(playAgainButton);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(mainMenuButton);

        add(centerPanel, BorderLayout.CENTER); 

        playAgainButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parentFrame.getContentPane().removeAll();
                parentFrame.add(new ModeSelectScreen(parentFrame));
                parentFrame.revalidate();
                parentFrame.repaint();
            }
        });

        mainMenuButton.addActionListener(new ActionListener() {
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