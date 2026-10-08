package com.backend.dto.admin;

/**
 * PATCH /admin/users/{id}/block body. Accepted for spec-shape compatibility but
 * deliberately NOT persisted: no reader exists for it anywhere in the spec, and
 * write-only columns are speculation, not audit. If an audit UI ever needs it,
 * the column gets added together with its reader.
 */
public class BlockUserRequest {

    private String reason;

    public BlockUserRequest() {
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
