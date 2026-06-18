package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private S3Client s3Client;

    // FIXED cr-java-0061, cr-java-0062, cr-java-0063: Replaced hard-coded file paths with Amazon S3
    // All file operations now use S3 for durable, scalable, cloud-native storage
    @Value("${aws.s3.reports.bucket:resorts-reports-bucket}")
    private String reportsBucketName;

    @Value("${aws.s3.backups.bucket:resorts-backups-bucket}")
    private String backupsBucketName;

    // FIXED cr-java-0077: Replaced hard-coded port with environment variable from AWS Parameter Store
    @Value("${server.port:8080}")
    private int serverPort;

    // FIXED cr-java-0071: Externalized report download URL to environment variable
    @Value("${app.reports.base.url:https://reports.resorts-internal.com}")
    private String reportsBaseUrl;

    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";

        Map<String, Object> result = new HashMap<>();

        try {
            // FIXED cr-java-0062, cr-java-0063: Replaced local file write with S3 upload
            // Data is now written directly to S3 instead of ephemeral local file system
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n".getBytes());
            outputStream.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n".getBytes());
            outputStream.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n".getBytes());

            byte[] reportData = outputStream.toByteArray();

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(reportsBucketName)
                    .key("reports/" + fileName)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(reportData));

            result.put("status", "generated");
            result.put("s3Bucket", reportsBucketName);
            result.put("s3Key", "reports/" + fileName);
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    public String buildReportDownloadUrl(String reportName) {
        // FIXED cr-java-0071: Using externalized HTTPS URL from environment variable
        // URL is now retrieved from AWS Systems Manager Parameter Store
        return reportsBaseUrl + "/download/" + reportName;
    }

    public Map<String, Object> getSystemInfo() {
        // FIXED cr-java-0111: Replaced java.util.Date with java.time API and standardized on UTC
        // All timestamps now use UTC timezone to ensure consistency across distributed cloud instances
        ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
        String timestamp = utcNow.format(DateTimeFormatter.ISO_INSTANT);

        Map<String, Object> info = new HashMap<>();
        info.put("reportsBucket", reportsBucketName);
        info.put("backupsBucket", backupsBucketName);
        info.put("serverPort", serverPort);
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }
}
