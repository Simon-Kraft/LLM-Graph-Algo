public class Queue {

    private static final int EMPTY = -1;

    private int[] items;
    private int front;
    private int rear;
    private int size;

    public Queue(int size) {
        this.items = new int[size];
        this.front = EMPTY;
        this.rear = EMPTY;
        this.size = size;
    }

    public boolean isEmpty() {
        return rear == EMPTY;
    }

    public boolean isFull() {
        return rear == size - 1;
    }

    public void enqueue(int value) {
        if (isFull()) {
            System.err.println("Queue is full.");
        } else {
            if (front == EMPTY) {
                front = 0;
                rear = 0;
                items[rear] = value;
            } else {
                rear++;
                items[rear] = value;
            }
        }
    }

    public int dequeue() {
        int item;
        if (isEmpty()) {
            System.out.println("Queue is empty.");
            item = EMPTY;
        } else {
            item = items[front];
            front++;
            if (front > rear) {
                front = EMPTY;
                rear = EMPTY;
            }
        }
        return item;
    }
}
