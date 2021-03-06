/*
 * Copyright 2013-2015 Colby Skeggs
 *
 * This file is part of the CCRE, the Common Chicken Runtime Engine.
 *
 * The CCRE is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * The CCRE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the CCRE.  If not, see <http://www.gnu.org/licenses/>.
 */
package ccre.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;

/**
 * A set of useful array-related utilites.
 *
 * @author skeggsc
 */
public class CArrayUtils {

    /**
     * An empty list. Immutable.
     */
    public static final CList<?> EMPTY_LIST = new FixedArrayList<Object>(new Object[0]);

    /**
     * Return an empty list of the given element type.
     *
     * @param <T> the element type.
     * @return the new empty list.
     */
    @SuppressWarnings("unchecked")
    public static <T> CList<T> getEmptyList() {
        return (CList<T>) EMPTY_LIST;
    }

    /**
     * Cast the given array to a generic array. This should not be done unless
     * you know what you are doing! This method only exists to suppress
     * warnings.
     *
     * @param <T> the element type.
     * @param arr the original array.
     * @return the casted array.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] castToGeneric(Object[] arr) {
        return (T[]) arr;
    }

    /**
     * Create a copy of the specified Object[] array, with the specified new
     * length. Added indexes will be filled with null.
     *
     * @param values the old array.
     * @param nlen the length of the new array.
     * @return the new array.
     */
    public static Object[] copyOf(Object[] values, int nlen) {
        Object[] out = new Object[nlen];
        if (nlen < values.length) {
            System.arraycopy(values, 0, out, 0, nlen);
        } else {
            System.arraycopy(values, 0, out, 0, values.length);
        }
        return out;
    }

    /**
     * Create a fixed-size list of from the specified array. Modifications to
     * one will modify the other.
     *
     * @param <T> the element type.
     * @param arr the data to convert to a list.
     * @return the list version of the specified array.
     */
    public static <T> CList<T> asList(final T... arr) {
        if (arr.length == 0) {
            return getEmptyList();
        } else {
            return new FixedArrayList<T>(arr);
        }
    }

    private CArrayUtils() {
    }

    private static class FixedArrayList<T> extends CAbstractList<T> {

        private final T[] arr;

        FixedArrayList(T[] arr) {
            this.arr = arr;
        }

        public int size() {
            return arr.length;
        }

        public T get(int index) {
            return arr[index];
        }

        @Override
        public T set(int index, T val) {
            T old = arr[index];
            arr[index] = val;
            return old;
        }

        @Override
        public int indexOf(Object o) {
            T[] locT = this.arr;
            if (o == null) {
                for (int i = 0; i < locT.length; i++) {
                    if (locT[i] == null) {
                        return i;
                    }
                }
                return -1;
            } else {
                for (int i = 0; i < locT.length; i++) {
                    if (o.equals(locT[i])) {
                        return i;
                    }
                }
                return -1;
            }
        }

        @Override
        public int lastIndexOf(Object o) {
            T[] locT = this.arr;
            if (o == null) {
                for (int i = locT.length - 1; i >= 0; i--) {
                    if (locT[i] == null) {
                        return i;
                    }
                }
                return -1;
            } else {
                for (int i = locT.length - 1; i >= 0; i--) {
                    if (o.equals(locT[i])) {
                        return i;
                    }
                }
                return -1;
            }
        }
    }

    /**
     * Sorts the given list. This is equivalent to dumping the list to an array,
     * running Arrays.sort on it, and then putting the elements back into the
     * list.
     *
     * @param <T> the type of the list elements.
     * @param list the list to sort.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> void sort(CList<T> list) {
        Object[] elements = new Object[list.size()];
        if (list.fillArray(elements) != 0) {
            throw new ConcurrentModificationException();
        }
        Arrays.sort(elements);
        if (list instanceof CLinkedList) {
            ((CLinkedList<T>) list).setAll(elements);
        } else {
            for (int i = 0; i < elements.length; i++) {
                list.set(i, (T) elements[i]);
            }
        }
        if (elements.length != list.size()) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Sorts the given array. Equivalent to Arrays.sort.
     *
     * @param <T> the type of the array elements.
     * @param list the array to sort.
     */
    public static <T extends Comparable<T>> void sort(T[] list) {
        Arrays.sort(list);
    }

    /**
     * Sorts the given array. Equivalent to Arrays.sort.
     *
     * @param list the array to sort.
     */
    public static void sort(Integer[] list) {
        Arrays.sort(list);
    }

    /**
     * Collect everything yielded by an iterable into a CArrayList.
     *
     * @param elements the iterable to collect from.
     * @param <T> the element type of the iterable and therefore the resulting collection.
     * @return the resulting collection, as a CArrayList.
     */
    public static <T> CArrayList<T> collectIterable(Iterable<T> elements) {
        CArrayList<T> out = new CArrayList<T>();
        for (T elem : elements) {
            out.add(elem);
        }
        return out;
    }
}
