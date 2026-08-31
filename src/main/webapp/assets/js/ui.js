/* =====================================================================
   Sunrise Dental Clinic - shared interface helpers
   Plain JavaScript, no library.
   ===================================================================== */

/** Opens a modal dialog by id. */
function openModal(id) {
    var modal = document.getElementById(id);
    if (!modal) {
        return;
    }
    modal.classList.add('is-open');
    modal.setAttribute('aria-hidden', 'false');
    document.body.classList.add('modal-open');
}

/** Closes a modal dialog by id. */
function closeModal(id) {
    var modal = document.getElementById(id);
    if (!modal) {
        return;
    }
    modal.classList.remove('is-open');
    modal.setAttribute('aria-hidden', 'true');
    document.body.classList.remove('modal-open');
}

/* Escape closes whichever modal is open. */
document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
        document.querySelectorAll('.modal.is-open').forEach(function (modal) {
            closeModal(modal.id);
        });
    }
});

/**
 * Shows a short message in the corner of the screen.
 *
 * @param message text to show
 * @param type    "success", "error" or "info"
 */
function showToast(message, type) {
    var host = document.getElementById('toastHost');
    if (!host) {
        host = document.createElement('div');
        host.id = 'toastHost';
        host.className = 'toast-host';
        document.body.appendChild(host);
    }

    var toast = document.createElement('div');
    toast.className = 'toast toast--' + (type || 'info');
    toast.textContent = message;
    host.appendChild(toast);

    setTimeout(function () {
        toast.classList.add('is-leaving');
        setTimeout(function () {
            toast.remove();
        }, 250);
    }, 3200);
}

/** Highlights the time slot the receptionist clicked. */
function selectSlot(button) {
    if (button.classList.contains('slot-chip--booked')) {
        showToast('That time is already booked. Please choose another slot.', 'error');
        return;
    }
    document.querySelectorAll('.slot-chip.is-selected').forEach(function (chip) {
        chip.classList.remove('is-selected');
    });
    button.classList.add('is-selected');

    var field = document.getElementById('appointmentTime');
    if (field) {
        field.value = button.dataset.time || button.textContent.trim();
    }
}
