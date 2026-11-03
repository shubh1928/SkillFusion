
package com.barterlink.sessions.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SessionProofFileDownloadDto {

    private final byte[] fileData;

    private final String fileName;

    private final String contentType;
}
