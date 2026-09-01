package com.sunrise.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ValidationService}.
 *
 * <p><b>Test driven development.</b> This class was written and committed
 * before {@code ValidationService} had any working code, so the first run
 * fails on purpose. The implementation is then written until every test
 * passes.</p>
 *
 * <p>The rules tested here are the server side ones. The browser also checks
 * the same fields, but browser checks can be bypassed with the developer
 * tools or by sending the request directly, so the server must never trust
 * them.</p>
 */
@DisplayName("ValidationService - server side input rules")
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // =================================================================
    //  Patient and dentist names
    // =================================================================
    @Nested
    @DisplayName("Name validation")
    class NameValidation {

        @ParameterizedTest(name = "TC-01 accepts \"{0}\"")
        @ValueSource(strings = {
                "Saman Kumara",
                "Dilini Rathnayake",
                "Dr. Anura Jayasinghe",
                "A P Silva",
                "Nimal"
        })
        void acceptsRealNames(String name) {
            assertTrue(validationService.isValidName(name));
        }

        @ParameterizedTest(name = "TC-02 rejects blank name \"{0}\"")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void rejectsBlankNames(String name) {
            assertFalse(validationService.isValidName(name));
        }

        @Test
        @DisplayName("TC-03 rejects a name shorter than three characters")
        void rejectsTooShortName() {
            assertAll(
                    () -> assertFalse(validationService.isValidName("A")),
                    () -> assertFalse(validationService.isValidName("Ab")),
                    () -> assertTrue(validationService.isValidName("Abc"))
            );
        }

        @Test
        @DisplayName("TC-04 rejects a name longer than one hundred characters")
        void rejectsTooLongName() {
            String hundred = "a".repeat(100);
            assertAll(
                    () -> assertTrue(validationService.isValidName(hundred)),
                    () -> assertFalse(validationService.isValidName(hundred + "a"))
            );
        }

        @ParameterizedTest(name = "TC-05 rejects unsafe input \"{0}\"")
        @ValueSource(strings = {
                "Robert'); DROP TABLE patients; --",
                "<script>alert(1)</script>",
                "Saman123",
                "user@@name"
        })
        void rejectsUnsafeOrNumericInput(String name) {
            assertFalse(validationService.isValidName(name));
        }
    }

    // =================================================================
    //  Contact numbers
    // =================================================================
    @Nested
    @DisplayName("Contact number validation")
    class ContactNumberValidation {

        @ParameterizedTest(name = "TC-06 accepts \"{0}\"")
        @ValueSource(strings = {"0712345678", "0771234567", "0112345678", "0761234567"})
        void acceptsSriLankanNumbers(String number) {
            assertTrue(validationService.isValidContactNumber(number));
        }

        @ParameterizedTest(name = "TC-07 rejects \"{0}\"")
        @ValueSource(strings = {
                "071234567",     // nine digits, one short
                "07123456789",   // eleven digits, one too many
                "1712345678",    // does not start with zero
                "071 234 5678",  // spaces are not accepted by the server
                "071-2345678",
                "abcdefghij"
        })
        void rejectsWrongFormats(String number) {
            assertFalse(validationService.isValidContactNumber(number));
        }

        @ParameterizedTest(name = "TC-08 rejects blank number")
        @NullAndEmptySource
        void rejectsBlankNumber(String number) {
            assertFalse(validationService.isValidContactNumber(number));
        }
    }

    // =================================================================
    //  Appointment dates
    // =================================================================
    @Nested
    @DisplayName("Appointment date validation")
    class DateValidation {

        @Test
        @DisplayName("TC-09 accepts today and future dates")
        void acceptsTodayAndFuture() {
            assertAll(
                    () -> assertTrue(validationService.isBookableDate(LocalDate.now())),
                    () -> assertTrue(validationService.isBookableDate(LocalDate.now().plusDays(1))),
                    () -> assertTrue(validationService.isBookableDate(LocalDate.now().plusMonths(3)))
            );
        }

        @Test
        @DisplayName("TC-10 rejects a date in the past")
        void rejectsPastDate() {
            assertAll(
                    () -> assertFalse(validationService.isBookableDate(LocalDate.now().minusDays(1))),
                    () -> assertFalse(validationService.isBookableDate(LocalDate.of(2020, 1, 1)))
            );
        }

        @Test
        @DisplayName("TC-11 rejects a null date and a date too far ahead")
        void rejectsNullAndUnreasonablyFarDate() {
            assertAll(
                    () -> assertFalse(validationService.isBookableDate(null)),
                    () -> assertFalse(validationService.isBookableDate(LocalDate.now().plusYears(2)))
            );
        }
    }

    // =================================================================
    //  Email, optional but must be sensible when given
    // =================================================================
    @Nested
    @DisplayName("Email validation")
    class EmailValidation {

        @Test
        @DisplayName("TC-12 an empty email is allowed because the field is optional")
        void allowsEmptyEmail() {
            assertAll(
                    () -> assertTrue(validationService.isValidOptionalEmail(null)),
                    () -> assertTrue(validationService.isValidOptionalEmail("")),
                    () -> assertTrue(validationService.isValidOptionalEmail("   "))
            );
        }

        @ParameterizedTest(name = "TC-13 accepts \"{0}\"")
        @ValueSource(strings = {"saman@gmail.com", "a.b@sunrisedental.lk", "nimali_p@clinic.co.uk"})
        void acceptsRealEmails(String email) {
            assertTrue(validationService.isValidOptionalEmail(email));
        }

        @ParameterizedTest(name = "TC-14 rejects \"{0}\"")
        @ValueSource(strings = {"saman", "saman@", "@gmail.com", "saman@gmail", "a b@gmail.com"})
        void rejectsMalformedEmails(String email) {
            assertFalse(validationService.isValidOptionalEmail(email));
        }
    }

    // =================================================================
    //  Money
    // =================================================================
    @Nested
    @DisplayName("Money validation")
    class MoneyValidation {

        @Test
        @DisplayName("TC-15 accepts zero and positive amounts")
        void acceptsZeroAndPositive() {
            assertAll(
                    () -> assertTrue(validationService.isValidAmount(BigDecimal.ZERO)),
                    () -> assertTrue(validationService.isValidAmount(new BigDecimal("1500.00"))),
                    () -> assertTrue(validationService.isValidAmount(new BigDecimal("85000")))
            );
        }

        @Test
        @DisplayName("TC-16 rejects a negative amount and a null amount")
        void rejectsNegativeAndNull() {
            assertAll(
                    () -> assertFalse(validationService.isValidAmount(new BigDecimal("-1"))),
                    () -> assertFalse(validationService.isValidAmount(null))
            );
        }

        @Test
        @DisplayName("TC-17 rejects an amount above the clinic ceiling")
        void rejectsUnreasonablyLargeAmount() {
            assertFalse(validationService.isValidAmount(new BigDecimal("10000000")));
        }
    }

    // =================================================================
    //  The whole appointment form
    // =================================================================
    @Nested
    @DisplayName("Whole appointment form")
    class AppointmentFormValidation {

        @Test
        @DisplayName("TC-18 a correctly filled form produces no errors")
        void validFormHasNoErrors() {
            List<String> errors = validationService.validateAppointmentForm(
                    "Saman Kumara",
                    "No 45, Galle Road, Colombo 03",
                    "0712345678",
                    "saman@gmail.com",
                    "1",
                    "2",
                    LocalDate.now().plusDays(1),
                    "09:30");

            assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
        }

        @Test
        @DisplayName("TC-19 an empty form reports every missing field at once")
        void emptyFormReportsAllProblems() {
            List<String> errors = validationService.validateAppointmentForm(
                    "", "", "", "", "", "", null, "");

            assertAll(
                    () -> assertFalse(errors.isEmpty()),
                    () -> assertTrue(errors.size() >= 6,
                            "The staff member should see every problem at once, got: " + errors)
            );
        }

        @Test
        @DisplayName("TC-20 a past date is reported as an error")
        void pastDateIsReported() {
            List<String> errors = validationService.validateAppointmentForm(
                    "Saman Kumara",
                    "No 45, Galle Road, Colombo 03",
                    "0712345678",
                    null,
                    "1",
                    "2",
                    LocalDate.now().minusDays(1),
                    "09:30");

            assertTrue(errors.stream().anyMatch(e -> e.toLowerCase().contains("date")),
                    "Expected a date error, got: " + errors);
        }
    }
}
