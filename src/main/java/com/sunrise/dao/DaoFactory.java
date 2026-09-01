package com.sunrise.dao;

import com.sunrise.dao.impl.DoctorDaoImpl;
import com.sunrise.dao.impl.UserDaoImpl;

/**
 * Creates the data access objects used by the service layer.
 *
 * <p><b>Design pattern: Factory.</b> A service asks for a {@code UserDao} and
 * receives one, without ever naming {@code UserDaoImpl}. Two things follow
 * from that:</p>
 *
 * <ul>
 *   <li>the service layer compiles without any knowledge of JDBC, so the
 *       dependency between the tiers stays one directional;</li>
 *   <li>if the clinic ever moves to a different storage technology, the new
 *       implementations are wired up in this one class instead of in every
 *       service that uses them.</li>
 * </ul>
 *
 * <p>Each DAO is created once and reused. The objects hold no per-request
 * state, only a reference to the shared {@link DBConnection}, so sharing one
 * instance across requests is safe.</p>
 */
public final class DaoFactory {

    private static final UserDao USER_DAO = new UserDaoImpl();
    private static final DoctorDao DOCTOR_DAO = new DoctorDaoImpl();

    /** Utility class, never instantiated. */
    private DaoFactory() {
        throw new AssertionError("DaoFactory must not be instantiated");
    }

    /**
     * @return the data access object for staff accounts
     */
    public static UserDao getUserDao() {
        return USER_DAO;
    }

    /**
     * @return the data access object for dentists
     */
    public static DoctorDao getDoctorDao() {
        return DOCTOR_DAO;
    }
}
