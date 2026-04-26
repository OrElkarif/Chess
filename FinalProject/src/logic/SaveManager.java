package logic;

import java.io.*;

public class SaveManager {
    private static final String SAVE_FILE = "chess_save.dat";

    public static void save(GameState state, boolean twoPlayerMode) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(SAVE_FILE))) {
            oos.writeObject(state);
            oos.writeBoolean(twoPlayerMode);
        } catch (IOException e) {
            System.err.println("Save failed: " + e.getMessage());
        }
    }

    public static Object[] load() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(SAVE_FILE))) {
            GameState state = (GameState) ois.readObject();
            
            // --- קריאה לפונקציית התיקון שיצרנו ---
            state.fixColorReferences();
            
            boolean twoPlayer = ois.readBoolean();
            return new Object[]{state, twoPlayer};
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }
    

    public static boolean hasSave() {
        return new File(SAVE_FILE).exists();
    }

    public static void deleteSave() {
        new File(SAVE_FILE).delete();
    }
}