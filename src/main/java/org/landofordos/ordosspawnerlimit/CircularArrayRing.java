package org.landofordos.ordosspawnerlimit;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class CircularArrayRing<E> extends AbstractCollection<E> implements Ring<E> {

	// This code taken from work done for COMP1201 Tutorial 2
	// No credit claimed for this module

	private Object[] arrRing;
	private int itemCount = 0;
	private int head = 0;

	// default constructor uses size 10
	public CircularArrayRing() {
		super();
		arrRing = new Object[10];
	}

	// can specify a size
	public CircularArrayRing(int size) {
		super();
		arrRing = new Object[size];
	}

	@Override
	public Iterator<E> iterator() {
		return new RingIterator(this);
	}

	private class RingIterator implements Iterator<E> {

		private CircularArrayRing<E> theRing;
		private int index;

		public RingIterator(CircularArrayRing<E> theRing) {
			this.theRing = theRing;
			index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < theRing.itemCount;
		}

		@Override
		public E next() {
			if (!(hasNext())) {
				throw new NoSuchElementException();
			}
			E result = theRing.get(index);
			index = index + 1;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

	@Override
	public int size() {
		return itemCount;
	}

	@Override
	public boolean add(E e) {
		arrRing[head] = e;
		head = (head + 1) % arrRing.length;
		if (itemCount < arrRing.length) {
			itemCount = itemCount + 1;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> e) {
		return false;
	}

	@Override
	public void clear() {
		arrRing = new Object[arrRing.length];
		head = 0;
		itemCount = 0;
	}

	@Override
	public boolean contains(Object e) {
		for (Object anArrRing : arrRing) {
			if (e.equals(anArrRing)) {
				return true;
			}
		}
		// if for loop ends without returning true, return false
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> e) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return (itemCount == 0);
	}

	@Override
	public Object[] toArray() {
		return arrRing;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E get(int index) throws IndexOutOfBoundsException {
		if ((index < 0) || (index > arrRing.length)) {
			throw new IndexOutOfBoundsException();
		}
		int position = (head) - (index + 1);
		// this avoids if statement, marginally faster
		position = (position + arrRing.length) % arrRing.length;
		// not sure how I can avoid returning E for this
		return (E) arrRing[position];
	}
}
