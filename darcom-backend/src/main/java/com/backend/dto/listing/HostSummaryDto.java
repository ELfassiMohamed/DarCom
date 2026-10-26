package com.backend.dto.listing;

import com.backend.domain.User;
import java.util.UUID;

public record HostSummaryDto(

    UUID id,
    String fullName
) {
    public static HostSummaryDto from(User user) {
        return new HostSummaryDto(user.getId(), user.getFullName());
    }
}