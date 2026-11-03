package com.barterlink.resources.ipfs.service;

import com.barterlink.resources.ipfs.dto.PinataUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PinataService {

    PinataUploadResponse uploadFile(
            MultipartFile file
    ) throws IOException;

    void deleteFile(
            String pinataFileId
    ) throws IOException;

}