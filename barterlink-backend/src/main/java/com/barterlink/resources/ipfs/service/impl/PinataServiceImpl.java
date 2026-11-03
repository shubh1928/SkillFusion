package com.barterlink.resources.ipfs.service.impl;

import com.barterlink.resources.ipfs.config.PinataConfig;
import com.barterlink.resources.ipfs.dto.PinataResponse;
import com.barterlink.resources.ipfs.dto.PinataUploadResponse;
import com.barterlink.resources.ipfs.service.PinataService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.common.exception.BadRequestException;

import java.util.Set;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PinataServiceImpl
        implements PinataService {

    private static final String PINATA_UPLOAD_URL =
            "https://uploads.pinata.cloud/v3/files";

    private static final String PINATA_DELETE_URL =
            "https://api.pinata.cloud/v3/files/public/";

    private static final long MAX_FILE_SIZE =
            20 * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of(

                    "pdf",

                    "doc",

                    "docx",

                    "ppt",

                    "pptx",

                    "txt",

                    "zip",

                    "rar",

                    "java",

                    "js",

                    "ts",

                    "tsx",

                    "jsx",

                    "html",

                    "css",

                    "sql",

                    "json",

                    "xml",

                    "png",

                    "xls",

                    "xlsx",

                    "jpg",

                    "jpeg",

                    "gif"

            );

    private final RestTemplate restTemplate;

    private final PinataConfig pinataConfig;


    @Override
    public PinataUploadResponse uploadFile(
            MultipartFile file
    ) throws IOException {

        validateFile(file);

        ByteArrayResource resource =
                new ByteArrayResource(file.getBytes()) {

                    @Override
                    public String getFilename() {
                        return file.getOriginalFilename();
                    }

                };

        MultiValueMap<String, Object> body =
                new LinkedMultiValueMap<>();

        body.add("file", resource);
        body.add("network", "public");

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                pinataConfig.getJwt()
        );

        headers.setContentType(
                MediaType.MULTIPART_FORM_DATA
        );

        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<PinataResponse> response =
                restTemplate.exchange(
                        PINATA_UPLOAD_URL,
                        HttpMethod.POST,
                        request,
                        PinataResponse.class
                );

        PinataResponse.PinataFile uploaded =
                response.getBody().getData();

        return PinataUploadResponse.builder()
                .id(uploaded.getId())
                .cid(uploaded.getCid())
                .fileName(uploaded.getName())
                .fileUrl(
                        "https://gateway.pinata.cloud/ipfs/"
                                + uploaded.getCid()
                )
                .fileSize(uploaded.getSize())
                .build();

    }


    @Override
    public void deleteFile(
            String pinataFileId
    ) throws IOException {

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(
                pinataConfig.getJwt()
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        restTemplate.exchange(

                PINATA_DELETE_URL + pinataFileId,

                HttpMethod.DELETE,

                request,

                Void.class

        );

    }


    private void validateFile(
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new BadRequestException(
                    "Please select a file to upload."
            );

        }

        if (file.getSize() > MAX_FILE_SIZE) {

            throw new BadRequestException(
                    "Maximum allowed file size is 20 MB."
            );

        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null || !fileName.contains(".")) {

            throw new BadRequestException(
                    "Invalid file."
            );

        }

        String extension =

                fileName.substring(
                        fileName.lastIndexOf('.') + 1
                ).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {

            throw new BadRequestException(

                    "File type ." + extension +

                            " is not supported."

            );

        }

    }


}