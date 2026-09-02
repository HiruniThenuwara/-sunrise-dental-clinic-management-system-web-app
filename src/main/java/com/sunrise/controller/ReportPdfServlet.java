package com.sunrise.controller;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sunrise.model.User;
import com.sunrise.service.AuthService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Builds the management report as a downloadable PDF file.
 *
 * <p>The browser is sent {@code Content-Disposition: attachment}, so the file
 * is saved to disk rather than opened in a tab. The clinic manager can then
 * email or file the report, which the print dialogue alone does not allow.</p>
 *
 * <p>The page is written with OpenPDF, a small library for producing PDF
 * documents. It replaces no part of the MVC structure: this class is still an
 * ordinary servlet in the CONTROLLER layer, and it sits behind
 * {@code AuthFilter} like every other page under {@code /admin/}.</p>
 */
@WebServlet(name = "ReportPdfServlet", urlPatterns = {"/admin/reports/pdf"})
public class ReportPdfServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Color NAVY = new Color(11, 31, 58);
    private static final Color TEAL = new Color(13, 148, 136);
    private static final Color HEAD_BG = new Color(238, 244, 249);
    private static final Color LINE = new Color(210, 220, 231);
    private static final Color GREY = new Color(100, 116, 139);

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String from = valueOr(request.getParameter("from"), "2026-08-01");
        String to = valueOr(request.getParameter("to"), "2026-08-31");
        String preparedBy = currentUserName(request);

        String fileName = "sunrise-clinic-report-" + LocalDate.now().format(FILE_DATE) + ".pdf";

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        Document document = new Document(PageSize.A4, 42, 42, 46, 46);

        try (OutputStream out = response.getOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.addTitle("Sunrise Dental Clinic - Management Report");
            document.addAuthor("Sunrise Dental Clinic Management System");
            document.open();

            writeHeader(document, from, to, preparedBy);
            writeSummary(document);
            writeDentistWorkload(document);
            writeRevenueByTreatment(document);
            writeFooter(document);

            document.close();

        } catch (DocumentException e) {
            throw new ServletException("Could not build the PDF report", e);
        }
    }

    /** Clinic name, report title and the period it covers. */
    private void writeHeader(Document document, String from, String to, String preparedBy)
            throws DocumentException {

        Paragraph clinic = new Paragraph("Sunrise Dental Clinic",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, NAVY));
        clinic.setAlignment(Element.ALIGN_CENTER);
        document.add(clinic);

        Paragraph address = new Paragraph(
                "No 128, Galle Road, Colombo 03  |  Tel: 011 234 5678  |  info@sunrisedental.lk",
                FontFactory.getFont(FontFactory.HELVETICA, 9, GREY));
        address.setAlignment(Element.ALIGN_CENTER);
        address.setSpacingAfter(14f);
        document.add(address);

        Paragraph title = new Paragraph("MANAGEMENT REPORT",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEAL));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph(
                "Period: " + from + "  to  " + to
                        + "     |     Prepared by: " + preparedBy
                        + "     |     Generated: " + LocalDate.now().format(LONG_DATE),
                FontFactory.getFont(FontFactory.HELVETICA, 9, GREY));
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(20f);
        document.add(period);
    }

    /** The four headline figures. */
    private void writeSummary(Document document) throws DocumentException {
        document.add(sectionTitle("1. Summary"));

        PdfPTable table = newTable(new float[]{3f, 2f, 3f});
        addHeaderRow(table, "Measure", "Value", "Note");

        addRow(table, "Total appointments", "148", "12 percent above the previous month");
        addRow(table, "Total revenue (LKR)", "1,420,000.00", "All completed and paid visits");
        addRow(table, "Cancellations", "9", "6.1 percent of all bookings");
        addRow(table, "New patients registered", "27", "4 more than the previous month");

        document.add(table);
    }

    /** How much work each dentist carried. */
    private void writeDentistWorkload(Document document) throws DocumentException {
        document.add(sectionTitle("2. Dentist Workload"));

        PdfPTable table = newTable(new float[]{3.4f, 1.6f, 1.6f, 2.2f});
        addHeaderRow(table, "Dentist", "Appointments", "Completed", "Revenue (LKR)");

        addRow(table, "Dr. Anura Jayasinghe", "52", "48", "386,000.00");
        addRow(table, "Dr. Sanduni Fernando", "38", "35", "612,500.00");
        addRow(table, "Dr. Kasun Silva", "34", "31", "341,000.00");
        addRow(table, "Dr. Malsha Weerasinghe", "24", "22", "84,500.00");
        addTotalRow(table, "Total", "148", "136", "1,424,000.00");

        document.add(table);
    }

    /** Which treatments bring in the income. */
    private void writeRevenueByTreatment(Document document) throws DocumentException {
        document.add(sectionTitle("3. Revenue by Treatment Type"));

        PdfPTable table = newTable(new float[]{3f, 1.3f, 2f, 2.2f});
        addHeaderRow(table, "Treatment", "Count", "Unit Price (LKR)", "Revenue (LKR)");

        addRow(table, "Braces Fitting", "6", "85,000.00", "510,000.00");
        addRow(table, "Root Canal", "14", "25,000.00", "350,000.00");
        addRow(table, "Crown Fitting", "7", "35,000.00", "245,000.00");
        addRow(table, "Filling", "29", "6,000.00", "174,000.00");
        addRow(table, "Scaling", "31", "4,500.00", "139,500.00");
        addTotalRow(table, "Total", "87", "", "1,418,500.00");

        document.add(table);

        Paragraph note = new Paragraph(
                "Observation: braces and root canal treatments produce most of the income even "
                        + "though they are the least frequent. Thursday is the busiest day of the "
                        + "week, so adding a second dentist on Thursday would reduce patient "
                        + "waiting time.",
                FontFactory.getFont(FontFactory.HELVETICA, 9.5f, GREY));
        note.setSpacingBefore(10f);
        document.add(note);
    }

    /** Confidentiality line at the bottom. */
    private void writeFooter(Document document) throws DocumentException {
        Paragraph footer = new Paragraph(
                "This report is generated by the Sunrise Dental Clinic Management System. "
                        + "It contains confidential clinic information and is intended for "
                        + "management use only.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, GREY));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(26f);
        document.add(footer);
    }

    // -----------------------------------------------------------------
    //  small helpers that keep the report code readable
    // -----------------------------------------------------------------

    private Paragraph sectionTitle(String text) {
        Paragraph heading = new Paragraph(text,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY));
        heading.setSpacingBefore(16f);
        heading.setSpacingAfter(8f);
        return heading;
    }

    private PdfPTable newTable(float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidths(widths);
        table.setWidthPercentage(100f);
        return table;
    }

    private void addHeaderRow(PdfPTable table, String... titles) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, NAVY);
        for (String title : titles) {
            PdfPCell cell = new PdfPCell(new Phrase(title, font));
            cell.setBackgroundColor(HEAD_BG);
            cell.setBorderColor(LINE);
            cell.setPadding(7f);
            table.addCell(cell);
        }
    }

    private void addRow(PdfPTable table, String... values) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.BLACK);
        for (int i = 0; i < values.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(values[i], font));
            cell.setBorderColor(LINE);
            cell.setPadding(6.5f);
            if (i > 0) {
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            table.addCell(cell);
        }
    }

    private void addTotalRow(PdfPTable table, String... values) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, NAVY);
        for (int i = 0; i < values.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(values[i], font));
            cell.setBackgroundColor(HEAD_BG);
            cell.setBorderColor(LINE);
            cell.setPadding(6.5f);
            if (i > 0) {
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            table.addCell(cell);
        }
    }

    /** Name of the staff member who asked for the report. */
    private String currentUserName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object attribute = session.getAttribute(AuthService.SESSION_USER_KEY);
            if (attribute instanceof User) {
                return ((User) attribute).getFullName();
            }
        }
        return "Clinic staff";
    }

    private String valueOr(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
