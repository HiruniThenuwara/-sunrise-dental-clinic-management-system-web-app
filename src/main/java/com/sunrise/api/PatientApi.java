package com.sunrise.api;

import com.sunrise.dao.DaoFactory;
import com.sunrise.model.PatientSummary;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Web service that looks up existing patients.
 *
 * <p>{@code GET /api/patients?q=saman} searches the name, telephone number
 * and NIC, and returns the best few matches.</p>
 *
 * <p>The appointment form calls this while the receptionist types, so a
 * returning patient can be found and their details filled in with one click
 * instead of being typed again. That matters for more than convenience:
 * retyping an address is how the same person ends up in the system twice,
 * with their history split across two records.</p>
 *
 * <pre>
 * 200 OK
 * { "count": 1,
 *   "patients": [ { "id": 1, "name": "Saman Kumara", "contactNumber": "0712345678",
 *                   "address": "No 45, Galle Road, Colombo 03", "visits": 3 } ] }
 * </pre>
 */
@WebServlet(name = "PatientApi", urlPatterns = {"/api/patients"})
public class PatientApi extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Short enough to be quick, long enough to be useful in a dropdown. */
    private static final int MAX_RESULTS = 8;

    /** Below this, a search would match half the clinic. */
    private static final int MIN_TERM_LENGTH = 2;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String term = request.getParameter("q");

        if (term == null || term.trim().length() < MIN_TERM_LENGTH) {
            // Not an error: the caller simply has not typed enough yet.
            JsonWriter.send(response, HttpServletResponse.SC_OK, new JsonWriter()
                    .beginObject()
                        .name("count").value(0)
                        .name("patients").beginArray().endArray()
                    .endObject()
                    .toJson());
            return;
        }

        List<PatientSummary> matches =
                DaoFactory.getPatientDao().findAllWithHistory(term.trim());

        JsonWriter json = new JsonWriter();
        json.beginObject()
                .name("count").value(Math.min(matches.size(), MAX_RESULTS))
                .name("patients").beginArray();

        matches.stream().limit(MAX_RESULTS).forEach(match -> json.beginObject()
                .name("id").value(match.getPatient().getPatientId())
                .name("name").value(match.getPatient().getPatientName())
                .name("contactNumber").value(match.getPatient().getContactNumber())
                .name("address").value(match.getPatient().getAddress())
                .name("email").value(match.getPatient().getEmail())
                .name("nic").value(match.getPatient().getNic())
                .name("gender").value(match.getPatient().getGender() == null
                        ? null : match.getPatient().getGender().name())
                .name("visits").value(match.getVisitCount())
                .name("lastVisit").value(match.getFormattedLastVisit())
            .endObject());

        json.endArray().endObject();

        JsonWriter.send(response, HttpServletResponse.SC_OK, json.toJson());
    }
}
