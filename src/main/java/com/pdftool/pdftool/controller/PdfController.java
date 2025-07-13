package com.pdftool.pdftool.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdftool.pdftool.dto.EditRequest;
import com.pdftool.pdftool.service.PdfService;
import com.pdftool.pdftool.utility.HistoryLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    // 1. Merge PDFs
    @PostMapping("/merge")
    public ResponseEntity<byte[]> mergePdfs(@RequestParam("files") MultipartFile[] files) throws IOException {
        byte[] mergedPdf = pdfService.mergePdfFiles(files);
        HistoryLogger.log("anonymous", "Merged PDFs", files.length + " files");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merged.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(mergedPdf);
    }

    // 2. Resize Image
    @PostMapping("/resize-image")
    public ResponseEntity<byte[]> resizeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("width") int width,
            @RequestParam("height") int height) throws IOException {

        byte[] resized = pdfService.resizeImage(file, width, height);
        HistoryLogger.log("anonymous", "Resized Image", file.getOriginalFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resized.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(resized);
    }

    // 3. Resize PDF
    @PostMapping("/resize-pdf")
    public ResponseEntity<byte[]> resizePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("width") float width,
            @RequestParam("height") float height) throws IOException {

        byte[] resizedPdf = pdfService.resizePdf(file, width, height);
        HistoryLogger.log("anonymous", "Resized PDF", file.getOriginalFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=resized.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resizedPdf);
    }

    // 4. Lock PDF
    @PostMapping("/lock-pdf")
    public ResponseEntity<byte[]> lockPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) throws IOException {

        byte[] locked = pdfService.lockPdf(file, password);
        HistoryLogger.log("anonymous", "Locked PDF", file.getOriginalFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=locked.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(locked);
    }

    // 5. Unlock PDF
    @PostMapping("/unlock-pdf")
    public ResponseEntity<byte[]> unlockPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) throws IOException {

        byte[] unlocked = pdfService.unlockPdf(file, password);
        HistoryLogger.log("anonymous", "Unlocked PDF", file.getOriginalFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=unlocked.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(unlocked);
    }

    // 6. Convert DOC to PDF (via Cloudmersive API)
    @PostMapping("/convert-doc-to-pdf")
    public ResponseEntity<byte[]> convertDocToPdf(@RequestParam("file") MultipartFile file) {
        try {
            byte[] pdfDoc = pdfService.convertDocToPdfViaApi(file);
            HistoryLogger.log("anonymous", "Converted DOC to PDF", file.getOriginalFilename());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfDoc);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    // 7. Edit PDF by coordinates (EditRequest list)
    @PostMapping(value = "/edit-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> editPdf(
            @RequestPart("file") MultipartFile file,
            @RequestPart("edits") String editsJson) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        List<EditRequest> edits = objectMapper.readValue(editsJson, new TypeReference<>() {});
        byte[] editedPdf = pdfService.editPdfText(file, edits);

        HistoryLogger.log("anonymous", "Edited PDF Text", file.getOriginalFilename());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=edited.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(editedPdf);
    }

    // 8. Convert PDF to HTML (LibreOffice or other)
    @PostMapping("/html-to-pdf")
    public ResponseEntity<byte[]> convertHtmlToPdf(@RequestParam("html") MultipartFile htmlFile) {
        try {
            byte[] pdfBytes = pdfService.convertHtmlToPdf(htmlFile);  // You need to define this in service
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=converted.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
    @PostMapping("/pdf-to-html")
    public ResponseEntity<String> convertPdfToHtml(@RequestParam("file") MultipartFile file) {
        try {
            String htmlContent = pdfService.convertPdfToHtml(file);  // ✅ This must exist in the service
            HistoryLogger.log("anonymous", "Converted PDF to HTML", file.getOriginalFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(htmlContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to convert PDF to HTML: " + e.getMessage());
        }
    }

}
