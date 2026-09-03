package com.sunrise.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One page of records, together with everything the screen needs to draw
 * the page links.
 *
 * <p>The list pages ask the database for ten rows at a time rather than
 * reading a whole table into memory and hiding most of it. The clinic's
 * activity log already holds more than a hundred rows after a few days;
 * a year of it would be tens of thousands, and sending all of them to a
 * browser to show ten would waste the database, the network and the
 * browser at once.</p>
 *
 * <p>The arithmetic lives here rather than in each servlet, so that "what
 * is the last page when 119 rows are shown ten at a time" is answered in
 * one place and can be unit tested.</p>
 *
 * @param <T> what is being listed
 */
public class Page<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** How many rows a list screen shows at once. */
    public static final int DEFAULT_SIZE = 10;

    /** A gap in the page numbers, drawn as an ellipsis. */
    public static final int GAP = -1;

    private final List<T> items;
    private final int pageNumber;
    private final int pageSize;
    private final int totalItems;

    public Page(List<T> items, int pageNumber, int pageSize, int totalItems) {
        this.items = items == null ? List.of() : items;
        this.pageSize = pageSize < 1 ? DEFAULT_SIZE : pageSize;
        this.totalItems = Math.max(totalItems, 0);
        this.pageNumber = clamp(pageNumber);
    }

    /**
     * Works out which page was asked for, given whatever arrived in the
     * query string. Anything unreadable, missing or out of range becomes a
     * page that exists, because a typed web address is not to be trusted
     * and an empty screen is not a helpful answer.
     *
     * @param value the raw {@code page} parameter
     * @return a page number of at least 1
     */
    public static int requestedPage(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            return Math.max(Integer.parseInt(value.trim()), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * @param pageNumber the page being asked for
     * @param pageSize   rows per page
     * @return how many rows to skip in the SQL query
     */
    public static int offsetFor(int pageNumber, int pageSize) {
        return (Math.max(pageNumber, 1) - 1) * Math.max(pageSize, 1);
    }

    /** How a screen fetches the rows once the offset is known. */
    @FunctionalInterface
    public interface Loader<T> {
        List<T> load(int offset, int limit);
    }

    /**
     * Builds a page from the raw request parameter.
     *
     * <p>The total is counted first so that the page number can be brought
     * inside the range that exists before any rows are fetched. Asking for
     * page 99 of a four page list should show the last page, not an empty
     * screen, and the offset must match whichever page is finally shown.</p>
     *
     * @param requestedPage the {@code page} parameter, exactly as it arrived
     * @param totalItems    how many rows match, counted in the database
     * @param loader        fetches one page of rows
     */
    public static <T> Page<T> of(String requestedPage, int totalItems, Loader<T> loader) {
        return of(requestedPage, totalItems, DEFAULT_SIZE, loader);
    }

    /** As {@link #of(String, int, Loader)}, with a page size of your own. */
    public static <T> Page<T> of(String requestedPage, int totalItems,
                                 int pageSize, Loader<T> loader) {

        int size = pageSize < 1 ? DEFAULT_SIZE : pageSize;
        int total = Math.max(totalItems, 0);
        int lastPage = total <= 0 ? 1 : (total + size - 1) / size;
        int current = Math.min(requestedPage(requestedPage), lastPage);

        return new Page<>(loader.load(offsetFor(current, size), size), current, size, total);
    }

    /** Keeps the page inside the range that actually has rows. */
    private int clamp(int requested) {
        int last = getTotalPages();
        if (requested < 1) {
            return 1;
        }
        return Math.min(requested, last);
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    /** @return at least 1, so an empty list still reads as "page 1 of 1" */
    public int getTotalPages() {
        if (totalItems <= 0) {
            return 1;
        }
        return (totalItems + pageSize - 1) / pageSize;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** @return {@code true} when the page links are worth drawing at all */
    public boolean isPaged() {
        return getTotalPages() > 1;
    }

    public boolean isHasPrevious() {
        return pageNumber > 1;
    }

    public boolean isHasNext() {
        return pageNumber < getTotalPages();
    }

    public int getPreviousPage() {
        return Math.max(pageNumber - 1, 1);
    }

    public int getNextPage() {
        return Math.min(pageNumber + 1, getTotalPages());
    }

    /** @return the number of the first row on this page, counting from 1 */
    public int getFirstItem() {
        return totalItems == 0 ? 0 : offsetFor(pageNumber, pageSize) + 1;
    }

    /** @return the number of the last row on this page */
    public int getLastItem() {
        return Math.min(offsetFor(pageNumber, pageSize) + items.size(), totalItems);
    }

    /**
     * The page numbers worth drawing: the two ends and a window around the
     * current page, with {@link #GAP} standing where numbers were left out.
     *
     * <p>Twelve pages fit across the screen. Two hundred do not, and a row
     * of two hundred numbers is no easier to use than none at all.</p>
     */
    public List<Integer> getNumbers() {

        int last = getTotalPages();
        List<Integer> numbers = new ArrayList<>();

        if (last <= 7) {
            for (int i = 1; i <= last; i++) {
                numbers.add(i);
            }
            return numbers;
        }

        List<Integer> wanted = new ArrayList<>();
        wanted.add(1);
        wanted.add(last);
        for (int i = pageNumber - 1; i <= pageNumber + 1; i++) {
            wanted.add(i);
        }
        if (pageNumber <= 3) {
            wanted.add(2);
            wanted.add(3);
            wanted.add(4);
        }
        if (pageNumber >= last - 2) {
            wanted.add(last - 1);
            wanted.add(last - 2);
            wanted.add(last - 3);
        }

        List<Integer> kept = wanted.stream()
                .filter(n -> n >= 1 && n <= last)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        for (int i = 0; i < kept.size(); i++) {
            if (i > 0 && kept.get(i) - kept.get(i - 1) > 1) {
                numbers.add(GAP);
            }
            numbers.add(kept.get(i));
        }
        return numbers;
    }

    @Override
    public String toString() {
        return "Page " + pageNumber + " of " + getTotalPages()
                + " (" + items.size() + " of " + totalItems + " rows)";
    }
}
