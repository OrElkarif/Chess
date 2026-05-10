package logic;

import java.io.*;

public class SaveManager {//מחלקה שאחראית לכתוב ולקרוא מהמחשב כדי לאפשר שמירות
    private static final String SAVE_FILE = "chess_save.dat";

    public static void save(GameState state, boolean twoPlayerMode) {//הופכת את כל הלוח והזיכרון לקובץ בינארי ושותלת בכונן
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(state);
            oos.writeBoolean(twoPlayerMode);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public static Object[] load() {//קוראת את הקובץ ובונה ממנו בחזרה את כל משתני המשחק כדי להמשיך
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(SAVE_FILE))) {
            GameState state = (GameState) ois.readObject();
            
            state.fixColorReferences();
            
            boolean twoPlayer = ois.readBoolean();
            return new Object[]{state, twoPlayer};
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
    

    public static boolean hasSave() {//בודקת אם בכלל קיים קובץ שמירה כדי לדעת אם להציג כפתור טעינה
        return new File(SAVE_FILE).exists();
    }

    public static void deleteSave() {//מנקה   את קובץ השמירה ברגע שהמשחק נגמר 
        new File(SAVE_FILE).delete();
    }
}