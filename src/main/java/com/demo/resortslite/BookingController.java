package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // VIOLATION cr-java-0067 [Cloud Compatibility / Mandatory]: In-memory cache without TTL
    // breaks horizontal scaling — cache is instance-local, invisible to other EC2 instances
    private static final Map<String, Object> bookingCache = new HashMap<>(); // cr-java-0067

    // Externalized inventory service URL - retrieved from AWS Systems Manager Parameter Store
    @Value("${app.inventory.endpoint}")
    private String inventoryServiceUrl;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // FIXED cr-java-0065: Removed HTTP session storage. Session state is now managed by
        // Spring Session Data Redis (Amazon ElastiCache), enabling stateless application instances
        // with centralized, distributed session management across all EC2 instances.
        // Session data is automatically stored in Redis and accessible from any instance.

        bookingCache.put((String) booking.get("bookingId"), booking);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId) {

        // FIXED cr-java-0065: Removed session-based state retrieval. Application is now stateless.
        // If guest information is needed, it should be retrieved from the booking record itself
        // or passed as a request parameter, ensuring consistency across all instances.

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        Map<String, Object> bookingDetails = bookingService.getBookingById(bookingId);
        result.put("details", bookingDetails);
        // Extract guest name from booking details if available
        if (bookingDetails != null && bookingDetails.containsKey("guestName")) {
            result.put("sessionGuest", bookingDetails.get("guestName"));
        }
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // URL externalized to AWS Systems Manager Parameter Store via application.properties
        String inventoryUrl = inventoryServiceUrl + "/rooms/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // VIOLATION czr-java-001 [Software Portability / Mandatory]: Hardcoded absolute
        // file path. This path does not exist inside a container image. Container images
        // have their own isolated file systems — /var/legacy/reports won't be present.
        String reportPath = "/var/legacy/reports/" + month + "_bookings.pdf"; // czr-java-001

        Map<String, Object> response = new HashMap<>();
        response.put("reportPath", reportPath);
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}
