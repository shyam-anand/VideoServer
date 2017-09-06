package com.shyamanand.fileupload.web.models;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Shyam Anand (shyamwdr@gmail.com)
 *         04/09/17
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {
    private String title;
    private String details;
    private String code;
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
