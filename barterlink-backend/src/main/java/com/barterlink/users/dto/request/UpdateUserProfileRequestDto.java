package com.barterlink.users.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserProfileRequestDto {

    @NotBlank(message = "Name is required")
    @Size(max = 500, message = "Name cannot exceed 500 characters")
    private String name;

    @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
    private String bio;
}
