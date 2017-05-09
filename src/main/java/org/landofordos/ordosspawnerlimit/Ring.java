package org.landofordos.ordosspawnerlimit;

import java.util.Collection;
import java.util.Iterator;

public interface Ring<E> extends Collection<E> {

    // Taken from code provided as part of COMP1201 Tutorial 2
    // No credit claimed for this module

    E get(int index) throws IndexOutOfBoundsException;

    Iterator<E> iterator();

    int size();
}
