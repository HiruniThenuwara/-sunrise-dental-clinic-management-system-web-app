package com.sunrise.api;

import com.sunrise.dao.DaoFactory;
import com.sunrise.model.TimeSlot;
import com.sunrise.service.SlotService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Web service returning the bookable times for a dentist on a date.
 *
 * <p>{@code GET /api/slots?doctorId=1&date=2026-09-14}</p>
 *
 * <p>The appointment form calls this whenever the receptionist changes the
 * dentist or the date, so the times refresh without reloading the page. The
 * booked times are included and marked {@code "available": false} rather than
 * being left out, so the receptionist can see that a time exists and is
 * simply taken.</p>
 *
 * <pre>
 * 200 OK
 * { "doctorId": 1, "date": "2026-09-14", "total": 16, "free": 14,
 *   "slots": [ { "time": "09:00", "available": false }, ... ] }
 * </pre>
 */
@WebServlet(name = "SlotApi", urlPatterns = {"/api/slots"})
public class SlotApi extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient SlotService slotService;

    @Override
    public void init() {
        this.slotService = new SlotService(
                DaoFactory.getDoctorScheduleDao(), DaoFactory.getAppointmentDao());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        int doctorId = parseId(request.getParameter("doctorId"));
        if (doctorId <= 0) {
            JsonWriter.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "A doctorId is required, for example /api/slots?doctorId=1&date=2026-09-14");
            return;
        }

        LocalDate date;
        try {
            String value = request.getParameter("date");
            date = (value == null || value.isBlank()) ? LocalDate.now() : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            JsonWriter.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "The date must be written as yyyy-MM-dd.");
            return;
        }

        List<TimeSlot> slots = slotService.generateSlots(doctorId, date);
        long free = slots.stream().filter(TimeSlot::isAvailable).count();

        JsonWriter json = new JsonWriter();
        json.beginObject()
                .name("doctorId").value(doctorId)
                .name("date").value(date.toString())
                .name("dayOfWeek").value(date.getDayOfWeek().name())
                .name("total").value(slots.size())
                .name("free").value(free)
                .name("slots").beginArray();

        for (TimeSlot slot : slots) {
            json.beginObject()
                    .name("time").value(slot.getValue())
                    .name("label").value(slot.getLabel())
                    .name("available").value(slot.isAvailable())
                    .name("past").value(slot.isPast())
                    .name("reason").value(slot.getUnavailableReason())
                .endObject();
        }

        json.endArray().endObject();

        JsonWriter.send(response, HttpServletResponse.SC_OK, json.toJson());
    }

    private int parseId(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
