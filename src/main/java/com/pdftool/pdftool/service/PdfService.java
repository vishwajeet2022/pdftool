package com.pdftool.pdftool.service;

import com.pdftool.pdftool.dto.EditRequest;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

@Service
public class PdfService {

    // 1. Merge PDF files
    public byte[] mergePdfFiles(MultipartFile[] files) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        merger.setDestinationStream(outputStream);

        for (MultipartFile file : files) {
            merger.addSource(file.getInputStream());
        }
        merger.mergeDocuments(null);
        return outputStream.toByteArray();
    }

    // 2. Resize Image
    public byte[] resizeImage(MultipartFile file, int targetWidth, int targetHeight) throws IOException {
        String formatName = "png";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            formatName = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) throw new IOException("Unsupported or corrupted image format");

        Image resized = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resized, 0, 0, null);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(outputImage, formatName, baos);
        return baos.toByteArray();
    }

    // 3. Resize PDF
    public byte[] resizePdf(MultipartFile file, float newWidth, float newHeight) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());

        for (PDPage page : document.getPages()) {
            PDRectangle newSize = new PDRectangle(newWidth, newHeight);
            PDRectangle originalSize = page.getMediaBox();

            float scaleX = newWidth / originalSize.getWidth();
            float scaleY = newHeight / originalSize.getHeight();
            float scale = Math.min(scaleX, scaleY);

            PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, true);
            cs.transform(Matrix.getScaleInstance(scale, scale));
            cs.close();

            page.setMediaBox(newSize);
            page.setCropBox(newSize);
            page.setTrimBox(newSize);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        return baos.toByteArray();
    }

    // 4. Lock PDF
    public byte[] lockPdf(MultipartFile file, String password) throws IOException {
        PDDocument doc = PDDocument.load(file.getInputStream());

        AccessPermission ap = new AccessPermission();
        StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, ap);
        policy.setEncryptionKeyLength(128);
        policy.setPermissions(ap);
        doc.protect(policy);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }

    // 5. Unlock PDF
    public byte[] unlockPdf(MultipartFile file, String password) throws IOException {
        PDDocument doc = PDDocument.load(file.getInputStream(), password);
        doc.setAllSecurityToBeRemoved(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();
        return baos.toByteArray();
    }

    // 6. Convert DOCX to PDF via Cloudmersive API
    public byte[] convertDocToPdfViaApi(MultipartFile file) throws IOException {
        String apiKey = "48e11892-0770-4c44-9a07-6754b340dfbd"; // move to env in production

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Apikey", apiKey);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentDispositionFormData("file", file.getOriginalFilename());
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<InputStreamResource> fileEntity = new HttpEntity<>(
                new InputStreamResource(file.getInputStream()), fileHeaders
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "https://api.cloudmersive.com/convert/docx/to/pdf";

        ResponseEntity<byte[]> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, requestEntity, byte[].class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new IOException("Cloudmersive API failed: " + response.getStatusCode());
        }
    }

    // 7. Edit PDF Text
    public byte[] editPdfText(MultipartFile file, List<EditRequest> edits) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());

        for (EditRequest edit : edits) {
            PDPage page = document.getPage(edit.getPageNumber());
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            contentStream.setNonStrokingColor(Color.WHITE);
            contentStream.addRect(edit.getX(), edit.getY(), edit.getWidth(), edit.getHeight());
            contentStream.fill();

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.setNonStrokingColor(Color.BLACK);
            contentStream.newLineAtOffset(edit.getX(), edit.getY());
            contentStream.showText(edit.getNewText());
            contentStream.endText();

            contentStream.close();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        document.close();

        return outputStream.toByteArray();
    }

    // 8. Convert PDF to HTML using LibreOffice
    // 8. Convert PDF to HTML using Cloudmersive
    public String convertPdfToHtml(MultipartFile file) throws IOException {
        String apiKey = "48e11892-0770-4c44-9a07-6754b340dfbd"; // Replace with env-safe key

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Apikey", apiKey);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentDispositionFormData("file", file.getOriginalFilename());
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        HttpEntity<InputStreamResource> fileEntity = new HttpEntity<>(
                new InputStreamResource(file.getInputStream()), fileHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileEntity);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "https://api.cloudmersive.com/convert/pdf/to/html";

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new IOException("Cloudmersive PDF to HTML failed: " + response.getStatusCode());
        }
    }

    // 9. Convert HTML back to PDF using LibreOffice
    public byte[] convertHtmlToPdf(MultipartFile htmlFile) throws IOException {
        String apiKey = "your-api-key-here";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Apikey", apiKey);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentDispositionFormData("inputFile", htmlFile.getOriginalFilename());
        fileHeaders.setContentType(MediaType.TEXT_HTML);

        HttpEntity<InputStreamResource> fileEntity = new HttpEntity<>(
                new InputStreamResource(htmlFile.getInputStream()), fileHeaders
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("inputFile", fileEntity);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        String apiUrl = "https://api.cloudmersive.com/convert/html/to/pdf";  // ✅ Valid Cloudmersive endpoint

        ResponseEntity<byte[]> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            throw new IOException("Cloudmersive HTML to PDF conversion failed: " + response.getStatusCode());
        }
    }

}
