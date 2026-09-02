package com.sunrise.api;

import com.sunrise.model.Appointment;
import com.sunrise.model.User;
import com.sunrise.service.AppointmentService;
import com.sunrise.service.AuthService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Web service for appointments.
 *
 * <ul>
 *   <li>{@code GET  /api/appointments?no=APT-20260914-002} - one visit</li>
 *   <li>{@code GET  /api/appointments?date=2026-09-14} - a day's list</li>
 *   <li>{@code GET  /api/appointments} - the most recent visits</li>
 *   <li>{@code POST /api/appointments} - register a visit</li>
 * </ul>
 *
 * <p>The POST endpoint calls exactly the same {@link AppointmentService} as
 * the web form, so the double booking rule and the validation apply equally
 * to a request that arrives over the network. The reply status carries the
 * outcome: <b>201</b> created, <b>400</b> invalid input, <b>409</b> the slot
 * is already taken.</p>
 *
 * <p>Returning 409 rather than a generic error is what lets another system,
 * such as a future patient booking app, tell "you typed something wrong"
 * apart from "somebody else just took that time".</p>
 */
@WebServlet(name = "AppointmentApi", urlPatterns = {"/api/appointments"})
public class AppointmentApi extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient AppointmentService appointmentService;

    @Override
    public void init() {
        this.appointmentService = new AppointmentService();
    }

    // -----------------------------------------------------------------
    //  reading
    // -----------------------------------------------------------------
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String appointmentNo = request.getParameter("no");
        String dateText = request.getParameter("date");

        if (appointmentNo != null && !appointmentNo.isBlank()) {
            Optional<Appointment> found = appointmentService.findByNumber(appointmentNo);

            if (found.isEmpty()) {
                JsonWriter.sendError(response, HttpServletResponse.SC_NOT_FOUND,
                        "No appointment found with the number " + appointmentNo.trim() + ".");
                return;
            }
            JsonWriter json = new JsonWriter();
            writeAppointment(json, found.get());
            JsonWriter.send(response, HttpServletResponse.SC_OK, json.toJson());
            return;
        }

        List<Appointment> appointments;
        if (dateText != null && !dateText.isBlank()) {
            try {
                appointments = appointmentService.findByDate(LocalDate.parse(dateText.trim()));
            } catch (DateTimeParseException e) {
                JsonWriter.sendError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "The date must be written as yyyy-MM-dd.");
                return;
            }
        } else {
            appointments = appointmentService.findRecent(50);
        }

        JsonWriter json = new JsonWriter();
        json.beginObject()
                .name("count").value(appointments.size())
                .name("appointments").beginArray();

        for (Appointment appointment : appointments) {
            writeAppointment(json, appointment);
        }

        json.endArray().endObject();
        JsonWriter.send(response, HttpServletResponse.SC_OK, json.toJson());
    }

    // -----------------------------------------------------------------
    //  registering
    // -----------------------------------------------------------------
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");

        LocalDate appointmentDate;
        try {
            String value = request.getParameter("appointmentDate");
            appointmentDate = (value == null || value.isBlank())
                    ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            appointmentDate = null;
        }

        AppointmentService.RegistrationResult result = appointmentService.register(
                request.getParameter("patientName"),
                request.getParameter("address"),
                request.getParameter("contactNumber"),
                request.getParameter("email"),
                request.getParameter("nic"),
                request.getParameter("doctorId"),
                request.getParameter("treatmentId"),
                appointmentDate,
                request.getParameter("appointmentTime"),
                request.getParameter("notes"),
                currentUser(request));

        if (result.isSuccess()) {
            JsonWriter json = new JsonWriter();
            writeAppointment(json, result.getAppointment());
            JsonWriter.send(response, HttpServletResponse.SC_CREATED, json.toJson());
            return;
        }

        // A clash with an existing booking is a conflict, not bad input.
        boolean slotTaken = result.getErrors().stream()
                .anyMatch(error -> error.toLowerCase().contains("already booked"));

        int status = slotTaken
                ? HttpServletResponse.SC_CONFLICT
                : HttpServletResponse.SC_BAD_REQUEST;

        JsonWriter json = new JsonWriter();
        json.beginObject()
                .name("error").value(slotTaken
                        ? "That time is already booked for this dentist."
                        : "The appointment could not be registered.")
                .name("details").beginArray();

        for (String error : result.getErrors()) {
            json.value(error);
        }

        json.endArray().endObject();
        JsonWriter.send(response, status, json.toJson());
    }

    // -----------------------------------------------------------------
    //  shared shape, so one visit always looks the same in every reply
    // -----------------------------------------------------------------
    private void writeAppointment(JsonWriter json, Appointment appointment) {
        json.beginObject()
                .name("appointmentNo").value(appointment.getAppointmentNo())
                .name("date").value(String.valueOf(appointment.getAppointmentDate()))
                .name("time").value(String.valueOf(appointment.getAppointmentTime()))
                .name("status").value(appointment.getStatus().name())
                .name("notes").value(appointment.getNotes())
                .name("patient").beginObject()
                    .name("name").value(appointment.getPatient() == null
                            ? null : appointment.getPatient().getPatientName())
                    .name("contactNumber").value(appointment.getPatient() == null
                            ? null : appointment.getPatient().getContactNumber())
                .endObject()
                .name("doctor").beginObject()
                    .name("id").value(appointment.getDoctor() == null
                            ? 0 : appointment.getDoctor().getDoctorId())
                    .name("name").value(appointment.getDoctor() == null
                            ? null : appointment.getDoctor().getDoctorName())
                    .name("consultationFee").value(appointment.getDoctor() == null
                            ? null : appointment.getDoctor().getConsultationFee())
                .endObject()
                .name("treatment").beginObject()
                    .name("id").value(appointment.getTreatment() == null
                            ? 0 : appointment.getTreatment().getTreatmentId())
                    .name("name").value(appointment.getTreatment() == null
                            ? null : appointment.getTreatment().getTreatmentName())
                    .name("baseCost").value(appointment.getTreatment() == null
                            ? null : appointment.getTreatment().getBaseCost())
                .endObject()
                .name("estimatedTotal").value(appointment.getEstimatedTotal())
            .endObject();
    }

    private User currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
        return (attribute instanceof User) ? (User) attribute : null;
    }
}
