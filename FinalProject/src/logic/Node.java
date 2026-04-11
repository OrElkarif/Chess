package logic;

public class Node<E> {
    private E data;           // התוכן של הצומת
    private Node<E> left;     // מצביע לבן השמאלי
    private Node<E> right;    // מצביע לבן הימני

    // בנאי לצומת שהוא "עלה" (ללא בנים)
    public Node(E data) {
        this.data = data;
        this.left = null;
        this.right = null;
    }

    // בנאי לצומת עם בנים
    public Node(E data, Node<E> left, Node<E> right) {
        this.data = data;
        this.left = left;
        this.right = right;
    }

    // פעולות מאחזרות
    public E getData() { return data; }
    public Node<E> getLeft() { return left; }
    public Node<E> getRight() { return right; }
}