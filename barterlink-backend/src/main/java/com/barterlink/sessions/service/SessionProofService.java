
package com.barterlink.sessions.service;

import com.barterlink.sessions.dto.request.UploadProofRequestDto;
import com.barterlink.sessions.dto.response.SessionProofResponseDto;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.sessions.dto.response.SessionProofFileDownloadDto;

import java.util.List;
import java.util.UUID;

public interface SessionProofService {

    SessionProofResponseDto uploadProof(
            UUID sessionId,
            UUID userId,
            MultipartFile file,
            UploadProofRequestDto request
    );

    List<SessionProofResponseDto> getSessionProofs(
            UUID sessionId,
            UUID userId
    );

    SessionProofFileDownloadDto getProofContent(
            UUID proofId,
            UUID userId
    );

    void deleteProof(
            UUID proofId,
            UUID userId
    );
}
