/*
 * Written by Doug Lea and Martin Buchholz with assistance from
 * members of JCP JSR-166 Expert Group and released to the public
 * domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package org.openjdk.tests.tck;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java8.lang.Iterables;
import java8.util.Iterators;
import java8.util.J8Arrays;
import java8.util.Spliterator;
import java8.util.Spliterators;
import java8.util.concurrent.Phaser;
import java8.util.concurrent.ThreadLocalRandom;
import java8.util.function.Consumer;
import java8.util.function.Predicate;
import java8.util.function.Supplier;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

/**
 * Contains tests applicable to all jdk8+ Collection implementations.
 * An extension of CollectionTest.
 */
@Test
public final class Collection8Test extends JSR166TestCase {
// CVS rev. 1.45

    Collection8Test() {
    }

    /** Tests are parameterized by a Collection implementation. */
    Collection8Test(CollectionImplementation impl, String methodName) {
        super(methodName);
    }

    public static junit.framework.Test testSuite(CollectionImplementation impl) {
        return parameterizedTestSuite(Collection8Test.class,
                                      CollectionImplementation.class,
                                      impl);
    }

    static Object bomb() {
        return new Object() {
                public boolean equals(Object x) { throw new AssertionError(); }
                public int hashCode() { throw new AssertionError(); }
            };
    }

    /** Checks properties of empty collections. */
    @Test(dataProvider = "Source")
    public void testEmptyMeansEmpty(String description, Supplier<CollectionImplementation> sci) throws Throwable {
        CollectionImplementation impl = sci.get();
        Collection<?> c = impl.emptyCollection();
        emptyMeansEmpty(c);

        if (c instanceof java.io.Serializable) {
            try {
                emptyMeansEmpty(serialClonePossiblyFailing(c));
            } catch (java.io.NotSerializableException ex) {
                // excusable when we have a serializable wrapper around
                // a non-serializable collection, as can happen with:
                // Vector.subList() => wrapped AbstractList$RandomAccessSubList
                if (testImplementationDetails
                    && (! c.getClass().getName().matches(
                                "java.util.Collections.*")))
                    throw ex;
            }
        }

        Collection<?> clone = cloneableClone(c);
        if (clone != null)
            emptyMeansEmpty(clone);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void emptyMeansEmpty(Collection<?> c) throws InterruptedException {
        assertTrue(c.isEmpty());
        assertEquals(0, c.size());
        assertEquals("[]", c.toString());
        {
            Object[] a = c.toArray();
            assertEquals(0, a.length);
            assertSame(Object[].class, a.getClass());
        }
        {
            Object[] a = new Object[0];
            assertSame(a, c.toArray(a));
        }
        {
            Integer[] a = new Integer[0];
            assertSame(a, c.toArray(a));
        }
        {
            Integer[] a = { 1, 2, 3};
            assertSame(a, c.toArray(a));
            assertNull(a[0]);
            assertSame(2, a[1]);
            assertSame(3, a[2]);
        }
        assertIteratorExhausted(c.iterator());
        Consumer alwaysThrows = e -> { throw new AssertionError(); };
        Iterables.forEach(c, alwaysThrows);
        Iterators.forEachRemaining(c.iterator(), alwaysThrows);
        Spliterators.spliterator(c).forEachRemaining(alwaysThrows);
        assertFalse(Spliterators.spliterator(c).tryAdvance(alwaysThrows));
        if (Spliterators.spliterator(c).hasCharacteristics(Spliterator.SIZED))
            assertEquals(0, Spliterators.spliterator(c).estimateSize());
        assertFalse(c.contains(bomb()));
        assertFalse(c.remove(bomb()));
        if (c instanceof Queue) {
            Queue<?> q = (Queue<?>) c;
            assertNull(q.peek());
            assertNull(q.poll());
        }
        if (c instanceof Deque) {
            Deque<?> d = (Deque<?>) c;
            assertNull(d.peekFirst());
            assertNull(d.peekLast());
            assertNull(d.pollFirst());
            assertNull(d.pollLast());
            assertIteratorExhausted(d.descendingIterator());
            Iterators.forEachRemaining(d.descendingIterator(), alwaysThrows);
            assertFalse(d.removeFirstOccurrence(bomb()));
            assertFalse(d.removeLastOccurrence(bomb()));
        }
        if (c instanceof BlockingQueue) {
            BlockingQueue<?> q = (BlockingQueue<?>) c;
            assertNull(q.poll(0L, MILLISECONDS));
        }
        if (c instanceof BlockingDeque) {
            BlockingDeque<?> q = (BlockingDeque<?>) c;
            assertNull(q.pollFirst(0L, MILLISECONDS));
            assertNull(q.pollLast(0L, MILLISECONDS));
        }
    }

    @Test(dataProvider = "Source")
    public void testNullPointerExceptions(String description, Supplier<CollectionImplementation> sci) throws InterruptedException {
        CollectionImplementation impl = sci.get();
        Collection<?> c = impl.emptyCollection();
        assertThrows(
            NullPointerException.class,
            () -> c.addAll(null),
            () -> c.containsAll(null),
            () -> c.retainAll(null),
            () -> c.removeAll(null),
            () -> Iterables.removeIf(c, null),
            () -> Iterables.forEach(c, null),
            () -> Iterators.forEachRemaining(c.iterator(), null),
            () -> Spliterators.spliterator(c).forEachRemaining(null),
            () -> Spliterators.spliterator(c).tryAdvance(null),
            () -> c.toArray(null));

        if (!impl.permitsNulls()) {
            assertThrows(
                NullPointerException.class,
                () -> c.add(null));
        }
        if (!impl.permitsNulls() && c instanceof Queue) {
            Queue<?> q = (Queue<?>) c;
            assertThrows(
                NullPointerException.class,
                () -> q.offer(null));
        }
        if (!impl.permitsNulls() && c instanceof Deque) {
            Deque<?> d = (Deque<?>) c;
            assertThrows(
                NullPointerException.class,
                () -> d.addFirst(null),
                () -> d.addLast(null),
                () -> d.offerFirst(null),
                () -> d.offerLast(null),
                () -> d.push(null),
                () -> Iterators.forEachRemaining(d.descendingIterator(), null));
        }
        if (c instanceof BlockingQueue) {
            BlockingQueue<?> q = (BlockingQueue<?>) c;
            assertThrows(
                NullPointerException.class,
                () -> {
                    try { q.offer(null, 1L, HOURS); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.put(null); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }});
        }
        if (c instanceof BlockingDeque) {
            BlockingDeque<?> q = (BlockingDeque<?>) c;
            assertThrows(
                NullPointerException.class,
                () -> {
                    try { q.offerFirst(null, 1L, HOURS); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.offerLast(null, 1L, HOURS); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.putFirst(null); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }},
                () -> {
                    try { q.putLast(null); }
                    catch (InterruptedException ex) {
                        throw new AssertionError(ex);
                    }});
        }
    }

    @Test(dataProvider = "Source")
    public void testNoSuchElementExceptions(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        Collection<?> c = impl.emptyCollection();
        assertThrows(
            NoSuchElementException.class,
            () -> c.iterator().next());

        if (c instanceof Queue) {
            Queue<?> q = (Queue<?>) c;
            assertThrows(
                NoSuchElementException.class,
                () -> q.element(),
                () -> q.remove());
        }
        if (c instanceof Deque) {
            Deque<?> d = (Deque<?>) c;
            assertThrows(
                NoSuchElementException.class,
                () -> d.getFirst(),
                () -> d.getLast(),
                () -> d.removeFirst(),
                () -> d.removeLast(),
                () -> d.pop(),
                () -> d.descendingIterator().next());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testRemoveIf(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        Collection c = impl.emptyCollection();
        boolean ordered =
            Spliterators.spliterator(c).hasCharacteristics(Spliterator.ORDERED);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        AtomicReference threwAt = new AtomicReference<>(null);
        List<?> orig = rnd.nextBoolean()
            ? new ArrayList<>(c)
            : Arrays.asList(c.toArray());

        // Merely creating an iterator can change ArrayBlockingQueue behavior
        Iterator<?> it = rnd.nextBoolean() ? c.iterator() : null;

        ArrayList survivors = new ArrayList<>();
        ArrayList accepts = new ArrayList<>();
        ArrayList rejects = new ArrayList<>();

        Predicate randomPredicate = e -> {
            assertNull(threwAt.get());
            switch (rnd.nextInt(3)) {
            case 0: accepts.add(e); return true;
            case 1: rejects.add(e); return false;
            case 2: threwAt.set(e); throw new ArithmeticException();
            default: throw new AssertionError();
            }
        };
        try {
            try {
                boolean modified = Iterables.removeIf(c, randomPredicate);
                assertNull(threwAt.get());
                assertEquals(modified, accepts.size() > 0);
                assertEquals(modified, rejects.size() != n);
                assertEquals(accepts.size() + rejects.size(), n);
                if (ordered) {
                    assertEquals(rejects,
                                 Arrays.asList(c.toArray()));
                } else {
                    assertEquals(new HashSet<>(rejects),
                                 new HashSet<>(Arrays.asList(c.toArray())));
                }
            } catch (ArithmeticException ok) {
                assertNotNull(threwAt.get());
                assertTrue(c.contains(threwAt.get()));
            }
            if (it != null && impl.isConcurrent())
                // check for weakly consistent iterator
                while (it.hasNext()) assertTrue(orig.contains(it.next()));
            switch (rnd.nextInt(4)) {
            case 0: survivors.addAll(c); break;
            case 1: survivors.addAll(Arrays.asList(c.toArray())); break;
            case 2: Iterables.forEach(c, survivors::add); break;
            case 3: for (Object e : c) survivors.add(e); break;
            }
            assertTrue(orig.containsAll(accepts));
            assertTrue(orig.containsAll(rejects));
            assertTrue(orig.containsAll(survivors));
            assertTrue(orig.containsAll(c));
            assertTrue(c.containsAll(rejects));
            assertTrue(c.containsAll(survivors));
            assertTrue(survivors.containsAll(rejects));
            if (threwAt.get() == null) {
                assertEquals(n - accepts.size(), c.size());
                for (Object x : accepts) assertFalse(c.contains(x));
            } else {
                // Two acceptable behaviors: entire removeIf call is one
                // transaction, or each element processed is one transaction.
                assertTrue(n == c.size() || n == c.size() + accepts.size());
                int k = 0;
                for (Object x : accepts) if (c.contains(x)) k++;
                assertTrue(k == accepts.size() || k == 0);
            }
        } catch (Throwable ex) {
            System.err.println(impl.klazz());
            // c is at risk of corruption if we got here, so be lenient
            try { System.err.printf("c=%s%n", c); }
            catch (Throwable t) { t.printStackTrace(); }
            System.err.printf("n=%d%n", n);
            System.err.printf("orig=%s%n", orig);
            System.err.printf("accepts=%s%n", accepts);
            System.err.printf("rejects=%s%n", rejects);
            System.err.printf("survivors=%s%n", survivors);
            System.err.printf("threwAt=%s%n", threwAt.get());
            throw ex;
        }
    }

    /**
     * All elements removed in the middle of CONCURRENT traversal.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(dataProvider = "Source")
    public void testElementRemovalDuringTraversal(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        if (hasSpliteratorNodeSelfLinkingBug
                && LinkedBlockingQueue.class.equals(
                        impl.klazz())) {
            // LinkedBlockingQueue spliterator needs to support node self-linking
            // https://bugs.openjdk.java.net/browse/JDK-8171051
            return;
        }
        Collection<Object> c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        ArrayList copy = new ArrayList();
        for (int i = 0; i < n; i++) {
            Object x = impl.makeElement(i);
            copy.add(x);
            c.add(x);
        }
        ArrayList iterated = new ArrayList();
        ArrayList spliterated = new ArrayList();
        Spliterator<?> s = Spliterators.spliterator(c);
        Iterator<?> it = c.iterator();
        for (int i = rnd.nextInt(n + 1); --i >= 0; ) {
            assertTrue(s.tryAdvance(spliterated::add));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            iterated.add(it.next());
        }
        Consumer alwaysThrows = e -> { throw new AssertionError(); };
        if (s.hasCharacteristics(Spliterator.CONCURRENT)) {
            c.clear();          // TODO: many more removal methods
            if (testImplementationDetails
                && !(c instanceof java.util.concurrent.ArrayBlockingQueue)) {
                if (rnd.nextBoolean())
                    assertFalse(s.tryAdvance(alwaysThrows));
                else
                    s.forEachRemaining(alwaysThrows);
            }
            if (it.hasNext()) iterated.add(it.next());
            if (rnd.nextBoolean()) assertIteratorExhausted(it);
        }
        assertTrue(copy.containsAll(iterated));
        assertTrue(copy.containsAll(spliterated));
    }

    /**
     * Some elements randomly disappear in the middle of traversal.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testRandomElementRemovalDuringTraversal(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        ArrayList copy = new ArrayList();
        for (int i = 0; i < n; i++) {
            Object x = impl.makeElement(i);
            copy.add(x);
            c.add(x);
        }
        ArrayList iterated = new ArrayList();
        ArrayList spliterated = new ArrayList();
        ArrayList removed = new ArrayList();
        Spliterator<?> s = Spliterators.spliterator(c);
        Iterator<?> it = c.iterator();
        if (! (s.hasCharacteristics(Spliterator.CONCURRENT) ||
               s.hasCharacteristics(Spliterator.IMMUTABLE)))
            return;
        for (int i = rnd.nextInt(n + 1); --i >= 0; ) {
            assertTrue(s.tryAdvance(e -> {}));
            if (rnd.nextBoolean()) assertTrue(it.hasNext());
            it.next();
        }
//        Consumer<?> alwaysThrows = e -> { throw new AssertionError(); };
        // TODO: many more removal methods
        if (rnd.nextBoolean()) {
            for (Iterator<?> z = c.iterator(); z.hasNext(); ) {
                Object e = z.next();
                if (rnd.nextBoolean()) {
                    try {
                        z.remove();
                    } catch (UnsupportedOperationException ok) { return; }
                    removed.add(e);
                }
            }
        } else {
            Predicate randomlyRemove = e -> {
                if (rnd.nextBoolean()) { removed.add(e); return true; }
                else return false;
            };
            Iterables.removeIf(c, randomlyRemove);
        }
        s.forEachRemaining(spliterated::add);
        while (it.hasNext())
            iterated.add(it.next());
        assertTrue(copy.containsAll(iterated));
        assertTrue(copy.containsAll(spliterated));
        assertTrue(copy.containsAll(removed));
        if (s.hasCharacteristics(Spliterator.CONCURRENT)) {
            ArrayList<?> iteratedAndRemoved = new ArrayList<>(iterated);
            ArrayList<?> spliteratedAndRemoved = new ArrayList<>(spliterated);
            iteratedAndRemoved.retainAll(removed);
            spliteratedAndRemoved.retainAll(removed);
            assertTrue(iteratedAndRemoved.size() <= 1);
            assertTrue(spliteratedAndRemoved.size() <= 1);
            if (testImplementationDetails
                && !(c instanceof java.util.concurrent.ArrayBlockingQueue))
                assertTrue(spliteratedAndRemoved.isEmpty());
        }
    }

    /**
     * Various ways of traversing a collection yield same elements
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testTraversalEquivalence(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = rnd.nextInt(6);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        ArrayList iterated = new ArrayList();
        ArrayList iteratedForEachRemaining = new ArrayList();
        ArrayList tryAdvanced = new ArrayList();
        ArrayList spliterated = new ArrayList();
        ArrayList splitonced = new ArrayList();
        ArrayList forEached = new ArrayList();
        ArrayList streamForEached = new ArrayList();
        ConcurrentLinkedQueue parallelStreamForEached = new ConcurrentLinkedQueue();
        ArrayList removeIfed = new ArrayList();
        for (Object x : c) iterated.add(x);
        Iterators.forEachRemaining(c.iterator(), iteratedForEachRemaining::add);
        for (Spliterator s = Spliterators.spliterator(c);
             s.tryAdvance(tryAdvanced::add); ) {}
        Spliterators.spliterator(c).forEachRemaining(spliterated::add);
        {                       // trySplit returns "strict prefix"
            Spliterator<?> s1 = Spliterators.spliterator(c), s2 = s1.trySplit();
            if (s2 != null) s2.forEachRemaining(splitonced::add);
            s1.forEachRemaining(splitonced::add);
        }
        Iterables.forEach(c, forEached::add);
        StreamSupport.stream(c).forEach(streamForEached::add);
        StreamSupport.parallelStream(c).forEach(parallelStreamForEached::add);
        Iterables.removeIf(c, e -> { removeIfed.add(e); return false; });
        boolean ordered =
            Spliterators.spliterator(c).hasCharacteristics(Spliterator.ORDERED);
        if (c instanceof List || c instanceof Deque)
            assertTrue(ordered);
        HashSet<?> cset = new HashSet<>(c);
        assertEquals(cset, new HashSet<>(parallelStreamForEached));
        if (ordered) {
            assertEquals(iterated, iteratedForEachRemaining);
            assertEquals(iterated, tryAdvanced);
            assertEquals(iterated, spliterated);
            assertEquals(iterated, splitonced);
            assertEquals(iterated, forEached);
            assertEquals(iterated, streamForEached);
            assertEquals(iterated, removeIfed);
        } else {
            assertEquals(cset, new HashSet<>(iterated));
            assertEquals(cset, new HashSet<>(iteratedForEachRemaining));
            assertEquals(cset, new HashSet<>(tryAdvanced));
            assertEquals(cset, new HashSet<>(spliterated));
            assertEquals(cset, new HashSet<>(splitonced));
            assertEquals(cset, new HashSet<>(forEached));
            assertEquals(cset, new HashSet<>(streamForEached));
            assertEquals(cset, new HashSet<>(removeIfed));
        }
        if (c instanceof Deque) {
            Deque<?> d = (Deque<?>) c;
            ArrayList descending = new ArrayList();
            ArrayList descendingForEachRemaining = new ArrayList();
            for (Iterator<?> it = d.descendingIterator(); it.hasNext(); )
                descending.add(it.next());
            Iterators.forEachRemaining(d.descendingIterator(),
                e -> descendingForEachRemaining.add(e));
            Collections.reverse(descending);
            Collections.reverse(descendingForEachRemaining);
            assertEquals(iterated, descending);
            assertEquals(iterated, descendingForEachRemaining);
        }
    }

    /**
     * Iterator.forEachRemaining has same behavior as Iterator's
     * default implementation.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testForEachRemainingConsistentWithDefaultImplementation(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        Collection c = impl.emptyCollection();
        if (!testImplementationDetails
            || c.getClass() == java.util.LinkedList.class)
            return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = 1 + rnd.nextInt(3);
        for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
        ArrayList iterated = new ArrayList();
        ArrayList iteratedForEachRemaining = new ArrayList();
        Iterator<?> it1 = c.iterator();
        Iterator<?> it2 = c.iterator();
        assertTrue(it1.hasNext());
        assertTrue(it2.hasNext());
        c.clear();
        Object r1, r2;
        try {
            while (it1.hasNext()) iterated.add(it1.next());
            r1 = iterated;
        } catch (ConcurrentModificationException ex) {
            r1 = ConcurrentModificationException.class;
            assertFalse(impl.isConcurrent());
        }
        try {
            Iterators.forEachRemaining(it2, iteratedForEachRemaining::add);
            r2 = iteratedForEachRemaining;
        } catch (ConcurrentModificationException ex) {
            r2 = ConcurrentModificationException.class;
            assertFalse(impl.isConcurrent());
        }
        assertEquals(r1, r2);
    }

    /**
     * Calling Iterator#remove() after Iterator#forEachRemaining
     * should (maybe) remove last element
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testRemoveAfterForEachRemaining(String description, Supplier<CollectionImplementation> sci) {
        CollectionImplementation impl = sci.get();
        Collection c = impl.emptyCollection();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        testCollection: {
            int n = 3 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) c.add(impl.makeElement(i));
            Iterator<?> it = c.iterator();
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(0), it.next());
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(1), it.next());
            Iterators.forEachRemaining(it, e -> assertTrue(c.contains(e)));
            if (testImplementationDetails) {
                if (c instanceof java.util.concurrent.ArrayBlockingQueue) {
                    assertIteratorExhausted(it);
                } else {
                    try { it.remove(); }
                    catch (UnsupportedOperationException ok) {
                        break testCollection;
                    }
                    assertEquals(n - 1, c.size());
                    for (int i = 0; i < n - 1; i++)
                        assertTrue(c.contains(impl.makeElement(i)));
                    assertFalse(c.contains(impl.makeElement(n - 1)));
                }
            }
        }
        if (c instanceof Deque) {
            Deque d = (Deque) impl.emptyCollection();
            int n = 3 + rnd.nextInt(2);
            for (int i = 0; i < n; i++) d.add(impl.makeElement(i));
            Iterator<?> it = d.descendingIterator();
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(n - 1), it.next());
            assertTrue(it.hasNext());
            assertEquals(impl.makeElement(n - 2), it.next());
            Iterators.forEachRemaining(it, e -> assertTrue(c.contains(e)));
            if (testImplementationDetails) {
                it.remove();
                assertEquals(n - 1, d.size());
                for (int i = 1; i < n; i++)
                    assertTrue(d.contains(impl.makeElement(i)));
                assertFalse(d.contains(impl.makeElement(0)));
            }
        }
    }

    /**
     * stream().forEach returns elements in the collection
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testStreamForEach(String description, Supplier<CollectionImplementation> sci) throws Throwable {
        CollectionImplementation impl = sci.get();
        final Collection c = impl.emptyCollection();
//        final AtomicLong count = new AtomicLong(0L);
        final Object x = impl.makeElement(1);
        final Object y = impl.makeElement(2);
        final ArrayList found = new ArrayList();
        Consumer<Object> spy = o -> found.add(o);
        StreamSupport.stream(c).forEach(spy);
        assertTrue(found.isEmpty());

        assertTrue(c.add(x));
        StreamSupport.stream(c).forEach(spy);
        assertEquals(Collections.singletonList(x), found);
        found.clear();

        assertTrue(c.add(y));
        StreamSupport.stream(c).forEach(spy);
        assertEquals(2, found.size());
        assertTrue(found.contains(x));
        assertTrue(found.contains(y));
        found.clear();

        c.clear();
        StreamSupport.stream(c).forEach(spy);
        assertTrue(found.isEmpty());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testStreamForEachConcurrentStressTest(String description, Supplier<CollectionImplementation> sci) throws Throwable {
        CollectionImplementation impl = sci.get();
        if (!impl.isConcurrent()) return;
        final Collection c = impl.emptyCollection();
        final long testDurationMillis = timeoutMillis();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Object elt = impl.makeElement(1);
        final Future<?> f1, f2;
        final ExecutorService pool = Executors.newCachedThreadPool();
        PoolCleaner cleaner = null;
        try {
            cleaner = cleaner(pool, done);
            final CountDownLatch threadsStarted = new CountDownLatch(2);
            Runnable checkElt = () -> {
                threadsStarted.countDown();
                while (!done.get())
                    StreamSupport.stream(c).forEach(x -> assertSame(x, elt)); };
            Runnable addRemove = () -> {
                threadsStarted.countDown();
                while (!done.get()) {
                    assertTrue(c.add(elt));
                    assertTrue(c.remove(elt));
                }};
            f1 = pool.submit(checkElt);
            f2 = pool.submit(addRemove);
            Thread.sleep(testDurationMillis);
        } finally {
            if (cleaner != null) {
                cleaner.close();
            }
        }
        assertNull(f1.get(0L, MILLISECONDS));
        assertNull(f2.get(0L, MILLISECONDS));
    }

    /**
     * collection.forEach returns elements in the collection
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testForEach(String description, Supplier<CollectionImplementation> sci) throws Throwable {
        CollectionImplementation impl = sci.get();
        final Collection c = impl.emptyCollection();
//        final AtomicLong count = new AtomicLong(0L);
        final Object x = impl.makeElement(1);
        final Object y = impl.makeElement(2);
        final ArrayList found = new ArrayList();
        Consumer<Object> spy = o -> found.add(o);
        Iterables.forEach(c, spy);
        assertTrue(found.isEmpty());

        assertTrue(c.add(x));
        Iterables.forEach(c, spy);
        assertEquals(Collections.singletonList(x), found);
        found.clear();

        assertTrue(c.add(y));
        Iterables.forEach(c, spy);
        assertEquals(2, found.size());
        assertTrue(found.contains(x));
        assertTrue(found.contains(y));
        found.clear();

        c.clear();
        Iterables.forEach(c, spy);
        assertTrue(found.isEmpty());
    }

    /** TODO: promote to a common utility */
    static <T> T chooseOne(T ... ts) {
        return ts[ThreadLocalRandom.current().nextInt(ts.length)];
    }

    /** TODO: more random adders and removers */
    static <E> Runnable adderRemover(Collection<E> c, E e) {
        return chooseOne(
            (Runnable) () -> {
                assertTrue(c.add(e));
                assertTrue(c.contains(e));
                assertTrue(c.remove(e));
                assertFalse(c.contains(e));
            },
            (Runnable) () -> {
                assertTrue(c.add(e));
                assertTrue(c.contains(e));
                assertTrue(Iterables.removeIf(c, x -> x == e));
                assertFalse(c.contains(e));
            },
            (Runnable) () -> {
                assertTrue(c.add(e));
                assertTrue(c.contains(e));
                for (Iterator<E> it = c.iterator();; )
                    if (it.next() == e) {
                        try { it.remove(); }
                        catch (UnsupportedOperationException ok) {
                            c.remove(e);
                        }
                        break;
                    }
                assertFalse(c.contains(e));
            });
    }

    /**
     * Concurrent Spliterators, once exhausted, stay exhausted.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testStickySpliteratorExhaustion(String description, Supplier<CollectionImplementation> sci) throws Throwable {
        CollectionImplementation impl = sci.get();
        if (!impl.isConcurrent()) return;
        if (!testImplementationDetails) return;
        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final Consumer<?> alwaysThrows = e -> { throw new AssertionError(); };
        final Collection c = impl.emptyCollection();
        final Spliterator s = Spliterators.spliterator(c);
        if (rnd.nextBoolean()) {
            assertFalse(s.tryAdvance(alwaysThrows));
        } else {
            s.forEachRemaining(alwaysThrows);
        }
        final Object one = impl.makeElement(1);
        // Spliterator should not notice added element
        c.add(one);
        if (rnd.nextBoolean()) {
            assertFalse(s.tryAdvance(alwaysThrows));
        } else {
            s.forEachRemaining(alwaysThrows);
        }
    }

    /**
     * Motley crew of threads concurrently randomly hammer the collection.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test(dataProvider = "Source")
    public void testDetectRaces(String description, Supplier<CollectionImplementation> sci) throws Throwable {
        CollectionImplementation impl = sci.get();
        if (!impl.isConcurrent()) return;
        if (hasSpliteratorNodeSelfLinkingBug
                && LinkedBlockingDeque.class.equals(
                        impl.klazz())) {
            // LinkedBlockingDeque spliterator needs to support node self-linking
            // https://bugs.openjdk.java.net/browse/JDK-8169739
            return;
        }

        final ThreadLocalRandom rnd = ThreadLocalRandom.current();
        final Collection c = impl.emptyCollection();
        final long testDurationMillis
            = expensiveTests ? LONG_DELAY_MS : timeoutMillis();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Object one = impl.makeElement(1);
        final Object two = impl.makeElement(2);
        final Consumer checkSanity = x -> assertTrue(x == one || x == two);
        final Consumer<Object[]> checkArraySanity = array -> {
            // assertTrue(array.length <= 2); // duplicates are permitted
            for (Object x : array) assertTrue(x == one || x == two);
        };
        final Object[] emptyArray =
            (Object[]) java.lang.reflect.Array.newInstance(one.getClass(), 0);
        final List<Future<?>> futures;
        final Phaser threadsStarted = new Phaser(1); // register this thread
        final Runnable[] frobbers = {
            () -> Iterables.forEach(c, checkSanity),
            () -> StreamSupport.stream(c).forEach(checkSanity),
            () -> StreamSupport.parallelStream(c).forEach(checkSanity),
            () -> Spliterators.spliterator(c).trySplit(),
            () -> {
                Spliterator<?> s = Spliterators.spliterator(c);
                s.tryAdvance(checkSanity);
                s.trySplit();
            },
            () -> {
                Spliterator<?> s = Spliterators.spliterator(c);
                do {} while (s.tryAdvance(checkSanity));
            },
            () -> { for (Object x : c) checkSanity.accept(x); },
            () -> checkArraySanity.accept(c.toArray()),
            () -> checkArraySanity.accept(c.toArray(emptyArray)),
            () -> {
                Object[] a = new Object[5];
                Object three = impl.makeElement(3);
                Arrays.fill(a, 0, a.length, three);
                Object[] x = c.toArray(a);
                if (x == a)
                    for (int i = 0; i < a.length && a[i] != null; i++)
                        checkSanity.accept(a[i]);
                    // A careful reading of the spec does not support:
                    // for (i++; i < a.length; i++) assertSame(three, a[i]);
                else
                    checkArraySanity.accept(x);
                },
            adderRemover(c, one),
            adderRemover(c, two),
        };
        final List<Runnable> tasks =
            J8Arrays.stream(frobbers)
            .filter(task -> rnd.nextBoolean()) // random subset
            .map(task -> (Runnable) () -> {
                     threadsStarted.arriveAndAwaitAdvance();
                     while (!done.get())
                         task.run();
                 })
            .collect(Collectors.<Runnable>toList());
        final ExecutorService pool = Executors.newCachedThreadPool();
        PoolCleaner cleaner = null;
        try {
            cleaner = cleaner(pool, done);
            threadsStarted.bulkRegister(tasks.size());
            futures = StreamSupport.stream(tasks)
                .map(pool::submit)
                .collect(Collectors.toList());
            threadsStarted.arriveAndDeregister();
            Thread.sleep(testDurationMillis);
        } finally {
            if (cleaner != null) {
                cleaner.close();
            }
        }
        for (Future<?> future : futures)
            assertNull(future.get(0L, MILLISECONDS));
    }

    /**
     * Spliterators are either IMMUTABLE or truly late-binding or, if
     * concurrent, use the same "late-binding style" of returning
     * elements added between creation and first use.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test(dataProvider = "Source")
    public void testLateBindingStyle(String description, Supplier<CollectionImplementation> sci) {
        if (!testImplementationDetails) return;
        CollectionImplementation impl = sci.get();
        if (impl.klazz() == ArrayList.class) return; // for jdk8
        // Immutable (snapshot) spliterators are exempt
        if (Spliterators.spliterator(impl.emptyCollection())
            .hasCharacteristics(Spliterator.IMMUTABLE))
            return;
        final Object one = impl.makeElement(1);
        {
            final Collection c = impl.emptyCollection();
            final Spliterator<?> split = Spliterators.spliterator(c);
            c.add(one);
            assertTrue(split.tryAdvance(e -> { assertSame(e, one); }));
            assertFalse(split.tryAdvance(e -> { throw new AssertionError(); }));
            assertTrue(c.contains(one));
        }
        {
            final AtomicLong count = new AtomicLong(0);
            final Collection c = impl.emptyCollection();
            final Spliterator<?> split = Spliterators.spliterator(c);
            c.add(one);
            split.forEachRemaining(
                e -> { assertSame(e, one); count.getAndIncrement(); });
            assertEquals(1L, count.get());
            assertFalse(split.tryAdvance(e -> { throw new AssertionError(); }));
            assertTrue(c.contains(one));
        }
    }

    /**
     * Spliterator.getComparator throws IllegalStateException iff the
     * spliterator does not report SORTED.
     */
    @Test(dataProvider = "Source")
    public void testGetComparator_IllegalStateException(String description, Supplier<CollectionImplementation> sci) {
        Collection<?> c = sci.get().emptyCollection();
        Spliterator<?> s = Spliterators.spliterator(c);
        boolean reportsSorted = s.hasCharacteristics(Spliterator.SORTED);
        try {
            s.getComparator();
            assertTrue(reportsSorted);
        } catch (IllegalStateException ex) {
            assertFalse(reportsSorted);
        }
    }

//     public void testCollection8DebugFail() {
//         fail(impl.klazz().getSimpleName());
//     }

    final static class PQ extends AbstractColllectionImpl {
        public Class<?> klazz() { return PriorityQueue.class; }
        public Collection<?> emptyCollection() { return new PriorityQueue<>(); }
        public boolean isConcurrent() { return false; }
    }

    final static class PBQ extends AbstractColllectionImpl {
        public Class<?> klazz() { return PriorityBlockingQueue.class; }
        public Collection<?> emptyCollection() { return new PriorityBlockingQueue<>(); }
        public boolean isConcurrent() { return true; }
    }

    final static class LBQ extends AbstractColllectionImpl {
        public Class<?> klazz() { return LinkedBlockingQueue.class; }
        public Collection<?> emptyCollection() { return new LinkedBlockingQueue<>();    }
        public boolean isConcurrent() { return true; }
    }

    final static class LBD extends AbstractColllectionImpl {
        public Class<?> klazz() { return LinkedBlockingDeque.class; }
        public Collection<?> emptyCollection() { return new LinkedBlockingDeque<>(); }
        public boolean isConcurrent() { return true; }
    }

    abstract static class AbstractColllectionImpl implements CollectionImplementation {
        @Override
        public Object makeElement(int i) { return i; }
        @Override
        public boolean permitsNulls() { return false; }
    }

    static Object[][] collectionDataProvider;

    @DataProvider(name = "Source")
    static Object[][] collectionDataProvider() {
        if (collectionDataProvider != null) {
            return collectionDataProvider;
        }

        List<Object[]> data = new ArrayList<>();
        CollectionDataBuilder db = new CollectionDataBuilder(data);

        // j.u.PriorityQueue
        db.add(PQ::new);
        // j.u.c.PriorityBlockingQueue
        db.add(PBQ::new);
        // j.u.c.LinkedBlockingQueue
        db.add(LBQ::new);
        // j.u.c.LinkedBlockingDeque
        db.add(LBD::new);

        // TODO: add more collections

        return collectionDataProvider = data.toArray(new Object[0][]);
    }

    static final class CollectionDataBuilder {
        final List<Object[]> data;

        CollectionDataBuilder(List<Object[]> data) {
            this.data = data;
        }

        void add(Supplier<CollectionImplementation> s) {
            String description = s.get().klazz().getName();
            data.add(new Object[] { description, s });
        }
    }

    private static final String DISPLAY_METRICS = "android.util.DisplayMetrics";
    private static final String GLES32 = "android.opengl.GLES32$DebugProc";
    private static final boolean IS_OPENJDK_ANDROID = isOpenJDKAndroid();
    private static final boolean IS_SPLITERATOR_DELEGATION_ENABLED = isSpliteratorDelegationEnabled();
    private static final boolean hasSpliteratorNodeSelfLinkingBug = hasSpliteratorNodeSelfLinkingBug(); 

    /**
     * The Java 8 Spliterators for LinkedBlockingQueue and LinkedBlockingDeque
     * both have a bug that can lead to infinite loops in some circumstances.
     * See
     * 
     * https://bugs.openjdk.java.net/browse/JDK-8169739
     * https://bugs.openjdk.java.net/browse/JDK-8171051
     * 
     * We'd get test failures because of this when we run on Java 8 or Android
     * 7+ with Spliterator delegation enabled. This bug has been fixed in Java 9
     * ea build 151, so we require this as the minimum version for test runs on
     * Java 9 (otherwise we'd also get failures on Java 9 since we do not test
     * for class.version 53.0 here).
     * 
     * @return {@code true} on Java 8 or Android 7+ when Spliterator delegation
     *         hasn't been disabled, {@code false} otherwise.
     */
    private static boolean hasSpliteratorNodeSelfLinkingBug() {
        // a) must have exactly major version number 52 (Java 8)
        String ver = System.getProperty("java.class.version", "45");
        if (ver != null && ver.length() >= 2) {
            ver = ver.substring(0, 2);
            if ("52".equals(ver)) {
                // b) Spliterator delegation must not be disabled
                return IS_SPLITERATOR_DELEGATION_ENABLED;
            }
        }
        return IS_OPENJDK_ANDROID
                && IS_SPLITERATOR_DELEGATION_ENABLED;
    }

    private static boolean isSpliteratorDelegationEnabled() {
        String s = System.getProperty(Spliterators.class.getName()
                + ".jre.delegation.enabled", Boolean.TRUE.toString());
        return (s == null)
                || s.trim().equalsIgnoreCase(Boolean.TRUE.toString());
    }

    /**
     * Are we running on Android 7+ ?
     * 
     * @return {@code true} if yes, otherwise {@code false}.
     */
    private static boolean isOpenJDKAndroid() {
        return isClassPresent(DISPLAY_METRICS) && isClassPresent(GLES32);
    }

    private static boolean isClassPresent(String name) {
        Class<?> clazz = null;
        try {
            // avoid <clinit> which triggers a lot of JNI code in the case
            // of android.util.DisplayMetrics
            clazz = Class.forName(name, false,
                    Collection8Test.class.getClassLoader());
        } catch (Throwable notPresent) {
            // ignore
        }
        return clazz != null;
    }
}