package com.sunrise.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Page}.
 *
 * <p>Paging looks like arithmetic that cannot go wrong, which is exactly
 * why it does. The awkward cases are the ones a quick look at the screen
 * never reaches: the last page when the rows do not divide evenly, a page
 * number typed into the address bar, an empty table, and a filter that has
 * left fewer rows than the page the staff member was already on.</p>
 */
@DisplayName("Page - splitting a list into pages")
class PageTest {

    /** Stands in for the DAO: hands back the slice that was asked for. */
    private static Page.Loader<Integer> rowsOf(int total) {
        return (offset, limit) -> {
            List<Integer> slice = new ArrayList<>();
            for (int i = offset; i < Math.min(offset + limit, total); i++) {
                slice.add(i + 1);
            }
            return slice;
        };
    }

    @Nested
    @DisplayName("Reading the page number from the address")
    class RequestedPage {

        @Test
        @DisplayName("TC-01 a missing or empty parameter means page one")
        void defaultsToPageOne() {
            assertAll(
                    () -> assertEquals(1, Page.requestedPage(null)),
                    () -> assertEquals(1, Page.requestedPage("")),
                    () -> assertEquals(1, Page.requestedPage("   "))
            );
        }

        @Test
        @DisplayName("TC-02 rubbish in the address does not break the screen")
        void ignoresRubbish() {
            assertAll(
                    () -> assertEquals(1, Page.requestedPage("abc")),
                    () -> assertEquals(1, Page.requestedPage("2; DROP TABLE users")),
                    () -> assertEquals(1, Page.requestedPage("-4")),
                    () -> assertEquals(1, Page.requestedPage("0")),
                    () -> assertEquals(7, Page.requestedPage(" 7 "))
            );
        }
    }

    @Nested
    @DisplayName("The offset handed to the SQL")
    class Offsets {

        @Test
        @DisplayName("TC-03 page one skips nothing, page three skips twenty")
        void countsFromZero() {
            assertAll(
                    () -> assertEquals(0, Page.offsetFor(1, 10)),
                    () -> assertEquals(10, Page.offsetFor(2, 10)),
                    () -> assertEquals(20, Page.offsetFor(3, 10))
            );
        }
    }

    @Nested
    @DisplayName("Counting the pages")
    class Counting {

        @Test
        @DisplayName("TC-04 119 rows, ten at a time, is twelve pages")
        void roundsThePartialPageUp() {
            Page<Integer> page = Page.of("1", 119, rowsOf(119));

            assertAll(
                    () -> assertEquals(12, page.getTotalPages()),
                    () -> assertEquals(10, page.getItems().size()),
                    () -> assertTrue(page.isPaged())
            );
        }

        @Test
        @DisplayName("TC-05 rows that divide evenly do not add an empty last page")
        void doesNotAddAnEmptyPage() {
            assertEquals(2, Page.of("1", 20, rowsOf(20)).getTotalPages());
        }

        @Test
        @DisplayName("TC-06 a table that fits needs no page links")
        void oneShortPage() {
            Page<Integer> page = Page.of("1", 6, rowsOf(6));

            assertAll(
                    () -> assertEquals(1, page.getTotalPages()),
                    () -> assertFalse(page.isPaged(), "no links for a single page"),
                    () -> assertFalse(page.isHasPrevious()),
                    () -> assertFalse(page.isHasNext())
            );
        }

        @Test
        @DisplayName("TC-07 an empty table still reads as page one of one")
        void emptyTable() {
            Page<Integer> page = Page.of("1", 0, rowsOf(0));

            assertAll(
                    () -> assertEquals(1, page.getPageNumber()),
                    () -> assertEquals(1, page.getTotalPages()),
                    () -> assertTrue(page.isEmpty()),
                    () -> assertEquals(0, page.getFirstItem()),
                    () -> assertEquals(0, page.getLastItem())
            );
        }
    }

    @Nested
    @DisplayName("The 'showing x to y of z' line")
    class Showing {

        @Test
        @DisplayName("TC-08 the middle page counts from where it starts")
        void middlePage() {
            Page<Integer> page = Page.of("3", 119, rowsOf(119));

            assertAll(
                    () -> assertEquals(21, page.getFirstItem()),
                    () -> assertEquals(30, page.getLastItem()),
                    () -> assertEquals(21, page.getItems().get(0))
            );
        }

        @Test
        @DisplayName("TC-09 the last page stops at the last row, not at a round ten")
        void lastPageIsShort() {
            Page<Integer> page = Page.of("12", 119, rowsOf(119));

            assertAll(
                    () -> assertEquals(111, page.getFirstItem()),
                    () -> assertEquals(119, page.getLastItem()),
                    () -> assertEquals(9, page.getItems().size()),
                    () -> assertFalse(page.isHasNext())
            );
        }
    }

    @Nested
    @DisplayName("Page numbers that do not exist")
    class OutOfRange {

        @Test
        @DisplayName("TC-10 asking for page 99 of twelve shows the last page")
        void clampsToTheLastPage() {
            Page<Integer> page = Page.of("99", 119, rowsOf(119));

            assertAll(
                    () -> assertEquals(12, page.getPageNumber()),
                    () -> assertEquals(9, page.getItems().size(),
                            "the rows must match the page that is finally shown"),
                    () -> assertEquals(111, page.getFirstItem())
            );
        }

        @Test
        @DisplayName("TC-11 a filter that shrinks the list moves the reader back in range")
        void filterShrinksTheList() {
            // Page 8 of the whole log, then a filter leaves only 12 rows.
            Page<Integer> page = Page.of("8", 12, rowsOf(12));

            assertAll(
                    () -> assertEquals(2, page.getPageNumber()),
                    () -> assertEquals(2, page.getItems().size()),
                    () -> assertEquals(11, page.getFirstItem())
            );
        }
    }

    @Nested
    @DisplayName("Which page numbers to draw")
    class Numbers {

        @Test
        @DisplayName("TC-12 seven pages or fewer are all shown")
        void allOfThem() {
            assertEquals(List.of(1, 2, 3, 4, 5, 6, 7),
                         Page.of("1", 70, rowsOf(70)).getNumbers());
        }

        @Test
        @DisplayName("TC-13 a long list keeps the ends and a window, with a gap between")
        void windowed() {
            List<Integer> numbers = Page.of("6", 200, rowsOf(200)).getNumbers();

            assertAll(
                    () -> assertEquals(1, numbers.get(0), "the first page is always offered"),
                    () -> assertEquals(20, numbers.get(numbers.size() - 1),
                            "and so is the last"),
                    () -> assertTrue(numbers.contains(Page.GAP), "with a gap standing for the rest"),
                    () -> assertTrue(numbers.containsAll(List.of(5, 6, 7)),
                            "the current page keeps its neighbours"),
                    () -> assertTrue(numbers.size() <= 9, "and the row stays short")
            );
        }

        @Test
        @DisplayName("TC-14 near the start there is no gap before the first numbers")
        void noGapAtTheStart() {
            List<Integer> numbers = Page.of("2", 200, rowsOf(200)).getNumbers();

            assertAll(
                    () -> assertEquals(List.of(1, 2, 3, 4), numbers.subList(0, 4)),
                    () -> assertEquals(Page.GAP, numbers.get(4))
            );
        }
    }
}
