package com.barterlink.resources.ipfs.controller;

import com.barterlink.common.response.ApiResponse;
import com.barterlink.resources.ipfs.dto.PinataUploadResponse;
import com.barterlink.resources.ipfs.service.PinataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ipfs")
@RequiredArgsConstructor
public class PinataTestController {

    private final PinataService pinataService;

    @PostMapping("/upload")
    public ApiResponse<PinataUploadResponse> upload(
            @RequestParam("file")
            MultipartFile file
    ) throws IOException {

        return ApiResponse.success(

                pinataService.uploadFile(file)

        );

    }

}