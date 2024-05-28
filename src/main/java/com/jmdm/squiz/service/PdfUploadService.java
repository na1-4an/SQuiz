package com.jmdm.squiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmdm.squiz.domain.Member;
import com.jmdm.squiz.domain.Pdf;
import com.jmdm.squiz.dto.AiGetTextAndClassifyKcResponse;
import com.jmdm.squiz.dto.PdfUploadResponse;
import com.jmdm.squiz.enums.SubjectType;
import com.jmdm.squiz.exception.ErrorCode;
import com.jmdm.squiz.exception.model.AiServerException;
import com.jmdm.squiz.repository.MemberRepository;
import com.jmdm.squiz.repository.PdfRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PdfUploadService {
    private final PdfRepository pdfRepository;
    private final MemberRepository memberRepository;
    private final FileService fileService;
    @Value("${ai.server.url}")
    private String aiUrl;


    @Transactional
    public PdfUploadResponse uploadPdf(String memberId, SubjectType subjectType, MultipartFile pdf) throws IOException {
        if (pdf.isEmpty()) {
            return null; // 파일 에러 띄우기
        }

        // pdfId 얻기 위해 저장
        String uploadFileName = pdf.getOriginalFilename();
        int totalPageCount = getPageCount(pdf);
        Member member = memberRepository.findByMemberId(memberId);
        List<String> list = fileService.getStoredFileName(".txt");
        String storedFileName = list.get(0);
        String filePath = list.get(1);
        Pdf storedPdf = Pdf.builder()
                .member(member)
                .uploadFileName(uploadFileName)
                .totalPageCount(totalPageCount)
                .subjectType(subjectType)
                .storedFileName(storedFileName)
                .filePath(filePath)
                .build();
        pdfRepository.save(storedPdf);

        // ai post pdf text 생성, kc 분류
        AiGetTextAndClassifyKcResponse response = getTextAndKCs(storedPdf.getId(), subjectType, pdf);
        fileService.saveData(response.getPdfText(), filePath);
//        fileService.saveData("그냥 아무말아무말", filePath);
        // PdfToText, kc 저장
        storedPdf.setPageKcId(response.getPageKcId());
        pdfRepository.save(storedPdf);

        // pdf 업로드 response 생성
        return PdfUploadResponse.builder()
                .pdfId(storedPdf.getId())
                .uploadFileName(uploadFileName)
                .totalPageCount(totalPageCount)
                .build();
    }

    private int getPageCount(MultipartFile pdf) throws IOException {
        PDDocument document = PDDocument.load(pdf.getInputStream());
        int pageCount = document.getNumberOfPages();
        document.close();
        return pageCount;
    }

    private AiGetTextAndClassifyKcResponse getTextAndKCs(Long pdfId, SubjectType subjectType, MultipartFile pdf) throws IOException {
        String aiServerUrl = aiUrl + "/pdf/kc";

        HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("pdfId", pdfId);
        body.add("pdf", new MultipartInputStreamFileResource(pdf.getInputStream(), pdf.getOriginalFilename()));
        body.add("subject", subjectType.toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        System.out.println("전송");
        ResponseEntity<String> response = restTemplate.postForEntity(aiServerUrl, request, String.class);
        System.out.println("전송완료");
        if (! response.getStatusCode().is2xxSuccessful()) {
            pdfRepository.deleteById(pdfId);
            throw new AiServerException(ErrorCode.AI_SERVER_ERROR, ErrorCode.AI_SERVER_ERROR.getMessage());
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.getBody(), AiGetTextAndClassifyKcResponse.class);

    }

    private static class MultipartInputStreamFileResource extends InputStreamResource {
        private final String filename;

        MultipartInputStreamFileResource(InputStream inputStream, String filename) {
            super(inputStream);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return this.filename;
        }

        @Override
        public long contentLength() throws IOException {
            return -1; // We do not know the content length beforehand
        }
    }

}
