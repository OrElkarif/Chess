package main;
import javax.swing.JFrame;

public class main {//מפעילה את המשחק ופותחת את המסך הראשון

    public static void main(String[] args) {//מדליקה את התוכנית ויוצרת את החלון של המשחק
        JFrame frame = new JFrame("Chess Game");
        frame.add(new StartScreen(frame));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setSize(1025, 845);
    }

}