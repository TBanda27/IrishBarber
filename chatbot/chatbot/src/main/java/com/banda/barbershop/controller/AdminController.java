package com.banda.barbershop.controller;

import com.banda.barbershop.dto.BarberDTO;
import com.banda.barbershop.dto.BarberScheduleDTO;
import com.banda.barbershop.dto.BarberStatsDTO;
import com.banda.barbershop.dto.BookingDTO;
import com.banda.barbershop.dto.CustomerDTO;
import com.banda.barbershop.dto.DashboardStatsDTO;
import com.banda.barbershop.dto.ServiceDTO;
import com.banda.barbershop.service.AdminService;
import com.banda.barbershop.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final ReminderService reminderService;

    /**
     * Get comprehensive dashboard statistics
     * GET /api/admin/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        log.info("Fetching dashboard statistics");
        DashboardStatsDTO stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get bookings for a specific date
     * GET /api/admin/bookings?date=2025-01-15
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDTO>> getBookingsByDate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        LocalDate queryDate = date != null ? date : LocalDate.now();
        log.info("Fetching bookings for date: {}", queryDate);

        List<BookingDTO> bookings = adminService.getBookingsByDate(queryDate);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Get all customers
     * GET /api/admin/customers
     */
    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
        log.info("Fetching all customers");
        List<CustomerDTO> customers = adminService.getAllCustomers();
        return ResponseEntity.ok(customers);
    }

    /**
     * Get top customers by loyalty points
     * GET /api/admin/customers/top?limit=10
     */
    @GetMapping("/customers/top")
    public ResponseEntity<List<CustomerDTO>> getTopCustomers(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Fetching top {} customers", limit);
        List<CustomerDTO> topCustomers = adminService.getTopCustomers(limit);
        return ResponseEntity.ok(topCustomers);
    }

    /**
     * Mark booking as completed
     * PUT /api/admin/bookings/{bookingCode}/complete
     */
    @PutMapping("/bookings/{bookingCode}/complete")
    public ResponseEntity<?> markBookingAsCompleted(@PathVariable String bookingCode) {
        log.info("Marking booking {} as completed", bookingCode);

        boolean success = adminService.markAsCompleted(bookingCode);

        if (success) {
            return ResponseEntity.ok()
                .body(new StatusResponse(true, "Booking marked as completed"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Mark booking as no-show
     * PUT /api/admin/bookings/{bookingCode}/no-show
     */
    @PutMapping("/bookings/{bookingCode}/no-show")
    public ResponseEntity<?> markBookingAsNoShow(@PathVariable String bookingCode) {
        log.info("Marking booking {} as no-show", bookingCode);

        boolean success = adminService.markAsNoShow(bookingCode);

        if (success) {
            return ResponseEntity.ok()
                .body(new StatusResponse(true, "Booking marked as no-show"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all services
     * GET /api/admin/services
     */
    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getAllServices() {
        log.info("Fetching all services");
        List<ServiceDTO> services = adminService.getAllServices();
        return ResponseEntity.ok(services);
    }

    /**
     * Get service by ID
     * GET /api/admin/services/{id}
     */
    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceDTO> getServiceById(@PathVariable Long id) {
        log.info("Fetching service with ID: {}", id);
        ServiceDTO service = adminService.getServiceById(id);
        if (service != null) {
            return ResponseEntity.ok(service);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Create new service
     * POST /api/admin/services
     */
    @PostMapping("/services")
    public ResponseEntity<ServiceDTO> createService(@RequestBody ServiceDTO serviceDTO) {
        log.info("Creating new service: {}", serviceDTO.getName());
        ServiceDTO created = adminService.createService(serviceDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Update service
     * PUT /api/admin/services/{id}
     */
    @PutMapping("/services/{id}")
    public ResponseEntity<ServiceDTO> updateService(
            @PathVariable Long id,
            @RequestBody ServiceDTO serviceDTO) {
        log.info("Updating service with ID: {}", id);
        ServiceDTO updated = adminService.updateService(id, serviceDTO);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete service
     * DELETE /api/admin/services/{id}
     */
    @DeleteMapping("/services/{id}")
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        log.info("Deleting service with ID: {}", id);
        boolean success = adminService.deleteService(id);
        if (success) {
            return ResponseEntity.ok()
                .body(new StatusResponse(true, "Service deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    // ==================== Barber Management Endpoints ====================

    /**
     * Get all barbers
     * GET /api/admin/barbers
     */
    @GetMapping("/barbers")
    public ResponseEntity<List<BarberDTO>> getAllBarbers() {
        log.info("Fetching all barbers");
        List<BarberDTO> barbers = adminService.getAllBarbers();
        return ResponseEntity.ok(barbers);
    }

    /**
     * Get barber by ID
     * GET /api/admin/barbers/{id}
     */
    @GetMapping("/barbers/{id}")
    public ResponseEntity<BarberDTO> getBarberById(@PathVariable Long id) {
        log.info("Fetching barber with ID: {}", id);
        BarberDTO barber = adminService.getBarberById(id);
        if (barber != null) {
            return ResponseEntity.ok(barber);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Create new barber
     * POST /api/admin/barbers
     */
    @PostMapping("/barbers")
    public ResponseEntity<BarberDTO> createBarber(@RequestBody BarberDTO barberDTO) {
        log.info("Creating new barber: {}", barberDTO.getName());
        BarberDTO created = adminService.createBarber(barberDTO);
        return ResponseEntity.ok(created);
    }

    /**
     * Update barber
     * PUT /api/admin/barbers/{id}
     */
    @PutMapping("/barbers/{id}")
    public ResponseEntity<BarberDTO> updateBarber(
            @PathVariable Long id,
            @RequestBody BarberDTO barberDTO) {
        log.info("Updating barber with ID: {}", id);
        BarberDTO updated = adminService.updateBarber(id, barberDTO);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Delete barber (soft delete - deactivate)
     * DELETE /api/admin/barbers/{id}
     */
    @DeleteMapping("/barbers/{id}")
    public ResponseEntity<?> deleteBarber(@PathVariable Long id) {
        log.info("Deactivating barber with ID: {}", id);
        boolean success = adminService.deleteBarber(id);
        if (success) {
            return ResponseEntity.ok()
                .body(new StatusResponse(true, "Barber deactivated successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get barber schedule for specific date
     * GET /api/admin/barbers/{id}/schedule?date=2025-01-15
     */
    @GetMapping("/barbers/{id}/schedule")
    public ResponseEntity<BarberScheduleDTO> getBarberSchedule(
            @PathVariable Long id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        LocalDate queryDate = date != null ? date : LocalDate.now();
        log.info("Fetching schedule for barber {} on date: {}", id, queryDate);

        BarberScheduleDTO schedule = adminService.getBarberSchedule(id, queryDate);
        if (schedule != null) {
            return ResponseEntity.ok(schedule);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get barber statistics (performance metrics)
     * GET /api/admin/barbers/stats
     */
    @GetMapping("/barbers/stats")
    public ResponseEntity<List<BarberStatsDTO>> getBarberStats() {
        log.info("Fetching barber statistics");
        List<BarberStatsDTO> stats = adminService.getBarberStats();
        return ResponseEntity.ok(stats);
    }

    // ==================== Reminder Test Endpoints ====================

    /**
     * Manually trigger one-hour reminders (for testing)
     * POST /api/admin/reminders/one-hour
     */
    @PostMapping("/reminders/one-hour")
    public ResponseEntity<?> triggerOneHourReminders() {
        log.info("Manually triggering one-hour reminders");
        try {
            int sent = reminderService.sendOneHourReminders();
            return ResponseEntity.ok(new StatusResponse(true, "Sent " + sent + " one-hour reminders"));
        } catch (Exception e) {
            log.error("Error triggering one-hour reminders: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new StatusResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Manually trigger day-before reminders (for testing)
     * POST /api/admin/reminders/day-before
     */
    @PostMapping("/reminders/day-before")
    public ResponseEntity<?> triggerDayBeforeReminders() {
        log.info("Manually triggering day-before reminders");
        try {
            int sent = reminderService.sendDayBeforeReminders();
            return ResponseEntity.ok(new StatusResponse(true, "Sent " + sent + " day-before reminders"));
        } catch (Exception e) {
            log.error("Error triggering day-before reminders: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .body(new StatusResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Send test reminder to specific booking (for testing)
     * POST /api/admin/reminders/test/{bookingCode}
     */
    @PostMapping("/reminders/test/{bookingCode}")
    public ResponseEntity<?> sendTestReminder(@PathVariable String bookingCode) {
        log.info("Sending test reminder for booking: {}", bookingCode);
        try {
            reminderService.sendTestReminder(bookingCode);
            return ResponseEntity.ok(new StatusResponse(true, "Test reminder sent for booking " + bookingCode));
        } catch (Exception e) {
            log.error("Error sending test reminder: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new StatusResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * Simple response wrapper for status messages
     */
    private record StatusResponse(boolean success, String message) {}
}
