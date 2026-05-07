package logic;

public class Node<E> {//מחלקה גנרית לבניית עץ התנהגות
    private E data;           // התוכן של הצומת
    private Node<E> left;     // מצביע לבן השמאלי
    private Node<E> right;    // מצביע לבן הימני

    public Node(E data) {
        this.data = data;
        this.left = null;
        this.right = null;
    }

    public Node(E data, Node<E> left, Node<E> right) {
        this.data = data;
        this.left = left;
        this.right = right;
    }

    public E getData() { return data; }//נותנת את המידע
    public Node<E> getLeft() { return left; }
    public Node<E> getRight() { return right; }
}