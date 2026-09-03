/*
 * Pagination for every list table in the clinic system.
 *
 * One script rather than a pager written into each page: it finds every
 * table.table when the page loads, and if the table holds more rows than
 * fit comfortably it hides all but the current page and draws the controls
 * underneath. Adding a table to a new screen gets pagination for nothing.
 *
 * A table can ask for a different page size with data-page-size, or opt out
 * altogether with data-no-pager. Printing always shows every row, because a
 * printed page of ten rows out of forty would be misleading.
 */
(function () {
    'use strict';

    var DEFAULT_PAGE_SIZE = 10;

    function realRows(table) {
        if (!table.tBodies.length) {
            return [];
        }
        var rows = [];
        var all = table.tBodies[0].rows;

        for (var i = 0; i < all.length; i++) {
            // The "no records yet" row is a message, not data.
            if (!all[i].querySelector('.empty-state')) {
                rows.push(all[i]);
            }
        }
        return rows;
    }

    function button(label, className, ariaLabel) {
        var b = document.createElement('button');
        b.type = 'button';
        b.className = className;
        b.textContent = label;
        if (ariaLabel) {
            b.setAttribute('aria-label', ariaLabel);
        }
        return b;
    }

    /* The page numbers worth drawing: the ends, and a window around the
       current page, with a gap marker where numbers were left out. */
    function pageNumbers(current, total) {
        if (total <= 7) {
            var all = [];
            for (var i = 1; i <= total; i++) {
                all.push(i);
            }
            return all;
        }

        var wanted = [1, total, current, current - 1, current + 1];
        if (current <= 3) {
            wanted.push(2, 3, 4);
        }
        if (current >= total - 2) {
            wanted.push(total - 1, total - 2, total - 3);
        }

        var kept = wanted
            .filter(function (n) { return n >= 1 && n <= total; })
            .sort(function (a, b) { return a - b; })
            .filter(function (n, i, list) { return list.indexOf(n) === i; });

        var out = [];
        kept.forEach(function (n, i) {
            if (i > 0 && n - kept[i - 1] > 1) {
                out.push(null);          // a gap
            }
            out.push(n);
        });
        return out;
    }

    function paginate(table) {
        if (table.hasAttribute('data-no-pager')) {
            return;
        }

        var rows = realRows(table);
        var size = parseInt(table.getAttribute('data-page-size'), 10) || DEFAULT_PAGE_SIZE;

        if (rows.length <= size) {
            return;                      // it all fits, so no controls
        }

        var pages = Math.ceil(rows.length / size);
        var current = 1;

        var nav = document.createElement('nav');
        nav.className = 'pager';
        nav.setAttribute('aria-label', 'Table pages');

        var count = document.createElement('p');
        count.className = 'pager__count';

        var controls = document.createElement('div');
        controls.className = 'pager__controls';

        nav.appendChild(count);
        nav.appendChild(controls);

        // Sit the controls under the scrolling wrapper, not inside it.
        var wrap = table.closest('.table-wrap') || table;
        wrap.parentNode.insertBefore(nav, wrap.nextSibling);

        function show(page) {
            current = Math.min(Math.max(page, 1), pages);

            var first = (current - 1) * size;
            var last = Math.min(first + size, rows.length);

            rows.forEach(function (row, i) {
                row.hidden = i < first || i >= last;
            });

            count.textContent = 'Showing ' + (first + 1) + ' to ' + last
                                + ' of ' + rows.length;
            draw();
        }

        function draw() {
            controls.innerHTML = '';

            var prev = button('Previous', 'pager__btn', 'Previous page');
            prev.disabled = current === 1;
            prev.addEventListener('click', function () { show(current - 1); });
            controls.appendChild(prev);

            pageNumbers(current, pages).forEach(function (n) {
                if (n === null) {
                    var gap = document.createElement('span');
                    gap.className = 'pager__gap';
                    gap.textContent = '...';
                    controls.appendChild(gap);
                    return;
                }
                var b = button(String(n), 'pager__page', 'Page ' + n);
                if (n === current) {
                    b.classList.add('is-current');
                    b.setAttribute('aria-current', 'page');
                }
                b.addEventListener('click', function () { show(n); });
                controls.appendChild(b);
            });

            var next = button('Next', 'pager__btn', 'Next page');
            next.disabled = current === pages;
            next.addEventListener('click', function () { show(current + 1); });
            controls.appendChild(next);
        }

        show(1);
    }

    function start() {
        var tables = document.querySelectorAll('table.table');
        for (var i = 0; i < tables.length; i++) {
            paginate(tables[i]);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }

    /* Every row must be on the paper, whatever page is on screen. */
    window.addEventListener('beforeprint', function () {
        document.querySelectorAll('table.table tbody tr[hidden]')
            .forEach(function (row) { row.setAttribute('data-paged-out', ''); row.hidden = false; });
    });

    window.addEventListener('afterprint', function () {
        document.querySelectorAll('table.table tbody tr[data-paged-out]')
            .forEach(function (row) { row.removeAttribute('data-paged-out'); row.hidden = true; });
    });
}());
