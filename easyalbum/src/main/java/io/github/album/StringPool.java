package io.github.album;

final class StringPool {
    int size;
    Node[] nodes = new Node[16];

    private static class Node {
        final int hash;
        final String key;
        Node next;

        Node(int hash, String key, Node next) {
            this.hash = hash;
            this.key = key;
            this.next = next;
        }
    }

    public String getOrAdd(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        int h = key.hashCode();
        h = h ^ (h >>> 16);
        Node node = find(h, key);
        if (node != null) {
            return node.key;
        } else {
            add(h, key);
            return key;
        }
    }

    private Node find(int hash, String key) {
        int n = nodes.length;
        Node node = nodes[hash & (n - 1)];
        while (node != null) {
            if (node.hash == hash && node.key.equals(key)) {
                return node;
            }
            node = node.next;
        }
        return null;
    }

    private void add(int hash, String key) {
        int n = nodes.length;
        int index;
        if (size >= ((n >> 2) * 3)) {
            int newCapacity = n << 1;
            int mask = newCapacity - 1;
            Node[] newArray = new Node[newCapacity];
            for (Node value : nodes) {
                Node node = value;
                while (node != null) {
                    Node prev = node;
                    node = prev.next;
                    int i = prev.hash & mask;
                    prev.next = newArray[i];
                    newArray[i] = prev;
                }
            }
            nodes = newArray;
            index = hash & mask;
        } else {
            index = hash & (n - 1);
        }
        Node head = nodes[index];
        nodes[index] = new Node(hash, key, head);
        size++;
    }
}
