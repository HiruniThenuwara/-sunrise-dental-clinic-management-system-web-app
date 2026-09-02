package com.sunrise.dao;

import com.sunrise.dao.impl.ActivityLogDaoImpl;
import com.sunrise.dao.impl.AppointmentDaoImpl;
import com.sunrise.dao.impl.BillDaoImpl;
import com.sunrise.dao.impl.DoctorDaoImpl;
import com.sunrise.dao.impl.DoctorScheduleDaoImpl;
import com.sunrise.dao.impl.PatientDaoImpl;
import com.sunrise.dao.impl.ReportDaoImpl;
import com.sunrise.dao.impl.TreatmentDaoImpl;
import com.sunrise.dao.impl.UserDaoImpl;

/**
 * Creates the data access objects used by the service layer.
 *
 * <p><b>Design pattern: Factory.</b> A service asks for an
 * {@code AppointmentDao} and receives one, without ever naming
 * {@code AppointmentDaoImpl}. Two things follow from that:</p>
 *
 * <ul>
 *   <li>the service layer compiles with no knowledge of JDBC, so the
 *       dependency between the tiers stays one directional;</li>
 *   <li>if the clinic ever moves to different storage, the new
 *       implementations are wired up in this one class rather than in every
 *       service that uses them.</li>
 * </ul>
 *
 * <p>Each DAO is created once and reused. They hold no per-request state,
 * only a reference to the shared {@link DBConnection}, so one instance can be
 * shared safely across requests.</p>
 */
public final class DaoFactory {

    private static final UserDao USER_DAO = new UserDaoImpl();
    private static final DoctorDao DOCTOR_DAO = new DoctorDaoImpl();
    private static final PatientDao PATIENT_DAO = new PatientDaoImpl();
    private static final TreatmentDao TREATMENT_DAO = new TreatmentDaoImpl();
    private static final AppointmentDao APPOINTMENT_DAO = new AppointmentDaoImpl();
    private static final DoctorScheduleDao SCHEDULE_DAO = new DoctorScheduleDaoImpl();
    private static final BillDao BILL_DAO = new BillDaoImpl();
    private static final ReportDao REPORT_DAO = new ReportDaoImpl();
    private static final ActivityLogDao ACTIVITY_LOG_DAO = new ActivityLogDaoImpl();

    /** Utility class, never instantiated. */
    private DaoFactory() {
        throw new AssertionError("DaoFactory must not be instantiated");
    }

    /** @return the data access object for staff accounts */
    public static UserDao getUserDao() {
        return USER_DAO;
    }

    /** @return the data access object for dentists */
    public static DoctorDao getDoctorDao() {
        return DOCTOR_DAO;
    }

    /** @return the data access object for patients */
    public static PatientDao getPatientDao() {
        return PATIENT_DAO;
    }

    /** @return the data access object for treatment types */
    public static TreatmentDao getTreatmentDao() {
        return TREATMENT_DAO;
    }

    /** @return the data access object for appointments */
    public static AppointmentDao getAppointmentDao() {
        return APPOINTMENT_DAO;
    }

    /** @return the data access object for dentist working hours */
    public static DoctorScheduleDao getDoctorScheduleDao() {
        return SCHEDULE_DAO;
    }

    /** @return the data access object for bills */
    public static BillDao getBillDao() {
        return BILL_DAO;
    }

    /** @return the aggregate queries behind the management reports */
    public static ReportDao getReportDao() {
        return REPORT_DAO;
    }

    /** @return the append only store behind the activity log */
    public static ActivityLogDao getActivityLogDao() {
        return ACTIVITY_LOG_DAO;
    }
}
