
package com.barterlink.sessions.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.sessions.dto.request.UploadProofRequestDto;
import com.barterlink.sessions.dto.response.SessionProofResponseDto;
import com.barterlink.sessions.service.SessionProofService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.sessions.dto.response.SessionProofFileDownloadDto;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import com.barterlink.auth.services.UserActivityService;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/session-proofs")
@RequiredArgsConstructor
public class SessionProofController {

    private final SessionProofService sessionProofService;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping(
            value = "/{sessionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<SessionProofResponseDto> uploadProof(
            @PathVariable UUID sessionId,
            @RequestPart("request")
            @Valid UploadProofRequestDto request,
            @RequestPart(value = "file", required = false)
            MultipartFile file
    ) {
        SessionProofResponseDto result = sessionProofService.uploadProof(
                sessionId,
                currentUserUtil.getCurrentUserId(),
                file,
                request
        );

        return ApiResponse.success(
                "Proof submitted successfully",
                result
        );
    }



    @GetMapping("/{proofId}/content")
    public ResponseEntity<byte[]> downloadProofContent(
            @PathVariable UUID proofId) {

        SessionProofFileDownloadDto download =
                sessionProofService.getProofContent(
                        proofId,
                        currentUserUtil.getCurrentUserId()
                );

        byte[] fileData = download.getFileData();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                download.getFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build()
        );

        headers.setContentType(
                MediaType.parseMediaType(download.getContentType())
        );

        headers.setContentLength(fileData.length);
        headers.setCacheControl("no-store");
        headers.set("X-Content-Type-Options", "nosniff");

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileData);
    }


    @GetMapping("/{sessionId}")
    public ApiResponse<List<SessionProofResponseDto>> getSessionProofs(
            @PathVariable UUID sessionId
    ) {
        return ApiResponse.success(
                "Session proofs retrieved successfully",
                sessionProofService.getSessionProofs(
                        sessionId,
                        currentUserUtil.getCurrentUserId()
                )
        );
    }

    @DeleteMapping("/{proofId}")
    public ApiResponse<Void> deleteProof(
            @PathVariable UUID proofId
    ) {
        sessionProofService.deleteProof(
                proofId,
                currentUserUtil.getCurrentUserId()
        );

        return ApiResponse.success("Proof deleted successfully");
    }
}
