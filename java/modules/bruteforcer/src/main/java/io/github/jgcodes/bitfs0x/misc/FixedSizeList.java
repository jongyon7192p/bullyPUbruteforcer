package io.github.jgcodes.bitfs0x.misc;

import java.util.*;

/**
 * An array, packaged into a list. Useful when you want a list of a generic type and {@link Arrays#asList(Object[])}
 * won't cut it.
 * @param <E> The type of elements in this list.
 */
public class FixedSizeList<E> extends AbstractList<E> {
  private class Itr implements Iterator<E> {
    int idx = 0;
    @Override
    public boolean hasNext() {
      return idx < size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public E next() {
      if (!hasNext())
        throw new NoSuchElementException("End of list reached");
      return (E) elements[idx++];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("It's a fixed size list, you can't remove stuff");
    }
  }
  private class ListItr extends Itr implements ListIterator<E> {
    public ListItr(int index) {
      if (index < 0 || index > elements.length)
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      idx = index;
    }

    @Override
    public boolean hasPrevious() {
      return idx > 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E previous() {
      if (!hasPrevious())
        throw new NoSuchElementException("Start of list reached");
      return (E) elements[--idx];
    }

    @Override
    public int nextIndex() {
      return idx;
    }

    @Override
    public int previousIndex() {
      return idx - 1;
    }

    @Override
    public void set(E e) {
      elements[idx] = e;
    }

    @Override
    public void add(E e) {
      throw new UnsupportedOperationException("It's a fixed size list, you can't add stuff");
    }
  }
  Object[] elements;

  public FixedSizeList(int cap) {
    elements = new Object[cap];
  }

  @Override
  @SuppressWarnings("unchecked")
  public E get(int index) {
    return (E) elements[index];
  }

  @Override
  @SuppressWarnings("unchecked")
  public E set(int index, E element) {
    E result = (E) elements[index];
    elements[index] = element;
    return result;
  }

  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  @Override
  public ListIterator<E> listIterator() {
    return new ListItr(0);
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    return new ListItr(index);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Spliterator<E> spliterator() {
    return (Spliterator<E>) Arrays.spliterator(elements);
  }

  @Override
  public int size() {
    return elements.length;
  }
}
