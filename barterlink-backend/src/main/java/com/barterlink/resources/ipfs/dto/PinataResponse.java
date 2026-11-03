package com.barterlink.resources.ipfs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PinataResponse {

    private PinataFile data;

    @Data
    public static class PinataFile {

        private String id;

        private String cid;

        private String name;

        private Long size;

        @JsonProperty("mime_type")
        private String mimeType;

    }

}