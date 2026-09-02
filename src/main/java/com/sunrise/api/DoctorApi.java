package com.sunrise.api;

import com.sunrise.model.Doctor;
import com.sunrise.service.DoctorService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Web service listing the dentists.
 *
 * <p>{@code GET /api/doctors} returns every dentist who can accept bookings.
 * Adding {@code ?all=true} includes the inactive ones as well.</p>
 *
 * <p>This is part of what makes the system a distributed application. The
 * browser fetches the dentist list as JSON rather than having it written into
 * the page, and the same endpoint could serve a mobile application or a
 * second clinic branch without any change here.</p>
 *
 * <pre>
 * GET /api/doctors
 * 200 OK
 * { "count": 4,
 *   "doctors": [ { "id": 1, "name": "Dr. Anura Jayasinghe", ... } ] }
 * </pre>
 */
@WebServlet(name = "DoctorApi", urlPatterns = {"/api/doctors"})
public class DoctorApi extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient DoctorService doctorService;

    @Override
    public void init() {
        this.doctorService = new DoctorService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        boolean includeInactive = "true".equalsIgnoreCase(request.getParameter("all"));
        List<Doctor> doctors = includeInactive
                ? doctorService.findAll()
                : doctorService.findAllActive();

        JsonWriter json = new JsonWriter();
        json.beginObject()
                .name("count").value(doctors.size())
                .name("doctors").beginArray();

        for (Doctor doctor : doctors) {
            json.beginObject()
                    .name("id").value(doctor.getDoctorId())
                    .name("name").value(doctor.getDoctorName())
                    .name("specialization").value(doctor.getSpecialization())
                    .name("contactNumber").value(doctor.getContactNumber())
                    .name("consultationFee").value(doctor.getConsultationFee())
                    .name("active").value(doctor.isActive())
                .endObject();
        }

        json.endArray().endObject();

        JsonWriter.send(response, HttpServletResponse.SC_OK, json.toJson());
    }
}
