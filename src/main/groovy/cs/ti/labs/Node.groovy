package cs.ti.labs

import groovy.transform.Canonical
import groovy.transform.Sortable;

@Canonical
@Sortable(includes = ['freq', 'letter'])
@Newify
class Node {
    String letter
    int freq
    Node left
    Node right

    boolean isLeaf() { left == null && right == null }
}