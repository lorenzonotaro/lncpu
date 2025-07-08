package com.lnc;

import com.lnc.common.ExtendedListIterator;

import java.util.*;

public class ExtendedListIteratorTest {
    public static void main(String[] args) {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("A", "B", "C", "D"));
        ExtendedListIterator<String> it = new ExtendedListIterator<>(list);

        System.out.println("Initial list: " + list);

        // Test next(), peek(), removeNext()
        System.out.println("Next element: " + it.next()); // A
        System.out.println("Peek next: " + it.peek());    // B
        it.removeNext();                                   // remove B
        System.out.println("After removeNext: " + list); // [A, C, D]

        // Test addBeforeCurrent
        it.addBeforeCurrent("X");
        System.out.println("After addBeforeCurrent('X'): " + list); // [X, A, C, D]

        // Advance to C
        it.next(); // C

        // Test addSequenceBeforeCurrent
        it.addSequenceBeforeCurrent(Arrays.asList("M1", "M2"));
        System.out.println("After addSequenceBeforeCurrent(M1,M2): " + list);
        // Expect [X, A, M1, M2, C, D]

        // Test addAfterCurrent
        it.addAfterCurrent("Y");
        System.out.println("After addAfterCurrent('Y'): " + list); // [X, A, M1, M2, C, Y, D]

        // Test addSequenceAfterCurrent
        it.addSequenceAfterCurrent(Arrays.asList("N1", "N2", "N3"));
        System.out.println("After addSequenceAfterCurrent(N1,N2,N3): " + list);
        // Expect [X, A, M1, M2, C, N1, N2, N3, Y, D] ??? adjust based on current position

        // Test removeCurrent
        // Ensure we have called next() or previous()
        System.out.println("Current next(): " + it.next());
        it.removeCurrent();
        System.out.println("After removeCurrent: " + list);

        // Final traversal
        System.out.println("Final traversal:");
        while (it.hasNext()) {
            System.out.print(it.next() + " ");
        }
        System.out.println(list);
    }
}

