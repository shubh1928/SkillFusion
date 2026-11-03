package com.barterlink.resources.ipfs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinataUploadResponse {

    private String id;

    private String cid;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

}